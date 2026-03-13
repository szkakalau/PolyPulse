"""
Performance and load testing for PolyPulse backend
"""
import pytest
import time
import requests
import os
import subprocess
import sys
import tempfile
from urllib.parse import urlparse
from datetime import datetime, timedelta

BASE_URL = "http://127.0.0.1:8000"

def _get(url: str, timeout: float):
    return requests.get(url, timeout=timeout, proxies={"http": None, "https": None})

def _health_url():
    return f"{BASE_URL}/health"

@pytest.fixture(scope="module", autouse=True)
def ensure_backend_server():
    parsed = urlparse(BASE_URL)
    host = parsed.hostname or "127.0.0.1"
    if host == "localhost":
        host = "127.0.0.1"
    port = parsed.port or 8000

    try:
        response = _get(_health_url(), timeout=1)
        if response.status_code == 200:
            yield
            return
    except Exception:
        pass

    backend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    stdout_file = tempfile.TemporaryFile(mode="w+b")
    stderr_file = tempfile.TemporaryFile(mode="w+b")
    child_env = dict(os.environ)
    child_env.pop("DATABASE_URL", None)
    child_env.pop("REDIS_URL", None)
    db_file = tempfile.NamedTemporaryFile(delete=False)
    db_path = db_file.name
    db_file.close()
    process = subprocess.Popen(
        [
            sys.executable,
            "-m",
            "uvicorn",
            "main:app",
            "--host",
            host,
            "--port",
            str(port),
            "--log-level",
            "warning",
        ],
        cwd=backend_dir,
        stdout=stdout_file,
        stderr=stderr_file,
        env={**child_env, "PORT": str(port), "DISABLE_SCHEDULER": "1", "DB_PATH": db_path, "SEED_DEMO_DATA": "1"},
    )

    try:
        deadline = time.time() + 60.0
        while time.time() < deadline:
            if process.poll() is not None:
                break
            try:
                response = _get(_health_url(), timeout=1)
                if response.status_code == 200:
                    yield
                    return
            except Exception:
                pass
            time.sleep(0.25)

        stderr_file.seek(0)
        stderr_tail = stderr_file.read()[-4000:].decode("utf-8", errors="replace")
        raise RuntimeError(f"Backend server did not become ready in time. stderr tail:\n{stderr_tail}")
    finally:
        process.terminate()
        try:
            process.wait(timeout=5)
        except Exception:
            process.kill()
        stdout_file.close()
        stderr_file.close()
        try:
            os.unlink(db_path)
        except Exception:
            pass

def test_api_response_time():
    """Test that API endpoints respond within acceptable time limits"""
    endpoints_to_test = [
        "/health",
        "/metrics/prometheus",
        "/signals?limit=10",
        "/insights/credibility",
        "/insights/delivery"
    ]
    
    max_response_time = 2.0  # 2 seconds maximum
    
    for endpoint in endpoints_to_test:
        start_time = time.time()
        response = _get(f"{BASE_URL}{endpoint}", timeout=5)
        end_time = time.time()
        
        response_time = end_time - start_time
        
        assert response.status_code == 200, f"Endpoint {endpoint} returned {response.status_code}"
        assert response_time < max_response_time, f"Endpoint {endpoint} took {response_time:.2f}s (max: {max_response_time}s)"
        
        print(f"✓ {endpoint}: {response_time:.3f}s")

def test_concurrent_requests():
    """Test handling of concurrent requests"""
    import threading
    
    results = []
    errors = []
    
    def make_request():
        try:
            start_time = time.time()
            response = _get(f"{BASE_URL}/health", timeout=3)
            end_time = time.time()
            results.append({
                'response_time': end_time - start_time,
                'status_code': response.status_code
            })
        except Exception as e:
            errors.append(str(e))
    
    # Create 10 concurrent requests
    threads = []
    for _ in range(10):
        thread = threading.Thread(target=make_request)
        threads.append(thread)
        thread.start()
    
    # Wait for all threads to complete
    for thread in threads:
        thread.join()
    
    # Verify all requests succeeded
    assert len(errors) == 0, f"Concurrent requests failed: {errors}"
    assert len(results) == 10, f"Expected 10 results, got {len(results)}"
    
    # Calculate average response time
    avg_response_time = sum(r['response_time'] for r in results) / len(results)
    assert avg_response_time < 1.0, f"Average response time too high: {avg_response_time:.3f}s"
    
    print(f"✓ Concurrent requests: {avg_response_time:.3f}s average")

def test_rate_limiting():
    """Test that rate limiting works correctly"""
    # Make rapid requests to trigger rate limiting
    for i in range(15):  # More than default limit of 10/min
        response = _get(f"{BASE_URL}/health", timeout=5)
        
        if i >= 10:  # After 10 requests, should be rate limited
            if response.status_code == 429:
                print("✓ Rate limiting working correctly")
                return
            
        time.sleep(0.1)  # Small delay between requests
    
    # If we get here, rate limiting didn't trigger
    pytest.fail("Rate limiting did not trigger as expected")

def test_database_performance():
    """Test database query performance"""
    # Test a complex query performance
    start_time = time.time()
    
    response = _get(f"{BASE_URL}/insights/credibility", timeout=10)
    end_time = time.time()
    
    query_time = end_time - start_time
    
    assert response.status_code == 200
    assert query_time < 3.0, f"Database query took {query_time:.2f}s (too slow)"
    
    print(f"✓ Database query: {query_time:.3f}s")

if __name__ == "__main__":
    print("Running performance tests...")
    test_api_response_time()
    test_concurrent_requests() 
    test_database_performance()
    print("All performance tests passed! 🚀")
