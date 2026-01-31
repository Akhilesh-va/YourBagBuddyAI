package com.example.yourbagbuddy.presentation.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.yourbagbuddy.domain.model.ItemCategory
import com.example.yourbagbuddy.presentation.viewmodel.TripDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onDuplicateSuccess: (String) -> Unit = {},
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) {
        viewModel.loadChecklist(tripId)
    }

    fun shareListText(): String {
        val title = uiState.trip?.name ?: "Packing list"
        val lines = uiState.items.map { "• ${it.name}" }
        return listOf("$title", "").plus(lines).joinToString("\n")
    }

    fun copyToClipboardAndShare() {
        val text = shareListText()
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

    Scaffold(
        topBar = {
            com.example.yourbagbuddy.presentation.components.ModernTopAppBar(
                title = uiState.trip?.name ?: "Packing List",
                showBackButton = true,
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share list") },
                            onClick = {
                                showMenu = false
                                copyToClipboardAndShare()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate trip") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    viewModel.duplicateTrip(tripId).fold(
                                        onSuccess = { newTripId ->
                                            Toast.makeText(context, "Trip duplicated", Toast.LENGTH_SHORT).show()
                                            onDuplicateSuccess(newTripId)
                                        },
                                        onFailure = {
                                            Toast.makeText(context, "Could not duplicate trip", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Show add item dialog */ },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Item",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress Card
            ProgressCard(
                packedCount = uiState.packedCount,
                totalCount = uiState.totalCount,
                progress = uiState.progress
            )
            
            // Checklist Items – use weight(1f) so LazyColumn gets bounded height (fixes "infinity constraints" crash)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.items) { item ->
                    ChecklistItemRow(
                        item = item,
                        onTogglePacked = { isPacked ->
                            viewModel.toggleItemPacked(item.id, isPacked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressCard(
    packedCount: Int,
    totalCount: Int,
    progress: Float
) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Packing Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = "$packedCount / $totalCount items packed",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: com.example.yourbagbuddy.domain.model.ChecklistItem,
    onTogglePacked: (Boolean) -> Unit
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isPacked) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = item.isPacked,
                onCheckedChange = onTogglePacked,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
