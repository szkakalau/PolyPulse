# PolyPulse - Product Requirements Document (PRD)

## 1. Product Overview
*   **Product Name**: PolyPulse
*   **Mission**: To empower traders and researchers with real-time intelligence from prediction markets.
*   **Platform**: Android (Native) + Backend (Python/FastAPI)
*   **Target Audience**: Global English-speaking users, Crypto traders, Political analysts.
*   **Monetization**: Freemium (Basic free, Pro subscription for real-time alerts).

## 2. Features (MVP)

### 2.1 Android App
*   **Market Dashboard**:
    *   Display list of active markets from Polymarket.
    *   Sort by Volume, Liquidity, and Creation Date.
    *   Filter by Category: Politics, Crypto, Sports, Business.
    *   Visuals: Market image, Question title, Top 2 outcome odds (Yes/No).
*   **Search**: Search markets by keywords.
*   **Details View** (Phase 1.5): Click to see basic details (description, full outcome list).

### 2.2 Backend Service
*   **Data Poller**: Fetch data from `https://clob.polymarket.com/markets` and `/book` periodically.
*   **API Proxy**: Serve processed data to the App (optional for MVP, can go direct to Polymarket for read-only).

## 3. Technical Specifications

### 3.1 Android Client
*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
*   **Networking**: Retrofit + OkHttp + kotlinx.serialization
*   **Image Loading**: Coil

### 3.2 Data Source (Polymarket API)
*   **Base URL**: `https://clob.polymarket.com`
*   **Endpoints**:
    *   `GET /markets`: Get active markets.
    *   `GET /book?token_id={id}`: Get order book/odds.

## 4. Design Guidelines
*   **Theme**: Dark Mode default (Financial terminal feel).
*   **Color Palette**:
    *   Primary: Deep Blue/Purple (Polymarket brand alignment).
    *   Up/Yes: Green.
    *   Down/No: Red/Orange.
*   **Language**: English (en-US).

## 5. Roadmap
1.  **Phase 1 (Current)**: Android App Skeleton, Market List Display.
2.  **Phase 2**: Backend Poller, Push Notification Integration.
3.  **Phase 3**: Subscription System, Google Play Launch.
