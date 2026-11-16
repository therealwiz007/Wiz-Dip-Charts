package com.wizlabs.wizdipcharts

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.ui.graphics.toArgb
import com.wizlabs.update.UpdatePalette
import com.wizlabs.update.WizReview
import com.wizlabs.update.WizUpdate

/** Small bits of UI state the activity reads outside Compose (set from TankDipApp). */
internal object DipUi {
    /** True while My charts / Find my chart is open, the dip or volume field has focus, or a dialog is up. */
    @Volatile var busy = false
    /** Set when a share sheet opens; the next onResume may ask for a review. */
    @Volatile var reviewAfterShare = false
}

/** Wiz palette for the update dialog and restart bar, chosen by the current night mode. */
internal fun dipUpdatePalette(activity: Activity): UpdatePalette {
    val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val p = if (night == Configuration.UI_MODE_NIGHT_YES) DarkPal else LightPal
    return UpdatePalette(
        p.card.toArgb(), p.separator.toArgb(), p.label.toArgb(), p.secondary.toArgb(),
        p.accent.toArgb(), p.onAccent.toArgb(), 24,
    )
}

internal fun createWizUpdate(activity: Activity): WizUpdate = WizUpdate(activity, object : WizUpdate.Host {
    override fun palette() = dipUpdatePalette(activity)
    override fun mayPromptNow() = !DipUi.busy
    override fun appName() = "Wiz Dip Charts"
    override fun restartNote(): String? = null
}, BuildConfig.DEBUG)

/** Review card rules for Dip Charts (plan 2.6): 10 results on 4 or more active days. */
internal fun askForReview(activity: Activity) =
    WizReview.maybeAsk(activity, 10, 4, WizUpdate.wasShownThisProcess())
