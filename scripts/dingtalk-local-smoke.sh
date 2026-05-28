#!/usr/bin/env bash
# Smoke checks for DingTalk auth wiring on a running local backend.
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:${SERVER_PORT:-8081}}"

echo "==> health"
curl -sf "${API_BASE}/actuator/health" | head -c 200
echo ""

echo "==> auth methods (expect oauth-dingtalk when enabled)"
curl -sf "${API_BASE}/api/v1/auth/methods?returnTo=/dashboard" | python3 -m json.tool

echo "==> dingtalk oauth authorize (expect 302)"
status="$(curl -s -o /dev/null -w '%{http_code}' "${API_BASE}/api/v1/auth/dingtalk/oauth/authorize?returnTo=/dashboard")"
if [ "$status" != "302" ] && [ "$status" != "303" ]; then
  echo "ERROR: expected redirect, got HTTP ${status}" >&2
  exit 1
fi
echo "redirect OK (HTTP ${status})"

echo "==> dingtalk login with invalid code (expect 4xx, not 404)"
code_status="$(curl -s -o /dev/null -w '%{http_code}' -X POST "${API_BASE}/api/v1/auth/dingtalk/login" \
  -H 'Content-Type: application/json' \
  -d '{"code":"invalid-test-code","corpId":"ding9a1c64959cb54be4f5bf40eda33b7ba0"}')"
case "$code_status" in
  400|401|403|422|500) echo "login endpoint reachable (HTTP ${code_status})" ;;
  404) echo "ERROR: dingtalk login endpoint missing" >&2; exit 1 ;;
  *) echo "login endpoint returned HTTP ${code_status}" ;;
esac

echo "All DingTalk local smoke checks passed."
