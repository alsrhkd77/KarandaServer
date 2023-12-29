from pydantic import BaseModel


class BlacklistUserBase(BaseModel):
    target_discord_id: str


class BlacklistUserCreate(BlacklistUserBase):
    pass


class BlacklistUserUpdate(BlacklistUserBase):
    pass


class BlacklistUser(BlacklistUserBase):
    id: int
    owner_id: int
    block_code: str

    class Config:
        orm_mode = True
