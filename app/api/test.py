import json

from fastapi import APIRouter
from fastapi.encoders import jsonable_encoder
from starlette.requests import Request
from starlette.websockets import WebSocket, WebSocketDisconnect
from app.utils.web_socket_manager import trade_market_websocket_manager

router = APIRouter(prefix='/test')


@router.websocket('/echo')
async def echo(websocket: WebSocket):
    await trade_market_websocket_manager.accept(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            await websocket.send_text(json.dumps(jsonable_encoder({'msg': data})))
            await trade_market_websocket_manager.broadcast(json.dumps(jsonable_encoder({'msg': data})))
    except WebSocketDisconnect:
        print("disconnect")


@router.websocket('/echo-if')
async def echo_if(websocket: WebSocket):
    await trade_market_websocket_manager.accept(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            await websocket.send_text(json.dumps(jsonable_encoder({'msg': data})))
            if data == 'update':
                await trade_market_websocket_manager.broadcast(json.dumps(jsonable_encoder({'msg': data})))
    except WebSocketDisconnect:
        print("disconnect")


@router.websocket('/welcome')
async def welcome(websocket: WebSocket):
    await trade_market_websocket_manager.accept(websocket)
    await websocket.send_text(json.dumps(jsonable_encoder({'msg': 'welcome'})))
    try:
        while True:
            data = await websocket.receive_text()
            await websocket.send_text(json.dumps(jsonable_encoder({'msg': data})))
            await trade_market_websocket_manager.broadcast(json.dumps(jsonable_encoder({'msg': data})))
    except WebSocketDisconnect:
        print("disconnect")


@router.get('/get/{item}')
def get_test(request: Request, item: str):
    db = request.state.db
    return f'Hello, {item}!'
