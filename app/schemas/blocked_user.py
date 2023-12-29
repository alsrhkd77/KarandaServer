from pydantic import BaseModel


class BlockedUser(BaseModel):
    discord_id: str
    user_name: str
    class Config:
        orm_mode = True
