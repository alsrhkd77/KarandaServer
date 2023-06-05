from datetime import datetime
from typing import Generator, Optional
from fastapi import Request, HTTPException, responses
from starlette import status

from app.database.session import SessionLocal
from app.utils.token_factory import validate_access_token


def get_db() -> Generator:
    try:
        db = SessionLocal()
        yield db
    finally:
        db.close()


def get_uuid_from_token(request: Request) -> Optional[str]:
    if request.cookies.keys().__contains__('karanda'):
        token = request.cookies.get('karanda')
    elif request.headers.keys().__contains__('authorization'):
        token = request.headers.get('authorization')
    else:
        raise HTTPException(status_code=400, detail="X-Token header invalid")
    payload = validate_access_token(token=token)
    if payload.expire < datetime.utcnow():
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)   # need refresh
    request.state.user_uuid = payload.user_uuid
    request.state.expire = payload.expire
    return payload.user_uuid


def get_host_url(request: Request):
    return str(request.url).split('?')[0]
