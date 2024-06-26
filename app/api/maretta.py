import asyncio
import json
import queue

from fastapi import APIRouter, Depends, Request, Response, BackgroundTasks
from fastapi.encoders import jsonable_encoder
from sqlalchemy.orm import Session
from starlette import status
from starlette.websockets import WebSocket, WebSocketDisconnect

from app.api.dependencies import get_uuid_from_token, get_db
from app.crud.crud_maretta_status_report import crud_maretta_status_report
from app.crud.crud_user import crud_user
from app.database.firestore_provider import firestore_provider
from app.schemas.maretta_status_report import MarettaStatusReportCreate, MarettaStatusReportResponse
from app.utils.websocket_manager import maretta_websocket_manager

latest_report = queue.Queue()

router = APIRouter(prefix='/maretta')


def watch_maretta_status_report(doc_snapshot, changes, read_time):
    global latest_report
    for doc in doc_snapshot:
        data = MarettaStatusReportResponse(**doc.to_dict())
        data.report_at = data.report_at.replace(tzinfo=None)
        latest_report.put(data)


'''
async def broadcast_maretta_status_report():
    global reports
    while True:
        if reports:
            await maretta_websocket_manager.broadcast(json.dumps(jsonable_encoder(reports)))
            reports.clear()
        await asyncio.sleep(1)
'''


@router.websocket('/reports')
async def reports_websocket_endpoint(websocket: WebSocket,
                                     db: Session = Depends(get_db)):
    await maretta_websocket_manager.accept(websocket=websocket)
    data = crud_maretta_status_report.get_all(db=db)
    await websocket.send_text(json.dumps(jsonable_encoder(data)))
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        maretta_websocket_manager.disconnect(websocket=websocket)


@router.get('/get/reports')
async def get_reports(db: Session = Depends(get_db)):
    db = db
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
    firestore_provider.update_maretta_status(data=data)
    # await add_maretta_report(report=data, db=db)
    return Response(status_code=status.HTTP_200_OK)   # Swap in next version
    #return data
