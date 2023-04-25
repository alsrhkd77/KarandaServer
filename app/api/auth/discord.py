from fastapi import APIRouter, Request, Response, Depends
from fastapi.responses import RedirectResponse, JSONResponse
from requests import Session
from starlette import status

from app.discord_provider import discord_provider
from app.api.dependencies import get_db, get_uuid_from_token, get_host_url
from app.crud.crud_user import crud_user
from app.schemas.user import UserCreate
from app.utils.token_factory import create_access_token, validate_access_token

router = APIRouter(
    prefix='/discord',
)


def authenticate(access_token: str, db: Session):
    user_data = discord_provider.get_user_data(access_token)
    user = crud_user.get_by_discord_id(db, discord_id=user_data['id'])
    if user is None:
        user = crud_user.create(db, obj_in=UserCreate(discord_id=user_data['id']))
    token = create_access_token(uuid=user.uuid)
    return token


@router.get('/authenticate/windows')
def authentication_windows(code: str, host_url: str = Depends(get_host_url), db: Session = Depends(get_db)):
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    token = authenticate(access_token=data['access_token'], db=db)
    url = f"http://localhost:8082"
    redirect_url = f"{url}?token={token}&access_token={data['access_token']}&refresh_token={data['refresh_token']}"
    return RedirectResponse(url=redirect_url)


@router.get('/authenticate/web')
def authentication_web(code: str, host_url: str = Depends(get_host_url), db: Session = Depends(get_db)):
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    token = authenticate(access_token=data['access_token'], db=db)
    url = f"https://www.karanda.kr/auth/authenticate"
    # url = f"http://localhost:2345/#/auth/authenticate"
    redirect_url = f"{url}?token={token}&access_token={data['access_token']}&refresh_token={data['refresh_token']}"
    return RedirectResponse(url=redirect_url)


@router.get('/authorization')
def authorization(access_token: str, uuid: str = Depends(get_uuid_from_token), db: Session = Depends(get_db)):
    if uuid == '':
        return Response(status_code=status.HTTP_401_UNAUTHORIZED)
    data = discord_provider.get_user_data(access_token)
    user = crud_user.get_by_uuid(db, user_uuid=uuid)
    if data['id'] != user.discord_id:
        return Response(status_code=status.HTTP_401_UNAUTHORIZED)
    response = JSONResponse(content={
        'avatar': data['avatar'],
        'username': data['username'],
    })
    return response
