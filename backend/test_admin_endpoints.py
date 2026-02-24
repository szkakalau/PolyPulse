import os
import tempfile
from fastapi.testclient import TestClient
import app.database as app_db
from main import app

os.environ["DISABLE_SCHEDULER"] = "1"
os.environ["ADMIN_API_KEY"] = "unit-admin-key"
_temp_db = tempfile.NamedTemporaryFile(delete=False)
app_db.DB_PATH = _temp_db.name
app_db.init_db()

client = TestClient(app)


def test_admin_auto_signal_requires_key():
    r = client.post("/admin/auto-signal/trigger")
    assert r.status_code == 403
    r = client.post("/admin/auto-signal/trigger", headers={"X-Admin-Key": "wrong"})
    assert r.status_code == 403
    r = client.post("/admin/auto-signal/trigger", headers={"X-Admin-Key": "unit-admin-key"})
    assert r.status_code == 200
