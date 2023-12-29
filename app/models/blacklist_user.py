from sqlalchemy import Column, Integer, ForeignKey, String
from sqlalchemy.orm import relationship
from app.database.base_class import Base

'''
유저간 블랙리스트 테이블
 - 블락 당한 유저가 탈퇴 후 재가입 시 블락을 유지하기 위해 discord_id 사용 
'''


class BlacklistUser(Base):
    __tablename__ = "blacklist_user"

    id = Column(Integer, primary_key=True, autoincrement=True)
    owner_id = Column(Integer, ForeignKey("users.id"))
    target_discord_id = Column(String(18))
    blocking_code = Column(String(8))

    owner = relationship("User", back_populates="blacklist_users")
