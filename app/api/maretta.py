from fastapi import APIRouter, Depends, Request, Response
from starlette import status

from app.api.dependencies import get_uuid_from_token, get_db
from app.crud.crud_maretta_status_report import crud_maretta_status_report
from app.crud.crud_user import crud_user
from app.schemas.maretta_status_report import MarettaStatusReportCreate

router = APIRouter(prefix='/maretta')


@router.get('/get/reports')
def get_reports(request: Request):
    db = request.state.db
    data = crud_maretta_status_report.get_all(db=db)
    if data is None:
        return Response(status_code=status.HTTP_200_OK)
    return data


@router.post('/create/report')
def report_status(item_data: MarettaStatusReportCreate, request: Request, user_uuid: str = Depends(get_uuid_from_token)):
    db = request.state.db
    user = crud_user.get_by_user_uuid(db=db, user_uuid=user_uuid)
    data = crud_maretta_status_report.create(db=db, obj_in=item_data, reporter_id=user.id,
                                             reporter_discord_id=user.discord_id, reporter_name=user.user_name)
    if data is None:
        return Response(status_code=status.HTTP_200_OK)
    return data
