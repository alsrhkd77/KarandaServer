from fastapi import HTTPException, status

'''
Http status docs 참고 
https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
'''

token_credentials_exception = HTTPException(
    status_code=status.HTTP_401_UNAUTHORIZED,
    detail="Could not validate credentials",
    headers={"WWW-Authenticate": "Bearer"},
)

token_expired_exception = HTTPException(
    status_code=status.HTTP_401_UNAUTHORIZED,
    detail="Token expiration has expired",
    headers={"WWW-Authenticate": "Bearer"},
)
