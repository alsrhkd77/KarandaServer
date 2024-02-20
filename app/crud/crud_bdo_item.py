from typing import Optional, List

from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models import BdoItem


class CRUDBdoItem:
    def get_tradeable_item_by_item_num(self, db: Session, *, item_num: int) -> Optional[BdoItem]:
        return db.query(BdoItem).filter(BdoItem.tradeable == True).filter(BdoItem.item_num == item_num).one_or_none()

    def get_by_item_name(self, db: Session, *, item_name: str) -> Optional[BdoItem]:
        if not item_name:
            return None
        return db.query(BdoItem).filter(BdoItem.item_num == item_name).one_or_none()

    def get_all_tradeable_item_by_item_num(self, db: Session, item_num_list: list) -> Optional[List[BdoItem]]:
        if not item_num_list:
            return None
        return db.query(BdoItem).filter(BdoItem.tradeable == True).filter(BdoItem.item_num.in_(item_num_list)).all()


crud_bdo_item = CRUDBdoItem()
