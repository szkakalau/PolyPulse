import { useEffect, useMemo, useRef, useState } from "react"
import { Link, useParams } from "react-router-dom"
import { fetchSignalDetail } from "../api"

function upsertMeta(attr, key, content) {
  const selector = `meta[${attr}="${key}"]`
  let el = document.querySelector(selector)
  if (!el) {
    el = document.createElement("meta")
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute("content", content)
}

export default function SignalLanding() {
  const { signalId } = useParams()
  const [signal, setSignal] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [copied, setCopied] = useState(false)
  const [openHint, setOpenHint] = useState("")
  const [showInstallPrompt, setShowInstallPrompt] = useState(false)
  const openTimerRef = useRef(null)

  const createdAt = useMemo(() => {
    if (!signal?.createdAt) return ""
    const date = new Date(signal.createdAt)
    if (Number.isNaN(date.getTime())) return signal.createdAt
    return date.toLocaleString()
  }, [signal])

  useEffect(() => {
    if (!signalId) return
    let cancelled = false
    setLoading(true)
    setError("")
    fetchSignalDetail(signalId)
      .then((data) => {
        if (cancelled) return
        setSignal(data)
        const title = data?.title ? `${data.title} | PolyPulse Signal` : `Signal ${signalId} | PolyPulse`
        const description = data?.locked
          ? "Signal preview on PolyPulse. Unlock full details in the app."
          : `Signal details: ${data.title || "PolyPulse Signal"}`
        document.title = title
        upsertMeta("name", "description", description)
        upsertMeta("property", "og:title", title)
        upsertMeta("property", "og:description", description)
        upsertMeta("property", "og:type", "article")
        upsertMeta("property", "og:url", window.location.href)
        upsertMeta("name", "twitter:card", "summary")
        upsertMeta("name", "twitter:title", title)
        upsertMeta("name", "twitter:description", description)
      })
      .catch((e) => {
        if (cancelled) return
        setError(e.message || "Failed to load signal")
      })
      .finally(() => {
        if (cancelled) return
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [signalId])

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1000)
    } catch {
      setCopied(false)
    }
  }

  const handleOpenApp = () => {
    if (!signalId) return
    setOpenHint("Opening PolyPulse…")
    if (openTimerRef.current) {
      window.clearTimeout(openTimerRef.current)
    }
    openTimerRef.current = window.setTimeout(() => {
      setOpenHint("If the app didn’t open, install PolyPulse and try again.")
      setShowInstallPrompt(true)
      window.location.href = "https://play.google.com/store/apps/details?id=com.polypulse.app"
    }, 1200)
    window.location.href = `polypulse://signals/${signalId}`
  }

  return (
    <div className="landing-shell">
      <header className="landing-header">
        <Link className="landing-brand" to="/insights">
          <span className="brand-mark" />
          PolyPulse
        </Link>
        <div className="landing-actions">
          <button className="btn btn-outline" onClick={handleCopy}>
            {copied ? "Copied" : "Copy Link"}
          </button>
          <button className="btn btn-primary" onClick={handleOpenApp}>
            Open App
          </button>
        </div>
        {openHint && <div className="landing-hint">{openHint}</div>}
      </header>
      <main className="landing-main">
        <section className="landing-card">
          {error && <div className="notice error">{error}</div>}
          {loading && (
            <div className="landing-loading">
              <div className="skeleton" style={{ height: 24, marginBottom: 12 }} />
              <div className="skeleton" style={{ height: 120 }} />
            </div>
          )}
          {!loading && signal && (
            <>
              <div className="landing-meta">
                <span className={`chip ${signal.locked ? "warning" : "success"}`}>
                  {signal.locked ? "Locked" : "Unlocked"}
                </span>
                {createdAt && <span className="subtle">Updated {createdAt}</span>}
              </div>
              <h1 className="landing-title">{signal.title}</h1>
              <p className="landing-description">
                {signal.locked
                  ? "Unlock the full signal analysis, rationale, and execution plan in the app."
                  : signal.content || "Open the app for the full trade context and alerts."}
              </p>
              {signal.evidence && (
                <div className="landing-evidence">
                  <div className="landing-evidence-title">Evidence</div>
                  <div className="landing-evidence-grid">
                    <div className="landing-evidence-item">
                      <div className="subtle">Source</div>
                      <div>{signal.evidence.sourceType}</div>
                    </div>
                    <div className="landing-evidence-item">
                      <div className="subtle">Triggered</div>
                      <div>{new Date(signal.evidence.triggeredAt).toLocaleString()}</div>
                    </div>
                    <div className="landing-evidence-item">
                      <div className="subtle">Market</div>
                      <div className="mono">{signal.evidence.marketId}</div>
                    </div>
                    <div className="landing-evidence-item">
                      <div className="subtle">Wallet</div>
                      <div className="mono">{signal.evidence.makerAddress}</div>
                    </div>
                  </div>
                  {signal.evidence.evidenceUrl && (
                    <a className="landing-link" href={signal.evidence.evidenceUrl} target="_blank" rel="noreferrer">
                      View source data
                    </a>
                  )}
                </div>
              )}
            </>
          )}
        </section>
        <aside className="landing-sidebar">
          <div className="landing-cta">
            <div className="landing-cta-title">Get real-time signal delivery</div>
            <div className="subtle">
              Unlock the full signal, push alerts, and portfolio context inside PolyPulse.
            </div>
            <button className="btn btn-primary" onClick={handleOpenApp}>
              Open in App
            </button>
            <Link className="btn btn-outline" to="/insights">
              View credibility dashboard
            </Link>
          </div>
          {showInstallPrompt && (
            <div className="landing-install">
              <div className="landing-install-title">App not installed?</div>
              <div className="subtle">Install PolyPulse or copy the link to open later.</div>
              <div className="landing-install-actions">
                <a
                  className="btn btn-primary"
                  href="https://play.google.com/store/apps/details?id=com.polypulse.app"
                  target="_blank"
                  rel="noreferrer"
                >
                  Open Play Store
                </a>
                <button className="btn btn-outline" onClick={handleCopy}>
                  {copied ? "Copied" : "Copy Link"}
                </button>
              </div>
            </div>
          )}
          <div className="landing-stats">
            <div className="landing-stat">
              <div className="subtle">Signal ID</div>
              <div className="mono">{signalId}</div>
            </div>
            <div className="landing-stat">
              <div className="subtle">Tier required</div>
              <div>{signal?.tierRequired || "free"}</div>
            </div>
          </div>
        </aside>
      </main>
    </div>
  )
}
