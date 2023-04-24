from fastapi import APIRouter, Request, Response, Depends
from fastapi.responses import RedirectResponse, JSONResponse
from requests import Session
from starlette import status

from app.discord_provider import discord_provider
from app.api.dependencies import get_db
from app.crud.crud_user import crud_user
from app.schemas.user import UserCreate
from app.utils.token_factory import create_access_token, validate_access_token

router = APIRouter(
    prefix='/discord',
)


@router.get('/authenticate')
def authenticate(platform: str, access_token: str, refresh_token: str, db: Session = Depends(get_db)):
    if platform == 'web':
        platform_url = f"https://www.karanda.kr/auth/authenticate"
        # platform_url = f"http://localhost:2345/#/auth/authenticate"
    if platform == "windows":
        platform_url = f"http://localhost:8082"
    user_data = discord_provider.get_user_data(access_token)
    user = crud_user.get_by_discord_id(db, discord_id=user_data['id'])
    if user is None:
        user = crud_user.create(db, obj_in=UserCreate(discord_id=user_data['id']))
    token = create_access_token(uuid=user.uuid)
    return RedirectResponse(
        url=f"{platform_url}?token={token}&access_token={access_token}&refresh_token={refresh_token}")


@router.get('/authenticate/windows')
def authentication_windows(code: str, request: Request):
    host_url = str(request.url).split('?')[0]
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    redirect_url = f"/auth/discord/authenticate?platform=windows&access_token={data['access_token']}&refresh_token={data['refresh_token']}"
    return RedirectResponse(url=redirect_url)


@router.get('/authenticate/web')
def authentication_web(code: str, request: Request):
    host_url = str(request.url).split('?')[0]
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    redirect_url = f"/auth/discord/authenticate?platform=web&access_token={data['access_token']}&refresh_token={data['refresh_token']}"
    return RedirectResponse(url=redirect_url)


@router.get('/authorization')
def authorization(access_token: str, request: Request, db: Session = Depends(get_db)):
    if request.headers.keys().__contains__('authentication'):
        token = request.headers.get('authentication')
    elif request.cookies.keys().__contains__('authentication'):
        token = request.cookies.get('authentication')
    else:
        return Response(status_code=status.HTTP_401_UNAUTHORIZED)
    uuid = validate_access_token(token=token)
    data = discord_provider.get_user_data(access_token)
    user = crud_user.get_by_uuid(db, user_uuid=uuid)
    if data['id'] != user.discord_id:
        return Response(status_code=status.HTTP_401_UNAUTHORIZED)
    response = JSONResponse(content={
        'avatar': data['avatar'],
        'username': data['username'],
    })
    return response
