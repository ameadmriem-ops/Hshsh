package com.example.admob

import android.content.Context
import android.content.SharedPreferences

class RewardPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("admob_reward_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_PROGRESS = 5
        const val AD_FREE_DURATION_MS = 24L * 60L * 60L * 1000L // 24 hours
        private const val KEY_PROGRESS = "reward_progress"
        private const val KEY_LAST_COMPLETED = "last_completed_time"
        private const val KEY_AD_FREE_UNTIL = "ad_free_until"
        private const val KEY_EXPIRED_NOTICE = "expired_notice"
    }

    data class AdFreeStatus(
        val isAdFree: Boolean,
        val progress: Int,
        val remainingMs: Long,
        val hasExpiredNotice: Boolean
    )

    fun checkStatus(): AdFreeStatus {
        val now = System.currentTimeMillis()
        val adFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)

        if (adFreeUntil > 0L) {
            if (now < adFreeUntil) {
                return AdFreeStatus(
                    isAdFree = true,
                    progress = MAX_PROGRESS,
                    remainingMs = adFreeUntil - now,
                    hasExpiredNotice = false
                )
            } else {
                // 24 hours have elapsed since last completion!
                // Reset counter to 0/5 and flag expiration
                prefs.edit()
                    .putInt(KEY_PROGRESS, 0)
                    .putLong(KEY_AD_FREE_UNTIL, 0L)
                    .putBoolean(KEY_EXPIRED_NOTICE, true)
                    .apply()
                return AdFreeStatus(
                    isAdFree = false,
                    progress = 0,
                    remainingMs = 0L,
                    hasExpiredNotice = true
                )
            }
        }

        val progress = prefs.getInt(KEY_PROGRESS, 0).coerceIn(0, MAX_PROGRESS)
        val hasExpiredNotice = prefs.getBoolean(KEY_EXPIRED_NOTICE, false)
        return AdFreeStatus(
            isAdFree = false,
            progress = progress,
            remainingMs = 0L,
            hasExpiredNotice = hasExpiredNotice
        )
    }

    fun getProgress(): Int {
        return prefs.getInt(KEY_PROGRESS, 0).coerceIn(0, MAX_PROGRESS)
    }

    fun setProgress(progress: Int) {
        prefs.edit().putInt(KEY_PROGRESS, progress.coerceIn(0, MAX_PROGRESS)).apply()
    }

    fun clearExpiredNotice() {
        prefs.edit().putBoolean(KEY_EXPIRED_NOTICE, false).apply()
    }

    fun activateAdFree24Hours(): Long {
        val now = System.currentTimeMillis()
        val until = now + AD_FREE_DURATION_MS
        prefs.edit()
            .putInt(KEY_PROGRESS, MAX_PROGRESS)
            .putLong(KEY_LAST_COMPLETED, now)
            .putLong(KEY_AD_FREE_UNTIL, until)
            .putBoolean(KEY_EXPIRED_NOTICE, false)
            .apply()
        return until
    }

    fun getRemainingAdFreeMillis(): Long {
        val until = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0L) remaining else 0L
    }
}
