from pydantic import BaseModel

from app.schemas.checklist_finished_item import ChecklistFinishedItem


class ChecklistItemBase(BaseModel):
    title: str
    enabled: bool
    cycle: str


class ChecklistItemCreate(ChecklistItemBase):
    pass


class ChecklistItemUpdate(ChecklistItemBase):
    id: int
    title: str
    enabled: bool
    cycle: str


class ChecklistItem(ChecklistItemBase):
    id: int
    owner_id: int
    finished_items: list[ChecklistFinishedItem] = []

    class Config:
        orm_mode = True
