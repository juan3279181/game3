package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserProfileEntity)

    @Query("UPDATE user_profile SET coinsBalance = coinsBalance + :coins, totalEarned = totalEarned + :earnedCoins, tasksCompletedCount = tasksCompletedCount + :tasksIncrement WHERE id = 1")
    suspend fun addCoins(coins: Int, earnedCoins: Int = coins, tasksIncrement: Int = 0)

    @Query("UPDATE user_profile SET coinsBalance = coinsBalance - :coins, totalSpent = totalSpent + :coins WHERE id = 1")
    suspend fun deductCoins(coins: Int)

    @Query("UPDATE user_profile SET dailyStreak = :streak, lastCheckInDate = :checkInDate, coinsBalance = coinsBalance + :rewardCoins, totalEarned = totalEarned + :rewardCoins WHERE id = 1")
    suspend fun claimDailyBonus(streak: Int, checkInDate: Long, rewardCoins: Int)

    @Query("UPDATE user_profile SET freeSpinsLeft = :spins, lastSpinDate = :date WHERE id = 1")
    suspend fun updateSpins(spins: Int, date: Long)

    @Query("UPDATE user_profile SET freeSpinsLeft = freeSpinsLeft + :spins WHERE id = 1")
    suspend fun addSpins(spins: Int)
}

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getCampaignById(id: Long): CampaignEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)

    @Query("UPDATE campaigns SET deliveredQuantity = deliveredQuantity + 1 WHERE id = :id")
    suspend fun incrementDelivered(id: Long)

    @Query("UPDATE campaigns SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: CampaignStatus)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteCampaign(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, rewardCoins DESC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY rewardCoins DESC, id DESC")
    fun getAvailableTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :timestamp WHERE id = :id")
    suspend fun markTaskCompleted(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM coin_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<CoinTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CoinTransactionEntity): Long
}
