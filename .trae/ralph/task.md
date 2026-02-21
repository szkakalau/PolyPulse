# Task: Fix Android Connectivity and Authentication Issues

**Goal**: Resolve the following errors shown in the user's screenshots:
1.  **Polymarket API Connection Failure**: `Failed to connect to clob.polymarket.com/[2001::...]` (IPv6/Network issue).
2.  **Authentication Error**: `No token found` on the Dashboard.
3.  **Backend Connection Failure**: `Failed to connect to backend: ... /10.0.2.2:8000` on Live Alerts.

**Success Criteria**:
-   The app can successfully fetch data from Polymarket API (Markets tab).
-   The app handles the "No token found" state gracefully (e.g., redirects to login) or allows the user to log in.
-   The app can connect to the local backend (Live Alerts tab).
