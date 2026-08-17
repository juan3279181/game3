package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {
    const val APP_ID = "ca-app-pub-3679817190185454~3130625638"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3679817190185454/4012462463"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3679817190185454/5070393265"
    const val ADS_TXT_RECORD = "google.com, pub-3679817190185454, DIRECT, f08c47fec0942fa0"

    private const val TAG = "AdMobManager"
    private var isInitialized = false
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                MobileAds.initialize(context) {
                    isInitialized = true
                    Log.d(TAG, "AdMob MobileAds initialized successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init MobileAds: ${e.message}")
            }
        }
    }

    fun loadRewardedAd(
        context: Context,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((String) -> Unit)? = null
    ) {
        if (rewardedAd != null) {
            onLoaded?.invoke()
            return
        }
        if (isLoadingRewarded) return

        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded Ad loaded successfully")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    Log.w(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                    onFailed?.invoke(loadAdError.message)
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (Int) -> Unit,
        onAdClosed: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    // Preload next ad
                    loadRewardedAd(activity.applicationContext)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    onAdFailed(adError.message)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad showed full screen content")
                }
            }

            currentAd.show(activity) { rewardItem ->
                val amount = if (rewardItem.amount > 0) rewardItem.amount else 100
                Log.d(TAG, "User earned reward: $amount coins")
                onUserEarnedReward(amount)
            }
        } else {
            // Load for next time and inform caller
            loadRewardedAd(activity.applicationContext)
            onAdFailed("Ad is loading, please try again in a few seconds")
        }
    }

    fun isAdReady(): Boolean = rewardedAd != null
}
