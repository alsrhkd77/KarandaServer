from datetime import datetime

from pydantic import BaseModel


class MarketWaitItem(BaseModel):
    item_num: int
    enhancement_level: int
    price: int
    target_time: datetime


class TradeMarketItem(BaseModel):
    item_num: int
    enhancement_level: int
    price: int
    current_stock: int
    cumulative_volume: int
