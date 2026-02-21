import { Link, Route, Routes, useLocation } from "react-router-dom"
import Whales from "./pages/Whales.jsx"
import Smart from "./pages/Smart.jsx"
import Trades from "./pages/Trades.jsx"
import { refreshData } from "./api"
import { useState } from "react"

export default function App() {
  const location = useLocation()
  const [refreshing, setRefreshing] = useState(false)

  const handleRefresh = async () => {
    setRefreshing(true)
    try {
      await refreshData()
    } finally {
      setRefreshing(false)
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="logo">PolyPulse Admin</div>
        <nav className="nav">
          <Link className={location.pathname === "/whales" ? "active" : ""} to="/whales">Whales</Link>
          <Link className={location.pathname === "/smart" ? "active" : ""} to="/smart">Smart Money</Link>
          <Link className={location.pathname === "/trades" ? "active" : ""} to="/trades">Trades</Link>
        </nav>
        <button className="refresh" onClick={handleRefresh} disabled={refreshing}>
          {refreshing ? "Refreshing..." : "Refresh"}
        </button>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<Whales />} />
          <Route path="/whales" element={<Whales />} />
          <Route path="/smart" element={<Smart />} />
          <Route path="/trades" element={<Trades />} />
        </Routes>
      </main>
    </div>
  )
}
