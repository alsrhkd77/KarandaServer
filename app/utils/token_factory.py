from datetime import timedelta, datetime
from typing import Annotated

import jwt
from fastapi import Depends, HTTPException
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError
from starlette import status

from app.database.firestore_provider import firestore_provider
from app.schemas.token import TokenPayload

properties = firestore_provider.get_token_settings()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

SECRET_KEY = properties["SECRET_KEY"]
ALGORITHM = properties["ALGORITHM"]
ACCESS_TOKEN_EXPIRE_MINUTES = properties["EXPIRE"]
REFRESH_TOKEN_EXPIRE_MINUTES = properties["REFRESH_EXPIRE"]

datetime_format = "%Y.%m.%d %H:%M:%S"


def create_access_token(user_uuid: str, expires_delta: timedelta = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)):
    expire = datetime.utcnow() + expires_delta
    to_encode = {"expire": expire.strftime(datetime_format), "open_id": user_uuid}
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def validate_access_token(token: Annotated[str, Depends(oauth2_scheme)]):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_uuid: str = payload.get("open_id")
        expire = datetime.strptime(payload.get("expire"), datetime_format)
        token_payload = TokenPayload(user_uuid=user_uuid, expire=expire)
    except JWTError:
        raise credentials_exception
    return token_payload
