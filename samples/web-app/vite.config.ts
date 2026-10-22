import { defineConfig } from "vite";
import path from "path";

export default defineConfig({
  resolve: {
    alias: {
      "@chaevi/analytics-web-sdk": path.resolve(__dirname, "../../sdk/analytics-web/src/index.ts"),
    },
  },
});
