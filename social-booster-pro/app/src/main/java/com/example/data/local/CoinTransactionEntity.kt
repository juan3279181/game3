package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    WELCOME_BONUS,
    TASK_REWARD,
    AD_REWARD,
    DAILY_BONUS,
    LUCKY_SPIN,
    PAYPAL_PURCHASE,
    CAMPAIGN_CREATION,
    CAMPAIGN_REFUND
}

@Entity(tableName = "coin_transactions")
data class CoinTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: TransactionType,
    val amount: Int, // Positive for earnings, negative for spending
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val referenceId: String? = null
)
