package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CampaignType
import com.example.data.model.SocialPlatform

enum class CampaignStatus {
    ACTIVE,
    PAUSED,
    COMPLETED
}

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val platform: SocialPlatform,
    val campaignType: CampaignType,
    val title: String,
    val targetUrl: String,
    val costPerAction: Int,
    val targetQuantity: Int,
    val deliveredQuantity: Int = 0,
    val status: CampaignStatus = CampaignStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalCost: Int get() = targetQuantity * costPerAction
    val remainingQuantity: Int get() = (targetQuantity - deliveredQuantity).coerceAtLeast(0)
    val progressPercentage: Float get() = if (targetQuantity > 0) (deliveredQuantity.toFloat() / targetQuantity).coerceIn(0f, 1f) else 0f
}
