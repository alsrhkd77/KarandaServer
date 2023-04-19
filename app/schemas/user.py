from pydantic import BaseModel


class UserBase(BaseModel):
    discord_id: str


class UserCreate(UserBase):
    discord_username: str
    discord_discriminator: str


class UserUpdate(UserBase):
    discord_username: str
    discord_discriminator: str


class User(UserBase):
    id: int


    class Config:
        orm_mode = True
