import { useEffect, useMemo, useState } from "react"
import { fetchWhales } from "../api"

export default function Whales({ refreshKey }) {
  const [rows, setRows] = useState([])
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const [query, setQuery] = useState("")
  const [minValue, setMinValue] = useState("")
  const [sort, setSort] = useState("latest")
  const [limit, setLimit] = useState(50)
  const [offset, setOffset] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [copiedId, setCopiedId] = useState("")

  const usd = useMemo(
    () =>
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: 0
      }),
    []
  )

  useEffect(() => {
    setOffset(0)
  }, [refreshKey, sort, limit])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError("")
    fetchWhales({ limit, offset, sort })
      .then((data) => {
        if (cancelled) return
        setRows((prev) => (offset === 0 ? data : [...prev, ...data]))
        setHasMore(data.length === limit)
      })
      .catch((e) => {
        if (cancelled) return
        setError(e.message)
      })
      .finally(() => {
        if (cancelled) return
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [refreshKey, limit, offset, sort])

  const filteredRows = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    const minValueNumber = minValue === "" ? null : Number(minValue)
    return rows.filter((row) => {
      const matchesQuery =
        !normalizedQuery ||
        row.market_question.toLowerCase().includes(normalizedQuery) ||
        row.maker_address.toLowerCase().includes(normalizedQuery)
      const matchesValue = minValueNumber === null || row.value_usd >= minValueNumber
      return matchesQuery && matchesValue
    })
  }, [rows, query, minValue])

  const handleCopy = async (id, value) => {
    try {
      await navigator.clipboard.writeText(value)
      setCopiedId(id)
      window.setTimeout(() => setCopiedId(""), 900)
    } catch {
      const el = document.createElement("textarea")
      el.value = value
      el.style.position = "fixed"
      el.style.left = "-9999px"
      document.body.appendChild(el)
      el.select()
      document.execCommand("copy")
      document.body.removeChild(el)
      setCopiedId(id)
      window.setTimeout(() => setCopiedId(""), 900)
    }
  }

  return (
    <div className="panel">
      {error && <div className="notice error">{error}</div>}
      
      <div className="toolbar">
        <div className="controls">
          <input
            className="input"
            placeholder="Search question or address"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            style={{ width: 240 }}
          />
          <input
            className="input"
            type="number"
            min="0"
            placeholder="Min value (USD)"
            value={minValue}
            onChange={(event) => setMinValue(event.target.value)}
            style={{ width: 140 }}
          />
          <select className="select" value={sort} onChange={(event) => setSort(event.target.value)}>
            <option value="latest">Latest</option>
            <option value="value">Highest Value</option>
          </select>
          <select
            className="select"
            value={limit}
            onChange={(event) => setLimit(Number(event.target.value))}
          >
            <option value={25}>25</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
          <div className="chip">
            Loaded <strong>{rows.length}</strong>
          </div>
        </div>
        <div className="subtle">{loading ? "Loading..." : `${filteredRows.length} shown`}</div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th style={{ width: '40%' }}>Question</th>
              <th className="text-right">Value</th>
              <th>Address</th>
              <th className="text-right">Time</th>
            </tr>
          </thead>
          <tbody>
            {loading && rows.length === 0 &&
              Array.from({ length: 5 }).map((_, idx) => (
                <tr key={`s-${idx}`}>
                  <td colSpan={4}>
                    <div className="skeleton" style={{ height: 20, borderRadius: 4 }} />
                  </td>
                </tr>
              ))}
              
            {filteredRows.map((row) => (
              <tr key={row.trade_id}>
                <td style={{ fontWeight: 500 }}>{row.market_question}</td>
                <td className="text-right mono" style={{ color: 'var(--accent-success)' }}>
                  {usd.format(row.value_usd)}
                </td>
                <td>
                  <div className="flex-row">
                    <span className="mono subtle" title={row.maker_address}>
                      {row.maker_address.slice(0, 6)}...{row.maker_address.slice(-4)}
                    </span>
                    <button
                      className="btn btn-outline"
                      style={{ height: 24, padding: '0 8px', fontSize: 11 }}
                      onClick={() => handleCopy(row.trade_id, row.maker_address)}
                    >
                      {copiedId === row.trade_id ? "Copied" : "Copy"}
                    </button>
                  </div>
                </td>
                <td className="text-right subtle">
                  {new Date(row.timestamp).toLocaleString()}
                </td>
              </tr>
            ))}
            
            {!loading && filteredRows.length === 0 && (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', padding: 40, color: 'var(--text-secondary)' }}>
                  No results found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="toolbar" style={{ justifyContent: 'center', gap: 20 }}>
        <button
          className="btn btn-outline"
          onClick={() => setOffset((value) => Math.max(0, value - limit))}
          disabled={loading || offset === 0}
        >
          Previous
        </button>
        <span className="subtle mono">Offset {offset}</span>
        <button
          className="btn btn-outline"
          onClick={() => setOffset((value) => value + limit)}
          disabled={loading || !hasMore}
        >
          Load more
        </button>
      </div>
    </div>
  )
}
