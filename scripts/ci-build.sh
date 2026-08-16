#!/usr/bin/env bash
set -euo pipefail

: "${BUILD_ID:=}"
: "${CALLBACK_URL:=}"
: "${SOURCE_URL:=}"
: "${APP_NAME:=IM Any Builder}"
: "${PACKAGE_NAME:=com.imanybuilder.app}"
: "${VERSION_NAME:=1.0.0}"
: "${VERSION_CODE:=1}"
: "${ORIENTATION:=Auto}"
: "${WEBVIEW_MODE:=Offline}"
: "${IM_BRANDING:=mandatory}"
: "${WORKER_SECRET:=}"

notify() {
  local status="$1"
  local extra="${2:-}"
  if [[ -n "$CALLBACK_URL" && -n "$BUILD_ID" && -n "$WORKER_SECRET" ]]; then
    local payload="{\"build_id\":\"$BUILD_ID\",\"status\":\"$status\"$extra}"
    curl --fail --silent --show-error --retry 2 -X POST "$CALLBACK_URL" \
      -H "Content-Type: application/json" \
      -H "Content-Length: ${#payload}" \
      -H "X-Worker-Secret: $WORKER_SECRET" \
      --data "$payload" || true
  fi
}

failure_handler() {
  notify failed ',\"error_message\":\"GitHub Actions Android build failed\"'
}
trap failure_handler ERR
notify building

if [[ -n "$SOURCE_URL" ]]; then
  case "$SOURCE_URL" in http://*|https://*) ;; *) echo "source_url must use http or https" >&2; exit 1;; esac
  curl --fail --location --max-time 120 --proto '=https' --proto-redir '=https' "$SOURCE_URL" --output source-payload
  test "$(wc -c < source-payload)" -le 26214400
  if printf '%s' "$SOURCE_URL" | grep -Eiq '\.html?(\?|$)'; then
    cp source-payload app/src/main/assets/index.html
  else
    SOURCE_ARCHIVE=source-payload node scripts/prepare-web-project.mjs
  fi
else
  node scripts/prepare-web-project.mjs
fi

printf '%s' "$IM_BRANDING" | grep -qx mandatory
case "$ORIENTATION" in Portrait|Landscape|Auto) ;; *) echo "invalid orientation" >&2; exit 1;; esac
case "$WEBVIEW_MODE" in Online|Offline) ;; *) echo "invalid WebView mode" >&2; exit 1;; esac
[[ "$VERSION_CODE" =~ ^[0-9]+$ ]]

if [[ -z "${ANDROID_KEYSTORE_PATH:-}" || -z "${ANDROID_KEYSTORE_PASSWORD:-}" || -z "${ANDROID_KEY_ALIAS:-}" || -z "${ANDROID_KEY_PASSWORD:-}" ]]; then
  export ANDROID_KEYSTORE_PATH="${RUNNER_TEMP:-/tmp}/im-any-builder-test.keystore"
  export ANDROID_KEYSTORE_PASSWORD="$(openssl rand -hex 24)"
  export ANDROID_KEY_ALIAS="im-any-builder-test"
  export ANDROID_KEY_PASSWORD="$(openssl rand -hex 24)"
  keytool -genkeypair -noprompt -keystore "$ANDROID_KEYSTORE_PATH" \
    -storetype JKS -storepass "$ANDROID_KEYSTORE_PASSWORD" \
    -keypass "$ANDROID_KEY_PASSWORD" -alias "$ANDROID_KEY_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 365 \
    -dname "CN=IM Any Builder Test, OU=IM, O=IM Any Builder, L=Dhaka, ST=Dhaka, C=BD" >/dev/null 2>&1
fi

gradle --no-daemon --stacktrace :app:assembleRelease \
  -PappName="$APP_NAME" \
  -PpackageName="$PACKAGE_NAME" \
  -PversionName="$VERSION_NAME" \
  -PversionCode="$VERSION_CODE" \
  -Porientation="$ORIENTATION" \
  -PwebViewMode="$WEBVIEW_MODE" \
  -PimBranding="$IM_BRANDING"

if [[ -n "$CALLBACK_URL" && -n "$BUILD_ID" && -n "$WORKER_SECRET" ]]; then
  artifact_url="${CALLBACK_URL%/callback}/artifact"
  response_file="${RUNNER_TEMP:-/tmp}/artifact-response.json"
  artifact_size="$(stat -c '%s' app/build/outputs/apk/release/app-release.apk)"
  curl --fail --silent --show-error --retry 2 -X POST "$artifact_url" \
    -H "Content-Type: application/vnd.android.package-archive" \
    -H "Content-Length: $artifact_size" \
    -H "X-Worker-Secret: $WORKER_SECRET" \
    -H "X-Build-Id: $BUILD_ID" \
    --data-binary @app/build/outputs/apk/release/app-release.apk > "$response_file"
  test "$(jq -r '.accepted' "$response_file")" = true
  artifact_key="$(jq -r '.artifact_key' "$response_file")"
  artifact_url="$(jq -r '.artifact_url' "$response_file")"
  notify ready ",\"artifact_key\":\"$artifact_key\",\"artifact_url\":\"$artifact_url\""
fi

trap - ERR

