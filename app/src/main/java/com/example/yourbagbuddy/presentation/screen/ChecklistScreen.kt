package com.example.yourbagbuddy.presentation.screen

import android.Manifest
import android.app.Activity
import android.os.Build
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
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.yourbagbuddy.domain.model.RepeatType
import com.example.yourbagbuddy.presentation.viewmodel.ChecklistViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChecklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    var showChecklistNameDialog by remember { mutableStateOf(false) }
    var checklistNameText by remember { mutableStateOf("") }
    var pendingItemName by remember { mutableStateOf("") }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
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
            if (uiState.selectedTripId != null) {
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
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            placeholder = { Text("List item") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surface
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

                item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Checklists",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
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
                        item {
                            AssistChip(
                                onClick = {
                                    // Show name dialog so user names the checklist; don't create blank ones.
                                    pendingItemName = ""
                                    checklistNameText = ""
                                    showChecklistNameDialog = true
                                },
                                label = { Text("New") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null
                                    )
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
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

                if (uiState.hasTrip) {
                    item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.checklistName.ifBlank { "Checklist" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${uiState.completedCount}/${uiState.totalCount} Completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = item.isPacked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Item Text
            Text(
                text = item.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isPacked) TextDecoration.LineThrough else null,
                color = if (item.isPacked)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Delete Button
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
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
