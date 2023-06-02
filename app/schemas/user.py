from pydantic import BaseModel

from app.schemas.checklist_item import ChecklistItem


class UserBase(BaseModel):
    pass


class DiscordUserCreate(UserBase):
    discord_id: str


class UserUpdate(UserBase):
    discord_id: str


class User(UserBase):
    id: int
    user_uuid: str
    checklist_cycle_items: list[ChecklistItem] = []

    class Config:
        orm_mode = True
