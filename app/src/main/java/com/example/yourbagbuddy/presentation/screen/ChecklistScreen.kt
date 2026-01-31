package com.example.yourbagbuddy.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.yourbagbuddy.MainActivity
import com.example.yourbagbuddy.domain.model.RepeatType
import com.example.yourbagbuddy.domain.model.TravelDocument
import com.example.yourbagbuddy.presentation.viewmodel.ChecklistViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    onNavigateBack: () -> Unit,
    initialJoinCode: String? = null,
    onConsumeJoinCode: () -> Unit = {},
    viewModel: ChecklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(uiState.trips, uiState.selectedTripId, uiState.hasTrip) {
        Log.d("ChecklistUI", "trips: count=${uiState.trips.size} names=${uiState.trips.map { it.name }} selectedTripId=${uiState.selectedTripId} hasTrip=${uiState.hasTrip}")
    }
    LaunchedEffect(initialJoinCode) {
        if (initialJoinCode != null) {
            viewModel.openJoinDialogWithCode(initialJoinCode)
            // Do NOT call onConsumeJoinCode() here: it navigates and replaces this screen,
            // creating a new ViewModel and losing the Join dialog. Stay on checklist?joinCode=...
            // so the dialog stays visible and the user can tap "Join".
        }
    }
    LaunchedEffect(uiState.shareErrorForToast) {
        uiState.shareErrorForToast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearShareErrorForToast()
        }
    }
    LaunchedEffect(uiState.createErrorForToast) {
        uiState.createErrorForToast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearCreateErrorForToast()
        }
    }
    LaunchedEffect(uiState.createSuccessForToast) {
        uiState.createSuccessForToast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearCreateSuccessForToast()
        }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    var showChecklistNameDialog by remember { mutableStateOf(false) }
    var checklistNameText by remember { mutableStateOf("") }
    var pendingItemName by remember { mutableStateOf("") }
    var showReminderTimeDialog by remember { mutableStateOf(false) }
    var showDeleteChecklistDialog by remember { mutableStateOf(false) }
    var showLeaveListDialog by remember { mutableStateOf(false) }
    var showChecklistOptionsMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Save reminder regardless; notification will fail silently if permission denied
        viewModel.saveReminder(
            reminderTimeMillis = uiState.reminderTimeMillis,
            repeatType = uiState.repeatType,
            repeatIntervalDays = uiState.repeatIntervalDays,
            isEnabled = true,
            stopWhenCompleted = uiState.stopWhenCompleted,
            stopAtTripStart = uiState.stopAtTripStart
        )
    }
    fun requestPermissionAndSaveReminder(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context is Activity && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.saveReminder(
            reminderTimeMillis = uiState.reminderTimeMillis,
            repeatType = uiState.repeatType,
            repeatIntervalDays = uiState.repeatIntervalDays,
            isEnabled = enabled,
            stopWhenCompleted = uiState.stopWhenCompleted,
            stopAtTripStart = uiState.stopAtTripStart
        )
    }

    val addItemAndClear = {
        val trimmed = newItemText.trim()
        if (trimmed.isNotBlank()) {
            if (uiState.selectedTripId != null || uiState.effectiveSharedListId != null) {
                viewModel.addItem(trimmed)
                newItemText = ""
            } else {
                pendingItemName = trimmed
                showChecklistNameDialog = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                com.example.yourbagbuddy.presentation.components.ModernTopAppBar(
                    title = "Your Checklist",
                    showBackButton = true,
                    onBackClick = onNavigateBack
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Single hero block: checklist picker + add item in one flowing section
                item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Checklists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when {
                                        uiState.selectedTripId != null || uiState.effectiveSharedListId != null ->
                                            "Add items to this list below"
                                        else -> "Select a list, join a group list, or create a new one"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            var selectedChecklistTab by remember { mutableStateOf(0) }
                            LaunchedEffect(uiState.selectedSharedListId) {
                                if (uiState.selectedSharedListId != null) selectedChecklistTab = 1
                            }
                            LaunchedEffect(uiState.selectedTripId) {
                                if (uiState.selectedTripId != null) selectedChecklistTab = 0
                            }
                            TabRow(
                                selectedTabIndex = selectedChecklistTab,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = selectedChecklistTab == 0,
                                    onClick = {
                                        selectedChecklistTab = 0
                                        viewModel.switchToMyChecklistsTab()
                                    },
                                    text = { Text("My checklists") }
                                )
                                Tab(
                                    selected = selectedChecklistTab == 1,
                                    onClick = {
                                        selectedChecklistTab = 1
                                        viewModel.switchToSharedListsTab()
                                    },
                                    text = { Text("Shared lists") }
                                )
                            }
                            // Scrollable row: only checklist names (so "Join list" and "New" stay visible below)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                if (selectedChecklistTab == 0) {
                                    items(uiState.trips, key = { it.id }) { trip ->
                                        FilterChip(
                                            selected = uiState.selectedTripId == trip.id,
                                            onClick = { viewModel.selectTrip(trip.id) },
                                            label = { Text(trip.name) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                } else {
                                    items(uiState.sharedLists, key = { it.id }) { sharedList ->
                                        FilterChip(
                                            selected = uiState.selectedSharedListId == sharedList.id,
                                            onClick = { viewModel.selectSharedList(sharedList.id) },
                                            label = { Text(sharedList.name) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                            )
                                        )
                                    }
                                }
                            }
                            // Always-visible actions: Join list and New (separate from scroll so users can always find them)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedChecklistTab == 0) {
                                    AssistChip(
                                        onClick = { viewModel.showJoinDialog() },
                                        label = { Text("Join list") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Group,
                                                contentDescription = null
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    AssistChip(
                                        onClick = {
                                            pendingItemName = ""
                                            checklistNameText = ""
                                            showChecklistNameDialog = true
                                        },
                                        label = { Text("New") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Create new checklist"
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    AssistChip(
                                        onClick = { viewModel.showJoinDialog() },
                                        label = { Text("Join list") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Group,
                                                contentDescription = null
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                            }
                            if (uiState.hasTrip || uiState.sharedLists.isNotEmpty()) {
                                val selectedSharedList = uiState.sharedLists.firstOrNull { it.id == uiState.selectedSharedListId }
                                    ?: uiState.trips.firstOrNull { it.id == uiState.selectedTripId }?.let { trip ->
                                        trip.sharedListId?.let { listId ->
                                            uiState.sharedLists.firstOrNull { it.id == listId }
                                        }
                                    }
                                val memberCount = selectedSharedList?.memberIds?.size ?: 0
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = uiState.checklistName.ifBlank { "Checklist" },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                if (uiState.isSharedList && memberCount > 0) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Group,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp),
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                                        )
                                                        Text(
                                                            text = if (memberCount == 1) "1 member" else "$memberCount members",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                                        )
                                                    }
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "${uiState.completedCount}/${uiState.totalCount} Completed",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                                )
                                            Box {
                                                IconButton(
                                                    onClick = { showChecklistOptionsMenu = true },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showChecklistOptionsMenu,
                                                    onDismissRequest = { showChecklistOptionsMenu = false }
                                                ) {
                                                    val selectedTrip = uiState.trips.firstOrNull { it.id == uiState.selectedTripId }
                                                    if (selectedTrip != null) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(20.dp))
                                                                    Text("Share with group")
                                                                }
                                                            },
                                                            onClick = {
                                                                showChecklistOptionsMenu = false
                                                                if (selectedTrip.sharedListId == null)
                                                                    viewModel.createSharedListAndLinkTrip()
                                                                else
                                                                    viewModel.showShareDialogForExistingList()
                                                            }
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                                                Text("Share list")
                                                            }
                                                        },
                                                        onClick = {
                                                            showChecklistOptionsMenu = false
                                                            val title = uiState.checklistName.ifBlank { "Packing list" }
                                                            val lines = uiState.items.map { "• ${it.name}" }
                                                            val text = listOf(title, "").plus(lines).joinToString("\n")
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Packing list", text))
                                                            Toast.makeText(context, "List copied to clipboard", Toast.LENGTH_SHORT).show()
                                                            context.startActivity(Intent.createChooser(
                                                                Intent(Intent.ACTION_SEND).apply {
                                                                    type = "text/plain"
                                                                    putExtra(Intent.EXTRA_TEXT, text)
                                                                },
                                                                "Share list"
                                                            ))
                                                        }
                                                    )
                                                    if (uiState.selectedTripId != null) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Delete,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(20.dp),
                                                                        tint = MaterialTheme.colorScheme.error
                                                                    )
                                                                    Text(
                                                                        "Delete checklist",
                                                                        color = MaterialTheme.colorScheme.error
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                showChecklistOptionsMenu = false
                                                                showDeleteChecklistDialog = true
                                                            }
                                                        )
                                                    }
                                                    if (uiState.selectedSharedListId != null) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Delete,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(20.dp),
                                                                        tint = MaterialTheme.colorScheme.error
                                                                    )
                                                                    Text(
                                                                        "Leave list",
                                                                        color = MaterialTheme.colorScheme.error
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                showChecklistOptionsMenu = false
                                                                showLeaveListDialog = true
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 1.dp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = newItemText,
                                    onValueChange = { newItemText = it },
                                    placeholder = {
                                        Text(
                                            if (uiState.selectedTripId != null)
                                                "Add to ${uiState.checklistName.ifBlank { "list" }}"
                                            else
                                                "Select a checklist above first"
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                                IconButton(
                                    onClick = addItemAndClear,
                                    enabled = newItemText.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add item",
                                        tint = if (newItemText.isNotBlank())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                }

                uiState.error?.let { errorMessage ->
                    item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    }
                }

                if (uiState.hasTrip && uiState.selectedTripId != null) {
                    item {
                    ReminderCard(
                        uiState = uiState,
                        onReminderToggle = { requestPermissionAndSaveReminder(it) },
                        onReminderTimeClick = { showReminderTimeDialog = true },
                        onRepeatTypeChange = { viewModel.updateReminderRepeatType(it, uiState.repeatIntervalDays); viewModel.saveReminder(uiState.reminderTimeMillis, it, uiState.repeatIntervalDays, uiState.reminderEnabled, uiState.stopWhenCompleted, uiState.stopAtTripStart) },
                        onRepeatIntervalDaysChange = { viewModel.updateReminderRepeatType(uiState.repeatType, it); viewModel.saveReminder(uiState.reminderTimeMillis, uiState.repeatType, it, uiState.reminderEnabled, uiState.stopWhenCompleted, uiState.stopAtTripStart) },
                        onStopConditionsChange = { stopWhenCompleted, stopAtTripStart -> viewModel.updateReminderStopConditions(stopWhenCompleted, stopAtTripStart); viewModel.saveReminder(uiState.reminderTimeMillis, uiState.repeatType, uiState.repeatIntervalDays, uiState.reminderEnabled, stopWhenCompleted, stopAtTripStart) }
                    )
                    }
                    item {
                    DocumentsCard(
                        travelDocuments = uiState.travelDocuments,
                        documentsLoading = uiState.documentsLoading,
                        onEnableDocuments = { viewModel.enableDocumentsChecklist() },
                        onToggleDocument = { id, checked -> viewModel.toggleDocumentChecked(id, checked) },
                        onOpenResource = { url ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) { }
                        }
                    )
                    }
                }

                when {
                    uiState.isLoading && uiState.items.isEmpty() -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    uiState.items.isEmpty() -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "No items yet",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap the + button to add items to your checklist",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> items(uiState.items, key = { it.id }) { item ->
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            ChecklistItemRow(
                                item = item,
                                onToggle = { isChecked ->
                                    viewModel.toggleItem(item.id, isChecked)
                                },
                                onDelete = {
                                    viewModel.deleteItem(item.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showReminderTimeDialog) {
            ReminderTimeDialog(
                initialMillis = uiState.reminderTimeMillis,
                onDismiss = { showReminderTimeDialog = false },
                onConfirm = { millis ->
                    viewModel.updateReminderTime(millis)
                    viewModel.saveReminder(millis, uiState.repeatType, uiState.repeatIntervalDays, uiState.reminderEnabled, uiState.stopWhenCompleted, uiState.stopAtTripStart)
                    showReminderTimeDialog = false
                }
            )
        }

        // Floating Action Button positioned above bottom navigation
        FloatingActionButton(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 100.dp
                ) // Position above bottom nav (80dp nav + 20dp margin)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Item",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    // Add Item Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newItemText = ""
            },
            title = { Text("Add Item") },
            text = {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        addItemAndClear()
                        if (newItemText.isBlank() && !showChecklistNameDialog) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newItemText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChecklistNameDialog) {
        BackHandler {
            showChecklistNameDialog = false
            checklistNameText = ""
            pendingItemName = ""
        }
        AlertDialog(
            onDismissRequest = {
                showChecklistNameDialog = false
                checklistNameText = ""
                pendingItemName = ""
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Name your checklist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Give this list a fun vibe. You can always rename it later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = checklistNameText,
                    onValueChange = { checklistNameText = it },
                    label = { Text("Checklist name") },
                    placeholder = { Text("Goa weekend escape") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(18.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingItemName.isNotBlank()) {
                            viewModel.createChecklistAndAddItem(
                                checklistName = checklistNameText,
                                itemName = pendingItemName
                            )
                        } else {
                            viewModel.createChecklist(checklistNameText)
                        }
                        showChecklistNameDialog = false
                        checklistNameText = ""
                        pendingItemName = ""
                        newItemText = ""
                        showAddDialog = false
                    },
                    enabled = checklistNameText.isNotBlank(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChecklistNameDialog = false
                        checklistNameText = ""
                        pendingItemName = ""
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Share with group dialog (invite code + shareable link)
    if (uiState.showShareDialog && uiState.inviteCodeForShare != null) {
        val inviteCode = uiState.inviteCodeForShare!!
        val inviteLink = MainActivity.buildJoinLinkHttps(inviteCode)
        val shareMessage = "Join my packing list in YourBagBuddy! Code: $inviteCode — or tap this link: $inviteLink"
        val isResharing = uiState.isResharingList
        AlertDialog(
            onDismissRequest = { viewModel.dismissShareDialog() },
            title = { Text(if (isResharing) "Reshare with group" else "Share with group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (isResharing) {
                            "Same code as before — share the link or code again with anyone who needs to join."
                        } else {
                            "Share a link or code with your group. When they tap the link (e.g. in WhatsApp) the app opens and they can join the list."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inviteCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp
                                )
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Invite code", inviteCode))
                                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Copy code")
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inviteLink,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Join link", inviteLink))
                                        Toast.makeText(context, "Link copied — tap it to open app and join", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy link")
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    putExtra(Intent.EXTRA_TITLE, "Join my packing list")
                                },
                                "Share link"
                            ))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share link")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissShareDialog() }) {
                    Text("Done", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Join list dialog
    if (uiState.showJoinDialog) {
        var joinCode by remember { mutableStateOf(uiState.pendingJoinCode ?: "") }
        LaunchedEffect(uiState.showJoinDialog, uiState.pendingJoinCode) {
            if (uiState.pendingJoinCode != null) {
                joinCode = uiState.pendingJoinCode!!
                viewModel.clearPendingJoinCode()
            } else if (uiState.showJoinDialog && joinCode.isEmpty()) {
                joinCode = ""
            }
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissJoinDialog() },
            title = { Text("Join a group list") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter the invite code shared by your group mate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase().take(6) },
                        label = { Text("Invite code") },
                        placeholder = { Text("ABC123") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    uiState.joinError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.joinSharedList(joinCode) },
                    enabled = joinCode.length >= 4,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissJoinDialog() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Delete checklist confirmation
    if (showDeleteChecklistDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChecklistDialog = false },
            title = { Text("Delete checklist?") },
            text = {
                Text(
                    "This will permanently delete \"${uiState.checklistName.ifBlank { "this checklist" }}\" and all its items. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCurrentChecklist()
                        showDeleteChecklistDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChecklistDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Leave shared list confirmation
    if (showLeaveListDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveListDialog = false },
            title = { Text("Leave list?") },
            text = {
                Text(
                    "You will be removed from \"${uiState.checklistName.ifBlank { "this list" }}\". You can rejoin later with the invite code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveSharedList()
                        showLeaveListDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveListDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar - you can use SnackbarHost for this
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: com.example.yourbagbuddy.domain.model.ChecklistItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isPacked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Text(
                text = item.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isPacked) TextDecoration.LineThrough else null,
                color = if (item.isPacked)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyChecklistState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No items yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap the + button to add items to your checklist",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DocumentsCard(
    travelDocuments: List<TravelDocument>,
    documentsLoading: Boolean,
    onEnableDocuments: () -> Unit,
    onToggleDocument: (String, Boolean) -> Unit,
    onOpenResource: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Documents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = "Passport, visa, tickets, insurance — with reminders and official links",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (travelDocuments.isEmpty()) {
                if (documentsLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onEnableDocuments,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add documents checklist")
                    }
                }
            } else if (expanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    travelDocuments.forEach { doc ->
                        TravelDocumentRow(
                            document = doc,
                            onToggle = { onToggleDocument(doc.id, it) },
                            onOpenResource = doc.resourceUrl?.let { { onOpenResource(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TravelDocumentRow(
    document: TravelDocument,
    onToggle: (Boolean) -> Unit,
    onOpenResource: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = document.isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (document.isChecked) TextDecoration.LineThrough else null,
                    color = if (document.isChecked)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                document.reminderText?.let { reminder ->
                    Text(
                        text = reminder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onOpenResource != null) {
                FilledTonalButton(
                    onClick = onOpenResource,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = "Official resource",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Link", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private val reminderTimeFormat = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    uiState: com.example.yourbagbuddy.presentation.viewmodel.ChecklistUiState,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimeClick: () -> Unit,
    onRepeatTypeChange: (RepeatType) -> Unit,
    onRepeatIntervalDaysChange: (Int) -> Unit,
    onStopConditionsChange: (Boolean, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header: title + expand/collapse + switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Packing reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
                    checked = uiState.reminderEnabled,
                    onCheckedChange = onReminderToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // When collapsed and reminders on: show one-line summary
            if (!expanded && uiState.reminderEnabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reminderTimeFormat.format(Date(if (uiState.reminderTimeMillis > 0) uiState.reminderTimeMillis else System.currentTimeMillis())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded && uiState.reminderEnabled) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )
                Spacer(Modifier.height(20.dp))

                // Time row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trip starting time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = reminderTimeFormat.format(Date(if (uiState.reminderTimeMillis > 0) uiState.reminderTimeMillis else System.currentTimeMillis())),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    FilledTonalButton(
                        onClick = onReminderTimeClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Change", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(RepeatType.ONCE, RepeatType.DAILY, RepeatType.EVERY_X_DAYS).forEach { type ->
                        FilterChip(
                            selected = uiState.repeatType == type,
                            onClick = { onRepeatTypeChange(type) },
                            label = {
                                Text(
                                    when (type) {
                                        RepeatType.ONCE -> "Once"
                                        RepeatType.DAILY -> "Daily"
                                        RepeatType.EVERY_X_DAYS -> "Every X days"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                if (uiState.repeatType == RepeatType.EVERY_X_DAYS) {
                    Spacer(Modifier.height(12.dp))
                    var daysText by remember(uiState.repeatIntervalDays) { mutableStateOf(uiState.repeatIntervalDays.toString()) }
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = {
                            daysText = it.filter { c -> c.isDigit() }.take(3)
                            daysText.toIntOrNull()?.coerceIn(1, 365)?.let { onRepeatIntervalDaysChange(it) }
                        },
                        label = { Text("Every (days)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Stop reminders when",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.stopWhenCompleted,
                        onCheckedChange = { onStopConditionsChange(it, uiState.stopAtTripStart) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        text = "All items checked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.stopAtTripStart,
                        onCheckedChange = { onStopConditionsChange(uiState.stopWhenCompleted, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        text = "Trip start date reached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initial = if (initialMillis > 0) initialMillis else System.currentTimeMillis() + 86400000L
    val cal = remember(initial) { Calendar.getInstance().apply { timeInMillis = initial } }
    var selectedDateMillis by remember(initial) { mutableStateOf<Long?>(initial) }
    var showTimeStep by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initial,
        yearRange = IntRange(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.YEAR) + 2)
    )

    // Step 1: Material3 Date picker dialog (no cramped layout, full calendar)
    if (!showTimeStep) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = dateState.selectedDateMillis ?: initial
                        showTimeStep = true
                    }
                ) {
                    Text("Next", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = dateState,
                title = {
                    Text(
                        "Select date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        }
        return
    }

    // Step 2: Material3 Time picker — use initial reminder time (cal) so the already-set time is pre-selected
    val timePickerState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select time",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth(),
                colors = TimePickerDefaults.colors(
                    selectorColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.primary,
                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateMillis = selectedDateMillis ?: initial
                    val result = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(result.timeInMillis)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { showTimeStep = false }) {
                Text("Back", color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp
    )
}
