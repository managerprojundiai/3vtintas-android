# 3V Tintas Android

This repository is the auditable Android client source for the 3V Tintas platform.
The backend, database, production infrastructure, signing material, production
configuration, and customer data are maintained outside this repository.

## Public build boundary

Pull requests and public CI build disconnected staging variants only:

- the production API URL is not embedded;
- Firebase is not configured;
- signing keys and passwords are not present;
- CI does not upload APK artifacts;
- public workflows run only on GitHub-hosted runners.

The public CI output is evidence that the source compiles and passes the Android
quality gate. It is not an official distribution channel. Production packages
are configured, signed, distributed, and physically accepted in a private
release process.

## Local verification

Use Java 17 and the Android SDK versions pinned in `gradle/libs.versions.toml`.

```bash
./gradlew clean quality \
  :app:assembleStaging \
  :app:assembleWorkforceStaging \
  --no-daemon --stacktrace

bash scripts/ci/verify-public-boundary.sh
bash scripts/ci/verify-disconnected-build.sh
```

## Security

Do not publish customer data, production configuration, authentication tokens,
location samples, screenshots from real accounts, or signed APKs in issues or
pull requests. See `SECURITY.md` for vulnerability reporting.

## Copyright

Copyright (c) 3V Tintas. All rights reserved. No open-source license is granted
unless a `LICENSE` file is added explicitly by the project owner.
