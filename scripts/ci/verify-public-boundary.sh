#!/usr/bin/env bash

set -Eeuo pipefail

forbidden_paths_regex='(^|/)(google-services\.json|local\.properties|.*\.(jks|keystore|p12|pfx|pem|key)|\.env($|\.)|service-account.*\.json)$'
forbidden_paths="$(git ls-files | grep -Ei "${forbidden_paths_regex}" || true)"
if [[ -n "${forbidden_paths}" ]]; then
  echo "Public snapshot contains forbidden credential or signing paths:" >&2
  printf '%s\n' "${forbidden_paths}" >&2
  exit 70
fi

secret_value_regex='-----BEGIN ([A-Z0-9 ]+ )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9_]{20,}|AIza[0-9A-Za-z_-]{35}|AKIA[0-9A-Z]{16}|sk-(proj-)?[A-Za-z0-9_-]{20,}'
secret_files="$(git grep -Il -E -e "${secret_value_regex}" -- . || true)"
if [[ -n "${secret_files}" ]]; then
  echo "Public snapshot contains credential-shaped values in:" >&2
  printf '%s\n' "${secret_files}" >&2
  exit 71
fi

password_assignment_regex='(storePassword|keyPassword|store_password|key_password)[[:space:]]*=[[:space:]]*["'"'][^"'"']+["'"']'
password_files="$(git grep -Il -E -e "${password_assignment_regex}" -- . || true)"
if [[ -n "${password_files}" ]]; then
  echo "Public snapshot contains signing password assignments in:" >&2
  printf '%s\n' "${password_files}" >&2
  exit 72
fi

production_api_base='https://www.3vtintas.com.br/api/mobile'
production_api_base+='/v1/'
if grep -Rqs -F \
    --exclude-dir=build \
    --exclude-dir=test \
    --exclude-dir=androidTest \
    --exclude-dir=physicalPilotAndroidTest \
    --include='*.java' \
    --include='*.kt' \
    --include='*.gradle' \
    --include='*.xml' \
    "${production_api_base}" \
    app core data feature platform lab gradle; then
  echo "The production mobile API base URL must not be embedded in public source." >&2
  exit 73
fi

if grep -Rqs --include='*.yml' --include='*.yaml' 'self-hosted' .github/workflows; then
  echo "A public repository must never dispatch untrusted code to a self-hosted runner." >&2
  exit 74
fi

if grep -Rqs --include='*.yml' --include='*.yaml' 'pull_request_target' .github/workflows; then
  echo "pull_request_target is forbidden for the public Android repository." >&2
  exit 75
fi

if grep -Rqs --include='*.yml' --include='*.yaml' 'actions/upload-artifact' .github/workflows; then
  echo "Public CI must not publish workforce or staging APK artifacts." >&2
  exit 76
fi

grep -Fq \
  'https://api.3vtintas.invalid/api/mobile/v1/' \
  app/build.gradle

echo "Public repository boundary: PASS"
