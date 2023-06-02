from typing import Optional, List, Union, Dict, Any

from fastapi.encoders import jsonable_encoder
from sqlalchemy import String, and_
from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models import ChecklistItem, User
from app.schemas.checklist_item import ChecklistItemCreate, ChecklistItemUpdate


class CRUDChecklistItem(CRUDBase[ChecklistItem, ChecklistItemCreate, ChecklistItemUpdate]):

    def get_all_by_user_uuid(self, db: Session, *, user_uuid: int):
        return db.query(ChecklistItem).join(User).filter_by(user_uuid=user_uuid).all()

    def get_by_title_and_owner_id(self, db: Session, *, title: String, owner_id: int) -> Optional[ChecklistItem]:
        return db.query(ChecklistItem).filter_by(title=title, owner_id=owner_id).one_or_none()

    def get_by_id_and_owner_id(self, db:Session, *, id: int, owner_id: int) -> Optional[ChecklistItem]:
        return db.query(ChecklistItem).filter_by(id=id, owner_id=owner_id).first()

    def create(self, db: Session, *, item: ChecklistItemCreate, owner_id: int) -> Optional[ChecklistItem]:
        if self.get_by_title_and_owner_id(db=db, title=item.title, owner_id=owner_id) is not None:
            return None
        db_item = ChecklistItem(**item.dict(), owner_id=owner_id)
        db.add(db_item)
        db.commit()
        db.refresh(db_item)
        return db_item

    def update(self, db: Session, *, db_obj: ChecklistItem,
               obj_in: Union[ChecklistItemUpdate, Dict[str, Any]]) -> Optional[ChecklistItem]:
        obj_data = jsonable_encoder(db_obj)
        if isinstance(obj_in, dict):
            update_data = obj_in
        else:
            update_data = obj_in.dict(exclude_unset=True)
        for field in obj_data:
            if field in update_data:
                setattr(db_obj, field, update_data[field])
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def delete(self, db: Session, *, item_id: int, owner_id: int):
        db_item = db.query(ChecklistItem).filter_by(id=item_id, owner_id=owner_id).first()
        if db_item is None:
            return False
        db.delete(db_item)
        db.commit()
        return True


crud_checklist_cycle_item = CRUDChecklistItem(ChecklistItem)
