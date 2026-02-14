from pydantic import BaseModel
from typing import List, Optional

class Token(BaseModel):
    token_id: str
    outcome: str
    price: float
    winner: bool = False

class Market(BaseModel):
    condition_id: str
    question: str
    tokens: List[Token]
    volume: float = 0.0
    tags: List[str] = []
    market_slug: str = ""
