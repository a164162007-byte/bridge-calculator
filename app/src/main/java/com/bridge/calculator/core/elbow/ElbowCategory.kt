package com.bridge.calculator.core.elbow

/**
 * 桥架弯头8大分类（对应陈工68种做法）
 */
enum class ElbowCategory(val displayName: String, val description: String) {
    UP_DOWN_RAMP("上下爬坡", "垂直方向爬升/下降的弯头，包括左爬坡、右爬坡、垂直弯等"),
    HORIZONTAL_TURN("水平转向", "水平面内改变方向的弯头，包括左弯、右弯、水平弯"),
    COMPOSITE_FLIP("组合翻弯", "多个角度组合的复杂弯头，包括45+45组合、之字弯等"),
    SPECIAL_SHAPE("特殊造型", "特殊形状的桥架弯头，包括弧形弯、圆形弯等"),
    REDUCING("变径", "改变桥架尺寸的连接件，包括大小头、变高/变宽变径"),
    TEE_CROSS("三通/四通", "多路分支的连接件，包括水平三通、垂直三通、十字四通等"),
    FOLDED_ANGLE("折角", "带折角的弯头，包括折角弯头、盖板等"),
    CUSTOM_ANGLE("任意角", "支持0-180度任意角度的万能弯头");
}
