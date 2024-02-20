import json
from datetime import datetime, timedelta

from fastapi import APIRouter
from fastapi.encoders import jsonable_encoder
from starlette.requests import Request
from starlette.websockets import WebSocket, WebSocketDisconnect

from app.crud.crud_market_data import crud_market_data
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
        trade_market_websocket_manager.disconnect(websocket)
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
        trade_market_websocket_manager.disconnect(websocket)
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
        trade_market_websocket_manager.disconnect(websocket)
        print("disconnect")


last_update: datetime = None
item_list = []


async def check():
    global last_update, item_list
    if last_update is None or last_update < datetime.now() - timedelta(minutes=1):
        item_list.append(len(item_list))
        last_update = datetime.now()
    if item_list is not None and len(item_list) > 0:
        await trade_market_websocket_manager.broadcast(json.dumps(jsonable_encoder({'msg': item_list})))


@router.websocket('/broadcast')
async def broadcast(websocket: WebSocket):
    await trade_market_websocket_manager.accept(websocket)
    await websocket.send_text(json.dumps(jsonable_encoder({'msg': 'welcome'})))
    try:
        while True:
            data = await websocket.receive_text()
            if data == 'update':
                await check()
                # await trade_market_websocket_manager.broadcast(json.dumps(jsonable_encoder({'msg': data})))
            else:
                await websocket.send_text('failed')
    except WebSocketDisconnect:
        trade_market_websocket_manager.disconnect(websocket)
        print("disconnect")


@router.get('/get/{item}')
def get_test(request: Request, item: str):
    db = request.state.db
    data = crud_market_data.get_today_by_item_nums(db=db, item_nums=[11853])
    return f'Hello, {item}, {data[0].item_num}!'
