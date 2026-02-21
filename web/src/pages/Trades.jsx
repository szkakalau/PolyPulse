import { useEffect, useState } from "react"
import { fetchTrades } from "../api"

export default function Trades() {
  const [rows, setRows] = useState([])
  const [error, setError] = useState("")

  useEffect(() => {
    fetchTrades()
      .then(setRows)
      .catch((e) => setError(e.message))
  }, [])

  return (
    <div className="panel">
      <h2>Trades</h2>
      {error && <div className="error">{error}</div>}
      <div className="table">
        <div className="row trades header">
          <div>Question</div>
          <div>Side</div>
          <div>Value</div>
          <div>Address</div>
          <div>Time</div>
        </div>
        {rows.map((row) => (
          <div className="row trades" key={row.id}>
            <div className="question">{row.market_question}</div>
            <div>{row.side}</div>
            <div>${row.value.toFixed(2)}</div>
            <div className="mono">{row.address}</div>
            <div>{new Date(row.timestamp).toLocaleString()}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
