package com.bridge.calculator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bridge.calculator.ui.viewmodel.FormulaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaScreen(onBack: () -> Unit) {
    val viewModel = FormulaViewModel()
    Scaffold(
        topBar = { TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.padding(end = 8.dp)); Text(text = "计算公式手册", fontWeight = FontWeight.Bold) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SectionTitle(title = "万能公式", subtitle = "Core Formulas") }
            items(viewModel.getBasicFormulas()) { formula -> FormulaCard(formula = formula) }
            item { Spacer(modifier = Modifier.height(8.dp)); SectionTitle(title = "速算系数", subtitle = "Quick Coefficients") }
            item { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("30°" to "0.536" to Color(0xFF1976D2), "45°" to "0.828" to Color(0xFF388E3C), "60°" to "1.414" to Color(0xFFFFA000), "90°" to "1.0" to Color(0xFFE64A19)).forEach { (angle, coef, color) -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = angle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color); Text(text = coef, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } }
            items(viewModel.getQuickCoefficients()) { formula -> FormulaCard(formula = formula) }
            item { Spacer(modifier = Modifier.height(8.dp)); SectionTitle(title = "现场测量画线流程", subtitle = "Field Measurement Guide") }
            item { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { listOf("测量桥架宽度W和边高H", "确定弯头角度α（30°/45°/60°/90°）", "根据现场空间确定水平距离b", "计算底边X = b × tan(α/2)", "在桥架上标记折弯位置", "沿线切割并折弯成型").forEachIndexed { i, step -> Row(verticalAlignment = Alignment.Top) { Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Text(text = "${i+1}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White) }; Spacer(modifier = Modifier.width(12.dp)); Text(text = step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp)) } } } } }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) { Column { Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) } }

@Composable
private fun FormulaCard(formula: FormulaViewModel.FormulaItem) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(text = formula.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium); Text(text = formula.formula, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary); Text(text = formula.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
