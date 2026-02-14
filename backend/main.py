import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from apscheduler.schedulers.background import BackgroundScheduler
from typing import List

from app.database import init_db, get_recent_alerts, create_user, get_user_by_email
from app.services.auth_service import AuthService
from app.services.market_service import MarketService
from app.services.whale_service import WhaleService
from app.schemas import UserRegister, UserLogin, Token, UserResponse

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Services
auth_service = AuthService()
market_service = MarketService()
whale_service = WhaleService()

# Scheduler Jobs
def update_whale_data():
    logger.info("Scheduler: Fetching whale activity...")
    try:
        whale_service.fetch_whale_activity()
        logger.info("Scheduler: Whale activity updated.")
    except Exception as e:
        logger.error(f"Scheduler Error (Whale): {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting up PolyPulse Backend...")
    init_db()
    
    scheduler = BackgroundScheduler()
    # Fetch whale data every 2 minutes
    scheduler.add_job(update_whale_data, 'interval', minutes=2)
    
    # Analyze smart money (closed markets) every 6 hours
    scheduler.add_job(whale_service.analyze_smart_money, 'interval', hours=6)
    
    scheduler.start()
    
    # Run one immediate update
    update_whale_data()
    # Run smart money analysis in background (don't block startup)
    scheduler.add_job(whale_service.analyze_smart_money) 
    
    yield
    
    # Shutdown
    logger.info("Shutting down...")
    scheduler.shutdown()

app = FastAPI(title="PolyPulse API", lifespan=lifespan)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Auth
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

async def get_current_user(token: str = Depends(oauth2_scheme)):
    payload = auth_service.decode_token(token)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    user = get_user_by_email(payload.get("sub"))
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user

# --- Endpoints ---

@app.post("/register", response_model=UserResponse)
def register(user: UserRegister):
    existing = get_user_by_email(user.email)
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    hashed_pw = auth_service.get_password_hash(user.password)
    user_id = create_user(user.email, hashed_pw)
    
    if not user_id:
        raise HTTPException(status_code=500, detail="Failed to create user")
        
    return {"id": user_id, "email": user.email, "created_at": "now"}

@app.post("/token", response_model=Token)
def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends()):
    user = get_user_by_email(form_data.username)
    if not user or not auth_service.verify_password(form_data.password, user['password_hash']):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = auth_service.create_access_token(data={"sub": user['email']})
    return {"access_token": access_token, "token_type": "bearer"}

@app.get("/users/me", response_model=UserResponse)
def read_users_me(current_user: dict = Depends(get_current_user)):
    return {
        "id": current_user["id"],
        "email": current_user["email"],
        "created_at": current_user["created_at"]
    }

@app.get("/dashboard/stats")
def get_dashboard_stats(current_user: dict = Depends(get_current_user)):
    # Placeholder for dashboard stats
    return {
        "alerts_24h": 5,
        "watchlist_count": 12,
        "top_movers": []
    }

@app.get("/dashboard/alerts")
def get_alerts(current_user: dict = Depends(get_current_user)):
    return get_recent_alerts()

@app.get("/dashboard/whales")
def get_whale_activity(current_user: dict = Depends(get_current_user)):
    """Get recent whale activity (large trades)"""
    return whale_service.fetch_whale_activity()

@app.get("/dashboard/leaderboard")
def get_leaderboard(current_user: dict = Depends(get_current_user)):
    """Get top whale traders by volume"""
    return whale_service.get_leaderboard()

@app.get("/")
def read_root():
    return {"message": "PolyPulse Backend is running"}
