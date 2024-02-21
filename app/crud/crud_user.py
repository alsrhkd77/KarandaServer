import uuid
from typing import Union, Dict, Any, Optional

from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.user import User
from app.schemas.user import DiscordUserCreate, UserUpdate


class CRUDUser(CRUDBase[User, DiscordUserCreate, UserUpdate]):
    def get_by_discord_id(self, db: Session, *, discord_id: str) -> Optional[User]:
        return db.query(User).filter(User.discord_id == discord_id).first()

    def get_by_user_uuid(self, db: Session, *, user_uuid: str) -> Optional[User]:
        return db.query(User).filter(User.user_uuid == user_uuid).one_or_none()

    def create(self, db: Session, *, obj_in: DiscordUserCreate) -> User:
        db_obj = User(
            discord_id=obj_in.discord_id,
            user_uuid=str(uuid.uuid1())
        )
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def update(self, db: Session, *, db_obj: User, obj_in: Union[UserUpdate, Dict[str, Any]]) -> User:
        if isinstance(obj_in, dict):
            update_data = obj_in
        else:
            update_data = obj_in.dict(exclude_unset=True)
        for key, value in update_data.items():
            setattr(db_obj, key, value)
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        # return super().update(db, db_obj=db_obj, obj_in=update_data)
        return db_obj


crud_user = CRUDUser(User)
