package com.example.admob

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class RewardUiState(
    val isAdFree: Boolean = false,
    val progress: Int = 0,
    val isRewardProcessing: Boolean = false,
    val isSearching: Boolean = false,
    val searchSecondsLeft: Int = 9,
    val hasExpiredNotice: Boolean = false,
    val showDialog: Boolean = false,
    val toastMessage: String? = null,
    val remainingAdFreeFormatted: String = ""
)

class RewardViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = RewardPreferences(application)
    private val adMobManager = AdMobManager(application)

    private val _uiState = MutableStateFlow(RewardUiState())
    val uiState: StateFlow<RewardUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        refreshStatus()
        startTicker()
    }

    /**
     * Re-checks ad-free status from SharedPreferences and updates the UI state.
     */
    fun refreshStatus() {
        val status = preferences.checkStatus()
        val remainingFormatted = formatRemainingTime(status.remainingMs)

        _uiState.update { current ->
            current.copy(
                isAdFree = status.isAdFree,
                progress = status.progress,
                hasExpiredNotice = status.hasExpiredNotice,
                showDialog = !status.isAdFree,
                remainingAdFreeFormatted = remainingFormatted
            )
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val remainingMs = preferences.getRemainingAdFreeMillis()
                val isAdFree = remainingMs > 0L

                if (!isAdFree && _uiState.value.isAdFree) {
                    // Just transitioned to expired while the app is running!
                    refreshStatus()
                } else if (isAdFree) {
                    _uiState.update {
                        it.copy(
                            remainingAdFreeFormatted = formatRemainingTime(remainingMs)
                        )
                    }
                }
            }
        }
    }

    fun dismissExpiredNotice() {
        preferences.clearExpiredNotice()
        _uiState.update { it.copy(hasExpiredNotice = false) }
    }

    fun openDialog() {
        refreshStatus()
        _uiState.update { it.copy(showDialog = true) }
    }

    fun closeDialog() {
        if (_uiState.value.isAdFree) {
            _uiState.update { it.copy(showDialog = false) }
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    /**
     * Triggered when the user taps "مشاهدة إعلان".
     */
    fun onWatchAdClicked(activity: Activity) {
        if (_uiState.value.isRewardProcessing) return

        _uiState.update {
            it.copy(
                isRewardProcessing = true,
                isSearching = false,
                searchSecondsLeft = 9
            )
        }

        adMobManager.startAdAttempt(activity, object : AdMobManager.AdAttemptCallback {
            override fun onSearchingForAd() {
                _uiState.update {
                    it.copy(
                        isSearching = true,
                        searchSecondsLeft = 9
                    )
                }
            }

            override fun onCountdownTick(secondsRemaining: Int) {
                _uiState.update {
                    it.copy(
                        searchSecondsLeft = secondsRemaining
                    )
                }
            }

            override fun onAdShowing() {
                _uiState.update {
                    it.copy(
                        isSearching = false
                    )
                }
            }

            override fun onPointEarned(fromRealAd: Boolean) {
                val currentProgress = _uiState.value.progress
                val newProgress = (currentProgress + 1).coerceAtMost(RewardPreferences.MAX_PROGRESS)
                preferences.setProgress(newProgress)

                if (newProgress >= RewardPreferences.MAX_PROGRESS) {
                    // 5 / 5 Reached! Activate 24 hours
                    val until = preferences.activateAdFree24Hours()
                    val remainingMs = until - System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            progress = newProgress,
                            isAdFree = true,
                            showDialog = false,
                            isSearching = false,
                            isRewardProcessing = false,
                            hasExpiredNotice = false,
                            toastMessage = "تم تفعيل الاستخدام بدون إعلانات لمدة 24 ساعة",
                            remainingAdFreeFormatted = formatRemainingTime(remainingMs)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            progress = newProgress,
                            isSearching = false
                        )
                    }
                }
            }

            override fun onAttemptComplete() {
                _uiState.update {
                    it.copy(
                        isRewardProcessing = false,
                        isSearching = false
                    )
                }
            }
        })
    }

    private fun formatRemainingTime(ms: Long): String {
        if (ms <= 0L) return "0 دقيقة"
        val totalMinutes = ms / (60 * 1000L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d ساعة و %d دقيقة", hours, minutes)
        } else {
            String.format(Locale.getDefault(), "%d دقيقة", minutes)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
