import json
from datetime import datetime, timedelta

from fastapi.encoders import jsonable_encoder

from app.trade_market_provider import trade_market_provider
from app.utils.web_socket_manager import WebSocketManager


class TradeMarketWaitListManager(WebSocketManager):
    def __init__(self):
        super().__init__()
        self.last_update: datetime = None
        self.wait_item_list = []

    async def check_wait_list(self):
        if self.last_update is None or self.last_update < datetime.now() - timedelta(seconds=90):
            self.wait_item_list = trade_market_provider.wait_list()
            self.last_update = datetime.now()

        if self.wait_item_list is not None or self.wait_item_list != []:
            await self.broadcast(json.dumps(jsonable_encoder(self.wait_item_list)))

    async def send_to_last(self):
        await self.send_message(json.dumps(jsonable_encoder(self.wait_item_list)), self.active_connections[-1])


trade_market_wait_list_manager = TradeMarketWaitListManager()
