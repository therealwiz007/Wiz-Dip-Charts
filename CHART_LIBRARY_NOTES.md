# Calibration chart library

The built-in library contains 19 profiles: 17 from the supplied Excel workbooks and photos, plus two added in 1.6 from photographed charts.

- Each profile retains its own chart ID, nominal capacity, diameter, length, P1, P2, deadwood, maximum dip and litre readings.
- Profiles with the same nominal capacity remain separate when their source sheets differ.
- The `70kl` sheet in `DIP CHART 10 15 20 50 70 KL.xlsx` is labelled as a 20 KL profile, per the app owner's clarification; its dimensions are 210 × 600 cm.
- No chart data is user-importable or editable in the app. Users only save/unsave built-in profiles in My Charts.
- Saved profile selections are stored locally using Android SharedPreferences and work offline.

Important source limitation: the photographed 15 KL chart shows dimensions of 190.5 × 556 cm, while the workbook's nearest IBP profile is 191 × 551 cm. These are not treated as the same chart. The photographed chart has not been transcribed into a complete 1-to-maximum-dip data series in this build, so it is not silently approximated or substituted.


## Added in 1.6 (2026-11-12)

- `20-2100-6000-IOC1987`: IOC Northern Region "Calibration table for 20 KL tanks", horizontal cylindrical, Dia 2.1 m × Length 6.0 m, IS 2166:1963 / IS 2009:1975. The sheet lists every 2 cm with a per-mm difference, so odd centimetres are the linear midpoint (exactly how the per-mm column is applied). The sheet states no deadwood, so the label reads `DW: N/A`. The photo is partly unreadable, so each even-cm value is cross-checked against the flat-end cylinder formula for 210 × 600 cm: 96 readings agree within 0.06 L and are used as printed. Nine unreadable rows (48, 68, 82, 112, 150, 152, 156, 180, 194 cm) use the formula plus the sheet's median offset (+0.021 L). The dip 210 value (20785.684 L) is used as printed.
- `70-2896-10973-UG`: "Underground Tank [70 KL] Calibration Chart", chart as per IS 2166-1963 (Appx A), Dia 289.56 cm × Length 1097.28 cm, P1/P2 0, deadwood 81 L (as stated on the chart), dips 1–289 cm (the chart prints #NUM! beyond 289). Every value was read from two photos and checked against the printed per-mm DIFF column.

## Find my chart

Dealers who do not know which chart they have pick the capacity and type their chart's litres at dip 50 cm and dip 100 cm. The app ranks every chart of that capacity by mean absolute difference: within 1 L is an exact match, within 10 L is close.
