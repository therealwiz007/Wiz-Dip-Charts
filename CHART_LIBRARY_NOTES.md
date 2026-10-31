# Calibration chart library

The built-in library currently contains 16 distinct profiles extracted from the two supplied Excel workbooks.

- Each profile retains its own chart ID, nominal capacity, diameter, length, P1, P2, deadwood, maximum dip and litre readings.
- Profiles with the same nominal capacity remain separate when their source sheets differ.
- The `70kl` sheet in `DIP CHART 10 15 20 50 70 KL.xlsx` is labelled as a 20 KL profile, per the app owner's clarification; its dimensions are 210 × 600 cm.
- No chart data is user-importable or editable in the app. Users only save/unsave built-in profiles in My Charts.
- Saved profile selections are stored locally using Android SharedPreferences and work offline.

Important source limitation: the photographed 15 KL chart shows dimensions of 190.5 × 556 cm, while the workbook's nearest IBP profile is 191 × 551 cm. These are not treated as the same chart. The photographed chart has not been transcribed into a complete 1-to-maximum-dip data series in this build, so it is not silently approximated or substituted.
