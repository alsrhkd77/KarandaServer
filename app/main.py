from fastapi import FastAPI, Request
from fastapi.responses import RedirectResponse
from fastapi.middleware.cors import CORSMiddleware
import app.discord_provider as discord_provider


app = FastAPI()

origins = [
    "http://localhost",
    "https://www.karanda.kr",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
)


@app.get("/")
async def root():
    return {"message": "Hello World"}


@app.get("/hello/{name}")
async def say_hello(name: str):
    return {"message": f"Hello {name}"}


@app.get("/auth/discord/authorize/")
async def discord_authorize(code: str, request: Request):
    if code == '':
        return 'false'
    host_url = str(request.url).split('?')[0]
    data = discord_provider.exchange_code(code=code, redirect_url=host_url)
    redirect_url = f"http://localhost:8082?access_token={data['access_token']}&refresh_token={data['refresh_token']}"
    return RedirectResponse(url=redirect_url)

@app.get("/auth/discord/user-data/{token}")
async def discord_user_data(token: str):
    if token == '':
        return 'false'
    return discord_provider.get_user_data(token)
