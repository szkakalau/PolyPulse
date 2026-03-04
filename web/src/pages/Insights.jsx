import { useEffect, useMemo, useState } from "react"
import { fetchSignalCredibility } from "../api"

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

function Histogram({ items }) {
  const maxCount = useMemo(() => Math.max(1, ...items.map((i) => i.count)), [items])
  return (
    <div className="hist">
      {items.map((item) => (
        <div className="hist-row" key={item.bucket}>
          <div className="hist-label">{item.bucket}</div>
          <div className="hist-track" aria-hidden="true">
            <div className="hist-bar" style={{ width: `${(item.count / maxCount) * 100}%` }} />
          </div>
          <div className="hist-count">{item.count}</div>
        </div>
      ))}
    </div>
  )
}

function WindowCard({ title, window }) {
  const ciWidth = window.hitRateCiHigh - window.hitRateCiLow
  const confidenceTag =
    window.evaluatedTotal >= 30 && ciWidth <= 0.25
      ? "High confidence"
      : window.evaluatedTotal >= 10
        ? "Medium confidence"
        : "Low confidence"
  const leadHint = window.leadCount > 0 ? `P90 ${formatSeconds(window.leadP90Seconds)}` : "No lead samples"

  return (
    <section className="card">
      <div className="card-head">
        <div className="card-title">{title}</div>
        <div className="chip">
          Confidence <strong>{confidenceTag}</strong>
        </div>
      </div>

      <div className="metrics">
        <div className="metric">
          <div className="k">Hit rate</div>
          <div className="v">{formatPercent(window.hitRate)}</div>
          <div className="s">
            95% CI {formatPercent(window.hitRateCiLow)}–{formatPercent(window.hitRateCiHigh)}
          </div>
        </div>

        <div className="metric">
          <div className="k">Sample size</div>
          <div className="v">{window.evaluatedTotal}</div>
          <div className="s">{window.signalsTotal} signals in window</div>
        </div>

        <div className="metric">
          <div className="k">Evidence coverage</div>
          <div className="v">{formatPercent(window.evidenceRate)}</div>
          <div className="s">{window.signalsWithEvidence} with evidence</div>
        </div>

        <div className="metric">
          <div className="k">Signal latency</div>
          <div className="v">{formatSeconds(window.latencyP50Seconds)}</div>
          <div className="s">P90 {formatSeconds(window.latencyP90Seconds)}</div>
        </div>

        <div className="metric">
          <div className="k">Lead time</div>
          <div className="v">{formatSeconds(window.leadP50Seconds)}</div>
          <div className="s">{leadHint}</div>
        </div>
      </div>

      <div className="split">
        <div className="section-title">Latency distribution</div>
        <Histogram items={window.latencyHistogram} />
      </div>

      <div className="split">
        <div className="section-title">Lead distribution</div>
        <Histogram items={window.leadHistogram} />
      </div>
    </section>
  )
}

export default function Insights({ refreshKey }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [data, setData] = useState(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError("")
    fetchSignalCredibility()
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
        <div className="subtle">7/30-day proof: hit rate, evidence, latency, lead time, sample size</div>
        <div className="chip">
          Status <strong>{loading ? "Computing" : "Live"}</strong>
        </div>
      </div>

      {error && <div className="notice error">{error}</div>}

      {loading && !data && (
        <div className="grid">
          <div className="card skeleton" style={{ height: 360 }} />
          <div className="card skeleton" style={{ height: 360 }} />
        </div>
      )}

      {data && (
        <div className="grid">
          <WindowCard title="Last 7 days" window={data.window7d} />
          <WindowCard title="Last 30 days" window={data.window30d} />
        </div>
      )}
    </div>
  )
}
