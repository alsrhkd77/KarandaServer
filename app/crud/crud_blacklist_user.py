from typing import Optional, List

from sqlalchemy.orm import Session, aliased

from app.crud.base import CRUDBase
from app.models import User
from app.models.blacklist_user import BlacklistUser
from app.schemas.blacklist_user import BlacklistUserCreate, BlacklistUserUpdate
from app.schemas.blocked_user import BlockedUser


class CRUDBlacklistUser(CRUDBase[BlacklistUser, BlacklistUserCreate, BlacklistUserUpdate]):

    def get_by_target_discord_id_and_owner_id_and_blocking_code(self, db: Session, *, target_discord_id: str,
                                                                owner_id: int, blocking_code: str) -> Optional[
        BlacklistUser]:
        return db.query(BlacklistUser).filter_by(
            target_discord_id=target_discord_id,
            blocking_code=blocking_code,
            owner_id=owner_id).one_or_none()

    def get_all_by_user_uuid_and_blocking_code(self, db: Session, *, user_uuid: int, blocking_code: str) -> Optional[
        List[BlockedUser] | None]:
        blocked = aliased(User)
        return (db.query(blocked)
                .join(BlacklistUser, BlacklistUser.target_discord_id == blocked.discord_id)
                .filter(BlacklistUser.blocking_code == blocking_code)
                .join(User)
                .filter(User.user_uuid == user_uuid)
                .all())

    def create(self, db: Session, *, item: BlacklistUserCreate, owner_id: int, blocking_code: str) -> Optional[
        BlacklistUser]:
        if self.get_by_target_discord_id_and_user_uuid_and_blocking_code(db=db,
                                                                         target_discord_id=item.target_discord_id,
                                                                         owner_id=owner_id,
                                                                         blocking_code=blocking_code) is not None:
            return None
        db_item = BlacklistUser(**item.dict(), blocking_code=blocking_code, owner_id=owner_id)
        db.add(db_item)
        db.commit()
        db.refresh(db_item)
        return db_item


crud_blacklist_user = CRUDBlacklistUser(BlacklistUser)
