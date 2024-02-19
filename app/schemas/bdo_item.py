from pydantic import BaseModel


class BdoItemBase(BaseModel):
    item_num: int
    item_name_kr: str
    max_enhancement_level: int
    grade: int
    category_num: str
    category_name_kr: str
    tradeable: bool


class BdoItem(BdoItemBase):
    id: int

    class Config:
        orm_mode = True
