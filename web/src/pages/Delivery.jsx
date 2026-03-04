import { useEffect, useMemo, useState } from "react"
import { fetchDeliveryObservability } from "../api"

function formatPercent(value) {
  return `${(value * 100).toFixed(1)}%`
}

function formatSeconds(value) {
  if (value === null || value === undefined) return "—"
  if (value < 60) return `${value}s`
  const m = Math.floor(value / 60)
  const s = value % 60
  return `${m}m ${s}s`
}

function StatRow({ label, value, hint }) {
  return (
    <div className="metric">
      <div className="k">{label}</div>
      <div className="v">{value}</div>
      <div className="s">{hint}</div>
    </div>
  )
}

function WindowCard({ title, window }) {
  const delivered = window.sent
  const failures = window.failed
  const attempts = window.attemptsTotal
  const gated = window.delayed + window.disabled
  const notReachable = window.noTokens

  return (
    <section className="card">
      <div className="card-head">
        <div className="card-title">{title}</div>
        <div className="chip">
          Success <strong>{formatPercent(window.successRate)}</strong>
        </div>
      </div>

      <div className="metrics">
        <StatRow
          label="Delivered"
          value={delivered}
          hint={`${attempts} attempts · ${failures} failed`}
        />
        <StatRow
          label="CTR (opens)"
          value={formatPercent(window.clickThroughRate)}
          hint={`${window.pushOpenCount} opens · based on push_open`}
        />
        <StatRow
          label="Queue delay"
          value={formatSeconds(window.queueDelayP50Seconds)}
          hint={`P90 ${formatSeconds(window.queueDelayP90Seconds)}`}
        />
        <StatRow
          label="Dispatch delay"
          value={formatSeconds(window.dispatchDelayP50Seconds)}
          hint={`P90 ${formatSeconds(window.dispatchDelayP90Seconds)}`}
        />
      </div>

      <div className="split">
        <div className="section-title">Breakdown</div>
        <div className="rows">
          <div className="rowline">
            <span className="muted">Queued</span>
            <span className="value">{window.queued}</span>
          </div>
          <div className="rowline">
            <span className="muted">Delayed (tier)</span>
            <span className="value">{window.delayed}</span>
          </div>
          <div className="rowline">
            <span className="muted">Disabled</span>
            <span className="value">{window.disabled}</span>
          </div>
          <div className="rowline">
            <span className="muted">No tokens</span>
            <span className="value">{notReachable}</span>
          </div>
          <div className="rowline">
            <span className="muted">Gated total</span>
            <span className="value">{gated}</span>
          </div>
        </div>
      </div>
    </section>
  )
}

export default function Delivery({ refreshKey }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [data, setData] = useState(null)

  const queue = useMemo(() => {
    if (!data) return null
    return {
      depth: data.redisQueueDepth,
      oldestDueSeconds: data.redisOldestDueSeconds
    }
  }, [data])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError("")
    fetchDeliveryObservability()
      .then((payload) => {
        if (cancelled) return
        setData(payload)
      })
      .catch((e) => {
        if (cancelled) return
        setError(e.message || "Failed to load")
      })
      .finally(() => {
        if (cancelled) return
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [refreshKey])

  return (
    <div className="panel">
      <div className="toolbar" style={{ justifyContent: 'space-between' }}>
        <div className="subtle">Latency, success, retries, opens</div>
        <div className="chip">
          Queue <strong>{queue?.depth ?? "—"}</strong>
        </div>
      </div>

      {queue?.oldestDueSeconds !== null && queue?.oldestDueSeconds !== undefined && (
        <div className="notice">
          Oldest due delta <strong>{formatSeconds(Math.max(0, queue.oldestDueSeconds))}</strong>
        </div>
      )}

      {error && <div className="notice error">{error}</div>}

      {loading && !data && (
        <div className="grid">
          <div className="card skeleton" style={{ height: 360 }} />
          <div className="card skeleton" style={{ height: 360 }} />
        </div>
      )}

      {data && (
        <div className="grid">
          <WindowCard title="Last 24 hours" window={data.window1d} />
          <WindowCard title="Last 7 days" window={data.window7d} />
        </div>
      )}
    </div>
  )
}
