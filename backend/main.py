import logging
from fastapi import FastAPI
from contextlib import asynccontextmanager
from apscheduler.schedulers.background import BackgroundScheduler
from app.services.market_service import MarketService
from app.services.alert_service import AlertService
from app.database import init_db

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# Initialize Services
market_service = MarketService()
alert_service = AlertService()

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

@app.get("/")
def read_root():
    return {"message": "PolyPulse Backend is Running"}

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.get("/alerts")
def get_alerts():
    return {"alerts": alert_service.get_recent_alerts()}
