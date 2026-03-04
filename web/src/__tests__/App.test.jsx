import { render, screen } from "@testing-library/react"
import { MemoryRouter } from "react-router-dom"
import App from "../App"

test("renders topbar and navigation links", () => {
  render(
    <MemoryRouter initialEntries={["/"]}>
      <App />
    </MemoryRouter>
  )
  expect(screen.getByText("PolyPulse")).toBeInTheDocument()
  // "Credibility" appears in Nav and Page Title
  expect(screen.getAllByText("Credibility").length).toBeGreaterThan(0)
  expect(screen.getByText("Delivery")).toBeInTheDocument()
  expect(screen.getByText("Whale Radar")).toBeInTheDocument()
  expect(screen.getByText("Smart Money")).toBeInTheDocument()
  expect(screen.getByText("Trade Feed")).toBeInTheDocument()
  expect(screen.getByRole("button", { name: /Sync/i })).toBeInTheDocument()
})
