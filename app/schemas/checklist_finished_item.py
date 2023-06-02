from datetime import datetime

from pydantic import BaseModel


class ChecklistFinishedItemBase(BaseModel):
    finish_at: datetime


class ChecklistFinishedItemCreate(ChecklistFinishedItemBase):
    pass


class ChecklistFinishedItemUpdate(ChecklistFinishedItemBase):
    pass


class ChecklistFinishedItem(ChecklistFinishedItemBase):
    id: int
    checklist_item_id: int

    class Confing:
        orm_mode = True
