package com.example.yourbagbuddy.data.remote.firebase

import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.model.SharedListItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firestore structure:
 * - sharedLists/{listId}: name, tripId, ownerId, memberIds (array), inviteCode
 * - sharedLists/{listId}/checklistItems/{itemId}: id, listId, name, category, isPacked, addedByUserId
 */
class FirebaseSharedListDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val sharedListsRef get() = firestore.collection(COLLECTION_SHARED_LISTS)

    suspend fun createSharedList(
        listId: String,
        name: String,
        tripId: String?,
        ownerId: String,
        inviteCode: String
    ): Result<Unit> {
        return try {
            val doc = mapOf(
                KEY_NAME to name,
                KEY_TRIP_ID to (tripId ?: ""),
                KEY_OWNER_ID to ownerId,
                KEY_MEMBER_IDS to listOf(ownerId),
                KEY_INVITE_CODE to inviteCode
            )
            sharedListsRef.document(listId).set(doc).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSharedList(listId: String): Result<SharedList?> {
        return try {
            val snap = sharedListsRef.document(listId).get().await()
            val data = snap.data ?: return Result.success(null)
            val list = SharedList(
                id = snap.id,
                name = (data[KEY_NAME] as? String) ?: "",
                tripId = (data[KEY_TRIP_ID] as? String)?.takeIf { it.isNotBlank() },
                ownerId = (data[KEY_OWNER_ID] as? String) ?: "",
                memberIds = (data[KEY_MEMBER_IDS] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                inviteCode = (data[KEY_INVITE_CODE] as? String) ?: ""
            )
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSharedListByInviteCode(inviteCode: String): Result<SharedList?> {
        return try {
            val snap = sharedListsRef
                .whereEqualTo(KEY_INVITE_CODE, inviteCode.uppercase())
                .limit(1)
                .get()
                .await()
            val doc = snap.documents.firstOrNull() ?: return Result.success(null)
            val data = doc.data ?: return Result.success(null)
            val list = SharedList(
                id = doc.id,
                name = (data[KEY_NAME] as? String) ?: "",
                tripId = (data[KEY_TRIP_ID] as? String)?.takeIf { it.isNotBlank() },
                ownerId = (data[KEY_OWNER_ID] as? String) ?: "",
                memberIds = (data[KEY_MEMBER_IDS] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                inviteCode = (data[KEY_INVITE_CODE] as? String) ?: ""
            )
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(listId: String, userId: String): Result<Unit> {
        return try {
            val ref = sharedListsRef.document(listId)
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val current = (snap.get(KEY_MEMBER_IDS) as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                if (userId !in current) {
                    current.add(userId)
                    tx.update(ref, KEY_MEMBER_IDS, current)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(listId: String, userId: String): Result<Unit> {
        return try {
            val ref = sharedListsRef.document(listId)
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val current = (snap.get(KEY_MEMBER_IDS) as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                current.remove(userId)
                tx.update(ref, KEY_MEMBER_IDS, current)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSharedListsForUser(userId: String): Flow<List<SharedList>> = callbackFlow {
        val listener = sharedListsRef
            .whereArrayContains(KEY_MEMBER_IDS, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lists = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    SharedList(
                        id = doc.id,
                        name = (data[KEY_NAME] as? String) ?: "",
                        tripId = (data[KEY_TRIP_ID] as? String)?.takeIf { it.isNotBlank() },
                        ownerId = (data[KEY_OWNER_ID] as? String) ?: "",
                        memberIds = (data[KEY_MEMBER_IDS] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        inviteCode = (data[KEY_INVITE_CODE] as? String) ?: ""
                    )
                } ?: emptyList()
                trySend(lists)
            }
        awaitClose { listener.remove() }
    }

    private fun itemsRef(listId: String) = sharedListsRef.document(listId).collection(SUBCOLLECTION_ITEMS)

    suspend fun upsertSharedListItem(listId: String, item: SharedListItem): Result<Unit> {
        return try {
            itemsRef(listId).document(item.id).set(
                mapOf(
                    KEY_ID to item.id,
                    KEY_LIST_ID to listId,
                    KEY_NAME to item.name,
                    KEY_CATEGORY to item.category.name,
                    KEY_IS_PACKED to item.isPacked,
                    KEY_ADDED_BY_USER_ID to (item.addedByUserId ?: "")
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSharedListItem(listId: String, itemId: String): Result<Unit> {
        return try {
            itemsRef(listId).document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Deletes the shared list document. List disappears for all members (owner and joined). */
    suspend fun deleteSharedList(listId: String): Result<Unit> {
        return try {
            sharedListsRef.document(listId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSharedListItems(listId: String): Flow<List<SharedListItem>> = callbackFlow {
        val listener = itemsRef(listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val categoryStr = data[KEY_CATEGORY] as? String ?: "OTHER"
                    val category = try {
                        com.example.yourbagbuddy.domain.model.ItemCategory.valueOf(categoryStr)
                    } catch (_: Exception) {
                        com.example.yourbagbuddy.domain.model.ItemCategory.OTHER
                    }
                    SharedListItem(
                        id = doc.id,
                        listId = listId,
                        name = (data[KEY_NAME] as? String) ?: "",
                        category = category,
                        isPacked = (data[KEY_IS_PACKED] as? Boolean) ?: false,
                        addedByUserId = (data[KEY_ADDED_BY_USER_ID] as? String)?.takeIf { it.isNotBlank() }
                    )
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSharedListItemsOnce(listId: String): Result<List<SharedListItem>> {
        return try {
            val snapshot = itemsRef(listId).get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val categoryStr = data[KEY_CATEGORY] as? String ?: "OTHER"
                val category = try {
                    com.example.yourbagbuddy.domain.model.ItemCategory.valueOf(categoryStr)
                } catch (_: Exception) {
                    com.example.yourbagbuddy.domain.model.ItemCategory.OTHER
                }
                SharedListItem(
                    id = doc.id,
                    listId = listId,
                    name = (data[KEY_NAME] as? String) ?: "",
                    category = category,
                    isPacked = (data[KEY_IS_PACKED] as? Boolean) ?: false,
                    addedByUserId = (data[KEY_ADDED_BY_USER_ID] as? String)?.takeIf { it.isNotBlank() }
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val COLLECTION_SHARED_LISTS = "sharedLists"
        private const val SUBCOLLECTION_ITEMS = "checklistItems"
        private const val KEY_NAME = "name"
        private const val KEY_TRIP_ID = "tripId"
        private const val KEY_OWNER_ID = "ownerId"
        private const val KEY_MEMBER_IDS = "memberIds"
        private const val KEY_INVITE_CODE = "inviteCode"
        private const val KEY_ID = "id"
        private const val KEY_LIST_ID = "listId"
        private const val KEY_CATEGORY = "category"
        private const val KEY_IS_PACKED = "isPacked"
        private const val KEY_ADDED_BY_USER_ID = "addedByUserId"
    }
}
