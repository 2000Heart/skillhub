#!/usr/bin/env bash
# Start ngrok for local DingTalk testing and sync SKILLHUB_PUBLIC_BASE_URL into .dev/dingtalk.env.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_PORT="${LOCAL_PORT:-3000}"
DINGTALK_ENV="${ROOT}/.dev/dingtalk.env"
NGROK_API="http://127.0.0.1:4040/api/tunnels"

if ! command -v ngrok >/dev/null 2>&1; then
  echo "ERROR: ngrok not found. Install: brew install ngrok/ngrok/ngrok" >&2
  exit 1
fi

if [ ! -f "$DINGTALK_ENV" ]; then
  echo "ERROR: missing $DINGTALK_ENV" >&2
  exit 1
fi

if ! curl -sf "http://127.0.0.1:${LOCAL_PORT}/" >/dev/null 2>&1; then
  echo "WARN: nothing listening on http://127.0.0.1:${LOCAL_PORT}/ — start frontend first (make dev-web)" >&2
fi

if ! curl -sf "$NGROK_API" >/dev/null 2>&1; then
  echo "Starting ngrok http ${LOCAL_PORT} ..."
  nohup ngrok http "${LOCAL_PORT}" >/dev/null 2>&1 &
  for _ in $(seq 1 20); do
    curl -sf "$NGROK_API" >/dev/null 2>&1 && break
    sleep 1
  done
fi

public_url="$(
  curl -sf "$NGROK_API" | python3 -c "
import json, sys
data = json.load(sys.stdin)
for tunnel in data.get('tunnels', []):
    url = tunnel.get('public_url', '')
    if url.startswith('https://'):
        print(url.rstrip('/'))
        break
"
)"

if [ -z "$public_url" ]; then
  echo "ERROR: could not read ngrok HTTPS URL. Is ngrok authenticated? Run: ngrok config add-authtoken <token>" >&2
  exit 1
fi

tmp="$(mktemp)"
if grep -q '^SKILLHUB_PUBLIC_BASE_URL=' "$DINGTALK_ENV"; then
  sed "s|^SKILLHUB_PUBLIC_BASE_URL=.*|SKILLHUB_PUBLIC_BASE_URL=${public_url}|" "$DINGTALK_ENV" >"$tmp"
else
  cat "$DINGTALK_ENV" >"$tmp"
  printf '\nSKILLHUB_PUBLIC_BASE_URL=%s\n' "$public_url" >>"$tmp"
fi
mv "$tmp" "$DINGTALK_ENV"

runtime_cfg="${ROOT}/web/public/runtime-config.js"
if [ -f "$runtime_cfg" ]; then
  sed -i '' "s|appBaseUrl: '[^']*'|appBaseUrl: '${public_url}'|" "$runtime_cfg" 2>/dev/null \
    || sed -i "s|appBaseUrl: '[^']*'|appBaseUrl: '${public_url}'|" "$runtime_cfg"
fi

callback="${public_url}/api/v1/auth/dingtalk/oauth/callback"
echo ""
echo "ngrok public URL:  ${public_url}"
echo "OAuth callback:    ${callback}"
echo "H5 safe domain:    $(echo "$public_url" | sed -E 's#https?://##; s#/.*##')"
echo "App homepage:      ${public_url}/"
echo ""
echo "Updated: ${DINGTALK_ENV}"
echo "Restart backend:   make dev-server-restart"
echo "Restart frontend:  make dev-web   (after vite.config allowedHosts change)"
echo ""
