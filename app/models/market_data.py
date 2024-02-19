from sqlalchemy import Column, Integer, DateTime, Index, BigInteger

from app.database.base_class import Base


class MarketData(Base):
    __tablename__ = 'market_data'
    id = Column(Integer, primary_key=True, autoincrement=True)
    item_num = Column(Integer)
    enhancement_level = Column(Integer, default=0)  # 강화 단계
    price = Column(BigInteger)  # 가격(Base price)
    cumulative_volume = Column(BigInteger, default=0)  # 누적 거래량
    current_stock = Column(BigInteger, default=0)  # 현재 등록 갯수
    date = Column(DateTime)  # 날짜 (시,분,초 무시)

    __table_args__ = (Index('item_num_enhancement_level_data_idx', 'item_num', 'enhancement_level', 'date'),)
