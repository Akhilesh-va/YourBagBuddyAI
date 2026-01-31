package com.example.yourbagbuddy.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.ItemCategory
import com.example.yourbagbuddy.domain.model.RepeatType
import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.model.TripType
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.usecase.checklist.AddChecklistItemUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.DeleteChecklistItemUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.GetChecklistItemsUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.ToggleItemPackedUseCase
import com.example.yourbagbuddy.domain.model.TravelDocument
import com.example.yourbagbuddy.domain.usecase.reminder.GetReminderForChecklistUseCase
import com.example.yourbagbuddy.domain.usecase.reminder.ScheduleReminderUseCase
import com.example.yourbagbuddy.domain.usecase.traveldocument.EnableDocumentsChecklistUseCase
import com.example.yourbagbuddy.domain.usecase.traveldocument.GetTravelDocumentsUseCase
import com.example.yourbagbuddy.domain.usecase.traveldocument.ToggleTravelDocumentCheckedUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.AddSharedListItemUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.CreateSharedListAndLinkTripUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.DeleteSharedListItemUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.DeleteSharedListUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.GetSharedListUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.GetSharedListsForUserUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.JoinSharedListUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.LeaveSharedListUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.ObserveSharedListItemsUseCase
import com.example.yourbagbuddy.domain.usecase.sharedlist.ToggleSharedListItemPackedUseCase
import com.example.yourbagbuddy.domain.usecase.trip.CreateTripUseCase
import com.example.yourbagbuddy.domain.usecase.trip.DeleteTripUseCase
import com.example.yourbagbuddy.domain.usecase.trip.GetAllTripsUseCase
import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.model.SharedListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

private const val TAG = "ChecklistVM"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val getAllTripsUseCase: GetAllTripsUseCase,
    private val getChecklistItemsUseCase: GetChecklistItemsUseCase,
    private val addChecklistItemUseCase: AddChecklistItemUseCase,
    private val toggleItemPackedUseCase: ToggleItemPackedUseCase,
    private val deleteChecklistItemUseCase: DeleteChecklistItemUseCase,
    private val createTripUseCase: CreateTripUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val getReminderForChecklistUseCase: GetReminderForChecklistUseCase,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val getTravelDocumentsUseCase: GetTravelDocumentsUseCase,
    private val enableDocumentsChecklistUseCase: EnableDocumentsChecklistUseCase,
    private val toggleTravelDocumentCheckedUseCase: ToggleTravelDocumentCheckedUseCase,
    private val createSharedListAndLinkTripUseCase: CreateSharedListAndLinkTripUseCase,
    private val addSharedListItemUseCase: AddSharedListItemUseCase,
    private val toggleSharedListItemPackedUseCase: ToggleSharedListItemPackedUseCase,
    private val deleteSharedListItemUseCase: DeleteSharedListItemUseCase,
    private val deleteSharedListUseCase: DeleteSharedListUseCase,
    private val joinSharedListUseCase: JoinSharedListUseCase,
    private val leaveSharedListUseCase: LeaveSharedListUseCase,
    private val getSharedListsForUserUseCase: GetSharedListsForUserUseCase,
    private val getSharedListUseCase: GetSharedListUseCase,
    private val observeSharedListItemsUseCase: ObserveSharedListItemsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()
    
    private val selectedTripId = MutableStateFlow<String?>(null)
    private val selectedSharedListId = MutableStateFlow<String?>(null)
    private val sharedListsState = MutableStateFlow<List<SharedList>>(emptyList())
    /** Trip id we last received items for. Used to avoid auto-deleting a newly selected empty trip. */
    private var lastTripIdItemsReceivedFor: String? = null

    init {
        loadChecklist()
    }
    
    private fun loadChecklist() {
        viewModelScope.launch {
            authRepository.currentUser.flatMapLatest { user ->
                if (user?.id != null) getSharedListsForUserUseCase(user.id)
                else flowOf(emptyList())
            }.collect { sharedListsState.value = it }
        }
        viewModelScope.launch {
            getAllTripsUseCase().collect { trips ->
                val currentSelected = selectedTripId.value
                Log.d(TAG, "trips flow emitted: count=${trips.size}, ids=${trips.map { it.id }}, names=${trips.map { it.name }}, currentSelected=$currentSelected")
                // If current selection is in the list, keep it. If not in list (e.g. newly created
                // and list hasn't re-emitted yet), keep current selection so new checklist stays
                // selected. Only fall back to first trip when we have no selection.
                val nextSelected = when {
                    trips.any { it.id == currentSelected } -> currentSelected
                    currentSelected != null -> currentSelected
                    else -> trips.firstOrNull()?.id
                }
                // Avoid overwriting with a stale emission that's missing the selected trip
                // (e.g. after create, flatMapLatest can re-emit before Room has the new row).
                // If the selected trip is in current state but not in the flow result, merge it in
                // so the newly created list always shows in the UI.
                val tripsToShow = when {
                    currentSelected != null && !trips.any { it.id == currentSelected } -> {
                        val selectedFromState = _uiState.value.trips.firstOrNull { it.id == currentSelected }
                        if (selectedFromState != null) {
                            Log.d(TAG, "merge: selected trip not in flow, adding from state: ${selectedFromState.name} (${selectedFromState.id})")
                            trips + selectedFromState
                        } else {
                            Log.d(TAG, "keep: selected trip not in flow, keeping state trips: count=${_uiState.value.trips.size}, names=${_uiState.value.trips.map { it.name }}")
                            _uiState.value.trips
                        }
                    }
                    else -> trips
                }

                Log.d(TAG, "tripsToShow: count=${tripsToShow.size}, names=${tripsToShow.map { it.name }}, hasTrip=${tripsToShow.isNotEmpty()}")
                selectedTripId.value = nextSelected
                _uiState.value = _uiState.value.copy(
                    trips = tripsToShow,
                    hasTrip = tripsToShow.isNotEmpty()
                )
            }
        }

        viewModelScope.launch {
            combine(
                selectedTripId,
                selectedSharedListId,
                getAllTripsUseCase(),
                sharedListsState
            ) { tripId, sharedListId, trips, sharedLists ->
                val selectedTrip = trips.firstOrNull { it.id == tripId }
                val effectiveSharedListId = selectedTrip?.sharedListId ?: sharedListId
                val selectedSharedList = sharedLists.firstOrNull { it.id == sharedListId }
                val checklistName = selectedTrip?.name ?: selectedSharedList?.name ?: ""
                Log.d(TAG, "combine: tripId=$tripId tripsCount=${trips.size} selectedTrip=${selectedTrip?.name ?: "null"} checklistName=$checklistName")
                _uiState.value = _uiState.value.copy(
                    selectedTripId = tripId,
                    selectedSharedListId = sharedListId,
                    effectiveSharedListId = effectiveSharedListId,
                    sharedLists = sharedLists,
                    checklistName = checklistName,
                    isSharedList = effectiveSharedListId != null
                )
                Pair(effectiveSharedListId, tripId)
            }.flatMapLatest { (effectiveSharedListId, tripId) ->
                if (effectiveSharedListId != null) {
                    observeSharedListItemsUseCase(effectiveSharedListId).map { sharedItems ->
                        sharedItems.map { it.toChecklistItem(effectiveSharedListId) }
                    }
                } else if (tripId != null) {
                    getChecklistItemsUseCase(tripId)
                } else {
                    flowOf(emptyList())
                }
            }.collect { items ->
                updateUiState(items)
            }
        }

        // Load reminder for selected checklist when selection changes
        viewModelScope.launch {
            selectedTripId.collect { tripId ->
                if (tripId != null) {
                    _uiState.value = _uiState.value.copy(reminderLoading = true)
                    val reminder = getReminderForChecklistUseCase(tripId)
                    _uiState.value = _uiState.value.copy(
                        reminderEnabled = reminder?.isEnabled == true,
                        reminderTimeMillis = reminder?.reminderTime?.time ?: defaultReminderTimeMillis(),
                        repeatType = reminder?.repeatType ?: RepeatType.ONCE,
                        repeatIntervalDays = reminder?.repeatIntervalDays ?: 1,
                        stopWhenCompleted = reminder?.stopWhenCompleted ?: true,
                        stopAtTripStart = reminder?.stopAtTripStart ?: true,
                        reminderLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        reminderEnabled = false,
                        reminderTimeMillis = defaultReminderTimeMillis(),
                        repeatType = RepeatType.ONCE,
                        repeatIntervalDays = 1,
                        stopWhenCompleted = true,
                        stopAtTripStart = true,
                        reminderLoading = false
                    )
                }
            }
        }

        // Load travel documents for selected trip
        viewModelScope.launch {
            selectedTripId.flatMapLatest { tripId ->
                if (tripId != null) getTravelDocumentsUseCase(tripId)
                else flowOf(emptyList())
            }.collect { documents ->
                _uiState.value = _uiState.value.copy(travelDocuments = documents)
            }
        }
    }

    private fun defaultReminderTimeMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 20)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    private fun updateUiState(items: List<ChecklistItem>) {
        val currentTripId = selectedTripId.value
        val isSharedListMode = _uiState.value.effectiveSharedListId != null

        // Only auto-delete the trip when the user had items and deleted the last one.
        // Do NOT delete when totalCount is already 0 (e.g. newly created empty checklist).
        // Do NOT auto-delete when viewing a shared list (group list).
        // Do NOT auto-delete when we just switched to this trip (totalCount is stale from previous trip).
        val isSameTripWeHadItemsFor = lastTripIdItemsReceivedFor == currentTripId
        if (!isSharedListMode && currentTripId != null && items.isEmpty() && _uiState.value.totalCount > 0 && isSameTripWeHadItemsFor) {
            Log.d(TAG, "updateUiState: auto-deleting trip $currentTripId (last item removed)")
            viewModelScope.launch {
                deleteTripUseCase(currentTripId)
                selectedTripId.value = null
            }

            _uiState.value = _uiState.value.copy(
                items = emptyList(),
                completedCount = 0,
                totalCount = 0,
                isLoading = false,
                trips = _uiState.value.trips.filterNot { it.id == currentTripId },
                selectedTripId = null,
                hasTrip = _uiState.value.trips.any { it.id != currentTripId }
            )
            return
        }

        lastTripIdItemsReceivedFor = currentTripId
        val completedCount = items.count { it.isPacked }
        val totalCount = items.size
        _uiState.value = _uiState.value.copy(
            items = items.sortedBy { it.isPacked }, // Show incomplete items first
            completedCount = completedCount,
            totalCount = totalCount,
            isLoading = false
        )
    }
    
    fun addItem(name: String, category: ItemCategory = ItemCategory.OTHER) {
        viewModelScope.launch {
            val effectiveSharedListId = _uiState.value.effectiveSharedListId
            val tripId = selectedTripId.value
            if (effectiveSharedListId == null && tripId == null) {
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = if (effectiveSharedListId != null) {
                val userId = authRepository.getCurrentUser()?.id
                addSharedListItemUseCase(effectiveSharedListId, name, category, userId)
            } else {
                addChecklistItemUseCase(tripId!!, name, category)
            }
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to add item"
                    )
                }
            )
        }
    }

    fun createChecklistAndAddItem(
        checklistName: String,
        itemName: String,
        category: ItemCategory = ItemCategory.OTHER
    ) {
        viewModelScope.launch {
            val trimmedChecklistName = checklistName.trim()
            val trimmedItemName = itemName.trim()
            if (trimmedChecklistName.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "Checklist name cannot be empty"
                )
                return@launch
            }
            if (trimmedItemName.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "Item name cannot be empty"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val now = Date()
            val userId = authRepository.getCurrentUser()?.id
            val createResult = createTripUseCase(
                name = trimmedChecklistName,
                destination = trimmedChecklistName,
                startDate = now,
                endDate = now,
                numberOfPeople = 1,
                tripType = TripType.VACATION,
                userId = userId
            )

            createResult.fold(
                onSuccess = { tripId ->
                    selectedTripId.value = tripId
                    val newTrip = Trip(
                        id = tripId,
                        name = trimmedChecklistName,
                        destination = trimmedChecklistName,
                        startDate = now,
                        endDate = now,
                        numberOfPeople = 1,
                        tripType = TripType.VACATION,
                        createdDate = now,
                        userId = userId
                    )
                    _uiState.value = _uiState.value.copy(
                        trips = _uiState.value.trips + newTrip,
                        hasTrip = true,
                        checklistName = trimmedChecklistName,
                        selectedTripId = tripId
                    )
                    val addResult = addChecklistItemUseCase(tripId, trimmedItemName, category)
                    addResult.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = null,
                                createSuccessForToast = "Created: $trimmedChecklistName"
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to add item"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create checklist"
                    )
                }
            )
        }
    }
    
    fun toggleItem(itemId: String, isPacked: Boolean) {
        viewModelScope.launch {
            val effectiveSharedListId = _uiState.value.effectiveSharedListId
            if (effectiveSharedListId != null) {
                val item = _uiState.value.items.find { it.id == itemId } ?: return@launch
                val sharedItem = SharedListItem(
                    id = item.id,
                    listId = effectiveSharedListId,
                    name = item.name,
                    category = item.category,
                    isPacked = isPacked,
                    addedByUserId = null
                )
                toggleSharedListItemPackedUseCase(effectiveSharedListId, sharedItem, isPacked)
            } else {
                toggleItemPackedUseCase(itemId, isPacked)
            }
        }
    }
    
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val effectiveSharedListId = _uiState.value.effectiveSharedListId
            val tripId = selectedTripId.value
            val shouldDeleteTrip = !_uiState.value.isSharedList && _uiState.value.totalCount <= 1 && tripId != null

            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = if (effectiveSharedListId != null) {
                deleteSharedListItemUseCase(effectiveSharedListId, itemId)
            } else {
                deleteChecklistItemUseCase(itemId)
            }
            result.fold(
                onSuccess = {
                    if (shouldDeleteTrip) {
                        deleteTripUseCase(tripId!!)
                        selectedTripId.value = null
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete item"
                    )
                }
            )
        }
    }

    /** Deletes the currently selected checklist (trip) and all its items. Only for own trips (when a trip is selected).
     * If the trip is linked to a shared list, deletes the shared list in Firestore too so it disappears for all joined members. */
    fun deleteCurrentChecklist() {
        val tripId = selectedTripId.value ?: return
        val sharedListId = _uiState.value.trips.firstOrNull { it.id == tripId }?.sharedListId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (sharedListId != null) {
                deleteSharedListUseCase(sharedListId).fold(
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to delete shared list"
                        )
                        return@launch
                    },
                    onSuccess = {
                        sharedListsState.value = sharedListsState.value.filterNot { it.id == sharedListId }
                    }
                )
            }
            deleteTripUseCase(tripId).fold(
                onSuccess = {
                    selectedTripId.value = null
                    if (sharedListId != null) selectedSharedListId.value = null
                    val tripsAfter = _uiState.value.trips.filterNot { it.id == tripId }
                    val sharedListsAfter = sharedListId?.let { id ->
                        _uiState.value.sharedLists.filterNot { it.id == id }
                    } ?: _uiState.value.sharedLists
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = emptyList(),
                        completedCount = 0,
                        totalCount = 0,
                        trips = tripsAfter,
                        sharedLists = sharedListsAfter,
                        selectedTripId = null,
                        hasTrip = tripsAfter.isNotEmpty() || sharedListsAfter.isNotEmpty(),
                        checklistName = "",
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete checklist"
                    )
                }
            )
        }
    }

    fun selectTrip(tripId: String) {
        selectedTripId.value = tripId
        selectedSharedListId.value = null
    }

    fun selectSharedList(listId: String?) {
        selectedSharedListId.value = listId
        if (listId != null) selectedTripId.value = null
    }

    /** Switch to "My checklists" tab: clear shared list selection, select first trip if any. */
    fun switchToMyChecklistsTab() {
        selectedSharedListId.value = null
        selectedTripId.value = _uiState.value.trips.firstOrNull()?.id
    }

    /** Switch to "Shared lists" tab: clear trip selection, select first shared list if any. */
    fun switchToSharedListsTab() {
        selectedTripId.value = null
        selectedSharedListId.value = _uiState.value.sharedLists.firstOrNull()?.id
    }

    fun leaveSharedList() {
        viewModelScope.launch {
            val listId = selectedSharedListId.value ?: return@launch
            val userId = authRepository.getCurrentUser()?.id ?: return@launch
            leaveSharedListUseCase(listId, userId).fold(
                onSuccess = {
                    selectedSharedListId.value = null
                    selectedTripId.value = _uiState.value.trips.firstOrNull()?.id
                    _uiState.value = _uiState.value.copy(
                        checklistName = _uiState.value.trips.firstOrNull()?.name ?: "",
                        isSharedList = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Could not leave list"
                    )
                }
            )
        }
    }

    fun createSharedListAndLinkTrip() {
        viewModelScope.launch {
            val tripId = selectedTripId.value
            val ownerId = authRepository.getCurrentUser()?.id
            when {
                tripId == null -> {
                    _uiState.value = _uiState.value.copy(error = "Select a checklist first")
                    return@launch
                }
                ownerId == null -> {
                    _uiState.value = _uiState.value.copy(error = "Sign in to share with group")
                    return@launch
                }
                else -> {
                    val listName = _uiState.value.checklistName.ifBlank { "Group list" }
                    val currentItems = _uiState.value.items
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    createSharedListAndLinkTripUseCase(tripId, listName, currentItems, ownerId)
                        .fold(
                            onSuccess = { (_, inviteCode) ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    inviteCodeForShare = inviteCode,
                                    showShareDialog = true,
                                    isResharingList = false,
                                    error = null
                                )
                            },
                            onFailure = { e ->
                                val message = friendlyShareErrorMessage(e)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = message,
                                    shareErrorForToast = message
                                )
                            }
                        )
                }
            }
        }
    }

    fun dismissShareDialog() {
        _uiState.value = _uiState.value.copy(
            showShareDialog = false,
            inviteCodeForShare = null,
            isResharingList = false
        )
    }

    /** Show the share dialog with the existing invite code when this trip is already shared. */
    fun showShareDialogForExistingList() {
        viewModelScope.launch {
            val tripId = selectedTripId.value ?: return@launch
            val trip = _uiState.value.trips.firstOrNull { it.id == tripId } ?: return@launch
            val sharedListId = trip.sharedListId ?: return@launch
            var sharedList = _uiState.value.sharedLists.firstOrNull { it.id == sharedListId }
                ?: sharedListsState.value.firstOrNull { it.id == sharedListId }
            if (sharedList == null) {
                getSharedListUseCase(sharedListId).fold(
                    onSuccess = { sharedList = it },
                    onFailure = { Log.w(TAG, "Failed to fetch shared list for invite code", it) }
                )
            }
            val code = sharedList?.inviteCode?.takeIf { it.isNotBlank() }
            if (code != null) {
                _uiState.value = _uiState.value.copy(
                    inviteCodeForShare = code,
                    showShareDialog = true,
                    isResharingList = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Could not load invite code. Try again."
                )
            }
        }
    }

    fun clearShareErrorForToast() {
        _uiState.value = _uiState.value.copy(shareErrorForToast = null)
    }

    fun clearCreateErrorForToast() {
        _uiState.value = _uiState.value.copy(createErrorForToast = null)
    }

    fun clearCreateSuccessForToast() {
        _uiState.value = _uiState.value.copy(createSuccessForToast = null)
    }

    private fun friendlyShareErrorMessage(e: Throwable): String {
        return friendlyNetworkErrorMessage(e) ?: (e.message ?: "Failed to share list")
    }

    private fun friendlyJoinErrorMessage(e: Throwable): String {
        return friendlyNetworkErrorMessage(e) ?: (e.message ?: "Invalid code or couldn't join")
    }

    private fun friendlyNetworkErrorMessage(e: Throwable): String? {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("resolve") || msg.contains("unknownhost") || msg.contains("unavailable")
                || msg.contains("no address") || msg.contains("hostname") || msg.contains("connection") ->
                "No internet. Check your connection and try again."
            else -> null
        }
    }

    fun showJoinDialog() {
        _uiState.value = _uiState.value.copy(showJoinDialog = true, joinError = null, pendingJoinCode = null)
    }

    /** Open Join dialog with code prefilled (e.g. from invite link yourbagbuddy://join/CODE). */
    fun openJoinDialogWithCode(code: String) {
        _uiState.value = _uiState.value.copy(
            showJoinDialog = true,
            joinError = null,
            pendingJoinCode = code.uppercase().take(6)
        )
    }

    fun clearPendingJoinCode() {
        _uiState.value = _uiState.value.copy(pendingJoinCode = null)
    }

    fun dismissJoinDialog() {
        _uiState.value = _uiState.value.copy(
            showJoinDialog = false,
            joinError = null
        )
    }

    fun joinSharedList(inviteCode: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.id
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    joinError = "Sign in to join a list"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(joinError = null)
            joinSharedListUseCase(inviteCode.trim(), userId).fold(
                onSuccess = { sharedList ->
                    selectedSharedListId.value = sharedList.id
                    selectedTripId.value = null
                    _uiState.value = _uiState.value.copy(
                        showJoinDialog = false,
                        joinError = null,
                        checklistName = sharedList.name,
                        isSharedList = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        joinError = friendlyJoinErrorMessage(e)
                    )
                }
            )
        }
    }

    fun createChecklist(name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "Checklist name cannot be empty"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val now = Date()
            val userId = authRepository.getCurrentUser()?.id
            val result = createTripUseCase(
                name = trimmed,
                destination = trimmed,
                startDate = now,
                endDate = now,
                numberOfPeople = 1,
                tripType = TripType.VACATION,
                userId = userId
            )

            result.fold(
                onSuccess = { tripId ->
                    val newTrip = Trip(
                        id = tripId,
                        name = trimmed,
                        destination = trimmed,
                        startDate = now,
                        endDate = now,
                        numberOfPeople = 1,
                        tripType = TripType.VACATION,
                        createdDate = now,
                        userId = userId
                    )
                    val tripsBefore = _uiState.value.trips.size
                    selectedTripId.value = tripId
                    _uiState.value = _uiState.value.copy(
                        trips = _uiState.value.trips + newTrip,
                        hasTrip = true,
                        checklistName = trimmed,
                        selectedTripId = tripId,
                        isLoading = false,
                        error = null,
                        createSuccessForToast = "Created: $trimmed"
                    )
                    Log.d(TAG, "createChecklist SUCCESS: tripId=$tripId name=$trimmed tripsBefore=$tripsBefore tripsAfter=${_uiState.value.trips.size} names=${_uiState.value.trips.map { it.name }}")
                },
                onFailure = { error ->
                    val message = error.message ?: "Failed to create checklist"
                    Log.e(TAG, "createChecklist FAILED: $message", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message,
                        createErrorForToast = message
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Reminder: UI requests notification permission before calling saveReminder with isEnabled = true
    fun saveReminder(
        reminderTimeMillis: Long,
        repeatType: RepeatType,
        repeatIntervalDays: Int,
        isEnabled: Boolean,
        stopWhenCompleted: Boolean,
        stopAtTripStart: Boolean
    ) {
        val checklistId = selectedTripId.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(reminderLoading = true, error = null)
            val result = scheduleReminderUseCase(
                checklistId = checklistId,
                reminderTime = Date(reminderTimeMillis),
                repeatType = repeatType,
                repeatIntervalDays = if (repeatType == RepeatType.EVERY_X_DAYS) repeatIntervalDays else null,
                isEnabled = isEnabled,
                stopWhenCompleted = stopWhenCompleted,
                stopAtTripStart = stopAtTripStart
            )
            _uiState.value = _uiState.value.copy(
                reminderLoading = false,
                reminderEnabled = isEnabled,
                reminderTimeMillis = reminderTimeMillis,
                repeatType = repeatType,
                repeatIntervalDays = repeatIntervalDays,
                stopWhenCompleted = stopWhenCompleted,
                stopAtTripStart = stopAtTripStart,
                error = result.fold(
                    onSuccess = { null },
                    onFailure = { it.message ?: "Failed to save reminder" }
                )
            )
        }
    }

    fun updateReminderTime(reminderTimeMillis: Long) {
        _uiState.value = _uiState.value.copy(reminderTimeMillis = reminderTimeMillis)
    }

    fun updateReminderRepeatType(repeatType: RepeatType, intervalDays: Int) {
        _uiState.value = _uiState.value.copy(
            repeatType = repeatType,
            repeatIntervalDays = intervalDays.coerceAtLeast(1)
        )
    }

    fun updateReminderStopConditions(stopWhenCompleted: Boolean, stopAtTripStart: Boolean) {
        _uiState.value = _uiState.value.copy(
            stopWhenCompleted = stopWhenCompleted,
            stopAtTripStart = stopAtTripStart
        )
    }

    fun enableDocumentsChecklist() {
        val tripId = selectedTripId.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(documentsLoading = true, error = null)
            enableDocumentsChecklistUseCase(tripId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(documentsLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        documentsLoading = false,
                        error = e.message ?: "Failed to add documents checklist"
                    )
                }
            )
        }
    }

    fun toggleDocumentChecked(documentId: String, isChecked: Boolean) {
        viewModelScope.launch {
            toggleTravelDocumentCheckedUseCase(documentId, isChecked)
        }
    }
}

data class ChecklistUiState(
    val items: List<ChecklistItem> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val hasTrip: Boolean = false,
    val checklistName: String = "",
    val trips: List<Trip> = emptyList(),
    val selectedTripId: String? = null,
    val error: String? = null,
    // Reminder settings (for selected checklist)
    val reminderEnabled: Boolean = false,
    val reminderTimeMillis: Long = 0L,
    val repeatType: RepeatType = RepeatType.ONCE,
    val repeatIntervalDays: Int = 1,
    val stopWhenCompleted: Boolean = true,
    val stopAtTripStart: Boolean = true,
    val reminderLoading: Boolean = false,
    // Optional Documents sub-checklist per trip
    val travelDocuments: List<TravelDocument> = emptyList(),
    val documentsLoading: Boolean = false,
    // Shared list (group) collaboration
    val sharedLists: List<SharedList> = emptyList(),
    val selectedSharedListId: String? = null,
    val effectiveSharedListId: String? = null,
    val isSharedList: Boolean = false,
    val inviteCodeForShare: String? = null,
    val showShareDialog: Boolean = false,
    /** True when showing the share dialog for a list that was already shared (same code as before). */
    val isResharingList: Boolean = false,
    val showJoinDialog: Boolean = false,
    val joinError: String? = null,
    /** When opening Join dialog from invite link, this code is prefilled; cleared after use. */
    val pendingJoinCode: String? = null,
    /** One-shot message to show as Toast when share fails (so user sees it without scrolling). */
    val shareErrorForToast: String? = null,
    /** One-shot message to show as Toast when create checklist fails. */
    val createErrorForToast: String? = null,
    /** One-shot message to show as Toast when create checklist succeeds. */
    val createSuccessForToast: String? = null
)
