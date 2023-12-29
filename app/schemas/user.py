from pydantic import BaseModel

from app.schemas.blacklist_user import BlacklistUser
from app.schemas.checklist_item import ChecklistItem


class UserBase(BaseModel):
    pass


class DiscordUserCreate(UserBase):
    discord_id: str


class UserUpdate(UserBase):
    id: int
    user_name: str


class User(UserBase):
    id: int
    user_uuid: str
    checklist_cycle_items: list[ChecklistItem] = []
    blacklist: list[BlacklistUser] = []
    user_name: str

    class Config:
        orm_mode = True
