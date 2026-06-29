package com.bridge.calculator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.bridge.calculator.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = remember { SettingsViewModel() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "设置", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SettingsSection(title = "单位设置") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "长度单位", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UnitChip(label = "毫米 (mm)", selected = viewModel.unitSystem == "mm", onClick = { viewModel.unitSystem = "mm" }, modifier = Modifier.weight(1f))
                            UnitChip(label = "厘米 (cm)", selected = viewModel.unitSystem == "cm", onClick = { viewModel.unitSystem = "cm" }, modifier = Modifier.weight(1f))
                            UnitChip(label = "米 (m)", selected = viewModel.unitSystem == "m", onClick = { viewModel.unitSystem = "m" }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SettingsSection(title = "外观") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (viewModel.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column { Text(text = "深色模式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium); Text(text = if (viewModel.isDarkTheme) "已开启" else "已关闭", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Switch(checked = viewModel.isDarkTheme, onCheckedChange = { viewModel.isDarkTheme = it })
                    }
                }
            }
            item { SettingsSection(title = "桥架颜色") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "3D模型显示颜色", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(viewModel.colorOptions) { color -> ColorOption(color = Color(color), selected = viewModel.bridgeColor == color, onClick = { viewModel.bridgeColor = color }) }
                        }
                    }
                }
            }
            item { SettingsSection(title = "关于") }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AboutItem(icon = Icons.Default.Info, title = "版本", value = "1.0.0")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        AboutItem(icon = Icons.Default.Calculate, title = "弯头类型", value = "68种")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        AboutItem(icon = Icons.Default.CloudOff, title = "网络", value = "完全离线")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        AboutItem(icon = Icons.Default.Android, title = "最低版本", value = "Android 8.0+")
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
            item { Text(text = "离线桥架弯头计算器 v1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth()) }
            item { Text(text = "68种弯头做法 · 3D实时渲染 · 万能公式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun SettingsSection(title: String) { Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }

@Composable
private fun UnitChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(8.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
        Text(text = label, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ColorOption(color: Color, selected: Boolean, onClick: () -> Unit) {
    val borderModifier = if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(color).then(borderModifier).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = "选中", tint = if (color == Color.White) Color.Black else Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AboutItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(text = title, style = MaterialTheme.typography.bodyMedium) }
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
