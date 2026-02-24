import { render, screen } from "@testing-library/react"
import { MemoryRouter } from "react-router-dom"
import App from "../App"

test("renders topbar and navigation links", () => {
  render(
    <MemoryRouter initialEntries={["/"]}>
      <App />
    </MemoryRouter>
  )
  expect(screen.getByText("PolyPulse Admin")).toBeInTheDocument()
  expect(screen.getByText("Whales")).toBeInTheDocument()
  expect(screen.getByText("Smart Money")).toBeInTheDocument()
  expect(screen.getByText("Trades")).toBeInTheDocument()
  expect(screen.getByRole("button", { name: /Refresh/i })).toBeInTheDocument()
})
