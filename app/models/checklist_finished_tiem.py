from sqlalchemy import Column, Integer, ForeignKey, DateTime
from sqlalchemy.orm import relationship
from app.database.base_class import Base


class ChecklistFinishedItem(Base):
    __tablename__ = "checklist_finished_item"

    id = Column(Integer, primary_key=True, autoincrement=True)
    finish_at = Column(DateTime)
    checklist_item_id = Column(Integer, ForeignKey("checklist_item.id"))

    checklist_item = relationship("ChecklistItem", back_populates="finished_items")
