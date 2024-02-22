import json
from datetime import datetime, timedelta

from fastapi import APIRouter, Depends, Request, Response
from fastapi.encoders import jsonable_encoder
from sqlalchemy.orm import Session
from starlette import status
from starlette.websockets import WebSocket, WebSocketDisconnect

from app.api.dependencies import get_uuid_from_token, get_db
from app.crud.crud_maretta_status_report import crud_maretta_status_report
from app.crud.crud_user import crud_user
from app.schemas.maretta_status_report import MarettaStatusReportCreate, MarettaStatusReportResponse
from app.utils.web_socket_manager import maretta_websocket_manager

last_update: datetime = None
reports: list[MarettaStatusReportResponse] = []

router = APIRouter(prefix='/maretta')


async def sync_maretta_report(db: Session):
    global reports, last_update
    if last_update is None or last_update < datetime.now() - timedelta(minutes=30):
        reports = crud_maretta_status_report.get_all(db=db)
        await maretta_websocket_manager.broadcast(json.dumps(jsonable_encoder(reports)))
        last_update = datetime.now()
        return True
    return False


async def add_maretta_report(report: MarettaStatusReportResponse, db: Session):
    global reports
    reports.append(report)
    if maretta_websocket_manager.active_connections and not await sync_maretta_report(db=db):
        await maretta_websocket_manager.broadcast(json.dumps(jsonable_encoder([report])))


@router.websocket('/reports')
async def reports_websocket_endpoint(websocket: WebSocket, db: Session = Depends(get_db)):
    await maretta_websocket_manager.accept(websocket=websocket)
    if not await sync_maretta_report(db=db):
        await websocket.send_text(json.dumps(jsonable_encoder(reports)))
    try:
        while True:
            data = await websocket.receive_text()
            if data == 'update':
                await sync_maretta_report(db=db)
    except WebSocketDisconnect:
        maretta_websocket_manager.disconnect(websocket=websocket)


@router.get('/get/reports')
def get_reports(request: Request):
    db = request.state.db
    data = crud_maretta_status_report.get_all(db=db)
    if data is None:
        return Response(status_code=status.HTTP_200_OK)
    return data


@router.post('/create/report')
async def report_status(item_data: MarettaStatusReportCreate, request: Request,
                        user_uuid: str = Depends(get_uuid_from_token)):
    db = request.state.db
    user = crud_user.get_by_user_uuid(db=db, user_uuid=user_uuid)
    data = crud_maretta_status_report.create(db=db, obj_in=item_data, reporter_id=user.id,
                                             reporter_discord_id=user.discord_id, reporter_name=user.user_name)
    if data is None:
        return Response(status_code=status.HTTP_503_SERVICE_UNAVAILABLE)
    await add_maretta_report(report=data, db=db)
    # return Response(status_code=status.HTTP_200_OK)   # Swap in next version
    return data
