package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.model.SharedListItem
import kotlinx.coroutines.flow.Flow

interface SharedListRepository {
    fun observeSharedListItems(listId: String): Flow<List<SharedListItem>>
    fun getSharedListsForUser(userId: String): Flow<List<SharedList>>
    suspend fun getSharedList(listId: String): Result<SharedList?>
    suspend fun getSharedListByInviteCode(inviteCode: String): Result<SharedList?>
    suspend fun createSharedList(
        listId: String,
        name: String,
        tripId: String?,
        ownerId: String,
        inviteCode: String
    ): Result<Unit>
    suspend fun addMemberByInviteCode(inviteCode: String, userId: String): Result<SharedList?>
    suspend fun leaveSharedList(listId: String, userId: String): Result<Unit>
    /** Deletes the shared list (and removes it for all members). Call when owner deletes their checklist. */
    suspend fun deleteSharedList(listId: String): Result<Unit>
    suspend fun addSharedListItem(listId: String, item: SharedListItem): Result<Unit>
    suspend fun updateSharedListItem(listId: String, item: SharedListItem): Result<Unit>
    suspend fun deleteSharedListItem(listId: String, itemId: String): Result<Unit>
    suspend fun getSharedListItemsOnce(listId: String): Result<List<SharedListItem>>
}
