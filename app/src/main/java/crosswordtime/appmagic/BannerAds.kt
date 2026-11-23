package crosswordtime.appmagic

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import crosswordtime.magicapp.BuildConfig

internal class BannerAds(private val activity: MainActivity, private val container: FrameLayout) {
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private var requestedConsent = false
    private var consentReady = false
    private var initializing = false
    private var initialized = false
    private var closed = false
    private var mayShowAds = false
    private var banner: AdView? = null

    val privacyOptionsRequired: Boolean
        get() = requestedConsent && consent.privacyOptionsRequirementStatus ==
            com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun setEligible(eligible: Boolean) {
        mayShowAds = eligible
        if (!eligible) {
            removeBanner()
            return
        }
        if (BuildConfig.ADMOB_BANNER_ID.isBlank()) return
        if (requestedConsent) {
            loadIfAllowed()
            return
        }
        requestedConsent = true
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                if (!closed) {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        consentReady = true
                        loadIfAllowed()
                    }
                }
            },
            {
                consentReady = true
                loadIfAllowed()
            },
        )
    }

    fun showPrivacyOptions() {
        if (!privacyOptionsRequired) return
        consentReady = false
        removeBanner()
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            consentReady = true
            loadIfAllowed()
        }
    }

    private fun loadIfAllowed() {
        if (closed || !mayShowAds || !consentReady || !consent.canRequestAds() || banner != null) return
        if (!initialized) {
            if (!initializing) {
                initializing = true
                MobileAds.initialize(activity) {
                    activity.runOnUiThread {
                        initialized = true
                        loadIfAllowed()
                    }
                }
            }
            return
        }

        val view = AdView(activity)
        view.adUnitId = BuildConfig.ADMOB_BANNER_ID
        view.setAdSize(AdSize.BANNER)
        view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (!closed && banner === view) container.visibility = View.VISIBLE
            }
        }
        banner = view
        container.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
        view.loadAd(AdRequest.Builder().build())
    }

    private fun removeBanner() {
        container.visibility = View.GONE
        banner?.let { view ->
            container.removeView(view)
            view.destroy()
        }
        banner = null
    }

    fun pause() { banner?.pause() }
    fun resume() { banner?.resume() }

    fun close() {
        closed = true
        removeBanner()
    }
}
