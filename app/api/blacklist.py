from fastapi import APIRouter, Depends, Request, HTTPException
from starlette import status

from app.api.dependencies import get_uuid_from_token
from app.crud.crud_blacklist_user import crud_blacklist_user
from app.crud.crud_user import crud_user
from app.models import User
from app.schemas.blacklist_user import BlacklistUserCreate
from app.schemas.blocked_user import BlockedUser

router = APIRouter(prefix='/blacklist', dependencies=[Depends(get_uuid_from_token)])


@router.post('/create/maretta')
def create_maretta_blacklist(request: Request, data: BlacklistUserCreate):
    user_uuid = request.state.user_uuid
    db = request.state.db
    user = crud_user.get_by_user_uuid(db=db, user_uuid=user_uuid)
    if user.discord_id == data.target_discord_id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST)
    data = crud_blacklist_user.create(db=db, item=data, owner_id=user.id, blocking_code="001")
    blocked_user = crud_user.get_by_discord_id(db=db, discord_id=data.target_discord_id)
    return user_to_blocked_user(blocked_user)


@router.get('/get/maretta')
def get_maretta_blacklist(request: Request):
    db = request.state.db
    user_uuid = request.state.user_uuid
    data = crud_blacklist_user.get_all_by_user_uuid_and_blocking_code(db=db, user_uuid=user_uuid, blocking_code="001")
    result = []
    for item in data:
        blocked = user_to_blocked_user(item)
        result.append(blocked)
    return result


def user_to_blocked_user(data: User):
    return BlockedUser(discord_id=data.discord_id, user_name=data.user_name)
