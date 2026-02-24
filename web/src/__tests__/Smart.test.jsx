import { render, screen, waitFor } from "@testing-library/react"
import Smart from "../pages/Smart"

vi.mock("../api", () => ({
  fetchSmartWallets: vi.fn().mockResolvedValue([
    {
      address: "0xsmart",
      profit: 100.5,
      win_rate: 0.5,
      roi: 0.12,
      total_trades: 7
    }
  ])
}))

test("renders smart header and one row", async () => {
  render(<Smart />)
  expect(screen.getByText("Smart Money")).toBeInTheDocument()
  await waitFor(() => {
    expect(screen.getByText("0xsmart")).toBeInTheDocument()
  })
})
