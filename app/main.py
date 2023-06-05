from fastapi import FastAPI, Request, Response, Depends
from fastapi.middleware.cors import CORSMiddleware

from app.api.auth import router as auth_router
from app.api.checklist import router as checklist_router
from app.database.base_class import Base
from app.database.session import engine, SessionLocal

from app.api.dependencies import get_uuid_from_token

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


@app.get("/")
async def root():
    return {"message": "Hello World"}


@app.get("/get-cookie")
def get_cookie():
    response = Response(status_code=200)
    response.set_cookie(key="karanda", value="asdf")
    return response


@app.get("test-cookies")
def test_cookies(request: Request, item = Depends(get_uuid_from_token)):
    return request.cookies.keys()


@app.middleware("http")
async def db_session_middleware(request: Request, call_next):
    response = Response("Internal server error", status_code=500)
    try:
        request.state.db = SessionLocal()
        response = await call_next(request)
    finally:
        request.state.db.close()
    return response
