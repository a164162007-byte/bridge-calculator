package com.bridge.calculator.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bridge.calculator.core.elbow.ElbowCategory
import com.bridge.calculator.core.elbow.ElbowRegistry
import com.bridge.calculator.core.elbow.ElbowSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(category: ElbowCategory, onBack: () -> Unit, onElbowClick: (ElbowSpec) -> Unit) {
    val elbows = ElbowRegistry.getByCategory(category)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text(text = category.displayName, fontWeight = FontWeight.Bold); Text(text = category.description, style = MaterialTheme.typography.bodySmall) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(text = "共 ${elbows.size} 种做法", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.height(8.dp)) }
            items(elbows) { elbow -> ElbowListItem(elbow = elbow, onClick = { onElbowClick(elbow) }) }
        }
    }
}
