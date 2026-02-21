from apscheduler.schedulers.background import BackgroundScheduler


def create_scheduler(refresh_func):
    scheduler = BackgroundScheduler()
    scheduler.add_job(refresh_func, "interval", minutes=1, id="polymarket_refresh", replace_existing=True)
    return scheduler
