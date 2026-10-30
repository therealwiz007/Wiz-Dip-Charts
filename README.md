# Wiz Dip Charts

Android tank dip / calibration lookup app by Wiz Labs.

## Included
- Dip → Volume lookup for 10 KL, 15 KL, 20 KL and 70 KL tanks.
- Reverse Volume → Dip lookup.
- Decimal dip interpolation between adjacent whole-centimetre chart rows.
- Tank-specific deadwood adjustment.
- Offline calibration data stored in `app/src/main/assets/charts.json`.
- Last selected tank and last dip remembered locally.
- Light and dark appearance follows the device setting.
- No ads, account, network permission, or analytics SDK.

## App identity
- Display name: Wiz Dip Charts
- Application ID: `com.wizlabs.wizdipcharts`
- Version: 1.0.0 (versionCode 1)
- Minimum Android: 7.0 (API 24)
- Target Android: 16 (API 36)

## Build through GitHub Actions
1. Upload the contents of this folder to a new GitHub repository (keep `.github/workflows/android.yml`).
2. Add repository Actions secrets for the app's upload keystore:
   - `KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
3. Push to `main` or `master` to run tests and produce a debug APK.
4. In GitHub → Actions → Android build → Run workflow, manually start the workflow to create the signed Play Store `.aab`.
5. Download the artifact named `wiz-dip-charts-signed-release-aab`, extract the ZIP, and upload the `.aab` in Play Console.

Use a dedicated upload keystore for this app, keep a secure offline backup, and never commit the `.jks` or passwords to the repository. Google Play App Signing should be enabled when creating the Play Console app.

## Calibration data release gate
The app validates chart structure at startup (zero starting value, strictly increasing rows, valid maximum dip, finite values, unique tank sizes, and deadwood below the maximum chart volume). This confirms data integrity only; it does **not** prove the values match an approved petroleum calibration chart. Before production release, independently compare every tank's chart rows and deadwood values with the approved source chart.

## Calculation rules
- Forward lookup: linear interpolation between whole-cm chart rows.
- Net volume = max(0, interpolated chart volume − tank deadwood).
- Reverse lookup: add deadwood to requested net litres, then inverse-interpolate the chart.
