package com.bridge.calculator.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bridge.calculator.core.elbow.*
import com.bridge.calculator.ui.components.BridgeElbowScene
import com.bridge.calculator.ui.viewmodel.ElbowDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElbowDetailScreen(elbowSpec: ElbowSpec, onBack: () -> Unit) {
    val viewModel = remember { ElbowDetailViewModel(elbowSpec) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text(text = elbowSpec.name, fontWeight = FontWeight.Bold); Text(text = "#${elbowSpec.id} · ${elbowSpec.category.displayName}", style = MaterialTheme.typography.bodySmall) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth().height(300.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BridgeElbowScene(params = viewModel.params, modelType = elbowSpec.modelType, modifier = Modifier.fillMaxSize())
                        Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                            Text(text = "双指缩放 · 单指拖动旋转", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "做法说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = elbowSpec.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text(text = "输入参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        elbowSpec.inputParams.forEach { param -> ParamInputField(param = param, value = getParamValue(viewModel.params, param.key), onValueChange = { viewModel.updateParam(param.key, it) }) }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column { Text(text = "计算公式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium); Text(text = elbowSpec.formula, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item { Text(text = "计算结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(viewModel.results) { result -> ResultCard(result = result) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ParamInputField(param: ParamDef, value: Double, onValueChange: (Double) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(value = textValue, onValueChange = { textValue = it; it.toDoubleOrNull()?.let { v -> onValueChange(v) } },
        modifier = Modifier.fillMaxWidth(), label = { Text("${param.name} (${param.unit})") }, placeholder = { Text("${param.defaultValue}") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(8.dp),
        supportingText = { if (param.description.isNotEmpty()) Text("${param.description}: ${param.minValue.toInt()}~${param.maxValue.toInt()}") })
}

@Composable
private fun ResultCard(result: CalcResult) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = result.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (result.formula.isNotEmpty()) Text(text = result.formula, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(text = "%.1f %s".format(result.value, result.unit), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun getParamValue(params: CalcParams, key: String): Double = when (key) {
    "width" -> params.width; "height" -> params.height; "angle" -> params.angle; "distance" -> params.distance; "heightOffset" -> params.heightOffset; "layers" -> params.layers.toDouble(); "layerSpacing" -> params.layerSpacing
    "beforeWidth" -> params.beforeWidth; "beforeHeight" -> params.beforeHeight; "afterWidth" -> params.afterWidth; "afterHeight" -> params.afterHeight; "branchPos" -> params.branchPos
    "distance1" -> params.distance1; "distance2" -> params.distance2; "angle1" -> params.angle1; "angle2" -> params.angle2
    else -> 0.0
}
