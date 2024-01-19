from sqlalchemy import Column, Integer, ForeignKey, String, DateTime, Boolean

from app.database.base_class import Base


class MarettaStatusReport(Base):
    __tablename__ = "maretta_status_report"

    id = Column(Integer, primary_key=True, autoincrement=True)
    reporter_id = Column(Integer, ForeignKey("users.id"))
    reporter_discord_id = Column(String(36))
    reporter_name = Column(String(32))
    report_at = Column(DateTime)
    channel = Column(String(12))
    channel_num = Column(Integer)
    alive = Column(Boolean)
