package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CampaignType
import com.example.data.model.SocialPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        CampaignEntity::class,
        TaskEntity::class,
        CoinTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun campaignDao(): CampaignDao
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "followme_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val userDao = database.userDao()
            val taskDao = database.taskDao()
            val transactionDao = database.transactionDao()
            val campaignDao = database.campaignDao()

            // 1. Initial User Profile
            userDao.insertOrUpdate(
                UserProfileEntity(
                    id = 1,
                    username = "SuperCreator_01",
                    avatarIndex = 1,
                    coinsBalance = 250, // 250 Coins Welcome Bonus!
                    vipLevel = 1,
                    totalEarned = 250,
                    totalSpent = 0,
                    tasksCompletedCount = 0,
                    dailyStreak = 1,
                    lastCheckInDate = 0L,
                    freeSpinsLeft = 3
                )
            )

            // 2. Welcome Transaction
            transactionDao.insertTransaction(
                CoinTransactionEntity(
                    type = TransactionType.WELCOME_BONUS,
                    amount = 250,
                    title = "Welcome Gift",
                    description = "Thanks for joining FollowMe community! Here are 250 free coins."
                )
            )

            // 3. Initial Community Tasks
            val sampleTasks = listOf(
                TaskEntity(
                    platform = SocialPlatform.YOUTUBE,
                    taskType = CampaignType.SUBSCRIBERS,
                    title = "MrBeast Highlights & Challenges",
                    author = "@BeastFanClub",
                    targetUrl = "https://youtube.com/@mrbeast",
                    rewardCoins = 35,
                    durationSeconds = 15,
                    category = "Gaming & Tech"
                ),
                TaskEntity(
                    platform = SocialPlatform.INSTAGRAM,
                    taskType = CampaignType.FOLLOWERS,
                    title = "Aesthetic Photography & Travel Stories",
                    author = "@wanderlust_lens",
                    targetUrl = "https://instagram.com/natgeo",
                    rewardCoins = 30,
                    durationSeconds = 10,
                    category = "Lifestyle & Travel"
                ),
                TaskEntity(
                    platform = SocialPlatform.TIKTOK,
                    taskType = CampaignType.FOLLOWERS,
                    title = "Viral Dance & Trend Hub Daily",
                    author = "@dailyvibez_official",
                    targetUrl = "https://tiktok.com/@tiktok",
                    rewardCoins = 25,
                    durationSeconds = 10,
                    category = "Entertainment"
                ),
                TaskEntity(
                    platform = SocialPlatform.YOUTUBE,
                    taskType = CampaignType.VIEWS,
                    title = "Android Jetpack Compose Full Tutorial 2026",
                    author = "@AndroidMastery",
                    targetUrl = "https://youtube.com/watch?v=dQw4w9WgXcQ",
                    rewardCoins = 20,
                    durationSeconds = 20,
                    category = "Education & Tech"
                ),
                TaskEntity(
                    platform = SocialPlatform.TWITTER_X,
                    taskType = CampaignType.FOLLOWERS,
                    title = "Tech Insider & AI News Alerts",
                    author = "@TechDailyRadar",
                    targetUrl = "https://x.com/Android",
                    rewardCoins = 25,
                    durationSeconds = 10,
                    category = "Tech News"
                ),
                TaskEntity(
                    platform = SocialPlatform.TWITCH,
                    taskType = CampaignType.FOLLOWERS,
                    title = "Pro Esports Live Streamer Community",
                    author = "@ApexLegendsPro",
                    targetUrl = "https://twitch.tv/twitch",
                    rewardCoins = 30,
                    durationSeconds = 15,
                    category = "Gaming"
                ),
                TaskEntity(
                    platform = SocialPlatform.INSTAGRAM,
                    taskType = CampaignType.LIKES,
                    title = "New Sunset Art Reel #Creative",
                    author = "@artstudio_vibes",
                    targetUrl = "https://instagram.com/instagram",
                    rewardCoins = 15,
                    durationSeconds = 10,
                    category = "Art & Design"
                ),
                TaskEntity(
                    platform = SocialPlatform.THREADS,
                    taskType = CampaignType.FOLLOWERS,
                    title = "Daily Motivational Creator Quotes",
                    author = "@mindset_daily",
                    targetUrl = "https://threads.net/@instagram",
                    rewardCoins = 20,
                    durationSeconds = 10,
                    category = "Motivation"
                )
            )
            taskDao.insertTasks(sampleTasks)

            // 4. Sample active starter campaigns to show progress
            campaignDao.insertCampaign(
                CampaignEntity(
                    platform = SocialPlatform.YOUTUBE,
                    campaignType = CampaignType.SUBSCRIBERS,
                    title = "My Gaming Channel Growth",
                    targetUrl = "https://youtube.com/@supergamer",
                    costPerAction = 20,
                    targetQuantity = 50,
                    deliveredQuantity = 18,
                    status = CampaignStatus.ACTIVE
                )
            )
        }
    }
}
