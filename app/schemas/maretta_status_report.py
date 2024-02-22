from datetime import datetime

from pydantic import BaseModel


class MarettaStatusReportBase(BaseModel):
    report_at: datetime
    channel: str
    channel_num: int
    alive: bool


class MarettaStatusReportCreate(MarettaStatusReportBase):
    pass


class MarettaStatusReportUpdate(MarettaStatusReportBase):
    pass


class MarettaStatusReportResponse(MarettaStatusReportBase):
    id: int
    reporter_discord_id: str
    reporter_name: str


class MarettaStatusReport(MarettaStatusReportBase):
    id: int
    reporter_id: int
    reporter_discord_id: str
    reporter_name: str

    class Config:
        orm_mode = True
