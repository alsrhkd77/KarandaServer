from typing import Optional

from sqlalchemy.orm import Session

from app.models import ChecklistItem, User, ChecklistFinishedItem
from app.crud.base import CRUDBase
from app.schemas.checklist_finished_item import ChecklistFinishedItemCreate, ChecklistFinishedItemUpdate


class CRUDChecklistFinishedItem(
    CRUDBase[ChecklistFinishedItem, ChecklistFinishedItemCreate, ChecklistFinishedItemUpdate]):
    def create(self, db: Session, *, item: ChecklistFinishedItemCreate, checklist_item: int) -> Optional[
        ChecklistFinishedItem]:
        db_item = ChecklistFinishedItem(**item.dict(), checklist_item_id=checklist_item)
        db.add(db_item)
        db.commit()
        db.refresh(db_item)
        return db_item

    def get_all_by_user_uuid(self, db: Session, *, user_uuid: int):
        return db.query(ChecklistFinishedItem).join(ChecklistItem).join(User).filter_by(
            user_uuid=user_uuid).all()

    def delete(self, db: Session, *, user_uuid: int, checklist_item: int, finished_item: int) -> object:
        item = db.query(ChecklistFinishedItem).filter_by(id=finished_item).join(ChecklistItem).filter_by(
            id=checklist_item).join(User).filter_by(user_uuid=user_uuid).first()
        if item is None:
            return False
        db.delete(item)
        db.commit()
        return True


crud_checklist_finished_item = CRUDChecklistFinishedItem(ChecklistFinishedItem)
