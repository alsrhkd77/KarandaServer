from datetime import datetime
from typing import Generator, Optional, Annotated
from fastapi import Request, HTTPException, Cookie, WebSocketException
from fastapi.params import Query
from starlette import status
from starlette.websockets import WebSocket

from app.database.session import SessionLocal
from app.utils.token_factory import validate_access_token
from app.utils.http_exceptions import token_expired_exception


def get_db() -> Generator:
    try:
        db = SessionLocal()
        yield db
    finally:
        db.close()


def get_uuid_from_token(request: Request) -> Optional[str]:
    if request.cookies.keys():
        print(request.cookies.keys())
    if 'authorization' in request.headers.keys():
        token = request.headers.get('authorization')
        token = token.replace('Bearer ', '')
    elif 'Authorization' in request.headers.keys():
        token = request.headers.get('Authorization')
        token = token.replace('Bearer ', '')
    else:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=f"X-Token header invalid")
    payload = validate_access_token(token=token)
    if payload.expire < datetime.utcnow():
        raise token_expired_exception  # need refresh
    request.state.user_uuid = payload.user_uuid
    request.state.expire = payload.expire
    return payload.user_uuid


def get_token_from_websocket(
        websocket: WebSocket,
        session: Annotated[str | None, Cookie()] = None,
        token: Annotated[str | None, Query()] = None,
) -> Optional[str]:
    print(session)
    if token is None:
        raise WebSocketException(code=status.WS_1008_POLICY_VIOLATION)
    payload = validate_access_token(token=token)
    if payload.expire < datetime.utcnow():
        raise WebSocketException(code=status.WS_1008_POLICY_VIOLATION)
    return token


def get_host_url(request: Request):
    return str(request.url).split('?')[0]
