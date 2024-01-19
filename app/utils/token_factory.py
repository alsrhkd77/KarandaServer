from datetime import timedelta, datetime, UTC
from typing import Annotated

import jwt
from fastapi import Depends
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError

from app.database.firestore_provider import firestore_provider
from app.schemas.token import TokenPayload
from app.utils.http_exceptions import token_credentials_exception

properties = firestore_provider.get_token_settings()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

SECRET_KEY = properties["SECRET_KEY"]
REFRESH_KEY = properties["REFRESH_KEY"]
ALGORITHM = properties["ALGORITHM"]
ACCESS_TOKEN_EXPIRE_MINUTES = properties["EXPIRE"]
REFRESH_TOKEN_EXPIRE_MINUTES = properties["REFRESH_EXPIRE"]

datetime_format = "%Y.%m.%d %H:%M:%S"


def create_access_token(user_uuid: str, expires_delta: timedelta = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)):
    expire = datetime.now(UTC) + expires_delta
    to_encode = {"expire": expire.strftime(datetime_format), "open_id": user_uuid}
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def create_refresh_token(user_uuid: str, expires_delta: timedelta = timedelta(minutes=REFRESH_TOKEN_EXPIRE_MINUTES)):
    expire = datetime.now(UTC) + expires_delta
    to_encode = {"expire": expire.strftime(datetime_format), "open_id": user_uuid}
    encoded_jwt = jwt.encode(to_encode, REFRESH_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def validate_access_token(token: Annotated[str, Depends(oauth2_scheme)]):
    return validate_token(token=token, secret=SECRET_KEY)


def validate_refresh_token(token: Annotated[str, Depends(oauth2_scheme)]):
    return validate_token(token=token, secret=REFRESH_KEY)


def validate_token(token: Annotated[str, Depends(oauth2_scheme)], secret):
    try:
        payload = jwt.decode(token, secret, algorithms=[ALGORITHM])
        user_uuid: str = payload.get("open_id")
        expire = datetime.strptime(payload.get("expire"), datetime_format)
        token_payload = TokenPayload(user_uuid=user_uuid, expire=expire)
    except JWTError:
        raise token_credentials_exception
    return token_payload
