import os
import tempfile
from fastapi.testclient import TestClient
import app.database as app_db
import main as main_module

os.environ["DISABLE_SCHEDULER"] = "1"
os.environ["ADMIN_API_KEY"] = "unit-admin-key"
_temp_db = tempfile.NamedTemporaryFile(delete=False)
app_db.DB_PATH = _temp_db.name
app_db.init_db()

from main import app

client = TestClient(app)


def _register_and_login(email: str, password: str) -> str:
    r = client.post("/register", json={"email": email, "password": password})
    assert r.status_code in (200, 400)
    r = client.post("/token", data={"username": email, "password": password})
    assert r.status_code == 200
    return r.json()["access_token"]


def test_users_me_requires_auth():
    r = client.get("/users/me")
    assert r.status_code == 401


import pytest

@pytest.mark.auth
def test_register_login_and_get_me():
    token = _register_and_login("auth_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.get("/users/me", headers=headers)
    assert r.status_code == 200
    data = r.json()
    assert data.get("email") == "auth_unit@example.com"


@pytest.mark.auth
def test_watchlist_requires_auth():
    r = client.get("/watchlist")
    assert r.status_code == 401


@pytest.mark.auth
def test_watchlist_flow_with_token():
    token = _register_and_login("watchlist_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.get("/watchlist", headers=headers)
    assert r.status_code == 200
    assert r.json() == []
    r = client.post("/watchlist/m-1", headers=headers)
    assert r.status_code == 200
    r = client.get("/watchlist", headers=headers)
    assert "m-1" in r.json()
    r = client.delete("/watchlist/m-1", headers=headers)
    assert r.status_code == 200
    r = client.get("/watchlist", headers=headers)
    assert "m-1" not in r.json()


@pytest.mark.auth
def test_notification_settings_with_token():
    token = _register_and_login("notify_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.get("/notification-settings", headers=headers)
    assert r.status_code == 200
    r = client.put("/notification-settings", headers=headers, json={"enabled": False})
    assert r.status_code == 200
    r = client.get("/notification-settings", headers=headers)
    assert r.json().get("enabled") is False


@pytest.mark.auth
def test_entitlements_me_with_token():
    token = _register_and_login("entitlements_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.get("/entitlements/me", headers=headers)
    assert r.status_code == 200
    data = r.json()
    assert data.get("tier") in {"free", "pro"}


@pytest.mark.auth
def test_trial_start_requires_auth():
    r = client.post("/trial/start")
    assert r.status_code == 401


@pytest.mark.auth
def test_trial_start_with_token():
    token = _register_and_login("trial_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.post("/trial/start", headers=headers)
    assert r.status_code == 200
    assert r.json().get("status") == "active"


@pytest.mark.auth
def test_notifications_register_requires_auth():
    r = client.post("/notifications/register", json={"token": "fcm-token-xyz"})
    assert r.status_code == 401


@pytest.mark.auth
def test_notifications_register_with_token():
    token = _register_and_login("notifyreg_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.post("/notifications/register", headers=headers, json={"token": "fcm-token-xyz"})
    assert r.status_code == 200
    assert r.json().get("status") == "ok"


@pytest.mark.auth
def test_billing_status_requires_auth():
    r = client.get("/billing/status")
    assert r.status_code == 401


@pytest.mark.auth
def test_billing_verify_requires_auth():
    r = client.post("/billing/verify", json={"platform": "google_play", "productId": "pro_monthly", "purchaseToken": "t"})
    assert r.status_code == 401


@pytest.mark.auth
def test_billing_status_default_none_with_token():
    token = _register_and_login("billstat_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.get("/billing/status", headers=headers)
    assert r.status_code == 200
    assert r.json().get("status") == "none"


@pytest.mark.auth
def test_billing_verify_with_token_sets_active():
    token = _register_and_login("billverify_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    payload = {"platform": "google_play", "productId": "pro_monthly", "purchaseToken": "order-123"}
    r = client.post("/billing/verify", headers=headers, json=payload)
    assert r.status_code == 200
    data = r.json()
    assert data.get("status") == "active"
    assert data.get("subscription", {}).get("planId") in {"pro_monthly", "pro_yearly"}


@pytest.mark.auth
def test_notifications_send_signal_not_found():
    token = _register_and_login("notifyfail_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    r = client.post("/notifications/send", headers=headers, json={"userId": 1, "signalId": 9999})
    assert r.status_code == 404


@pytest.mark.auth
def test_notifications_send_with_signal():
    token = _register_and_login("notifysend_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    signal_id = app_db.create_signal("notice", "content", "free")
    main_module.redis_client = None
    r = client.post("/notifications/send", headers=headers, json={"userId": 1, "signalId": signal_id})
    assert r.status_code == 200
    assert r.json().get("status") in {"sent", "queued", "delayed", "no_tokens"}


@pytest.mark.auth
def test_notifications_send_with_redis_queue():
    token = _register_and_login("notifyredis_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    signal_id = app_db.create_signal("queued", "content", "free")
    class _RedisStub:
        def zadd(self, key, mapping):
            self.last_key = key
            self.last_mapping = mapping
    stub = _RedisStub()
    main_module.redis_client = stub
    r = client.post("/notifications/send", headers=headers, json={"userId": 1, "signalId": signal_id})
    assert r.status_code == 200
    assert r.json().get("status") in {"queued", "delayed"}


@pytest.mark.auth
def test_billing_webhook_missing_order_id():
    r = client.post("/billing/webhook", json={"eventType": "SUBSCRIPTION_UPDATE"})
    assert r.status_code == 200
    assert r.json().get("status") == "ignored"


@pytest.mark.auth
def test_billing_webhook_unknown_order():
    r = client.post("/billing/webhook", json={"eventType": "SUBSCRIPTION_UPDATE", "orderId": "missing", "status": "active"})
    assert r.status_code == 200
    assert r.json().get("status") == "ignored"


@pytest.mark.auth
def test_billing_webhook_missing_status():
    token = _register_and_login("billhook_unit@example.com", "password123")
    headers = {"Authorization": f"Bearer {token}"}
    payload = {"platform": "google_play", "productId": "pro_monthly", "purchaseToken": "order-hook"}
    r = client.post("/billing/verify", headers=headers, json=payload)
    assert r.status_code == 200
    r = client.post("/billing/webhook", json={"eventType": "SUBSCRIPTION_UPDATE", "orderId": "order-hook"})
    assert r.status_code == 200
    assert r.json().get("status") == "ignored"


@pytest.mark.auth
def test_admin_create_signal_requires_key():
    payload = {"title": "t", "content": "c", "tierRequired": "free", "broadcast": False}
    r = client.post("/admin/signals", json=payload)
    assert r.status_code == 403
    r = client.post("/admin/signals", json=payload, headers={"X-Admin-Key": "wrong"})
    assert r.status_code == 403


@pytest.mark.auth
def test_admin_create_signal_with_key():
    payload = {"title": "t", "content": "c", "tierRequired": "free", "broadcast": False}
    r = client.post("/admin/signals", json=payload, headers={"X-Admin-Key": "unit-admin-key"})
    assert r.status_code == 200
    assert r.json().get("status") == "created"

@pytest.mark.auth
def test_admin_create_signal_with_key_broadcasts_queue():
    class _RedisStub:
        def zadd(self, key, mapping):
            self.last_key = key
            self.last_mapping = mapping

    original = main_module.redis_client
    main_module.redis_client = _RedisStub()
    try:
        app_db.upsert_fcm_token(1, "token-1")
        payload = {"title": "t", "content": "c", "tierRequired": "pro", "broadcast": True}
        r = client.post("/admin/signals", json=payload, headers={"X-Admin-Key": "unit-admin-key"})
        assert r.status_code == 200
        data = r.json()
        assert data.get("status") == "created"
        broadcast = data.get("broadcast")
        assert broadcast.get("status") == "ok"
        assert broadcast.get("users") >= 1
        assert broadcast.get("delayed") >= 1
        assert hasattr(main_module.redis_client, "last_mapping")
    finally:
        main_module.redis_client = original


@pytest.mark.auth
def test_admin_broadcast_signal_requires_key():
    signal_id = app_db.create_signal("admin", "signal", "free")
    r = client.post(f"/admin/signals/{signal_id}/broadcast")
    assert r.status_code == 403
    r = client.post(f"/admin/signals/{signal_id}/broadcast", headers={"X-Admin-Key": "wrong"})
    assert r.status_code == 403


@pytest.mark.auth
def test_admin_broadcast_signal_with_key_not_found():
    r = client.post("/admin/signals/9999/broadcast", headers={"X-Admin-Key": "unit-admin-key"})
    assert r.status_code == 404
