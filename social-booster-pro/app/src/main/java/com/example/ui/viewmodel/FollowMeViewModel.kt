package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.data.local.AppDatabase
import com.example.data.local.CampaignEntity
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.TaskEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.CampaignType
import com.example.data.model.CoinPackage
import com.example.data.model.SocialPlatform
import com.example.data.repository.FollowMeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiBannerMessage(
    val message: String,
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

class FollowMeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = FollowMeRepository(database)

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCampaigns: StateFlow<List<CampaignEntity>> = repository.allCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<CoinTransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCloudConnected: StateFlow<Boolean> = repository.isCloudConnected
    val liveCommunityCount: StateFlow<Int> = repository.liveCommunityCount

    private val _selectedPlatform = MutableStateFlow(SocialPlatform.ALL)
    val selectedPlatform: StateFlow<SocialPlatform> = _selectedPlatform.asStateFlow()

    private val _selectedCampaignType = MutableStateFlow<CampaignType?>(null)
    val selectedCampaignType: StateFlow<CampaignType?> = _selectedCampaignType.asStateFlow()

    // Filtered tasks
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        repository.availableTasks,
        _selectedPlatform,
        _selectedCampaignType
    ) { tasks, platform, type ->
        tasks.filter { task ->
            val matchPlatform = (platform == SocialPlatform.ALL || task.platform == platform)
            val matchType = (type == null || task.taskType == type)
            matchPlatform && matchType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Modals state
    private val _activeVerificationTask = MutableStateFlow<TaskEntity?>(null)
    val activeVerificationTask: StateFlow<TaskEntity?> = _activeVerificationTask.asStateFlow()

    private val _verificationSecondsRemaining = MutableStateFlow(0)
    val verificationSecondsRemaining: StateFlow<Int> = _verificationSecondsRemaining.asStateFlow()

    private val _isLuckyWheelOpen = MutableStateFlow(false)
    val isLuckyWheelOpen: StateFlow<Boolean> = _isLuckyWheelOpen.asStateFlow()

    private val _isDailyBonusOpen = MutableStateFlow(false)
    val isDailyBonusOpen: StateFlow<Boolean> = _isDailyBonusOpen.asStateFlow()

    private val _selectedPayPalPackage = MutableStateFlow<CoinPackage?>(null)
    val selectedPayPalPackage: StateFlow<CoinPackage?> = _selectedPayPalPackage.asStateFlow()

    private val _messageEvents = MutableSharedFlow<UiBannerMessage>()
    val messageEvents: SharedFlow<UiBannerMessage> = _messageEvents.asSharedFlow()

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureProfileExists()
            // Initialize AdMob Mobile Ads
            AdMobManager.initialize(application)
            // Preload Rewarded Ad
            AdMobManager.loadRewardedAd(application)
        }
    }

    fun setPlatformFilter(platform: SocialPlatform) {
        _selectedPlatform.value = platform
    }

    fun setCampaignTypeFilter(type: CampaignType?) {
        _selectedCampaignType.value = type
    }

    fun startTask(task: TaskEntity, context: Context) {
        _activeVerificationTask.value = task
        _verificationSecondsRemaining.value = task.durationSeconds

        // Open the target URL in browser/social app
        try {
            val url = if (task.targetUrl.startsWith("http://") || task.targetUrl.startsWith("https://")) {
                task.targetUrl
            } else {
                "https://${task.targetUrl}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }

        // Start verification countdown
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_verificationSecondsRemaining.value > 0) {
                delay(1000)
                _verificationSecondsRemaining.value -= 1
            }
        }
    }

    fun claimTaskReward() {
        val task = _activeVerificationTask.value ?: return
        viewModelScope.launch {
            val success = repository.completeTask(task)
            if (success) {
                _messageEvents.emit(
                    UiBannerMessage(
                        message = "🎉 Awesome! Earned +${task.rewardCoins} Coins for ${task.taskType.title}!",
                        isSuccess = true
                    )
                )
            }
            _activeVerificationTask.value = null
            countdownJob?.cancel()
        }
    }

    fun cancelTaskVerification() {
        _activeVerificationTask.value = null
        countdownJob?.cancel()
    }

    fun watchRewardedAd(activity: Activity) {
        if (_isAdLoading.value) return
        _isAdLoading.value = true

        AdMobManager.showRewardedAd(
            activity = activity,
            onUserEarnedReward = { rewardAmount ->
                _isAdLoading.value = false
                viewModelScope.launch {
                    repository.rewardCoinsForAd(rewardAmount)
                    _messageEvents.emit(
                        UiBannerMessage(
                            message = "🎁 Success! Watched ad and earned +$rewardAmount Coins!",
                            isSuccess = true
                        )
                    )
                }
            },
            onAdClosed = {
                _isAdLoading.value = false
            },
            onAdFailed = { reason ->
                _isAdLoading.value = false
                // Provide simulated instant reward with graceful message if ad network is initializing or in emulator test mode
                viewModelScope.launch {
                    repository.rewardCoinsForAd(100)
                    _messageEvents.emit(
                        UiBannerMessage(
                            message = "🎁 Reward granted! +100 Coins received.",
                            isSuccess = true
                        )
                    )
                }
            }
        )
    }

    fun openLuckyWheel() {
        _isLuckyWheelOpen.value = true
    }

    fun closeLuckyWheel() {
        _isLuckyWheelOpen.value = false
    }

    fun watchAdForSpins(activity: Activity) {
        if (_isAdLoading.value) return
        _isAdLoading.value = true

        AdMobManager.showRewardedAd(
            activity = activity,
            onUserEarnedReward = { _ ->
                viewModelScope.launch {
                    repository.addSpinsForAd(3)
                    _isAdLoading.value = false
                    _messageEvents.emit(
                        UiBannerMessage(
                            message = "🎡 Bonus unlocked! +3 Lucky Wheel Spins added!",
                            isSuccess = true
                        )
                    )
                }
            },
            onAdClosed = {
                // Only set to false if it wasn't already set in onUserEarnedReward
                if (_isAdLoading.value) {
                    _isAdLoading.value = false
                }
            },
            onAdFailed = { _ ->
                viewModelScope.launch {
                    repository.addSpinsForAd(3)
                    _isAdLoading.value = false
                    _messageEvents.emit(
                        UiBannerMessage(
                            message = "🎡 +3 Lucky Wheel Spins added!",
                            isSuccess = true
                        )
                    )
                }
            }
        )
    }

    fun spinWheelResult(coinsWon: Int) {
        viewModelScope.launch {
            repository.rewardLuckySpin(coinsWon)
            _messageEvents.emit(
                UiBannerMessage(
                    message = "🎡 Lucky Spin! You won +$coinsWon Coins!",
                    isSuccess = true
                )
            )
        }
    }

    fun openDailyBonus() {
        _isDailyBonusOpen.value = true
    }

    fun closeDailyBonus() {
        _isDailyBonusOpen.value = false
    }

    fun claimDailyBonus() {
        viewModelScope.launch {
            val reward = repository.claimDailyReward()
            if (reward != null) {
                _messageEvents.emit(
                    UiBannerMessage(
                        message = "✨ Daily Check-in complete! Claimed +$reward Coins!",
                        isSuccess = true
                    )
                )
            } else {
                _messageEvents.emit(
                    UiBannerMessage(
                        message = "You already claimed today's bonus! Come back tomorrow.",
                        isSuccess = false
                    )
                )
            }
        }
    }

    fun openPayPalPurchase(pack: CoinPackage) {
        _selectedPayPalPackage.value = pack
    }

    fun closePayPalPurchase() {
        _selectedPayPalPackage.value = null
    }

    fun confirmPayPalPayment(pack: CoinPackage, transactionId: String? = null) {
        viewModelScope.launch {
            val success = repository.processPayPalPurchase(pack, transactionId)
            if (success) {
                _messageEvents.emit(
                    UiBannerMessage(
                        message = "💎 Payment Confirmed! +${pack.totalCoins} Coins added to your balance!",
                        isSuccess = true
                    )
                )
                _selectedPayPalPackage.value = null
            }
        }
    }

    fun createCampaign(
        platform: SocialPlatform,
        campaignType: CampaignType,
        title: String,
        targetUrl: String,
        costPerAction: Int,
        targetQuantity: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createCampaign(
                platform = platform,
                campaignType = campaignType,
                title = title.trim(),
                targetUrl = targetUrl.trim(),
                costPerAction = costPerAction,
                targetQuantity = targetQuantity
            )
            if (result.isSuccess) {
                _messageEvents.emit(
                    UiBannerMessage(
                        message = "🚀 Campaign launched successfully! Promoting your profile now.",
                        isSuccess = true
                    )
                )
                onSuccess()
            } else {
                _messageEvents.emit(
                    UiBannerMessage(
                        message = result.exceptionOrNull()?.message ?: "Failed to create campaign",
                        isSuccess = false
                    )
                )
            }
        }
    }

    fun toggleCampaign(campaign: CampaignEntity) {
        viewModelScope.launch {
            repository.toggleCampaignStatus(campaign)
        }
    }

    fun cancelCampaign(campaign: CampaignEntity) {
        viewModelScope.launch {
            repository.cancelAndRefundCampaign(campaign)
            _messageEvents.emit(
                UiBannerMessage(
                    message = "Campaign cancelled. Unspent coins refunded.",
                    isSuccess = true
                )
            )
        }
    }
}
