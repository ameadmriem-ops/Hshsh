package com.example.admob

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Google AdMob Rewarded Ads with:
 * - Production Rewarded Ad Unit ID: ca-app-pub-2277779478101583/1580605088
 * - Preloading and caching of next ad
 * - Real 9-second timeout when ad is not ready
 * - Automatic +1 attempt credit if ad is unavailable after 9 seconds
 * - Strict duplicate-prevention flag to ensure exactly 1 point per attempt
 */
class AdMobManager(private val context: Context) {

    companion object {
        private const val TAG = "AdMobManager"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-2277779478101583/1580605088"
        const val SEARCH_TIMEOUT_MS = 9000L
    }

    private var cachedRewardedAd: RewardedAd? = null
    private var isAdLoading = false
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: $status")
                preloadAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob", e)
        }
    }

    fun isAdReady(): Boolean = cachedRewardedAd != null

    /**
     * Preloads a Rewarded Ad in the background.
     */
    fun preloadAd(onLoaded: (() -> Unit)? = null) {
        if (cachedRewardedAd != null || isAdLoading) {
            onLoaded?.invoke()
            return
        }
        isAdLoading = true
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    isAdLoading = false
                    cachedRewardedAd = rewardedAd
                    Log.d(TAG, "AdMob RewardedAd successfully loaded and cached.")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    cachedRewardedAd = null
                    Log.w(TAG, "AdMob RewardedAd failed to load: code=${loadAdError.code}, msg=${loadAdError.message}")
                }
            }
        )
    }

    interface AdAttemptCallback {
        fun onSearchingForAd()
        fun onCountdownTick(secondsRemaining: Int)
        fun onAdShowing()
        fun onPointEarned(fromRealAd: Boolean)
        fun onAttemptComplete()
    }

    /**
     * Handles one user click on "مشاهدة إعلان":
     * 1. If ad is cached, displays it immediately and waits for reward callback.
     * 2. If ad is not ready, searches with a 9s timeout and live countdown.
     * 3. If ad arrives within 9s, shows ad and waits for reward.
     * 4. If ad does not arrive within 9s, auto-credits +1 and continues.
     * 5. Strictly prevents duplicate crediting via atomic flag.
     */
    fun startAdAttempt(activity: Activity, callback: AdAttemptCallback) {
        val attemptCredited = AtomicBoolean(false)
        val attemptCompleted = AtomicBoolean(false)
        val searchCancelled = AtomicBoolean(false)

        fun markAttemptComplete() {
            if (attemptCompleted.compareAndSet(false, true)) {
                mainHandler.post {
                    callback.onAttemptComplete()
                }
            }
        }

        fun awardPoint(fromRealAd: Boolean) {
            if (attemptCredited.compareAndSet(false, true)) {
                mainHandler.post {
                    callback.onPointEarned(fromRealAd)
                }
            }
        }

        // Case 1: Ad already preloaded & ready
        val readyAd = cachedRewardedAd
        if (readyAd != null) {
            cachedRewardedAd = null
            callback.onAdShowing()
            showAdInternal(
                activity = activity,
                ad = readyAd,
                onReward = { awardPoint(true) },
                onDismiss = {
                    markAttemptComplete()
                    preloadAd()
                },
                onError = {
                    awardPoint(true)
                    markAttemptComplete()
                    preloadAd()
                }
            )
            return
        }

        // Case 2: Ad is not ready. Start 9s timeout & live countdown
        callback.onSearchingForAd()

        val countdownRunnables = mutableListOf<Runnable>()
        val totalSeconds = (SEARCH_TIMEOUT_MS / 1000L).toInt()

        for (sec in 1..totalSeconds) {
            val remainingSec = totalSeconds - sec
            val tickRunnable = Runnable {
                if (!searchCancelled.get()) {
                    callback.onCountdownTick(remainingSec)
                }
            }
            countdownRunnables.add(tickRunnable)
            mainHandler.postDelayed(tickRunnable, sec * 1000L)
        }

        val timeoutRunnable = Runnable {
            if (searchCancelled.compareAndSet(false, true)) {
                Log.d(TAG, "9-second search timeout finished. Auto-crediting attempt.")
                awardPoint(false)
                markAttemptComplete()
                preloadAd()
            }
        }

        mainHandler.postDelayed(timeoutRunnable, SEARCH_TIMEOUT_MS)

        // Asynchronously request ad loading
        preloadAd {
            mainHandler.post {
                val loadedAd = cachedRewardedAd
                if (loadedAd != null && searchCancelled.compareAndSet(false, true)) {
                    // Ad arrived in time! Cancel timeout and all tick runnables
                    mainHandler.removeCallbacks(timeoutRunnable)
                    for (r in countdownRunnables) {
                        mainHandler.removeCallbacks(r)
                    }
                    cachedRewardedAd = null
                    callback.onAdShowing()
                    showAdInternal(
                        activity = activity,
                        ad = loadedAd,
                        onReward = { awardPoint(true) },
                        onDismiss = {
                            markAttemptComplete()
                            preloadAd()
                        },
                        onError = {
                            awardPoint(true)
                            markAttemptComplete()
                            preloadAd()
                        }
                    )
                }
            }
        }
    }

    private fun showAdInternal(
        activity: Activity,
        ad: RewardedAd,
        onReward: () -> Unit,
        onDismiss: () -> Unit,
        onError: () -> Unit
    ) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "RewardedAd showed full screen.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "RewardedAd failed to show: ${adError.message}")
                onError()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "RewardedAd dismissed.")
                onDismiss()
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "AdMob reward earned: amount=${rewardItem.amount}, type=${rewardItem.type}")
            onReward()
        }
    }
}
