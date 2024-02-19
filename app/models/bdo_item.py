from sqlalchemy import Column, Integer, String, Boolean

from app.database.base_class import Base


class BdoItem(Base):
    __tablename__ = 'bdo_item'

    id = Column(Integer, primary_key=True, autoincrement=True)
    item_num = Column(Integer, unique=True)
    item_name_kr = Column(String(20), index=True)
    max_enhancement_level = Column(Integer)
    grade = Column(Integer)
    category_num = Column(String(8))
    category_name_kr = Column(String(20))
    tradeable = Column(Boolean, nullable=False)
