from datetime import datetime, timedelta, timezone
from typing import List, Optional, Type

from fastapi.encoders import jsonable_encoder
from sqlalchemy import and_, func
from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models import MarketData
from app.schemas.market_data import MarketDataCreate, MarketDataUpdate, MarketDataSearch, MarketDataResponse


class CRUDMarketData(CRUDBase[MarketData, MarketDataCreate, MarketDataUpdate]):
    async def get_today_by_item_nums(self, db: Session, item_nums: List[int]) -> List[Type[MarketData]]:
        now = (datetime.now(timezone.utc) + timedelta(hours=9)).replace(tzinfo=None)  # KR
        return (db.query(MarketData)
                .filter(MarketData.item_num.in_(item_nums))
                .filter(func.DATE(MarketData.date) == now.date())
                .order_by(MarketData.date.desc())
                .all())

    async def get_all_by_item_num(self, db:Session, item_num:int) -> Optional[List[MarketData]]:
        return db.query(MarketData).filter(MarketData.item_num == item_num).order_by(MarketData.date.desc()).all()

    async def create_from_list(self, db: Session, data: list[MarketDataCreate]) -> Optional[List[MarketDataResponse]]:
        if not data:
            return
        obj = []
        for item in data:
            obj.append(MarketData(**item.dict()))
        db.bulk_save_objects(obj)
        db.commit()
        return obj

    async def update_from_list(self, db: Session, data: list[MarketDataUpdate]) -> Optional[List[MarketDataResponse]]:
        if not data:
            return
        data_list = []
        for item in data:
            item.date = item.date.replace(microsecond=0)
            data_list.append(item.dict())
        db.bulk_update_mappings(MarketData, data_list)
        db.flush()
        db.commit()
        return data


crud_market_data = CRUDMarketData(MarketData)
