from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.database.base_class import Base
from app.api.auth import router as auth_router
from app.database.session import engine

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


@app.get("/")
async def root():
    return {"message": "Hello World"}
