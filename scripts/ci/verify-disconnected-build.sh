#!/usr/bin/env bash

set -Eeuo pipefail

build_config="app/build/generated/source/buildConfig/workforceStaging/br/com/tresvtintas/mobile/app/BuildConfig.java"
staging_apk="app/build/outputs/apk/staging/app-staging.apk"
workforce_apk="app/build/outputs/apk/workforceStaging/app-workforceStaging.apk"

for path in "${build_config}" "${staging_apk}" "${workforce_apk}"; do
  if [[ ! -f "${path}" ]]; then
    echo "Expected disconnected build output was not generated: ${path}" >&2
    exit 80
  fi
done

grep -Eq 'MOBILE_API_CONFIGURED[[:space:]]*=[[:space:]]*false' "${build_config}"
grep -Eq 'FIREBASE_CONFIGURED[[:space:]]*=[[:space:]]*false' "${build_config}"
grep -Fq 'https://api.3vtintas.invalid/api/mobile/v1/' "${build_config}"

production_api_base='https://www.3vtintas.com.br/api/mobile'
production_api_base+='/v1/'
if grep -Fq "${production_api_base}" "${build_config}"; then
  echo "Disconnected public APK unexpectedly contains the production API base URL." >&2
  exit 81
fi

echo "Disconnected Android artifacts: PASS"
