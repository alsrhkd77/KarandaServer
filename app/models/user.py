from sqlalchemy import Column, Integer, String

from app.database.base_class import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    discord_id = Column(String, unique=True)
    discord_username = Column(String)
    discord_discriminator = Column(String)
