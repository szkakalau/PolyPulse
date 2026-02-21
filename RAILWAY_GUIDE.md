# Railway Postgres Migration Guide

## 1. Setup Postgres on Railway
1. Go to your Railway project dashboard.
2. Click "New" -> "Database" -> "PostgreSQL".
3. Wait for the database to provision.
4. Click on the PostgreSQL service card -> "Connect" tab.
5. Copy the `DATABASE_URL` (it starts with `postgresql://...`).

## 2. Configure Backend Service
1. Go to your Backend service card -> "Variables" tab.
2. Add a new variable: `DATABASE_URL`.
3. Paste the `DATABASE_URL` you copied.
4. Add another variable: `PYTHONUNBUFFERED=1` (optional, for better logging).

## 3. Code Changes (Already Applied)
The backend code has been updated to automatically detect `DATABASE_URL`:
- **Market DB (`polymarket.db`)**: Automatically switches to Postgres if `DATABASE_URL` is set.
- **App DB (`polypulse.db`)**: Currently uses SQLite. **We need to migrate this to Postgres.**

## 4. Migration Strategy for App DB
Since the App DB uses raw SQL, we need to update `backend/app/database.py` to support Postgres syntax:
1. Replace `?` placeholders with `%s`.
2. Replace `INTEGER PRIMARY KEY AUTOINCREMENT` with `SERIAL PRIMARY KEY`.
3. Handle `lastrowid` using `RETURNING id`.
4. Use `psycopg2` driver.

## 5. Running the Migration
Once the code is updated and deployed:
1. The `init_db()` function will run on startup.
2. It will create the tables in Postgres if they don't exist.
3. **Note:** Existing data in SQLite will NOT be automatically migrated. You start fresh.
   - If you need to keep data, you must export SQLite data to CSV and import into Postgres manually.
