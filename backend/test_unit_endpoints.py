import os
import tempfile
import json
from datetime import datetime, timedelta
import pytest
from fastapi.testclient import TestClient
import app.database as app_db

os.environ["DISABLE_SCHEDULER"] = "1"
os.environ["SEED_DEMO_DATA"] = "1"
_temp_db = tempfile.NamedTemporaryFile(delete=False)
app_db.DB_PATH = _temp_db.name
app_db.init_db()

import main as main_module
from main import app, get_current_user, get_optional_user

client = TestClient(app)


@pytest.fixture(autouse=True)
def _override_auth():
    user = {"id": 1, "email": "unit@test.local", "created_at": "now"}
    app.dependency_overrides[get_current_user] = lambda: user
    app.dependency_overrides[get_optional_user] = lambda: user
    yield
    app.dependency_overrides.clear()


@pytest.mark.unit
def test_health_endpoint():
    r = client.get("/health")
    assert r.status_code == 200
    data = r.json()
    assert data.get("status") == "ok"
    assert "timestamp" in data


@pytest.mark.unit
def test_monitor_alert_info():
    r = client.post("/monitor/alert", json={"level": "info", "message": "hello", "source": "test"})
    assert r.status_code == 200
    assert r.json().get("status") == "ok"


@pytest.mark.unit
def test_feature_flags_default():
    r = client.get("/feature-flags")
    assert r.status_code == 200
    items = r.json()
    assert isinstance(items, list)


@pytest.mark.unit
def test_metrics_endpoint_shape():
    r = client.get("/metrics")
    assert r.status_code == 200
    data = r.json()
    assert "users" in data
    assert "signals" in data
    assert "alerts" in data


@pytest.mark.unit
def test_signals_list_unauthenticated():
    r = client.get("/signals", params={"limit": 5, "offset": 0})
    assert r.status_code == 200
    items = r.json()
    assert isinstance(items, list)
    assert len(items) >= 1
    for it in items:
        assert "id" in it
        assert "title" in it
        assert "locked" in it
        assert "tierRequired" in it


@pytest.mark.unit
def test_paywall_public():
    r = client.get("/paywall")
    assert r.status_code == 200
    data = r.json()
    assert "plans" in data
    assert any(p.get("id") == "free" for p in data.get("plans", []))


def test_watchlist_flow():
    r = client.get("/watchlist")
    assert r.status_code == 200
    assert r.json() == []
    r = client.post("/watchlist/market-123")
    assert r.status_code == 200
    r = client.get("/watchlist")
    assert "market-123" in r.json()
    r = client.delete("/watchlist/market-123")
    assert r.status_code == 200
    r = client.get("/watchlist")
    assert "market-123" not in r.json()


def test_notification_settings_flow():
    r = client.get("/notification-settings")
    assert r.status_code == 200
    r = client.put("/notification-settings", json={"enabled": False})
    assert r.status_code == 200
    r = client.get("/notification-settings")
    assert r.json().get("enabled") is False


def test_notifications_send():
    signal_id = app_db.create_signal("hello", "world", "free")
    main_module.deliver_notification = lambda user_id, signal_id: True
    payload = {"userId": 1, "signalId": signal_id}
    r = client.post("/notifications/send", json=payload)
    assert r.status_code == 200
    assert r.json().get("status") in {"sent", "queued", "delayed"}

@pytest.mark.unit
def test_seeded_signal_has_evidence():
    r = client.get("/signals", params={"limit": 10, "offset": 0})
    assert r.status_code == 200
    items = r.json()
    assert any(it.get("evidence") is not None for it in items)

@pytest.mark.unit
def test_signal_detail_with_evidence_and_locked():
    evidence = {
        "sourceType": "whale_trade",
        "triggeredAt": "2025-01-01 00:00:00",
        "marketId": "market-locked-1",
        "makerAddress": "0xabc",
        "evidenceUrl": "https://example.com",
        "dedupeKey": "dedupe-1"
    }
    signal_id = app_db.create_signal(
        "locked with evidence",
        "secret content",
        "pro",
        json.dumps(evidence)
    )
    r = client.get(f"/signals/{signal_id}")
    assert r.status_code == 200
    data = r.json()
    assert data.get("locked") is True
    assert data.get("content") is None
    assert data.get("evidence", {}).get("marketId") == "market-locked-1"
    r = client.get(f"/signals/{signal_id}", params={"requireUnlocked": True})
    assert r.status_code == 402

@pytest.mark.unit
def test_signal_detail_locked_flow():
    signal_id = app_db.create_signal("pro title", "pro content", "pro")
    r = client.get(f"/signals/{signal_id}")
    assert r.status_code == 200
    data = r.json()
    assert data.get("locked") is True
    assert data.get("content") is None
    r = client.get(f"/signals/{signal_id}", params={"requireUnlocked": True})
    assert r.status_code == 402

@pytest.mark.unit
def test_signal_detail_not_found():
    r = client.get("/signals/999999")
    assert r.status_code == 404

@pytest.mark.unit
def test_signal_stats_endpoint():
    r = client.get("/signals/stats")
    assert r.status_code == 200
    data = r.json()
    assert "signals7d" in data
    assert "evidence7d" in data

@pytest.mark.unit
def test_dashboard_stats():
    r = client.get("/dashboard/stats")
    assert r.status_code == 200
    data = r.json()
    assert "alerts_24h" in data
    assert "watchlist_count" in data
    assert "top_movers" in data

@pytest.mark.unit
def test_dashboard_alerts():
    original = main_module.get_recent_alerts
    main_module.get_recent_alerts = lambda: [{"message": "hello"}]
    try:
        r = client.get("/dashboard/alerts")
        assert r.status_code == 200
        data = r.json()
        assert isinstance(data, list)
        assert data[0].get("message") == "hello"
    finally:
        main_module.get_recent_alerts = original

@pytest.mark.unit
def test_dashboard_whales_and_leaderboard():
    original_fetch = main_module.whale_service.fetch_whale_activity
    original_leaderboard = main_module.whale_service.get_leaderboard
    main_module.whale_service.fetch_whale_activity = lambda: [{"market_question": "m", "value_usd": 1200}]
    main_module.whale_service.get_leaderboard = lambda: [{"maker_address": "0x1", "total_volume": 5000}]
    try:
        r = client.get("/dashboard/whales")
        assert r.status_code == 200
        data = r.json()
        assert isinstance(data, list)
        assert data[0].get("value_usd") == 1200
        r = client.get("/dashboard/leaderboard")
        assert r.status_code == 200
        data = r.json()
        assert isinstance(data, list)
        assert data[0].get("total_volume") == 5000
    finally:
        main_module.whale_service.fetch_whale_activity = original_fetch
        main_module.whale_service.get_leaderboard = original_leaderboard

@pytest.mark.unit
def test_api_whales_uses_session_rows():
    class _FakeWhale:
        def __init__(self):
            self.trade_id = "t1"
            self.value = 1234.0
            self.timestamp = datetime.utcnow()
            self.address = "0xabc"

    class _FakeTrade:
        def __init__(self):
            self.question = "Q"
            self.side = "BUY"
            self.price = 0.42
            self.size = 100
            self.market = "market-1"

    class _FakeSession:
        def __init__(self, rows):
            self._rows = rows
        def query(self, *args, **kwargs):
            return self
        def join(self, *args, **kwargs):
            return self
        def order_by(self, *args, **kwargs):
            return self
        def offset(self, *args, **kwargs):
            return self
        def limit(self, *args, **kwargs):
            return self
        def all(self):
            return self._rows
        def close(self):
            return None

    original = main_module.get_session
    main_module.get_session = lambda: _FakeSession([(_FakeWhale(), _FakeTrade())])
    try:
        r = client.get("/api/whales", params={"limit": 1, "offset": 0, "sort": "latest"})
        assert r.status_code == 200
        data = r.json()
        assert data[0].get("trade_id") == "t1"
    finally:
        main_module.get_session = original

@pytest.mark.unit
def test_api_trades_uses_session_rows():
    class _FakeTrade:
        def __init__(self):
            self.id = "t2"
            self.question = "Q2"
            self.address = "0x2"
            self.side = "SELL"
            self.price = 0.6
            self.size = 10
            self.value = 6.0
            self.timestamp = datetime.utcnow()
            self.market = "m2"

    class _FakeSession:
        def query(self, *args, **kwargs):
            return self
        def order_by(self, *args, **kwargs):
            return self
        def offset(self, *args, **kwargs):
            return self
        def limit(self, *args, **kwargs):
            return self
        def all(self):
            return [_FakeTrade()]
        def close(self):
            return None

    original = main_module.get_session
    main_module.get_session = lambda: _FakeSession()
    try:
        r = client.get("/api/trades", params={"limit": 1, "offset": 0})
        assert r.status_code == 200
        data = r.json()
        assert data[0].get("id") == "t2"
    finally:
        main_module.get_session = original

@pytest.mark.unit
def test_api_smart_wallets_uses_session_rows():
    class _FakeWallet:
        def __init__(self):
            self.address = "0xsmart"
            self.profit = 123.4
            self.roi = 0.12
            self.win_rate = 0.7
            self.total_trades = 9

    class _FakeSession:
        def query(self, *args, **kwargs):
            return self
        def order_by(self, *args, **kwargs):
            return self
        def offset(self, *args, **kwargs):
            return self
        def limit(self, *args, **kwargs):
            return self
        def all(self):
            return [_FakeWallet()]
        def close(self):
            return None

    original = main_module.get_session
    main_module.get_session = lambda: _FakeSession()
    try:
        r = client.get("/api/smart", params={"limit": 1, "offset": 0})
        assert r.status_code == 200
        data = r.json()
        assert data[0].get("address") == "0xsmart"
    finally:
        main_module.get_session = original

@pytest.mark.unit
def test_api_refresh_calls_refresh():
    called = {"value": False}
    original = main_module.refresh_polymarket_data
    main_module.refresh_polymarket_data = lambda: called.update(value=True)
    try:
        r = client.post("/api/refresh")
        assert r.status_code == 200
        assert r.json().get("status") == "ok"
        assert called["value"] is True
    finally:
        main_module.refresh_polymarket_data = original

@pytest.mark.unit
def test_daily_pulse_endpoint():
    conn = app_db.get_db_connection()
    cursor = conn.cursor()
    app_db.execute_sql(
        cursor,
        '''
        INSERT INTO daily_pulse (title, summary, content, created_at)
        VALUES (?, ?, ?, ?)
        ''',
        ("Daily", "Summary", "Content", "2025-01-01 00:00:00")
    )
    conn.commit()
    conn.close()
    r = client.get("/daily-pulse", params={"limit": 10, "offset": 0})
    assert r.status_code == 200
    data = r.json()
    assert data[0].get("title") == "Daily"

@pytest.mark.unit
def test_referral_code_and_redeem():
    r = client.get("/referral/code")
    assert r.status_code == 200
    code = r.json().get("code")
    assert code
    r = client.get("/referral/code")
    assert r.status_code == 200
    assert r.json().get("code") == code
    r = client.post("/referral/redeem", json={"code": "invalid-code"})
    assert r.status_code == 200
    assert r.json().get("status") == "invalid"

@pytest.mark.unit
def test_billing_status_expired_when_end_at_past():
    now = datetime.utcnow()
    start_at = (now - timedelta(days=10)).isoformat()
    end_at = (now - timedelta(days=1)).isoformat()
    app_db.upsert_subscription(
        user_id=1,
        platform="google_play",
        plan_id="pro_monthly",
        status="active",
        start_at=start_at,
        end_at=end_at,
        auto_renew=True
    )
    r = client.get("/billing/status")
    assert r.status_code == 200
    data = r.json()
    assert data.get("status") == "expired"

@pytest.mark.unit
def test_in_app_message():
    original = main_module._build_in_app_message
    def _fake_message(user_id):
        return main_module.InAppMessageResponse(
            id="m1",
            type="info",
            title="Hello",
            body="World",
            ctaText="Go",
            ctaAction="open_url",
            plans=[
                main_module.PaywallPlan(id="free", name="Free", price=0, currency="CNY", period="month", trialDays=0),
                main_module.PaywallPlan(id="pro_monthly", name="Pro Monthly", price=49, currency="CNY", period="month", trialDays=7)
            ]
        )
    main_module._build_in_app_message = _fake_message
    try:
        r = client.get("/in-app-message")
        assert r.status_code == 200
        data = r.json()
        assert data.get("id") == "m1"
    finally:
        main_module._build_in_app_message = original


def test_analytics_event():
    payload = {"eventName": "unit_test_event", "properties": {"k": "v"}}
    r = client.post("/analytics/event", json=payload)
    assert r.status_code == 200
    assert r.json().get("status") == "ok"
