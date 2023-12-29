import logging
from typing import Dict, Any

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware

from app.api.auth import router as auth_router
from app.api.checklist import router as checklist_router
from app.api.maretta import router as maretta_router
from app.api.blacklist import router as blacklist_router
from app.database.base_class import Base
from app.database.session import engine, SessionLocal

logger = logging.getLogger(__name__)

Base.metadata.create_all(bind=engine)
app = FastAPI()

origins = [
    "https://www.karanda.kr",
    "https://karanda.kr",
    "https://hwansangyeonhwa.github.io",
    "http://localhost:8082",
    "http://localhost:2345"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],  # OPTIONS는 꼭 포함해야함
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(checklist_router)
app.include_router(maretta_router)
app.include_router(blacklist_router)


@app.get("/")
async def root():
    return {"message": "Welcome to Karanda"}


@app.middleware("http")
async def db_session_middleware(request: Request, call_next):
    response = Response("Internal server error", status_code=500)
    try:
        request.state.db = SessionLocal()
        response = await call_next(request)
    finally:
        request.state.db.close()
    return response


@app.middleware("http")
async def logging_middleware(request: Request, call_next):
    response = Response("Internal server error", status_code=500)
    extra: Dict[str, Any] = {
        'httpMethod': request.method,
        'httpPath': request.url.path,
        'httpQueryString': request.query_params,
        'httpHeaders': request.headers,
        'host': request.client.host
    }
    logger.info("Request", extra=extra)
    response = await call_next(request)
    return response
