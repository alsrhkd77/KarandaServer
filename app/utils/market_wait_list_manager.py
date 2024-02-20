from datetime import datetime

from app.trade_market_provider import trade_market_provider
from app.utils.web_socket_manager import WebSocketManager


class MarketWaitListManager(WebSocketManager):
    def __init__(self):
        self.last_update: datetime = None
        self.wait_item_list: list = []

    def update_wait_list(self):
        self.wait_item_list = trade_market_provider.wait_list()
        self.last_update = datetime.now()