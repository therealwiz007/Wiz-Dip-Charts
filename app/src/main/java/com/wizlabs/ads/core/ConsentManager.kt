package com.wizlabs.ads.core

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.wizlabs.ads.AdsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google UMP wrapper (checked against user-messaging-platform 4.0.0). Constructing it touches
 * nothing; every entry point returns at once while ads are switched off, so no consent form and
 * no UMP request ever happen in such a build.
 */
class ConsentManager {
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var gathering = false

    /** Call after the first frame. Never blocks UI. Shows the form only where it is required. */
    fun gather(activity: Activity, config: AdsConfig, onDone: () -> Unit) {
        if (!config.enabled || gathering) return
        gathering = true
        runCatching {
            val info = UserMessagingPlatform.getConsentInformation(activity)
            if (info.canRequestAds()) { publish(info); onDone() }   // a previous session already allows ads
            val params = ConsentRequestParameters.Builder().apply {
                if (config.admobAppId.isNotEmpty()) setAdMobAppId(config.admobAppId)
                if (config.debug && config.consentDebugGeography != 0) {
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(config.consentDebugGeography)
                            .apply { config.testDeviceHashedIds.forEach { addTestDeviceHashedId(it) } }
                            .build(),
                    )
                }
            }.build()
            info.requestConsentInfoUpdate(activity, params, {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> finish(info, onDone) }
            }, { _ -> finish(info, onDone) })
        }.onFailure { gathering = false }
    }

    fun showPrivacyOptions(activity: Activity, config: AdsConfig, onClosed: () -> Unit) {
        if (!config.enabled) return
        runCatching {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
                publish(UserMessagingPlatform.getConsentInformation(activity))
                onClosed()
            }
        }
    }

    private fun finish(info: ConsentInformation, onDone: () -> Unit) {
        gathering = false
        publish(info)
        onDone()
    }

    private fun publish(info: ConsentInformation) {
        _canRequestAds.value = info.canRequestAds()
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }
}
