# Wiz Dip Charts STATUS

Current: update 1.10 (versionCode 8), stages 2, 3, 5 done in one session.

## Completed / verified
- ads-mobile-sdk 1.6.0 -> 1.7.0 (Play Console "SDK outdated" warning). Verified: `gradle :app:assembleDebug --offline` (Gradle 8.11.1) BUILD SUCCESSFUL.
- versionCode 7 -> 8, versionName 1.9 -> 1.10.
- Added fastlane changelog `8.txt`.

## Pending
- AAB validated (2026-11-21): CI run 44870227657, 3.7 MB, package com.wizlabs.wizdipcharts, versionName 1.10, signed by CN=Wiz Labs (SHA256 4E:2E:EA:81...1F:AC), has BundleConfig + dex.
- Not tested on phone/emulator (debug build only).
- Play Console upload: not started (stopped by request).

Remaining: phone smoke test (user), Play Console upload + release approval (stage 7, needs Chrome; stop for approval before submit).

## Decisions taken
- Changelog text copied from wiz-fuel-density 1.6 style.

## Next stage
Stage 7 (Play Console), Sonnet + Chrome, after CI AAB is green.
