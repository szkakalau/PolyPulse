import { Link, Route, Routes, useLocation } from "react-router-dom"
import Insights from "./pages/Insights.jsx"
import Delivery from "./pages/Delivery.jsx"
import Whales from "./pages/Whales.jsx"
import Smart from "./pages/Smart.jsx"
import Trades from "./pages/Trades.jsx"
import SignalLanding from "./pages/SignalLanding.jsx"
import { refreshData } from "./api"
import { useState } from "react"

export default function App() {
  const location = useLocation()
  const [refreshing, setRefreshing] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [lastUpdated, setLastUpdated] = useState("")
  const [refreshError, setRefreshError] = useState("")
  const isSignalLanding = location.pathname.startsWith("/signals/")

  const activeTab = (() => {
    if (location.pathname === "/" || location.pathname.startsWith("/insights")) return "insights"
    if (location.pathname.startsWith("/delivery")) return "delivery"
    if (location.pathname.startsWith("/smart")) return "smart"
    if (location.pathname.startsWith("/trades")) return "trades"
    return "whales"
  })()

  const handleRefresh = async () => {
    setRefreshing(true)
    setRefreshError("")
    try {
      await refreshData()
      setLastUpdated(new Date().toLocaleTimeString())
    } catch (e) {
      setRefreshError(e.message || "Refresh failed")
    } finally {
      setRefreshKey((value) => value + 1)
      setRefreshing(false)
    }
  }

  const getPageTitle = () => {
    switch (activeTab) {
      case "insights": return "Credibility"
      case "delivery": return "Delivery Observability"
      case "whales": return "Whale Radar"
      case "smart": return "Smart Money"
      case "trades": return "Trade Feed"
      default: return "Dashboard"
    }
  }

  if (isSignalLanding) {
    return (
      <div className="landing-app">
        <Routes>
          <Route path="/signals/:signalId" element={<SignalLanding />} />
        </Routes>
      </div>
    )
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-logo">
            <div className="brand-mark" />
            PolyPulse
          </div>
        </div>

        <nav className="nav" aria-label="Primary">
          <Link className={activeTab === "insights" ? "nav-item active" : "nav-item"} to="/insights">
            Credibility
          </Link>
          <Link className={activeTab === "delivery" ? "nav-item active" : "nav-item"} to="/delivery">
            Delivery
          </Link>
          <div className="section-title" style={{ padding: '16px 12px 4px' }}>Market</div>
          <Link className={activeTab === "whales" ? "nav-item active" : "nav-item"} to="/whales">
            Whale Radar
          </Link>
          <Link className={activeTab === "smart" ? "nav-item active" : "nav-item"} to="/smart">
            Smart Money
          </Link>
          <Link className={activeTab === "trades" ? "nav-item active" : "nav-item"} to="/trades">
            Trade Feed
          </Link>
        </nav>
      </aside>

      <div className="main-content">
        <header className="topbar">
          <div className="page-title">{getPageTitle()}</div>
          <div className="actions">
            <div className="status-text">
              {lastUpdated ? `Synced ${lastUpdated}` : "Ready"}
            </div>
            <button className="btn btn-primary" onClick={handleRefresh} disabled={refreshing}>
              {refreshing ? "Syncing..." : "Sync Data"}
            </button>
          </div>
        </header>

        <main className="content">
          {refreshError && <div className="notice error" style={{ marginBottom: 24 }}>{refreshError}</div>}
          <Routes>
            <Route path="/" element={<Insights refreshKey={refreshKey} />} />
            <Route path="/insights" element={<Insights refreshKey={refreshKey} />} />
            <Route path="/delivery" element={<Delivery refreshKey={refreshKey} />} />
            <Route path="/whales" element={<Whales refreshKey={refreshKey} />} />
            <Route path="/smart" element={<Smart refreshKey={refreshKey} />} />
            <Route path="/trades" element={<Trades refreshKey={refreshKey} />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}
