import { render, screen, waitFor } from "@testing-library/react"
import Whales from "../pages/Whales"

vi.mock("../api", () => ({
  fetchWhales: vi.fn().mockResolvedValue([
    {
      trade_id: "t1",
      market_question: "Will ABC happen?",
      value_usd: 123.45,
      maker_address: "0xabc",
      timestamp: new Date().toISOString()
    }
  ])
}))

test("renders whales header and one row", async () => {
  render(<Whales />)
  expect(screen.getByText("Whale Trades")).toBeInTheDocument()
  await waitFor(() => {
    expect(screen.getByText("Will ABC happen?")).toBeInTheDocument()
  })
})
