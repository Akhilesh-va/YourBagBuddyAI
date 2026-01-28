package com.example.yourbagbuddy.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.yourbagbuddy.presentation.viewmodel.ChecklistViewModel

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 80.dp) // Add bottom padding to prevent content from being hidden behind bottom nav
            ) {
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

                uiState.error?.let { errorMessage ->
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

                // Progress Header
                if (uiState.hasTrip) {
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

                // Checklist Items
                if (uiState.isLoading && uiState.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (uiState.items.isEmpty()) {
                    EmptyChecklistState()
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
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
