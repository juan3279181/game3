package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CampaignType
import com.example.data.model.SocialPlatform

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val platform: SocialPlatform,
    val taskType: CampaignType,
    val title: String,
    val author: String,
    val targetUrl: String,
    val rewardCoins: Int,
    val durationSeconds: Int = 15,
    val isCompleted: Boolean = false,
    val completedAt: Long = 0L,
    val category: String = "Community Task"
)
