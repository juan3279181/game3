package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseCommunityManager {
    companion object {
        private const val TAG = "FirebaseCommunityManager"
        private const val COLLECTION_CAMPAIGNS = "community_campaigns"

        @Volatile
        private var instance: FirebaseCommunityManager? = null

        fun getInstance(): FirebaseCommunityManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseCommunityManager().also { instance = it }
            }
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore initialization fallback: ${e.message}")
            null
        }
    }

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _liveCommunityCount = MutableStateFlow(0)
    val liveCommunityCount: StateFlow<Int> = _liveCommunityCount.asStateFlow()

    /**
     * Observes real-time active community campaigns uploaded by any user worldwide.
     */
    fun observeActiveCampaigns(): Flow<List<CommunityCampaignDto>> = callbackFlow {
        val db = firestore
        if (db == null) {
            _isCloudConnected.value = false
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var listenerRegistration: ListenerRegistration? = null
        try {
            val query = db.collection(COLLECTION_CAMPAIGNS)
                .whereEqualTo("status", "ACTIVE")
                .limit(50)

            listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore community listen error: ${error.message}")
                    _isCloudConnected.value = false
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                _isCloudConnected.value = true
                if (snapshot != null) {
                    val campaigns = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val creatorId = doc.getString("creatorId") ?: ""
                            val creatorName = doc.getString("creatorName") ?: "Creator"
                            val platform = doc.getString("platform") ?: "YOUTUBE"
                            val campaignType = doc.getString("campaignType") ?: "VIEWS"
                            val title = doc.getString("title") ?: ""
                            val targetUrl = doc.getString("targetUrl") ?: ""
                            val costPerAction = doc.getLong("costPerAction")?.toInt() ?: 15
                            val targetQuantity = doc.getLong("targetQuantity")?.toInt() ?: 10
                            val deliveredQuantity = doc.getLong("deliveredQuantity")?.toInt() ?: 0
                            val status = doc.getString("status") ?: "ACTIVE"
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            @Suppress("UNCHECKED_CAST")
                            val completedUserIds = (doc.get("completedUserIds") as? List<String>) ?: emptyList()

                            CommunityCampaignDto(
                                id = id,
                                creatorId = creatorId,
                                creatorName = creatorName,
                                platform = platform,
                                campaignType = campaignType,
                                title = title,
                                targetUrl = targetUrl,
                                costPerAction = costPerAction,
                                targetQuantity = targetQuantity,
                                deliveredQuantity = deliveredQuantity,
                                status = status,
                                timestamp = timestamp,
                                completedUserIds = completedUserIds
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping doc ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    _liveCommunityCount.value = campaigns.size
                    trySend(campaigns)
                } else {
                    _liveCommunityCount.value = 0
                    trySend(emptyList())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting snapshot listener: ${e.message}")
            _isCloudConnected.value = false
            trySend(emptyList())
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Publishes a user's new campaign to Firestore so all other users see it in real-time.
     */
    suspend fun publishCampaign(campaign: CommunityCampaignDto): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Cloud service unavailable"))
        return try {
            val docId = if (campaign.id.isNotBlank()) campaign.id else db.collection(COLLECTION_CAMPAIGNS).document().id
            val campaignWithId = campaign.copy(id = docId)

            val data = hashMapOf(
                "id" to docId,
                "creatorId" to campaignWithId.creatorId,
                "creatorName" to campaignWithId.creatorName,
                "platform" to campaignWithId.platform,
                "campaignType" to campaignWithId.campaignType,
                "title" to campaignWithId.title,
                "targetUrl" to campaignWithId.targetUrl,
                "costPerAction" to campaignWithId.costPerAction,
                "targetQuantity" to campaignWithId.targetQuantity,
                "deliveredQuantity" to campaignWithId.deliveredQuantity,
                "status" to campaignWithId.status,
                "timestamp" to campaignWithId.timestamp,
                "completedUserIds" to campaignWithId.completedUserIds
            )

            db.collection(COLLECTION_CAMPAIGNS).document(docId).set(data).await()
            _isCloudConnected.value = true
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish campaign to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Atomically records that an earner completed a community campaign's action.
     * Increments deliveredQuantity and records earner ID so they don't repeat it.
     */
    suspend fun completeCommunityAction(campaignId: String, earnerUserId: String): Result<Boolean> {
        val db = firestore ?: return Result.failure(Exception("Cloud service unavailable"))
        return try {
            val docRef = db.collection(COLLECTION_CAMPAIGNS).document(campaignId)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentDelivered = snapshot.getLong("deliveredQuantity")?.toInt() ?: 0
                    val targetQuantity = snapshot.getLong("targetQuantity")?.toInt() ?: 1
                    val newDelivered = currentDelivered + 1

                    val updates = mutableMapOf<String, Any>(
                        "deliveredQuantity" to FieldValue.increment(1),
                        "completedUserIds" to FieldValue.arrayUnion(earnerUserId)
                    )

                    if (newDelivered >= targetQuantity) {
                        updates["status"] = "COMPLETED"
                    }

                    transaction.update(docRef, updates)
                }
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete community action on Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Updates status (ACTIVE, PAUSED, COMPLETED)
     */
    suspend fun updateCampaignStatus(campaignId: String, status: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Cloud service unavailable"))
        return try {
            db.collection(COLLECTION_CAMPAIGNS).document(campaignId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes campaign from Firestore
     */
    suspend fun deleteCampaign(campaignId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Cloud service unavailable"))
        return try {
            db.collection(COLLECTION_CAMPAIGNS).document(campaignId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
