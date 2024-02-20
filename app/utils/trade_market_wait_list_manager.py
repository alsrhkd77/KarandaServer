import json
from datetime import datetime, timedelta

from fastapi.encoders import jsonable_encoder
from starlette.websockets import WebSocket

from app.trade_market_provider import trade_market_provider
from app.utils.web_socket_manager import WebSocketManager


class TradeMarketWaitListManager(WebSocketManager):
    def __init__(self):
        super().__init__()
        self.last_update: datetime = datetime.now() - timedelta(seconds=120)
        self.wait_item_list: list = []

    async def accept(self, websocket: WebSocket) -> None:
        await websocket.accept()
        self.active_connections.append(websocket)
        if self.last_update <= datetime.now() - timedelta(seconds=90):
            await self.check_wait_list()
        else:
            await self.send_message(websocket=websocket, message=json.dumps(jsonable_encoder(self.wait_item_list)))

    async def check_wait_list(self) -> None:
        if self.last_update <= datetime.now() - timedelta(seconds=90):
            self.wait_item_list = trade_market_provider.wait_list()
            self.last_update = datetime.now()
            await self.broadcast(json.dumps(jsonable_encoder(self.wait_item_list)))


trade_market_wait_list_manager = TradeMarketWaitListManager()
