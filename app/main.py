import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from starlette import status
from starlette.websockets import WebSocket, WebSocketDisconnect

from app.api.auth import router as auth_router
from app.api.checklist import router as checklist_router
from app.api.maretta import router as maretta_router, watch_maretta_status_report
from app.api.blacklist import router as blacklist_router
from app.api.trade_market import router as trade_market_router
from app.database.base_class import Base
from app.database.firestore_provider import firestore_provider
from app.database.session import engine, SessionLocal
from app.utils.websocket_publisher import maretta_publisher

Base.metadata.create_all(bind=engine)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Before application starts
    firestore_provider.watch_maretta_status(watch_maretta_status_report)
    threading.Thread(target=maretta_publisher, daemon=True).start()
    yield
    # After application finished
    firestore_provider.unsubscribe()


app = FastAPI(lifespan=lifespan)

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


@app.websocket("/echo")
async def echo(websocket: WebSocket):
    await websocket.accept()
    while True:
        msg = await websocket.receive_text()
        await websocket.send_text(msg)


"""
응답 헤더에 서버 처리 소요 시간을 추가하는 미들웨어
'''python
@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = time.time() - start_time
    response.headers["X-Process-Time"] = str(process_time)
    return response
'''
"""


@app.middleware("http")
async def db_session_middleware(request: Request, call_next):
    response = Response("Internal server error", status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)
    try:
        request.state.db = SessionLocal()
        response = await call_next(request)
    finally:
        request.state.db.close()
    return response
