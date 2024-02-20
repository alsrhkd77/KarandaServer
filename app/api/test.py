from fastapi import APIRouter
from starlette.requests import Request
from starlette.websockets import WebSocket, WebSocketDisconnect

router = APIRouter(prefix='/test')


@router.websocket('/echo')
async def echo(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            data = await websocket.receive_text()
            await websocket.send_text(data)
    except WebSocketDisconnect:
        print("disconnect")


@router.websocket('/echo-if')
async def echo_if(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            data = await websocket.receive_text()
            if data == 'update':
                await websocket.send_text(data)
    except WebSocketDisconnect:
        print("disconnect")


@router.websocket('/welcome')
async def welcome(websocket: WebSocket):
    await websocket.accept()
    await websocket.send_text('welcome')
    try:
        while True:
            data = await websocket.receive_text()
            await websocket.send_text(data)
    except WebSocketDisconnect:
        print("disconnect")

@router.get('/get/{item}')
def get_test(request: Request, item: str):
    db = request.state.db
    return f'Hello, {item}!'
