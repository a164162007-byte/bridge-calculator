package com.bridge.calculator.core.elbow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 公式引擎 - 桥架弯头核心计算
 */
object FormulaEngine {
    private const val DEG_TO_RAD = PI / 180.0
    
    fun calcBottomX(b: Double, alpha: Double): Double = b * tan(alpha * DEG_TO_RAD / 2)
    fun calcHypotenuse(b: Double, alpha: Double): Double = b / sin(alpha * DEG_TO_RAD)
    fun calcHeight(b: Double, alpha: Double): Double = b * tan(alpha * DEG_TO_RAD)
    fun calcCutWidth(hypotenuse: Double, alpha: Double): Double = hypotenuse * sin(alpha * DEG_TO_RAD / 2)
    
    object QuickCoefficients {
        const val ANGLE_30 = 0.536
        const val ANGLE_45 = 0.828
        const val ANGLE_60 = 1.414
        const val ANGLE_90 = 1.0
        fun getCoefficient(alpha: Double): Double = when {
            abs(alpha - 30.0) < 0.1 -> ANGLE_30
            abs(alpha - 45.0) < 0.1 -> ANGLE_45
            abs(alpha - 60.0) < 0.1 -> ANGLE_60
            abs(alpha - 90.0) < 0.1 -> ANGLE_90
            else -> tan(alpha * DEG_TO_RAD / 2)
        }
    }
    
    fun calcMultiLayerOffset(d: Double, b: Double, alpha: Double): List<Double> {
        val e = d + b * tan(alpha * DEG_TO_RAD / 2)
        val c = 2 * b * tan(alpha * DEG_TO_RAD / 2)
        val h = 2 * b * tan(alpha * DEG_TO_RAD / 2)
        return listOf(e, c, h)
    }
    
    fun calcReducingHypotenuse(beforeWidth: Double, afterWidth: Double, height: Double): Double {
        val widthDiff = abs(afterWidth - beforeWidth)
        return sqrt(widthDiff * widthDiff + height * height)
    }
    
    fun calcZigzag(b: Double, alpha: Double, layers: Int): List<Double> {
        val stepWidth = b / layers
        return (1..layers).map { stepWidth * it * tan(alpha * DEG_TO_RAD / 2) }
    }
    
    fun calcTeeFunnel(mainWidth: Double, branchWidth: Double, height: Double): Double = branchWidth * 1.5
    
    fun calcCustomAngle(params: CalcParams): List<CalcResult> {
        val x = calcBottomX(params.distance, params.angle)
        val l = calcHypotenuse(params.distance, params.angle)
        val h = calcHeight(params.distance, params.angle)
        val cut = calcCutWidth(l, params.angle)
        return listOf(
            CalcResult("底边 X", x, "mm", "X = b × tan(α/2)"),
            CalcResult("斜边 L", l, "mm", "L = b / sin(α)"),
            CalcResult("爬坡高度", h, "mm", "h = b × tan(α)"),
            CalcResult("切口宽", cut, "mm"),
            CalcResult("折弯位置", params.width / 2 - x / 2, "mm")
        )
    }
    
    fun calcRamp(params: CalcParams): List<CalcResult> {
        val x = calcBottomX(params.distance, params.angle)
        val l = calcHypotenuse(params.distance, params.angle)
        val h = calcHeight(params.distance, params.angle)
        val results = mutableListOf(
            CalcResult("底边 X", x, "mm"),
            CalcResult("斜边 L", l, "mm"),
            CalcResult("爬坡高度", h, "mm"),
            CalcResult("切口宽", calcCutWidth(l, params.angle), "mm"),
            CalcResult("折弯位置", params.width / 2 - x / 2, "mm")
        )
        if (params.layers > 1) {
            val offsets = calcMultiLayerOffset(params.layerSpacing, params.distance, params.angle)
            results.addAll(listOf(
                CalcResult("层偏移 e", offsets[0], "mm"),
                CalcResult("总宽度 c", offsets[1], "mm"),
                CalcResult("总高度 h", offsets[2], "mm")
            ))
        }
        return results
    }
    
    fun calcHorizontal(params: CalcParams): List<CalcResult> {
        val x = calcBottomX(params.distance, params.angle)
        val l = calcHypotenuse(params.distance, params.angle)
        return listOf(
            CalcResult("底边 X", x, "mm"),
            CalcResult("弧长 L", l, "mm"),
            CalcResult("切口宽", calcCutWidth(l, params.angle), "mm")
        )
    }
    
    fun calcComposite(params: CalcParams): List<CalcResult> {
        val x1 = calcBottomX(params.distance1, params.angle1)
        val l1 = calcHypotenuse(params.distance1, params.angle1)
        val x2 = calcBottomX(params.distance2, params.angle2)
        val l2 = calcHypotenuse(params.distance2, params.angle2)
        val totalHeight = params.height * 2  // 用height字段代替
        return listOf(
            CalcResult("第一段底边", x1, "mm"),
            CalcResult("第一段斜边", l1, "mm"),
            CalcResult("第二段底边", x2, "mm"),
            CalcResult("第二段斜边", l2, "mm"),
            CalcResult("总高度", totalHeight, "mm")
        )
    }
    
    fun calcReducing(params: CalcParams): List<CalcResult> {
        val deltaW = abs(params.afterWidth - params.beforeWidth)
        val deltaH = abs(params.afterHeight - params.beforeHeight)
        val l = calcReducingHypotenuse(params.beforeWidth, params.afterWidth, params.height)
        return listOf(
            CalcResult("宽度差", deltaW, "mm"),
            CalcResult("高度差", deltaH, "mm"),
            CalcResult("斜边长度", l, "mm"),
            CalcResult("切口宽", calcCutWidth(l, 45.0), "mm")
        )
    }
    
    fun calcTee(params: CalcParams): List<CalcResult> {
        val mainCut = params.width * 0.3
        val branchW = params.width * 0.5
        val funnel = calcTeeFunnel(params.width, branchW, params.height)
        return listOf(
            CalcResult("主路切口", mainCut, "mm"),
            CalcResult("支路宽度", branchW, "mm"),
            CalcResult("漏斗宽度", funnel, "mm"),
            CalcResult("漏斗高度", params.height * 0.5, "mm")
        )
    }
    
    fun calc90Degree(params: CalcParams): List<CalcResult> {
        val x = params.distance * QuickCoefficients.ANGLE_45
        val l = params.distance / sin(45.0 * DEG_TO_RAD)
        return listOf(
            CalcResult("底边 X", x, "mm"),
            CalcResult("斜边 L", l, "mm"),
            CalcResult("切口宽", calcCutWidth(l, 45.0), "mm"),
            CalcResult("弯曲半径", params.width / 2, "mm")
        )
    }
}
