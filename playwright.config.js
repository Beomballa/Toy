import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  expect: {
    timeout: 8_000,
    toHaveScreenshot: {
      animations: "disabled",
      maxDiffPixelRatio: 0.01
    }
  },
  use: {
    baseURL: process.env.FRONT_URL || "http://127.0.0.1:8080",
    locale: "ko-KR",
    timezoneId: "Asia/Seoul",
    trace: "retain-on-failure"
  },
  webServer: {
    command: "./gradlew :Front:bootRun",
    url: "http://127.0.0.1:8080/health/live",
    reuseExistingServer: true,
    timeout: 120_000
  },
  projects: [
    {
      name: "desktop-chromium",
      use: {
        ...devices["Desktop Chrome"],
        viewport: { width: 1280, height: 900 }
      }
    },
    {
      name: "mobile-chromium",
      use: {
        ...devices["Pixel 7"]
      }
    }
  ]
});
