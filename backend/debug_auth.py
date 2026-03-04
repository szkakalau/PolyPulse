import requests

session = requests.Session()
session.trust_env = False

# Test login first
print("Testing login...")
r = session.post('http://localhost:8000/token', data={'username': 'integration_test@example.com', 'password': 'password123'})
print('Login status:', r.status_code)

if r.status_code == 200:
    token = r.json()['access_token']
    headers = {'Authorization': f'Bearer {token}'}
    print('Token received successfully')
    
    # Test leaderboard immediately
    print("Testing leaderboard...")
    r2 = session.get('http://localhost:8000/dashboard/leaderboard', headers=headers)
    print('Leaderboard status:', r2.status_code)
    print('Leaderboard response:', r2.text[:200])
    
    # Test user info
    print("Testing user info...")
    r3 = session.get('http://localhost:8000/users/me', headers=headers)
    print('User info status:', r3.status_code)
    print('User info:', r3.text[:200])
else:
    print('Login failed:', r.text)