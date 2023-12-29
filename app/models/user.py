from sqlalchemy import Column, Integer, String
from sqlalchemy.orm import relationship

from app.database.base_class import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_uuid = Column(String(36), unique=True)
    discord_id = Column(String(36), unique=True)
    user_name = Column(String(36))

    checklist_items = relationship("ChecklistItem", back_populates="owner", cascade="all, delete")
    blacklist_users = relationship("BlacklistUser", back_populates="owner", cascade="all, delete")
