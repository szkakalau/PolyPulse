import { render, screen, waitFor } from "@testing-library/react"
import Trades from "../pages/Trades"

vi.mock("../api", () => ({
  fetchTrades: vi.fn().mockResolvedValue([
    {
      id: "tr-1",
      market_question: "Market Q",
      side: "BUY",
      value: 88.8,
      address: "0x123",
      timestamp: new Date().toISOString()
    }
  ])
}))

test("renders trades header and one row", async () => {
  render(<Trades />)
  expect(screen.getByText("Trades")).toBeInTheDocument()
  await waitFor(() => {
    expect(screen.getByText("Market Q")).toBeInTheDocument()
  })
})
