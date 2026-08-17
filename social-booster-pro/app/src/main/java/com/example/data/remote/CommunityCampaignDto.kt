package com.example.data.remote

import com.example.data.local.CampaignStatus
import com.example.data.local.TaskEntity
import com.example.data.model.CampaignType
import com.example.data.model.SocialPlatform

data class CommunityCampaignDto(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val platform: String = "YOUTUBE",
    val campaignType: String = "VIEWS",
    val title: String = "",
    val targetUrl: String = "",
    val costPerAction: Int = 15,
    val targetQuantity: Int = 10,
    val deliveredQuantity: Int = 0,
    val status: String = "ACTIVE",
    val timestamp: Long = System.currentTimeMillis(),
    val completedUserIds: List<String> = emptyList()
) {
    fun toSocialPlatform(): SocialPlatform {
        return try {
            SocialPlatform.valueOf(platform.uppercase())
        } catch (e: Exception) {
            SocialPlatform.YOUTUBE
        }
    }

    fun toCampaignType(): CampaignType {
        return try {
            CampaignType.valueOf(campaignType.uppercase())
        } catch (e: Exception) {
            CampaignType.VIEWS
        }
    }

    fun toTaskEntity(): TaskEntity {
        val sp = toSocialPlatform()
        val ct = toCampaignType()
        return TaskEntity(
            id = id.hashCode().toLong(),
            platform = sp,
            taskType = ct,
            title = title.ifBlank { "${sp.displayName} ${ct.title}" },
            author = if (creatorName.isNotBlank()) "@$creatorName" else "@CommunityCreator",
            targetUrl = targetUrl,
            rewardCoins = costPerAction,
            durationSeconds = if (ct == CampaignType.VIEWS) 30 else 15,
            isCompleted = false,
            category = "Community Post"
        )
    }
}
