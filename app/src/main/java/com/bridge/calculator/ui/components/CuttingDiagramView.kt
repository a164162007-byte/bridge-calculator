package com.bridge.calculator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridge.calculator.core.elbow.CalcParams
import com.bridge.calculator.core.elbow.CalcResult
import com.bridge.calculator.core.elbow.FormulaEngine
import com.bridge.calculator.core.elbow.ModelType
import kotlin.math.*

/**
 * 2D 下料展开图（可 360° 旋转查看）
 *
 * 对齐"陈工桥架计算器"核心功能：
 *  - 自动绘制展开/侧面轮廓图
 *  - 自动标注所有切割尺寸（下料长、斜边、高度、底边等）
 *  - 红色尺寸线 + 绿色数字 + 中文标注
 *  - 单指拖动旋转 360°
 *
 * 单位：CalcParams 输入为 mm，绘图时缩放到 Canvas 尺寸
 */
@Composable
fun CuttingDiagramView(
    params: CalcParams,
    modelType: ModelType,
    modifier: Modifier = Modifier
) {
    var rotationZ by remember { mutableStateOf(0f) }
    var lastDragX by remember { mutableStateOf(0f) }

    // 根据类型计算所有尺寸
    val results = remember(params, modelType) { calcForType(params, modelType) }

    // 背景卡片
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    rotationZ += (drag.x - lastDragX) * 0.5f
                    lastDragX = drag.x
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { rotationZ = 0f })
            },
        contentAlignment = Alignment.Center
    ) {
        // Canvas 绘制
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height

            // 根据类型绘制不同图形
            when (modelType) {
                ModelType.RAMP, ModelType.CUSTOM -> drawRampDiagrams(w, h, params, results)
                ModelType.HORIZONTAL -> drawHorizontalDiagrams(w, h, params, results)
                ModelType.TEE -> drawTeeDiagram(w, h, params, results)
                ModelType.REDUCING -> drawReducingDiagram(w, h, params, results)
                ModelType.COMPOSITE -> drawCompositeDiagram(w, h, params, results)
                ModelType.FOLDED -> drawFoldedDiagram(w, h, params, results)
            }
        }

        // 旋转角度提示
        Text(
            text = "拖动旋转 · 双击复位  ${rotationZ.toInt()}°",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(4.dp)
        )
    }
}

// ─── 计算函数 ─────────────────────────────────────────────

private fun calcForType(params: CalcParams, modelType: ModelType): List<CalcResult> {
    return when (modelType) {
        ModelType.RAMP, ModelType.CUSTOM -> FormulaEngine.calcRamp(params)
        ModelType.HORIZONTAL -> FormulaEngine.calcHorizontal(params)
        ModelType.TEE -> FormulaEngine.calcTee(params)
        ModelType.REDUCING -> FormulaEngine.calcReducing(params)
        ModelType.COMPOSITE -> FormulaEngine.calcComposite(params)
        ModelType.FOLDED -> {
            val x = FormulaEngine.calcBottomX(params.distance, params.angle)
            val l = FormulaEngine.calcHypotenuse(params.distance, params.angle)
            val h = FormulaEngine.calcHeight(params.distance, params.angle)
            listOf(
                CalcResult("折角X", x, "mm"),
                CalcResult("斜边L", l, "mm"),
                CalcResult("爬坡高度", h, "mm")
            )
        }
    }
}

private fun getResult(results: List<CalcResult>, name: String): Double =
    results.find { it.name.contains(name) }?.value ?: 0.0

// ─── 颜色常量 ─────────────────────────────────────────────

private val LINE_BLUE = Color(0xFF1976D2)
private val DIM_RED = Color(0xFFD32F2F)
private val TEXT_GREEN = Color(0xFF2E7D32)
private val FILL_LIGHT = Color(0x151976D2)

// ─── 绘图工具 ─────────────────────────────────────────────

private fun DrawScope.drawShapePath(points: List<Pair<Float, Float>>, fill: Boolean = true) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points[0].first, points[0].second)
        for (i in 1 until points.size) lineTo(points[i].first, points[i].second)
        close()
    }
    if (fill) drawPath(path, FILL_LIGHT)
    drawPath(path, LINE_BLUE, style = Stroke(width = 3f))
}

private fun DrawScope.drawDimLine(
    start: Offset, end: Offset, label: String,
    offset: Float = 30f, labelColor: Color = TEXT_GREEN
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val nx = -dy / len * offset
    val ny = dx / len * offset

    val s = Offset(start.x + nx, start.y + ny)
    val e = Offset(end.x + nx, end.y + ny)

    // 主尺寸线
    drawLine(DIM_RED, s, e, strokeWidth = 1.8f)

    // 延伸线
    drawLine(DIM_RED.copy(alpha = 0.4f), start, s, strokeWidth = 1f)
    drawLine(DIM_RED.copy(alpha = 0.4f), end, e, strokeWidth = 1f)

    // 箭头
    drawArrowHead(s, e, DIM_RED, 8f)
    drawArrowHead(e, s, DIM_RED, 8f)

    // 文字标签
    val mid = Offset((s.x + e.x) / 2f, (s.y + e.y) / 2f)
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        setColor(labelColor.toArgb())
        textSize = 14f * density
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    nativeCanvas.drawText(label, mid.x, mid.y - 6f, paint)
}

private fun DrawScope.drawArrowHead(from: Offset, to: Offset, color: Color, size: Float) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux
    val tip = to
    val left = Offset(to.x - ux * size + px * size * 0.4f, to.y - uy * size + py * size * 0.4f)
    val right = Offset(to.x - ux * size - px * size * 0.4f, to.y - uy * size - py * size * 0.4f)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawLabel(text: String, position: Offset, labelColor: Color = LINE_BLUE) {
    val paintColor = labelColor.toArgb()
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        setColor(paintColor)
        textSize = 13f * density
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    nativeCanvas.drawText(text, position.x, position.y, paint)
}

/** 格式化 mm 值为 cm（对齐陈工的 cm 单位显示） */
private fun fmtCm(mm: Double): String = "%.3f cm".format(mm / 10.0)

/** 缩放因子：将 mm 值缩放到 Canvas 像素 */
private fun scaleFactor(canvasSize: Float, maxValueMm: Float, padding: Float = 60f): Float =
    (canvasSize - padding * 2) / maxValueMm.coerceAtLeast(1f)

// ─── 各类型绘图 ───────────────────────────────────────────

/**
 * 爬坡弯头：左侧面轮廓 + 右侧展开图（对齐陈工截图布局）
 */
private fun DrawScope.drawRampDiagrams(w: Float, h: Float, params: CalcParams, results: List<CalcResult>) {
    val b = params.distance.toFloat()      // 水平距离
    val angle = params.angle.toFloat()
    val aRad = angle * PI.toFloat() / 180f
    val baseX = b * tan(aRad / 2f)        // 底边 X
    val hyp = b / sin(aRad)               // 斜边 L
    val riseH = b * tan(aRad)             // 爬坡高度

    val halfW = w / 2f
    val pad = 50f

    // ── 左侧：侧面轮廓（梯形） ──
    val leftCx = halfW / 2f
    val leftCy = h / 2f
    val maxDim = maxOf(b, riseH)
    val sc1 = scaleFactor(minOf(halfW - pad * 2, h - pad * 2), maxDim)

    val bw = b * sc1  // 底边像素
    val hw = riseH * sc1  // 高度像素
    val xw = baseX * sc1  // 底边差像素

    // 梯形顶点（左下→右下→右上斜→左上斜）
    val tl = listOf(
        Pair(leftCx - bw / 2f, leftCy + hw / 2f),         // 左下
        Pair(leftCx + bw / 2f, leftCy + hw / 2f),         // 右下
        Pair(leftCx + bw / 2f - xw, leftCy - hw / 2f),    // 右上（缩进 x）
        Pair(leftCx - bw / 2f, leftCy - hw / 2f)           // 左上
    )
    drawShapePath(tl)
    drawLabel("侧面轮廓", Offset(leftCx, leftCy + hw / 2f + 28f), Color.Gray)

    // 尺寸标注
    // 下料长度（底边 b）
    drawDimLine(
        Offset(tl[0].first, tl[0].second),
        Offset(tl[1].first, tl[1].second),
        "下料 ${fmtCm(b.toDouble())}", offset = 25f
    )
    // 爬坡高度（左边 H）
    drawDimLine(
        Offset(tl[0].first, tl[0].second),
        Offset(tl[3].first, tl[3].second),
        fmtCm(riseH.toDouble()), offset = -30f
    )
    // 斜边（斜边 L）
    drawDimLine(
        Offset(tl[1].first, tl[1].second),
        Offset(tl[2].first, tl[2].second),
        "斜边 ${fmtCm(hyp.toDouble())}", offset = -25f
    )

    // ── 右侧：展开切割图（直角三角形） ──
    val rightCx = halfW + halfW / 2f
    val rightCy = h / 2f
    val sc2 = scaleFactor(minOf(halfW - pad * 2, h - pad * 2), maxOf(baseX, riseH))

    val twx = baseX * sc2
    val thw = riseH * sc2

    // 直角三角形顶点
    val tr = listOf(
        Pair(rightCx - twx / 2f, rightCy + thw / 2f),     // 左下（直角）
        Pair(rightCx + twx / 2f, rightCy + thw / 2f),     // 右下
        Pair(rightCx - twx / 2f, rightCy - thw / 2f)       // 左上
    )
    drawShapePath(tr)
    drawLabel("展开切割", Offset(rightCx, rightCy + thw / 2f + 28f), Color.Gray)

    // 尺寸标注
    // 底边 a
    drawDimLine(
        Offset(tr[0].first, tr[0].second),
        Offset(tr[1].first, tr[1].second),
        fmtCm(baseX.toDouble()), offset = 25f
    )
    // 高 c
    drawDimLine(
        Offset(tr[0].first, tr[0].second),
        Offset(tr[2].first, tr[2].second),
        fmtCm(riseH.toDouble()), offset = -30f
    )
    // 斜边（同左侧斜边）
    drawDimLine(
        Offset(tr[1].first, tr[1].second),
        Offset(tr[2].first, tr[2].second),
        fmtCm(hyp.toDouble()), offset = -25f
    )
}

/**
 * 水平弯头：平面展开图
 */
private fun DrawScope.drawHorizontalDiagrams(w: Float, h: Float, params: CalcParams, results: List<CalcResult>) {
    val b = params.distance.toFloat()
    val angle = params.angle.toFloat()
    val aRad = angle * PI.toFloat() / 180f
    val baseX = b * tan(aRad / 2f)
    val arcL = b / sin(aRad)

    val cx = w / 2f
    val cy = h / 2f
    val sc = scaleFactor(minOf(w - 80f, h - 80f), maxOf(b, baseX))

    val bw = b * sc
    val xw = baseX * sc

    // 两个三角形（对称，左+右）
    // 左三角形
    val l1 = listOf(
        Pair(cx - bw / 2f - xw, cy - 40f),
        Pair(cx - bw / 2f, cy - 40f),
        Pair(cx - bw / 2f, cy + 40f),
        Pair(cx - bw / 2f - xw, cy + 40f)
    )
    drawShapePath(l1)

    // 右三角形
    val l2 = listOf(
        Pair(cx + bw / 2f, cy - 40f),
        Pair(cx + bw / 2f + xw, cy - 40f),
        Pair(cx + bw / 2f + xw, cy + 40f),
        Pair(cx + bw / 2f, cy + 40f)
    )
    drawShapePath(l2)

    // 中间连接线
    drawLine(LINE_BLUE, Offset(cx - bw / 2f, cy), Offset(cx + bw / 2f, cy), strokeWidth = 1.5f)

    // 标注
    drawDimLine(
        Offset(cx - bw / 2f, cy - 40f),
        Offset(cx + bw / 2f, cy - 40f),
        "下料 ${fmtCm(b.toDouble())}", offset = -30f
    )
    drawDimLine(
        Offset(cx - bw / 2f - xw, cy - 40f),
        Offset(cx - bw / 2f - xw, cy + 40f),
        fmtCm(80.0),  // 桥架高度
        offset = -30f
    )
    drawLabel("水平弯展开", Offset(cx, cy + 70f), Color.Gray)
}

/**
 * 三通：主管 + 支路漏斗展开
 */
private fun DrawScope.drawTeeDiagram(w: Float, h: Float, params: CalcParams, results: List<CalcResult>) {
    val mainW = params.width.toFloat()
    val branchW = params.width.toFloat() * 0.5f
    val funnelW = mainW * 0.75f
    val funnelH = params.height.toFloat() * 0.5f

    val cx = w / 2f
    val cy = h / 2f
    val sc = scaleFactor(minOf(w - 80f, h - 80f), mainW * 1.5f)

    val mw = mainW * sc
    val bw = branchW * sc
    val fw = funnelW * sc
    val fh = funnelH * sc

    // 主管（水平矩形）
    val main = listOf(
        Pair(cx - mw / 2f, cy - 30f),
        Pair(cx + mw / 2f, cy - 30f),
        Pair(cx + mw / 2f, cy + 30f),
        Pair(cx - mw / 2f, cy + 30f)
    )
    drawShapePath(main)

    // 支路切口（主管中间的开口）
    drawLine(DIM_RED, Offset(cx - bw / 2f, cy - 30f), Offset(cx - bw / 2f, cy + 30f), strokeWidth = 2f)
    drawLine(DIM_RED, Offset(cx + bw / 2f, cy - 30f), Offset(cx + bw / 2f, cy + 30f), strokeWidth = 2f)

    // 漏斗（支路下方展开）
    val funnel = listOf(
        Pair(cx - fw / 2f, cy + 30f),
        Pair(cx + fw / 2f, cy + 30f),
        Pair(cx + bw / 2f, cy + 30f + fh),
        Pair(cx - bw / 2f, cy + 30f + fh)
    )
    drawShapePath(funnel)

    // 标注
    drawDimLine(
        Offset(cx - mw / 2f, cy - 30f),
        Offset(cx + mw / 2f, cy - 30f),
        "主管 ${fmtCm(mainW.toDouble())}", offset = -25f
    )
    drawDimLine(
        Offset(cx - bw / 2f, cy + 30f + fh),
        Offset(cx + bw / 2f, cy + 30f + fh),
        "支管 ${fmtCm(branchW.toDouble())}", offset = 25f
    )
    drawDimLine(
        Offset(cx - fw / 2f, cy + 30f),
        Offset(cx - fw / 2f, cy + 30f + fh),
        "漏斗 ${fmtCm(funnelH.toDouble())}", offset = -30f
    )
    drawLabel("三通展开", Offset(cx, cy + 30f + fh + 40f), Color.Gray)
}

/**
 * 变径：梯形过渡展开
 */
private fun DrawScope.drawReducingDiagram(w: Float, h: Float, params: CalcParams, results: List<CalcResult>) {
    val w1 = params.beforeWidth.toFloat()
    val w2 = params.afterWidth.toFloat()
    val transH = params.height.toFloat()
    val deltaW = abs(w2 - w1)
    val hyp = sqrt(deltaW * deltaW + transH * transH)

    val cx = w / 2f
    val cy = h / 2f
    val sc = scaleFactor(minOf(w - 80f, h - 80f), maxOf(maxOf(w1, w2), transH))

    val w1w = w1 * sc
    val w2w = w2 * sc
    val th = transH * sc

    // 梯形（上窄下宽 或 上宽下窄）
    val shape = if (w1 >= w2) {
        listOf(
            Pair(cx - w1w / 2f, cy - th / 2f),
            Pair(cx + w1w / 2f, cy - th / 2f),
            Pair(cx + w2w / 2f, cy + th / 2f),
            Pair(cx - w2w / 2f, cy + th / 2f)
        )
    } else {
        listOf(
            Pair(cx - w2w / 2f, cy - th / 2f),
            Pair(cx + w2w / 2f, cy - th / 2f),
            Pair(cx + w1w / 2f, cy + th / 2f),
            Pair(cx - w1w / 2f, cy + th / 2f)
        )
    }
    drawShapePath(shape)

    // 标注
    drawDimLine(
        Offset(shape[0].first, shape[0].second),
        Offset(shape[1].first, shape[1].second),
        "宽 ${fmtCm(w1.toDouble())}", offset = -25f
    )
    drawDimLine(
        Offset(shape[2].first, shape[2].second),
        Offset(shape[3].first, shape[3].second),
        "宽 ${fmtCm(w2.toDouble())}", offset = 25f
    )
    drawDimLine(
        Offset(shape[0].first, shape[0].second),
        Offset(shape[3].first, shape[3].second),
        "高 ${fmtCm(transH.toDouble())}", offset = -30f
    )
    drawDimLine(
        Offset(shape[1].first, shape[1].second),
        Offset(shape[2].first, shape[2].second),
        "斜边 ${fmtCm(hyp.toDouble())}", offset = -30f
    )
    drawLabel("变径展开", Offset(cx, cy + th / 2f + 40f), Color.Gray)
}

/**
 * 组合翻弯：两段折线展开
 */
private fun DrawScope.drawCompositeDiagram(w: Float, h: Float, params: CalcParams, results: List<CalcResult>) {
    val b1 = params.distance1.toFloat()
    val a1 = params.angle1.toFloat()
    val b2 = params.distance2.toFloat()
    val a2 = params.angle2.toFloat()

    val x1 = b1 * tan(a1 * PI.toFloat() / 360f)
    val l1 = b1 / sin(a1 * PI.toFloat() / 180f)
    val h1 = b1 * tan(a1 * PI.toFloat() / 180f)

    val cx = w / 2f
    val cy = h / 2f
    val sc = scaleFactor(minOf(w - 80f, h - 80f), maxOf(b1 + b2, h1) * 1.2f)

    // 第一段
    val seg1W = b1 * sc
    val seg1H = h1 * sc
    val seg1 = listOf(
        Pair(cx - seg1W / 2f, cy),
        Pair(cx, cy),
        Pair(cx, cy - seg1H),
        Pair(cx - seg1W / 2f, cy - seg1H)
    )
    drawShapePath(seg1, fill = false)
    drawLine(LINE_BLUE, seg1[0].let { Offset(it.first, it.second) },
        seg1[2].let { Offset(it.first, it.second) }, strokeWidth = 1f)

    // 第二段
    val seg2W = b2 * sc
    val seg2H = h1 * sc  // 简化
    val seg2 = listOf(
        Pair(cx, cy),
        Pair(cx + seg2W / 2f, cy),
        Pair(cx + seg2W / 2f, cy - seg2H),
        Pair(cx, cy - seg2H)
    )
    drawShapePath(seg2, fill = false)

    // 折弯线
    drawLine(HIGHLIGHT_RED, Offset(cx, cy - seg1H), Offset(cx, cy), strokeWidth = 2.5f)

    // 标注
    drawDimLine(
        Offset(cx - seg1W / 2f, cy),
        Offset(cx, cy),
        "第1段 ${fmtCm(b1.toDouble())}", offset = 25f
    )
    drawDimLine(
        Offset(cx, cy),
        Offset(cx + seg2W / 2f, cy),
        "第2段 ${fmtCm(b2.toDouble())}", offset = 25f
    )
    drawDimLine(
        Offset(cx, cy),
        Offset(cx, cy - seg1H),
        "高度 ${fmtCm(h1.toDouble())}", offset = -30f,
        labelColor = HIGHLIGHT_RED
    )
    drawLabel("组合翻弯", Offset(cx, cy + 40f), Color.Gray)
}

private val HIGHLIGHT_RED = Color(0xFFD32F2F)

/**
 * 折角：简单折角展开
 */
private fun DrawScope.drawFoldedDiagram(w: Float, h: Float, params: CalcParams, results: List<CalcResult>) {
    val b = params.distance.toFloat()
    val angle = params.angle.toFloat()
    val aRad = angle * PI.toFloat() / 180f
    val baseX = b * tan(aRad / 2f)
    val hyp = b / sin(aRad)
    val riseH = b * tan(aRad)

    val cx = w / 2f
    val cy = h / 2f
    val sc = scaleFactor(minOf(w - 80f, h - 80f), maxOf(b, riseH))

    val bw = b * sc
    val hw = riseH * sc

    // 折角形状（L形）
    val shape = listOf(
        Pair(cx - bw / 2f, cy + hw / 2f),
        Pair(cx + bw / 2f, cy + hw / 2f),
        Pair(cx + bw / 2f, cy),
        Pair(cx, cy),
        Pair(cx, cy - hw / 2f),
        Pair(cx - bw / 2f, cy - hw / 2f)
    )
    drawShapePath(shape)

    // 折弯线
    drawLine(HIGHLIGHT_RED, Offset(cx, cy + hw / 2f), Offset(cx, cy), strokeWidth = 2.5f)
    drawLine(HIGHLIGHT_RED, Offset(cx, cy), Offset(cx - bw / 2f, cy - hw / 2f), strokeWidth = 2.5f)

    // 标注
    drawDimLine(
        Offset(cx - bw / 2f, cy + hw / 2f),
        Offset(cx + bw / 2f, cy + hw / 2f),
        "下料 ${fmtCm(b.toDouble())}", offset = 25f
    )
    drawDimLine(
        Offset(cx - bw / 2f, cy + hw / 2f),
        Offset(cx - bw / 2f, cy - hw / 2f),
        fmtCm(riseH.toDouble()), offset = -30f
    )
    drawDimLine(
        Offset(cx, cy),
        Offset(cx + bw / 2f, cy),
        "底边 ${fmtCm(baseX.toDouble())}", offset = -20f,
        labelColor = HIGHLIGHT_RED
    )
    drawLabel("折角展开", Offset(cx, cy + hw / 2f + 40f), Color.Gray)
}
