from datetime import datetime
from sqlalchemy import Column, String, Integer, Float, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from database import Base


class Market(Base):
    __tablename__ = "markets"

    id = Column(String, primary_key=True)
    question = Column(Text, nullable=False)
    volume = Column(Float, nullable=True)
    liquidity = Column(Float, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)


class Trade(Base):
    __tablename__ = "trades"

    id = Column(String, primary_key=True)
    market = Column(String, nullable=False)
    question = Column(Text, nullable=False)
    address = Column(String, nullable=False)
    side = Column(String, nullable=False)
    price = Column(Float, nullable=False)
    size = Column(Float, nullable=False)
    value = Column(Float, nullable=False)
    timestamp = Column(DateTime, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)


class Whale(Base):
    __tablename__ = "whales"

    id = Column(Integer, primary_key=True, autoincrement=True)
    trade_id = Column(String, ForeignKey("trades.id"), unique=True, nullable=False)
    address = Column(String, nullable=False)
    value = Column(Float, nullable=False)
    timestamp = Column(DateTime, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    trade = relationship("Trade")


class SmartWallet(Base):
    __tablename__ = "smart_wallets"

    id = Column(Integer, primary_key=True, autoincrement=True)
    address = Column(String, unique=True, nullable=False)
    profit = Column(Float, nullable=False)
    roi = Column(Float, nullable=False)
    win_rate = Column(Float, nullable=False)
    total_trades = Column(Integer, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow)
