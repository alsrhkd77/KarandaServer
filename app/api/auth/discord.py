from fastapi import APIRouter, Request, Response
from fastapi.responses import RedirectResponse, JSONResponse

from app import discord_provider

router = APIRouter(
    prefix='/discord'
)


@router.get('/authenticate/windows')
def authentication_windows(code: str, request: Request):
    if code == '':
        return False
    host_url = str(request.url).split('?')[0]
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    redirect_url = f"http://localhost:8082?access_token={data['access_token']}&refresh_token={data['refresh_token']}"
    return RedirectResponse(url=redirect_url)


@router.get('/authenticate/web')
def authentication_web(code: str, request: Request):
    if code == '':
        return False
    host_url = str(request.url).split('?')[0]
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    redirect_url = "https://discord.com"
    response = RedirectResponse(url=redirect_url)
    response.set_cookie(key="authentication", value=data['access_token'], httponly=True)
    return response


@router.post('/authorization')
def authorization(request: Request):
    if request.headers.keys().__contains__('authentication'):
        token = request.headers.get('authentication')
    elif request.cookies.keys().__contains__('authentication'):
        token = request.cookies.get('authentication')
    else:
        return False

    # TODO: 디스코드에 등록된 사용자인지 확인, karanda에 등록된 사용자인이 확인(미등록인 경우 등록+)
    data = discord_provider.get_user_data(token)
    response = JSONResponse(content={'avatar':data['avatar']})
    response.set_cookie(key="authentication", value=token, httponly=True)
    return response


@router.get('/user-profile')
def user_profile(request: Request):
    print(request.cookies.keys())
    if request.cookies.keys().__contains__('authentication'):
        token = request.cookies.get('authentication')
    else:
        return False
    data = discord_provider.get_user_data(token)
    return {'avatar':data['avatar']}
