#!/bin/sh
set -eu

: "${SKILLHUB_WEB_API_BASE_URL:=}"
: "${SKILLHUB_PUBLIC_BASE_URL:=}"
: "${SKILLHUB_WEB_AUTH_DIRECT_ENABLED:=false}"
: "${SKILLHUB_WEB_AUTH_DIRECT_PROVIDER:=}"

# Session-bootstrap variables are defaulted here so envsubst writes
# `authSessionBootstrapEnabled: "false"` into runtime-config.js instead of leaving
# the literal `${...}` placeholder. They are intentionally NOT exposed in
# compose.release.yml or .env.release.example: the matching server-side switch
# does not exist yet, so surfacing the toggle would let the frontend hit
# /api/v1/auth/session/bootstrap and receive 403. See PR #280 discussion.
: "${SKILLHUB_WEB_AUTH_SESSION_BOOTSTRAP_ENABLED:=false}"
: "${SKILLHUB_WEB_AUTH_SESSION_BOOTSTRAP_PROVIDER:=}"
: "${SKILLHUB_WEB_AUTH_SESSION_BOOTSTRAP_AUTO:=false}"

# DingTalk runtime switches (for enterprise web-app login).
: "${SKILLHUB_WEB_AUTH_MODE:=standard}"
: "${SKILLHUB_WEB_DINGTALK_AUTH_ENABLED:=false}"
: "${SKILLHUB_WEB_DINGTALK_CLIENT_ID:=}"
: "${SKILLHUB_WEB_DINGTALK_AUTO_LOGIN:=true}"
: "${SKILLHUB_WEB_DINGTALK_DEFAULT_CORP_ID:=${SKILLHUB_DINGTALK_DEFAULT_CORP_ID:-}}"

# Generate runtime-config.js
envsubst '${SKILLHUB_WEB_API_BASE_URL} ${SKILLHUB_PUBLIC_BASE_URL} ${SKILLHUB_WEB_AUTH_DIRECT_ENABLED} ${SKILLHUB_WEB_AUTH_DIRECT_PROVIDER} ${SKILLHUB_WEB_AUTH_SESSION_BOOTSTRAP_ENABLED} ${SKILLHUB_WEB_AUTH_SESSION_BOOTSTRAP_PROVIDER} ${SKILLHUB_WEB_AUTH_SESSION_BOOTSTRAP_AUTO} ${SKILLHUB_WEB_AUTH_MODE} ${SKILLHUB_WEB_DINGTALK_AUTH_ENABLED} ${SKILLHUB_WEB_DINGTALK_CLIENT_ID} ${SKILLHUB_WEB_DINGTALK_AUTO_LOGIN} ${SKILLHUB_WEB_DINGTALK_DEFAULT_CORP_ID}' \
  < /usr/share/nginx/html/runtime-config.js.template \
  > /usr/share/nginx/html/runtime-config.js

# Generate registry/skill.md with actual public URL
envsubst '${SKILLHUB_PUBLIC_BASE_URL}' \
  < /usr/share/nginx/html/registry/skill.md.template \
  > /usr/share/nginx/html/registry/skill.md
