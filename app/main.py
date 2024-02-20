import logging
import time

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from starlette import status

from app.api.auth import router as auth_router
from app.api.checklist import router as checklist_router
from app.api.maretta import router as maretta_router
from app.api.blacklist import router as blacklist_router
from app.api.trade_market import router as trade_market_router
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
app.include_router(trade_market_router)


@app.get("/")
async def root():
    return "Welcome to Karanda"


@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = time.time() - start_time
    response.headers["X-Process-Time"] = str(process_time)
    return response


@app.middleware("http")
async def db_session_middleware(request: Request, call_next):
    response = Response("Internal server error", status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)
    try:
        request.state.db = SessionLocal()
        response = await call_next(request)
    finally:
        request.state.db.close()
    return response
