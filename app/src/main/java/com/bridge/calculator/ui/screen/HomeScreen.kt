package com.bridge.calculator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridge.calculator.core.elbow.ElbowCategory
import com.bridge.calculator.core.elbow.ElbowRegistry
import com.bridge.calculator.core.elbow.ElbowSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCategory: (ElbowCategory) -> Unit,
    onNavigateToElbow: (ElbowSpec) -> Unit,
    onNavigateToFormula: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("桥架弯头计算器", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White),
                actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "设置", tint = Color.White) } }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, "首页") }, label = { Text("首页") }, selected = true, onClick = { })
                NavigationBarItem(icon = { Icon(Icons.Default.MenuBook, "手册") }, label = { Text("手册") }, selected = false, onClick = onNavigateToFormula)
                NavigationBarItem(icon = { Icon(Icons.Default.Settings, "设置") }, label = { Text("设置") }, selected = false, onClick = onNavigateToSettings)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索弯头类型...") }, leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "8大分类 · ${ElbowRegistry.elbows.size}种弯头", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ElbowCategory.entries.chunked(2)) { rowCategories ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowCategories.forEach { category ->
                            CategoryCard(category = category, count = ElbowRegistry.getByCategory(category).size, onClick = { onNavigateToCategory(category) }, modifier = Modifier.weight(1f))
                        }
                        if (rowCategories.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: ElbowCategory, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, color) = getCategoryStyle(category)
    Card(modifier = modifier.aspectRatio(1f).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = icon, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = category.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = color)
            Text(text = "$count 种", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ElbowListItem(elbow: ElbowSpec, onClick: () -> Unit) {
    val (icon, color) = getCategoryStyle(elbow.category)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text(text = icon, fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = elbow.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(text = elbow.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(text = "#${elbow.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

private fun getCategoryStyle(category: ElbowCategory): Pair<String, Color> = when (category) {
    ElbowCategory.UP_DOWN_RAMP -> "⬆️" to Color(0xFF1976D2); ElbowCategory.HORIZONTAL_TURN -> "↔️" to Color(0xFF388E3C)
    ElbowCategory.COMPOSITE_FLIP -> "🔄" to Color(0xFF7B1FA2); ElbowCategory.SPECIAL_SHAPE -> "⭐" to Color(0xFFFFA000)
    ElbowCategory.REDUCING -> "📐" to Color(0xFF00796B); ElbowCategory.TEE_CROSS -> "⬡" to Color(0xFFE64A19)
    ElbowCategory.FOLDED_ANGLE -> "📏" to Color(0xFF5D4037); ElbowCategory.CUSTOM_ANGLE -> "🎯" to Color(0xFF0097A7)
}
