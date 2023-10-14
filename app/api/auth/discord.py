from datetime import datetime, timedelta

from fastapi import APIRouter, Request, Response, Depends, HTTPException
from fastapi.responses import RedirectResponse, JSONResponse
from requests import Session
from starlette import status

from app.config.settings import settings
from app.discord_provider import discord_provider
from app.api.dependencies import get_db, get_uuid_from_token, get_host_url
from app.crud.crud_user import crud_user
from app.schemas.user import DiscordUserCreate
from app.utils.token_factory import create_access_token, validate_access_token, create_refresh_token, validate_refresh_token
from app.utils.http_exceptions import token_expired_exception

router = APIRouter(
    prefix='/discord',
)


def authenticate(access_token: str, db: Session):
    user_data = discord_provider.get_user_data(access_token)
    user = crud_user.get_by_discord_id(db, discord_id=user_data['id'])
    if user is None:
        user = crud_user.create(db, obj_in=DiscordUserCreate(discord_id=user_data['id']))
    token = create_access_token(user_uuid=user.user_uuid)
    refresh_token = create_refresh_token(user_uuid=user.user_uuid)
    return token, refresh_token


@router.get('/authenticate/windows')
def authentication_windows(code: str, request: Request, host_url: str = Depends(get_host_url)):
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    token, refresh_token = authenticate(access_token=data['access_token'], db=request.state.db)
    url = f"http://localhost:8082"
    # redirect_url = f"{url}?token={token}&social-token={data['access_token']}&refresh-token={data['refresh_token']}"
    redirect_url = f"{url}?token={token}&&refresh-token={refresh_token}"
    return RedirectResponse(url=redirect_url)


@router.get('/authenticate/web')
def authentication_web(code: str, request: Request, host_url: str = Depends(get_host_url)):
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    token, refresh_token = authenticate(access_token=data['access_token'], db=request.state.db)
    url = f"{settings.web_front_url}/auth/authenticate/#/"
    # redirect_url = f"{url}?token={token}&social-token={data['access_token']}&refresh-token={data['refresh_token']}"
    redirect_url = f"{url}?token={token}&&refresh-token={refresh_token}"
    response = RedirectResponse(url=redirect_url)
    # response.set_cookie(key='karanda', value=token, domain="karanda.kr", httponly=True)
    return response


@router.get('/authorization')
def authorization(request: Request, user_uuid: str = Depends(get_uuid_from_token)):
    if request.state.expire < datetime.utcnow() - timedelta(days=1):
        raise token_expired_exception   # need refresh
    if user_uuid != '':
        user = crud_user.get_by_user_uuid(request.state.db, user_uuid=user_uuid)
        if user is not None:
            data = discord_provider.get_user_data_with_id(user.discord_id)
            if data['id'] == user.discord_id:
                response = JSONResponse(content={
                    'avatar': f"{data['id']}/{data['avatar']}.png",
                    'username': data['username'],
                })
                return response
    return Response(status_code=status.HTTP_400_BAD_REQUEST)


@router.post('/refresh')
def refresh_access_token(request: Request):
    token = validate_access_token(token=request.headers['authorization'])
    refresh_token = validate_refresh_token(token=request.headers['refresh-token'])

    if refresh_token.expire < datetime.utcnow():
        raise token_expired_exception
    user = crud_user.get_by_user_uuid(db=request.state.db, user_uuid=refresh_token.user_uuid)
    if token.user_uuid == refresh_token.user_uuid == user.user_uuid:
        data = discord_provider.get_user_data_with_id(user.discord_id)
        token = create_access_token(user_uuid=user.user_uuid)
        refresh_token = create_refresh_token(user_uuid=user.user_uuid)
        return JSONResponse(content={
            'token': token,
            'refresh-token': refresh_token,
            'avatar': f"{data['id']}/{data['avatar']}.png",
            'username': data['username'],
        })
    return Response(status_code=status.HTTP_401_UNAUTHORIZED)


@router.delete('/unregister')
def unregister(request: Request, user_uuid: str = Depends(get_uuid_from_token)):
    if user_uuid != '':
        user = crud_user.get_by_user_uuid(request.state.db, user_uuid=user_uuid)
        if user is not None:
            crud_user.remove(db=request.state.db, id=user.id)
            return Response(status_code=200)
    return Response(status_code=status.HTTP_401_UNAUTHORIZED)
