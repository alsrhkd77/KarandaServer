from sqlalchemy import Column, Integer, String

from app.database.base_class import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    uuid = Column(String(36), unique=True)
    discord_id = Column(String(18), unique=True)

