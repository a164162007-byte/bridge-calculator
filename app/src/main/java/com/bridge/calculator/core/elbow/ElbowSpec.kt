package com.bridge.calculator.core.elbow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 参数定义
 */
data class ParamDef(
    val name: String,
    val key: String,
    val unit: String,
    val defaultValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val description: String = ""
)

/**
 * 计算结果
 */
data class CalcResult(
    val name: String,
    val value: Double,
    val unit: String,
    val formula: String = ""
)

/**
 * 计算输入参数
 */
data class CalcParams(
    val width: Double = 200.0,
    val height: Double = 100.0,
    val angle: Double = 45.0,
    val distance: Double = 300.0,
    val heightOffset: Double = 200.0,
    val layers: Int = 1,
    val layerSpacing: Double = 200.0,
    val beforeWidth: Double = 200.0,
    val beforeHeight: Double = 100.0,
    val afterWidth: Double = 150.0,
    val afterHeight: Double = 75.0,
    val branchPos: Double = 50.0,
    val distance1: Double = 150.0,
    val distance2: Double = 150.0,
    val angle1: Double = 45.0,
    val angle2: Double = 45.0
) {
    companion object {
        fun default() = CalcParams()
    }
}

/**
 * 弯头规格定义
 */
data class ElbowSpec(
    val id: Int,
    val name: String,
    val category: ElbowCategory,
    val description: String,
    val inputParams: List<ParamDef>,
    val resultParams: List<String>,
    val formula: String,
    val calculate: (CalcParams) -> List<CalcResult>,
    val modelType: ModelType = ModelType.RAMP
)

enum class ModelType {
    RAMP, HORIZONTAL, COMPOSITE, TEE, REDUCING, FOLDED, CUSTOM
}

object DefaultParams {
    val width = ParamDef("桥架宽度", "width", "mm", 200.0, 50.0, 1200.0, "W")
    val height = ParamDef("桥架边高", "height", "mm", 100.0, 30.0, 400.0, "H")
    val angle = ParamDef("弯头角度", "angle", "°", 45.0, 1.0, 179.0, "α")
    val distance = ParamDef("水平距离", "distance", "mm", 300.0, 50.0, 2000.0, "b")
    val heightOffset = ParamDef("爬坡高度", "heightOffset", "mm", 200.0, 50.0, 1000.0, "h")
    val layers = ParamDef("层数", "layers", "", 1.0, 1.0, 10.0)
    val layerSpacing = ParamDef("层间距", "layerSpacing", "mm", 200.0, 100.0, 500.0, "d")
    val beforeWidth = ParamDef("变径前宽度", "beforeWidth", "mm", 200.0, 50.0, 1200.0)
    val beforeHeight = ParamDef("变径前高度", "beforeHeight", "mm", 100.0, 30.0, 400.0)
    val afterWidth = ParamDef("变径后宽度", "afterWidth", "mm", 150.0, 50.0, 1200.0)
    val afterHeight = ParamDef("变径后高度", "afterHeight", "mm", 75.0, 30.0, 400.0)
    val branchPos = ParamDef("分支位置", "branchPos", "%", 50.0, 0.0, 100.0)
}
