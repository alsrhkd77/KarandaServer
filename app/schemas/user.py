from pydantic import BaseModel


class UserBase(BaseModel):
    discord_id: str


class UserCreate(UserBase):
    pass


class UserUpdate(UserBase):
    discord_id: str


class User(UserBase):
    id: int

    class Config:
        orm_mode = True
