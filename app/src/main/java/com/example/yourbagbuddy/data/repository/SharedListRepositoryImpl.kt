package com.example.yourbagbuddy.data.repository

import com.example.yourbagbuddy.data.remote.firebase.FirebaseSharedListDataSource
import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.model.SharedListItem
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SharedListRepositoryImpl @Inject constructor(
    private val firebaseSharedListDataSource: FirebaseSharedListDataSource
) : SharedListRepository {

    override fun observeSharedListItems(listId: String): Flow<List<SharedListItem>> {
        return firebaseSharedListDataSource.observeSharedListItems(listId)
    }

    override fun getSharedListsForUser(userId: String): Flow<List<SharedList>> {
        return firebaseSharedListDataSource.getSharedListsForUser(userId)
    }

    override suspend fun getSharedList(listId: String): Result<SharedList?> {
        return firebaseSharedListDataSource.getSharedList(listId)
    }

    override suspend fun getSharedListByInviteCode(inviteCode: String): Result<SharedList?> {
        return firebaseSharedListDataSource.getSharedListByInviteCode(inviteCode.trim().uppercase())
    }

    override suspend fun createSharedList(
        listId: String,
        name: String,
        tripId: String?,
        ownerId: String,
        inviteCode: String
    ): Result<Unit> {
        return firebaseSharedListDataSource.createSharedList(
            listId = listId,
            name = name,
            tripId = tripId,
            ownerId = ownerId,
            inviteCode = inviteCode.uppercase()
        )
    }

    override suspend fun addMemberByInviteCode(inviteCode: String, userId: String): Result<SharedList?> {
        val listResult = firebaseSharedListDataSource.getSharedListByInviteCode(inviteCode.trim().uppercase())
        listResult.onFailure { return Result.failure(it) }
        val list = listResult.getOrNull() ?: return Result.failure(IllegalArgumentException("Invalid invite code"))
        val addResult = firebaseSharedListDataSource.addMember(list.id, userId)
        return addResult.map { list }
    }

    override suspend fun leaveSharedList(listId: String, userId: String): Result<Unit> {
        return firebaseSharedListDataSource.removeMember(listId, userId)
    }

    override suspend fun deleteSharedList(listId: String): Result<Unit> {
        return firebaseSharedListDataSource.deleteSharedList(listId)
    }

    override suspend fun addSharedListItem(listId: String, item: SharedListItem): Result<Unit> {
        return firebaseSharedListDataSource.upsertSharedListItem(listId, item)
    }

    override suspend fun updateSharedListItem(listId: String, item: SharedListItem): Result<Unit> {
        return firebaseSharedListDataSource.upsertSharedListItem(listId, item)
    }

    override suspend fun deleteSharedListItem(listId: String, itemId: String): Result<Unit> {
        return firebaseSharedListDataSource.deleteSharedListItem(listId, itemId)
    }

    override suspend fun getSharedListItemsOnce(listId: String): Result<List<SharedListItem>> {
        return firebaseSharedListDataSource.getSharedListItemsOnce(listId)
    }
}
