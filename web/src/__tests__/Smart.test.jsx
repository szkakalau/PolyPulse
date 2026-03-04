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

test("renders smart table and one row", async () => {
  render(<Smart />)
  // Header removed, check table column
  expect(screen.getByText("Profit")).toBeInTheDocument()
  await waitFor(() => {
    // Address might be truncated, check by title attribute which contains full address
    expect(screen.getByTitle("0xsmart")).toBeInTheDocument()
  })
})
