import datetime

from sqlalchemy.orm import Session

from app.crud.base import CRUDBase
from app.models.maretta_status_report import MarettaStatusReport
from app.schemas.maretta_status_report import MarettaStatusReportCreate, MarettaStatusReportUpdate


class CRUDMarettaStatusReport(CRUDBase[MarettaStatusReport, MarettaStatusReportCreate, MarettaStatusReportUpdate]):
    def create(self, db: Session, *, obj_in: MarettaStatusReportCreate, reporter_id: str, reporter_discord_id: str,
               reporter_name: str) -> MarettaStatusReport:
        db_item = MarettaStatusReport(**obj_in.dict(), reporter_id=reporter_id, reporter_discord_id=reporter_discord_id,
                                      reporter_name=reporter_name)
        db.add(db_item)
        db.commit()
        db.refresh(db_item)
        return db_item

    def get_all(self, db: Session):
        last_two_hour = datetime.datetime.now(tz=datetime.timezone(datetime.timedelta(hours=9))) - datetime.timedelta(hours=2)
        return db.query(MarettaStatusReport).filter(MarettaStatusReport.report_at > last_two_hour).all()


crud_maretta_status_report = CRUDMarettaStatusReport(MarettaStatusReport)
