package com.example.yourbagbuddy.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yourbagbuddy.domain.model.ItemCategory
import com.example.yourbagbuddy.presentation.viewmodel.SmartPackViewModel

@Composable
fun AiListScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmartPackViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            com.example.yourbagbuddy.presentation.components.ModernTopAppBar(
                title = "Here is your AI list",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Review and add these smart suggestions to your checklist.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.generatedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No AI items yet. Go back and generate a packing list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val precautionItems = uiState.generatedItems.filter { it.category == ItemCategory.OTHER }
                val hasSelectedItems = uiState.generatedItems.any { it.isPacked }

                Text(
                    text = "AI Packing List",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                GeneratedItemsList(
                    items = uiState.generatedItems,
                    onItemChecked = viewModel::onGeneratedItemChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp),
                    inlineActionLabel = if (hasSelectedItems) "Add to your List" else null,
                    onInlineActionClick = {
                        viewModel.addSelectedGeneratedItemsToChecklist()
                        onNavigateBack()
                    }
                )

                if (precautionItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Precautions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    precautionItems.forEach { item ->
                        Text(
                            text = "• ${item.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // No separate bottom button; the primary action lives inside the card
            }
        }
    }
}

