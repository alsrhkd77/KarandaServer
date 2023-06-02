from sqlalchemy import Column, Integer, String, Boolean, ForeignKey
from sqlalchemy.orm import relationship

from app.database.base_class import Base


class ChecklistItem(Base):
    __tablename__ = "checklist_item"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(38))
    enabled = Column(Boolean)
    cycle = Column(String(7))
    owner_id = Column(Integer, ForeignKey("users.id"))

    owner = relationship("User", back_populates="checklist_items")
    finished_items = relationship("ChecklistFinishedItem", back_populates="checklist_item", lazy='joined',
                                  cascade="all, delete")
