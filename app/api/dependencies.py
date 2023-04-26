from typing import Generator
from fastapi import Request, HTTPException

from app.database.session import SessionLocal
from app.utils.token_factory import validate_access_token


def get_db() -> Generator:
    try:
        db = SessionLocal()
        yield db
    finally:
        db.close()


def get_discord_id_from_token(request: Request):
    if request.headers.keys().__contains__('authentication'):
        token = request.headers.get('authentication')
    elif request.cookies.keys().__contains__('authentication'):
        token = request.cookies.get('authentication')
        raise HTTPException(status_code=400, detail="Cookie has token!")
    else:
        raise HTTPException(status_code=400, detail="X-Token header invalid")
    return validate_access_token(token=token)


def get_host_url(request: Request):
    return str(request.url).split('?')[0]
