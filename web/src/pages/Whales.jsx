import { useEffect, useState } from "react"
import { fetchWhales } from "../api"

export default function Whales() {
  const [rows, setRows] = useState([])
  const [error, setError] = useState("")

  useEffect(() => {
    fetchWhales()
      .then(setRows)
      .catch((e) => setError(e.message))
  }, [])

  return (
    <div className="panel">
      <h2>Whale Trades</h2>
      {error && <div className="error">{error}</div>}
      <div className="table">
        <div className="row whales header">
          <div>Question</div>
          <div>Value</div>
          <div>Address</div>
          <div>Time</div>
        </div>
        {rows.map((row) => (
          <div className="row whales" key={row.trade_id}>
            <div className="question">{row.market_question}</div>
            <div>${row.value_usd.toFixed(2)}</div>
            <div className="mono">{row.maker_address}</div>
            <div>{new Date(row.timestamp).toLocaleString()}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
