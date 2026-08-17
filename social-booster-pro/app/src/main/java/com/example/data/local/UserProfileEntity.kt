package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "Creator_Pro",
    val avatarIndex: Int = 0,
    val coinsBalance: Int = 150, // Starter welcome gift!
    val vipLevel: Int = 1,
    val totalEarned: Int = 150,
    val totalSpent: Int = 0,
    val tasksCompletedCount: Int = 0,
    val dailyStreak: Int = 1,
    val lastCheckInDate: Long = 0L,
    val freeSpinsLeft: Int = 3,
    val lastSpinDate: Long = 0L
)
