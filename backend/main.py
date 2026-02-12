import logging
from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from contextlib import asynccontextmanager
from apscheduler.schedulers.background import BackgroundScheduler
from app.services.market_service import MarketService
from app.services.alert_service import AlertService
from app.services.auth_service import AuthService, ACCESS_TOKEN_EXPIRE_MINUTES
from app.database import init_db, create_user, get_user_by_email
from app.schemas import UserRegister, UserLogin, Token, UserResponse
from datetime import timedelta

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# Initialize Services
market_service = MarketService()
alert_service = AlertService()
auth_service = AuthService()

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/login")

def job_fetch_and_analyze():
    logger.info("Job: Fetching markets...")
    markets = market_service.fetch_active_markets()
    logger.info(f"Job: Got {len(markets)} markets. Checking for alerts...")
    alerts = alert_service.check_for_alerts(markets)
    if alerts:
        logger.info(f"Job: Generated {len(alerts)} alerts.")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize DB
    init_db()
    
    scheduler = BackgroundScheduler()
    # Run every 30 seconds
    scheduler.add_job(job_fetch_and_analyze, 'interval', seconds=30)
    scheduler.start()
    logger.info("Startup: Scheduler started")
    yield
    scheduler.shutdown()
    logger.info("Shutdown: Scheduler stopped")

app = FastAPI(title="PolyPulse API", lifespan=lifespan)

async def get_current_user(token: str = Depends(oauth2_scheme)):
    payload = auth_service.decode_token(token)
    if payload is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    email: str = payload.get("sub")
    if email is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
    user = get_user_by_email(email)
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user

@app.get("/")
def read_root():
    return {"message": "PolyPulse Backend is Running"}

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.get("/alerts")
def get_alerts():
    return {"alerts": alert_service.get_recent_alerts()}

# Auth Endpoints

@app.post("/auth/register", response_model=UserResponse)
def register(user: UserRegister):
    existing_user = get_user_by_email(user.email)
    if existing_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    hashed_password = auth_service.get_password_hash(user.password)
    new_user = create_user(user.email, hashed_password)
    if not new_user:
        raise HTTPException(status_code=500, detail="Failed to create user")
        
    return {
        "id": new_user["id"],
        "email": new_user["email"],
        "created_at": "Just now" # simplified for response
    }

@app.post("/auth/login", response_model=Token)
def login(user: UserLogin):
    db_user = get_user_by_email(user.email)
    if not db_user or not auth_service.verify_password(user.password, db_user["password_hash"]):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = auth_service.create_access_token(
        data={"sub": user.email}, expires_delta=access_token_expires
    )
    return {"access_token": access_token, "token_type": "bearer"}

@app.get("/auth/me", response_model=UserResponse)
def read_users_me(current_user: dict = Depends(get_current_user)):
    return {
        "id": current_user["id"],
        "email": current_user["email"],
        "created_at": current_user["created_at"]
    }
