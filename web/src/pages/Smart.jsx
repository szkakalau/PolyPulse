import { useEffect, useState } from "react"
import { fetchSmartWallets } from "../api"

export default function Smart() {
  const [rows, setRows] = useState([])
  const [error, setError] = useState("")

  useEffect(() => {
    fetchSmartWallets()
      .then(setRows)
      .catch((e) => setError(e.message))
  }, [])

  return (
    <div className="panel">
      <h2>Smart Money</h2>
      {error && <div className="error">{error}</div>}
      <div className="table">
        <div className="row smart header">
          <div>Address</div>
          <div>Profit</div>
          <div>Win Rate</div>
          <div>ROI</div>
          <div>Total Trades</div>
        </div>
        {rows.map((row) => (
          <div className="row smart" key={row.address}>
            <div className="mono">{row.address}</div>
            <div>${row.profit.toFixed(2)}</div>
            <div>{(row.win_rate * 100).toFixed(1)}%</div>
            <div>{(row.roi * 100).toFixed(2)}%</div>
            <div>{row.total_trades}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
