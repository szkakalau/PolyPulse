"""
Performance and load testing for PolyPulse backend
"""
import pytest
import time
import requests
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8000"

def test_api_response_time():
    """Test that API endpoints respond within acceptable time limits"""
    endpoints_to_test = [
        "/health",
        "/metrics",
        "/signals?limit=10",
        "/insights/credibility",
        "/insights/delivery"
    ]
    
    max_response_time = 2.0  # 2 seconds maximum
    
    for endpoint in endpoints_to_test:
        start_time = time.time()
        response = requests.get(f"{BASE_URL}{endpoint}", timeout=5)
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
            response = requests.get(f"{BASE_URL}/health", timeout=3)
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
        response = requests.get(f"{BASE_URL}/health")
        
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
    
    response = requests.get(f"{BASE_URL}/insights/credibility")
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