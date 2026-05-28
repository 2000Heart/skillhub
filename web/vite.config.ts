import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { resolveDevApiPort } from './dev-api-port'

const LEGACY_BROWSER_TARGETS = ['chrome83', 'edge83', 'firefox78', 'safari14']
const devApiPort = resolveDevApiPort()
const devApiTarget = `http://localhost:${devApiPort}`

const DINGTALK_H5_REMOTE_DEBUG_SCRIPT =
  "https://g.alicdn.com/code/npm/@ali/dingtalk-h5-remote-debug/0.1.3/index.js"

/** Injects DingTalk H5 remote debug helper during `pnpm run dev` (not production builds). */
function dingtalkH5RemoteDebugPlugin() {
  return {
    name: 'dingtalk-h5-remote-debug',
    apply: 'serve' as const,
    transformIndexHtml(html: string) {
      const tag = `<script src="${DINGTALK_H5_REMOTE_DEBUG_SCRIPT}"></script>`
      return html.replace('</head>', `    ${tag}\n  </head>`)
    },
  }
}

export default defineConfig({
  plugins: [
    react(),
    dingtalkH5RemoteDebugPlugin(),
    {
      name: 'skillhub-dev-api-proxy-log',
      configureServer(server) {
        server.httpServer?.once('listening', () => {
          // eslint-disable-next-line no-console
          console.log(`[skillhub] Vite API proxy -> ${devApiTarget}`)
        })
      },
    },
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    target: LEGACY_BROWSER_TARGETS,
    cssTarget: LEGACY_BROWSER_TARGETS,
  },
  optimizeDeps: {
    esbuildOptions: {
      target: LEGACY_BROWSER_TARGETS,
    },
  },
  test: {
    exclude: ['**/node_modules/**', '**/e2e/**'],
  },
  server: {
    port: 3000,
    // Allow ngrok / Cloudflare Tunnel hostnames when testing DingTalk callbacks locally.
    allowedHosts: ['.ngrok-free.dev', '.ngrok-free.app', '.ngrok.io', '.trycloudflare.com'],
    watch: {
      usePolling: true,
      interval: 150,
    },
    proxy: {
      '/api': {
        target: devApiTarget,
        changeOrigin: true,
      },
      '/oauth2': {
        target: devApiTarget,
        changeOrigin: true,
      },
    },
  },
})
