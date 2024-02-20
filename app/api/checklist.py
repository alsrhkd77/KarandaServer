from datetime import datetime

from fastapi import APIRouter, Depends, Request, Response, Body
from sqlalchemy.orm import Session
from starlette import status

from app.crud.crud_checklist_item import crud_checklist_cycle_item
from app.crud.crud_checklist_finished_item import crud_checklist_finished_item
from app.crud.crud_user import crud_user
from app.schemas.checklist_item import ChecklistItemCreate, ChecklistItemUpdate
from app.api.dependencies import get_uuid_from_token, get_db
from app.schemas.checklist_finished_item import ChecklistFinishedItemCreate

router = APIRouter(prefix='/checklist', dependencies=[Depends(get_uuid_from_token)])


@router.get('/get/checklist-items')
def get_checklist_items(request: Request, db: Session = Depends(get_db)):
    user_uuid = request.state.user_uuid
    db = db
    data = crud_checklist_cycle_item.get_all_by_user_uuid(db=db, user_uuid=user_uuid)
    if data is None:
        return Response(status_code=status.HTTP_200_OK)
    return data


@router.get('/get/finished-items')
def get_finished_items(request: Request, db: Session = Depends(get_db)):
    user_uuid = request.state.user_uuid
    db = db
    data = crud_checklist_finished_item.get_all_by_user_uuid(db=db, user_uuid=user_uuid)
    if data is None:
        return Response(status_code=status.HTTP_200_OK)
    return data


@router.post('/create/checklist-item')
def create_checklist_item(item_data: ChecklistItemCreate, request: Request):
    user_uuid = request.state.user_uuid
    db = request.state.db
    owner = crud_user.get_by_user_uuid(db=db, user_uuid=user_uuid)
    item = crud_checklist_cycle_item.create(db=db, item=item_data, owner_id=owner.id)
    if item is None:
        return Response(status_code=status.HTTP_201_CREATED)
    return item


@router.post('/create/finished-item')
def create_finished_item(data: dict, request: Request):
    finish_at = datetime.strptime(data['finish_at'], '%Y.%m.%d %H:%M:%S')
    item = ChecklistFinishedItemCreate(finish_at=finish_at)
    checklist_item = data['checklist_item']
    user_uuid = request.state.user_uuid
    db = request.state.db
    checklist_item_data = crud_checklist_cycle_item.get_by_title_and_user_uuid(db=db, title=checklist_item,
                                                                               user_uuid=user_uuid)
    finished_item = crud_checklist_finished_item.create(db=db, item=item, checklist_item=checklist_item_data.id)
    return finished_item


@router.delete('/delete/finished-item')
def delete_finished_item(data: dict, request: Request):
    user_uuid = request.state.user_uuid
    db = request.state.db
    return crud_checklist_finished_item.delete(db=db, user_uuid=user_uuid, checklist_item=data['checklist_item'],
                                               finished_item=data['finished_item'])


@router.delete('/delete/checklist-item')
def delete_checklist_item(data: dict, request: Request):
    item_id = int(data['item_id'])
    user_uuid = request.state.user_uuid
    db = request.state.db
    owner = crud_user.get_by_user_uuid(db=db, user_uuid=user_uuid)
    return crud_checklist_cycle_item.delete(db=db, item_id=item_id, owner_id=owner.id)


@router.patch('/update/checklist-item')
def update_checklist_cycle(item: ChecklistItemUpdate, request: Request):
    user_uuid = request.state.user_uuid
    db = request.state.db
    owner = crud_user.get_by_user_uuid(db=db, user_uuid=user_uuid)
    obj = crud_checklist_cycle_item.get_by_id_and_owner_id(db=db, id=item.id, owner_id=owner.id)
    return crud_checklist_cycle_item.update(db=db, db_obj=obj, obj_in=item)
