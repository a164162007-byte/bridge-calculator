package com.bridge.calculator.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bridge.calculator.core.elbow.*

class MainViewModel {
    val allElbows = ElbowRegistry.elbows
    val categoryStats = ElbowRegistry.getCategoryStats()
    var selectedCategory by mutableStateOf<ElbowCategory?>(null)
    var searchQuery by mutableStateOf("")
    fun getFilteredElbows(): List<ElbowSpec> {
        var result = allElbows
        if (selectedCategory != null) result = result.filter { it.category == selectedCategory }
        if (searchQuery.isNotBlank()) result = ElbowRegistry.search(searchQuery)
        return result
    }
    fun getElbowsByCategory(category: ElbowCategory): List<ElbowSpec> = ElbowRegistry.getByCategory(category)
}

class ElbowDetailViewModel(private val elbowSpec: ElbowSpec) {
    val spec = elbowSpec
    var params by mutableStateOf(CalcParams())
    var results by mutableStateOf<List<CalcResult>>(emptyList())
    var isCalculating by mutableStateOf(false)
    init { params = CalcParams(); calculate() }
    fun updateParam(key: String, value: Double) {
        params = when (key) {
            "width" -> params.copy(width = value); "height" -> params.copy(height = value)
            "angle" -> params.copy(angle = value); "distance" -> params.copy(distance = value)
            "heightOffset" -> params.copy(heightOffset = value); "layers" -> params.copy(layers = value.toInt())
            "layerSpacing" -> params.copy(layerSpacing = value); "beforeWidth" -> params.copy(beforeWidth = value)
            "beforeHeight" -> params.copy(beforeHeight = value); "afterWidth" -> params.copy(afterWidth = value)
            "afterHeight" -> params.copy(afterHeight = value); "branchPos" -> params.copy(branchPos = value)
            "distance1" -> params.copy(distance1 = value); "distance2" -> params.copy(distance2 = value)
            "angle1" -> params.copy(angle1 = value); "angle2" -> params.copy(angle2 = value)
            else -> params
        }
        calculate()
    }
    fun calculate() {
        isCalculating = true
        results = try { spec.calculate(params) } catch (e: Exception) { emptyList() }
        isCalculating = false
    }
}

class SettingsViewModel {
    var unitSystem by mutableStateOf("mm")
    var isDarkTheme by mutableStateOf(false)
    var bridgeColor by mutableStateOf(0xFF607D8B.toInt())
    val colorOptions = listOf(0xFF607D8B.toInt(), 0xFFFFFFFF.toInt(), 0xFF9E9E9E.toInt(), 0xFFFF7043.toInt(), 0xFF42A5F5.toInt(), 0xFF66BB6A.toInt())
}

class FormulaViewModel {
    data class FormulaItem(val title: String, val formula: String, val description: String)
    fun getBasicFormulas() = listOf(
        FormulaItem("万能公式", "X = b × tan(α/2)", "计算弯头底边尺寸"),
        FormulaItem("斜边公式", "L = b / sin(α)", "计算弯头斜边长度"),
        FormulaItem("爬坡高度", "h = b × tan(α)", "计算爬坡弯头的垂直高度"),
        FormulaItem("多层偏移", "e = d + b×tan(α/2)", "计算多层桥架的偏移量")
    )
    fun getQuickCoefficients() = listOf(
        FormulaItem("30°弯头", "下料x = 0.536 × H", "速算系数 0.536"),
        FormulaItem("45°弯头", "下料x = 0.828 × H", "速算系数 0.828"),
        FormulaItem("60°弯头", "下料x = 1.155 × H", "速算系数 1.155"),
        FormulaItem("90°弯头", "下料x = 2.0 × H", "两个45°组合")
    )
}
