from datetime import datetime

from pydantic import BaseModel

from app.schemas.trade_market_item import TradeMarketItem


class MarketDataBase(BaseModel):
    item_num: int
    enhancement_level: int


class MarketDataSearch(MarketDataBase):
    pass


class MarketDataResponse(MarketDataBase):
    price: int
    cumulative_volume: int
    current_stock: int
    date: datetime

    class Config:
        orm_mode = True


class MarketDataCreate(MarketDataBase):
    price: int
    cumulative_volume: int
    current_stock: int
    date: datetime


class MarketDataUpdate(MarketDataBase):
    id: int
    price: int
    cumulative_volume: int
    current_stock: int
    date: datetime

    def update(self, data: TradeMarketItem, date: datetime):
        self.price = data.price
        self.cumulative_volume = data.cumulative_volume
        self.current_stock = data.current_stock
        self.date = date

    class Config:
        orm_mode = True


class MarketData(MarketDataBase):
    id: int
    price: int
    cumulative_volume: int
    current_stock: int
    date: datetime

    class Config:
        orm_mode = True
