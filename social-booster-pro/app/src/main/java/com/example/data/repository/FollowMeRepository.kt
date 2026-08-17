package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.CampaignEntity
import com.example.data.local.CampaignStatus
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserProfileEntity
import com.example.data.model.CampaignType
import com.example.data.model.CoinPackage
import com.example.data.model.SocialPlatform
import com.example.data.remote.CommunityCampaignDto
import com.example.data.remote.FirebaseCommunityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class FollowMeRepository(
    private val database: AppDatabase,
    private val communityManager: FirebaseCommunityManager = FirebaseCommunityManager.getInstance()
) {
    private val userDao = database.userDao()
    private val campaignDao = database.campaignDao()
    private val taskDao = database.taskDao()
    private val transactionDao = database.transactionDao()

    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()
    val allCampaigns: Flow<List<CampaignEntity>> = campaignDao.getAllCampaigns()
    val activeCampaigns: Flow<List<CampaignEntity>> = campaignDao.getActiveCampaigns()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val transactions: Flow<List<CoinTransactionEntity>> = transactionDao.getAllTransactions()

    val isCloudConnected: StateFlow<Boolean> = communityManager.isCloudConnected
    val liveCommunityCount: StateFlow<Int> = communityManager.liveCommunityCount

    // Combined live tasks: Real-time community video/profile campaigns from Firestore + Local curated tasks
    val availableTasks: Flow<List<TaskEntity>> = combine(
        taskDao.getAvailableTasks(),
        communityManager.observeActiveCampaigns()
    ) { localTasks, remoteCampaigns ->
        val remoteTasks = remoteCampaigns.map { it.toTaskEntity() }
        // Remote community campaigns are displayed with high priority so creators see their content immediately
        (remoteTasks + localTasks).distinctBy { 
            if (it.targetUrl.isNotBlank()) it.targetUrl else "${it.platform}_${it.title}"
        }
    }

    suspend fun ensureProfileExists() {
        val current = userDao.getUserProfileDirect()
        if (current == null) {
            AppDatabase.populateInitialData(database)
        }
    }

    suspend fun completeTask(task: TaskEntity): Boolean {
        if (task.isCompleted) return false

        val profile = userDao.getUserProfileDirect()
        val userId = profile?.username ?: "User_${profile?.id ?: 1}"

        taskDao.markTaskCompleted(task.id)
        userDao.addCoins(coins = task.rewardCoins, earnedCoins = task.rewardCoins, tasksIncrement = 1)
        transactionDao.insertTransaction(
            CoinTransactionEntity(
                type = TransactionType.TASK_REWARD,
                amount = task.rewardCoins,
                title = "Task Completed: ${task.platform.displayName}",
                description = "Earned for ${task.taskType.title} on ${task.title}",
                referenceId = "TASK-${task.id}"
            )
        )

        // If this was a community campaign, update the Firestore counter in real time
        val cloudCampaignId = "CMP-${task.id}"
        communityManager.completeCommunityAction(cloudCampaignId, userId)

        return true
    }

    suspend fun rewardCoinsForAd(coins: Int = 100): Boolean {
        userDao.addCoins(coins = coins, earnedCoins = coins, tasksIncrement = 0)
        transactionDao.insertTransaction(
            CoinTransactionEntity(
                type = TransactionType.AD_REWARD,
                amount = coins,
                title = "Rewarded Video Ad",
                description = "Reward for watching sponsored video ad"
            )
        )
        return true
    }

    suspend fun addSpinsForAd(spins: Int = 3): Boolean {
        userDao.updateSpins(spins, System.currentTimeMillis())
        transactionDao.insertTransaction(
            CoinTransactionEntity(
                type = TransactionType.AD_REWARD,
                amount = 0,
                title = "Unlocked $spins Wheel Spins",
                description = "Watched rewarded video ad to unlock $spins Lucky Wheel spins!"
            )
        )
        return true
    }

    suspend fun rewardLuckySpin(coins: Int): Boolean {
        val profile = userDao.getUserProfileDirect() ?: return false
        val newSpins = (profile.freeSpinsLeft - 1).coerceAtLeast(0)
        userDao.updateSpins(newSpins, System.currentTimeMillis())

        if (coins > 0) {
            userDao.addCoins(coins = coins, earnedCoins = coins, tasksIncrement = 0)
            transactionDao.insertTransaction(
                CoinTransactionEntity(
                    type = TransactionType.LUCKY_SPIN,
                    amount = coins,
                    title = "Lucky Wheel Win",
                    description = "Won $coins coins on the Fortune Wheel!"
                )
            )
        }
        return true
    }

    suspend fun claimDailyReward(): Int? {
        val profile = userDao.getUserProfileDirect() ?: return null
        val now = System.currentTimeMillis()

        // Check if already claimed today
        val lastCal = Calendar.getInstance().apply { timeInMillis = profile.lastCheckInDate }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = profile.lastCheckInDate > 0 &&
                lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return null // already claimed
        }

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isConsecutive = profile.lastCheckInDate > 0 &&
                lastCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                lastCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        val newStreak = if (isConsecutive) ((profile.dailyStreak % 7) + 1) else 1
        val rewardAmount = when (newStreak) {
            1 -> 50
            2 -> 80
            3 -> 120
            4 -> 160
            5 -> 220
            6 -> 300
            7 -> 500
            else -> 50
        }

        userDao.claimDailyBonus(newStreak, now, rewardAmount)
        transactionDao.insertTransaction(
            CoinTransactionEntity(
                type = TransactionType.DAILY_BONUS,
                amount = rewardAmount,
                title = "Day $newStreak Daily Bonus",
                description = "Claimed consecutive check-in reward"
            )
        )
        return rewardAmount
    }

    suspend fun processPayPalPurchase(
        pack: CoinPackage,
        paypalTxId: String? = null
    ): Boolean {
        val totalCoins = pack.totalCoins
        userDao.addCoins(coins = totalCoins, earnedCoins = 0, tasksIncrement = 0)
        transactionDao.insertTransaction(
            CoinTransactionEntity(
                type = TransactionType.PAYPAL_PURCHASE,
                amount = totalCoins,
                title = "PayPal Store: ${pack.name}",
                description = "Purchased $totalCoins Coins for ${pack.formattedPrice} USD to ${CoinPackage.PAYPAL_RECEIVER_EMAIL}",
                referenceId = paypalTxId ?: "PP-${System.currentTimeMillis() % 1000000}"
            )
        )
        return true
    }

    suspend fun createCampaign(
        platform: SocialPlatform,
        campaignType: CampaignType,
        title: String,
        targetUrl: String,
        costPerAction: Int,
        targetQuantity: Int
    ): Result<Long> {
        val totalCost = costPerAction * targetQuantity
        val profile = userDao.getUserProfileDirect()
            ?: return Result.failure(Exception("User profile not found"))

        if (profile.coinsBalance < totalCost) {
            return Result.failure(Exception("Insufficient coins! Required: $totalCost, Available: ${profile.coinsBalance}"))
        }

        // Deduct coins
        userDao.deductCoins(totalCost)

        // Insert campaign into Room
        val campaign = CampaignEntity(
            platform = platform,
            campaignType = campaignType,
            title = title,
            targetUrl = targetUrl,
            costPerAction = costPerAction,
            targetQuantity = targetQuantity,
            deliveredQuantity = 0,
            status = CampaignStatus.ACTIVE
        )
        val campaignId = campaignDao.insertCampaign(campaign)

        // Log transaction
        transactionDao.insertTransaction(
            CoinTransactionEntity(
                type = TransactionType.CAMPAIGN_CREATION,
                amount = -totalCost,
                title = "Created Campaign: ${platform.displayName}",
                description = "Ordered $targetQuantity ${campaignType.title} for $title",
                referenceId = "CMP-$campaignId"
            )
        )

        // Also add to local community task feed
        taskDao.insertTask(
            TaskEntity(
                id = campaignId,
                platform = platform,
                taskType = campaignType,
                title = title,
                author = "@${profile.username}",
                targetUrl = targetUrl,
                rewardCoins = costPerAction,
                durationSeconds = if (campaignType == CampaignType.VIEWS) 30 else 15,
                category = "Community Post"
            )
        )

        // Publish to Firebase Firestore real-time cloud so everyone sees the post
        val cloudDto = CommunityCampaignDto(
            id = "CMP-$campaignId",
            creatorId = profile.username,
            creatorName = profile.username,
            platform = platform.name,
            campaignType = campaignType.name,
            title = title,
            targetUrl = targetUrl,
            costPerAction = costPerAction,
            targetQuantity = targetQuantity,
            deliveredQuantity = 0,
            status = "ACTIVE",
            timestamp = System.currentTimeMillis(),
            completedUserIds = emptyList()
        )
        communityManager.publishCampaign(cloudDto)

        return Result.success(campaignId)
    }

    suspend fun toggleCampaignStatus(campaign: CampaignEntity) {
        val newStatus = when (campaign.status) {
            CampaignStatus.ACTIVE -> CampaignStatus.PAUSED
            CampaignStatus.PAUSED -> CampaignStatus.ACTIVE
            CampaignStatus.COMPLETED -> CampaignStatus.COMPLETED
        }
        campaignDao.updateStatus(campaign.id, newStatus)
        // Sync status to Firestore
        communityManager.updateCampaignStatus("CMP-${campaign.id}", newStatus.name)
    }

    suspend fun cancelAndRefundCampaign(campaign: CampaignEntity) {
        val remaining = campaign.remainingQuantity
        val refundAmount = remaining * campaign.costPerAction

        campaignDao.deleteCampaign(campaign.id)
        // Delete from Firestore community feed
        communityManager.deleteCampaign("CMP-${campaign.id}")

        if (refundAmount > 0) {
            userDao.addCoins(coins = refundAmount, earnedCoins = 0, tasksIncrement = 0)
            transactionDao.insertTransaction(
                CoinTransactionEntity(
                    type = TransactionType.CAMPAIGN_REFUND,
                    amount = refundAmount,
                    title = "Campaign Refund",
                    description = "Refunded $remaining undelivered actions from '${campaign.title}'",
                    referenceId = "REF-${campaign.id}"
                )
            )
        }
    }
}

