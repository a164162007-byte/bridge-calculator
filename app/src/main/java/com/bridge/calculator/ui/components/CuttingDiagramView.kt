package com.bridge.calculator.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridge.calculator.core.elbow.CalcParams
import com.bridge.calculator.core.elbow.CalcResult
import com.bridge.calculator.core.elbow.FormulaEngine
import com.bridge.calculator.core.elbow.ModelType
import kotlin.math.*

/**
 * 2D 下料展开图 v2 — 对齐陈工桥架计算器视觉风格
 *
 * 视觉特征：
 *  - 浅蓝/青色渐变背景
 *  - 左侧小比例侧面轮廓（约1/4宽）
 *  - 右侧大比例展开切割图（约2/3宽）
 *  - 蓝色主图线 + 红色双箭头尺寸线 + 绿色数字标签
 *  - "下料"标签居中在图形正上方
 *  - 单指拖动旋转，双击复位
 */
@Composable
fun CuttingDiagramView(
    params: CalcParams,
    modelType: ModelType,
    modifier: Modifier = Modifier
) {
    var rotationZ by remember { mutableFloatStateOf(0f) }
    var lastDragX by remember { mutableFloatStateOf(0f) }
    val results = remember(params, modelType) { calcForType(params, modelType) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BG_GRADIENT)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    rotationZ += (drag.x - lastDragX) * 0.4f
                    lastDragX = drag.x
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { rotationZ = 0f })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            withTransform({ rotate(rotationZ, center) }) {
                when (modelType) {
                    ModelType.RAMP, ModelType.CUSTOM -> drawRampDiagram(this, params, results)
                    ModelType.HORIZONTAL -> drawHorizontalDiagram(this, params, results)
                    ModelType.TEE -> drawTeeDiagram(this, params, results)
                    ModelType.REDUCING -> drawReducingDiagram(this, params, results)
                    ModelType.COMPOSITE -> drawCompositeDiagram(this, params, results)
                    ModelType.FOLDED -> drawFoldedDiagram(this, params, results)
                }
            }
        }
    }
}

// ─── 颜色常量 ─────────────────────────────────────────────

private val BG_GRADIENT = Brush.verticalGradient(
    colors = listOf(Color(0xFFE0F2F7), Color(0xFFB3E5FC), Color(0xFFE0F7FA))
)
private val SHAPE_BLUE = Color(0xFF1565C0)
private val SHAPE_FILL = Color(0x1A1565C0)
private val DIM_RED = Color(0xFFC62828)
private val DIM_RED_LIGHT = Color(0x66C62828)
private val LABEL_GREEN = Color(0xFF2E7D32)
private val LABEL_GRAY = Color(0xFF616161)
private val LABEL_BLACK = Color(0xFF212121)

// ─── 计算函数 ─────────────────────────────────────────────

private fun calcForType(params: CalcParams, modelType: ModelType): List<CalcResult> = when (modelType) {
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

private fun getR(results: List<CalcResult>, name: String): Double =
    results.firstOrNull { it.name.contains(name) }?.value ?: 0.0

// ─── 工具函数 ─────────────────────────────────────────────

/** mm 转 cm，3位小数 */
private fun fmtCm(mm: Double): String = "%.3f cm".format(mm / 10.0)

/** 计算缩放因子 */
private fun calcScale(availablePx: Float, maxMm: Float): Float =
    availablePx / maxMm.coerceAtLeast(1f)

/** 绘制填充+描边形状 */
private fun DrawScope.drawShape(points: List<Offset>, fillColor: Color = SHAPE_FILL, strokeColor: Color = SHAPE_BLUE, strokeW: Float = 3f) {
    if (points.size < 3) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
    drawPath(path, fillColor)
    drawPath(path, strokeColor, style = Stroke(width = strokeW, pathEffect = PathEffect.cornerPathEffect(2f)))
}

/** 绘制红色尺寸线（双箭头+延伸线+绿色文字标签） */
private fun DrawScope.drawDim(
    p1: Offset, p2: Offset,
    label: String,
    offsetPx: Float = 28f,
    labelColor: Color = LABEL_GREEN,
    labelOffsetPx: Float = -8f
) {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    // 法向量
    val nx = -dy / len * offsetPx
    val ny = dx / len * offsetPx
    val s = Offset(p1.x + nx, p1.y + ny)
    val e = Offset(p2.x + nx, p2.y + ny)

    // 延伸线（虚线效果用细线）
    drawLine(DIM_RED_LIGHT, p1, s, strokeWidth = 1.2f)
    drawLine(DIM_RED_LIGHT, p2, e, strokeWidth = 1.2f)
    // 主线
    drawLine(DIM_RED, s, e, strokeWidth = 2f)
    // 双箭头
    drawArrow(s, e, DIM_RED, 10f)
    drawArrow(e, s, DIM_RED, 10f)

    // 文字标签
    val mid = Offset((s.x + e.x) / 2f, (s.y + e.y) / 2f)
    drawText(label, mid.x, mid.y + labelOffsetPx, labelColor, 16f * density, true)
}

/** 绘制箭头 */
private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color, size: Float) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val ux = dx / len; val uy = dy / len
    val px = -uy; val py = ux
    val tip = to
    val l = Offset(to.x - ux * size + px * size * 0.45f, to.y - uy * size + py * size * 0.45f)
    val r = Offset(to.x - ux * size - px * size * 0.45f, to.y - uy * size - py * size * 0.45f)
    val path = Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(r.x, r.y); close() }
    drawPath(path, color)
}

/** 绘制文字 */
private fun DrawScope.drawText(text: String, x: Float, y: Float, color: Color, textSizePx: Float, bold: Boolean = false) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setColor(color.toArgb())
        this.textSize = textSizePx
        textAlign = Paint.Align.CENTER
        if (bold) isFakeBoldText = true
    }
    canvas.drawText(text, x, y, paint)
}

/** 绘制顶部"下料"标签 */
private fun DrawScope.drawTopLabel(text: String, cx: Float, topY: Float) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setColor(LABEL_BLACK.toArgb())
        textSize = 15f * density
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(text, cx, topY, paint)
}

/** 绘制中文标注（如"斜边"、"侧面轮廓"） */
private fun DrawScope.drawAnnotation(text: String, x: Float, y: Float, color: Color = LABEL_GRAY) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setColor(color.toArgb())
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(text, x, y, paint)
}

// ─── 爬坡弯头展开图 ───────────────────────────────────────

private fun DrawScope.drawRampDiagram(params: CalcParams, results: List<CalcResult>) {
    val W = params.width.toFloat()        // 桥架宽度
    val b = params.distance.toFloat()     // 水平距离
    val angle = params.angle
    val aRad = angle * PI.toFloat() / 180f

    // 核心计算值
    val baseX = (b * tan(aRad / 2f)).toDouble()    // 底边 X
    val hypL = (b / sin(aRad)).toDouble()           // 斜边 L
    val riseH = (b * tan(aRad)).toDouble()          // 爬坡高度
    val cutW = (hypL * sin(aRad / 2f)).toDouble()   // 切口宽

    val cw = size.width
    val ch = size.height

    // ── 布局分区 ──
    // 左侧：侧面轮廓（约30%宽），右侧：展开图（约60%宽）
    val leftAreaW = cw * 0.30f
    val rightAreaW = cw * 0.58f
    val gap = cw * 0.06f
    val leftCx = leftAreaW / 2f
    val rightCx = leftAreaW + gap + rightAreaW / 2f
    val centerY = ch / 2f
    val pad = 55f  // 上下留白

    // ══════════════════════════════════════
    // 左侧：侧面轮廓（直角梯形）
    // ═══════════════════════════════════════
    val maxLeft = maxOf(b, riseH).toFloat()
    val scLeft = calcScale(minOf(leftAreaW - 30f, ch - pad * 2), maxLeft)

    val lw = b * scLeft      // 底边像素
    val lh = riseH * scLeft  // 高度像素
    val lx = baseX.toFloat() * scLeft  // 底边差像素

    // 直角梯形：左下→右下→右上(缩进)→左上
    val sideProfile = listOf(
        Offset(leftCx - lw / 2f, centerY + lh / 2f),     // 左下（直角）
        Offset(leftCx + lw / 2f, centerY + lh / 2f),     // 右下
        Offset(leftCx + lw / 2f - lx, centerY - lh / 2f), // 右上（缩进 baseX）
        Offset(leftCx - lw / 2f, centerY - lh / 2f)       // 左上
    )
    drawShape(sideProfile)

    // 顶部"下料"标签
    val sideTopY = centerY - lh / 2f - 32f * density
    drawTopLabel("下料", leftCx, sideTopY)

    // 底部标注
    drawAnnotation("侧面轮廓", leftCx, centerY + lh / 2f + 22f * density)

    // 侧面尺寸标注
    // 底边（水平距离 b）
    drawDim(
        sideProfile[0], sideProfile[1],
        fmtCm(b.toDouble()), offsetPx = 22f
    )
    // 左侧高（爬坡高度）
    drawDim(
        sideProfile[0], sideProfile[3],
        fmtCm(riseH), offsetPx = -22f
    )
    // 斜边
    drawDim(
        sideProfile[1], sideProfile[2],
        "斜边 ${fmtCm(hypL)}", offsetPx = -20f
    )

    // ═══════════════════════════════════════
    // 右侧：展开切割图（平行四边形）
    // ═══════════════════════════════════════
    val diagLen = sqrt((W * W + b * b).toDouble()).toFloat()  // 展开图对角线
    val maxRight = maxOf(diagLen, b)
    val scRight = calcScale(minOf(rightAreaW - 30f, ch - pad * 2), maxRight)

    val pw = W * scRight      // 平行四边形底边（桥架宽度）
    val ph = b * scRight      // 平行四边形高度（水平距离）
    val px_shift = diagLen * scRight * 0.3f  // 倾斜偏移（让形状更像平行四边形）

    // 平行四边形：左下→右下→右上→左上
    val expandShape = listOf(
        Offset(rightCx - pw / 2f, centerY + ph / 2f),          // 左下
        Offset(rightCx + pw / 2f, centerY + ph / 2f),          // 右下
        Offset(rightCx + pw / 2f + px_shift, centerY - ph / 2f), // 右上（偏移）
        Offset(rightCx - pw / 2f + px_shift, centerY - ph / 2f)  // 左上（偏移）
    )
    drawShape(expandShape)

    // 顶部"下料"标签
    val expandTopY = centerY - ph / 2f - 32f * density
    drawTopLabel("下料", rightCx + px_shift / 2f, expandTopY)

    // 底部标注
    drawAnnotation("展开切割", rightCx + px_shift / 2f, centerY + ph / 2f + 22f * density)

    // 展开图尺寸标注
    // 底边（桥架宽度 W）
    drawDim(
        expandShape[0], expandShape[1],
        fmtCm(W.toDouble()), offsetPx = 22f
    )
    // 左侧斜边
    drawDim(
        expandShape[0], expandShape[3],
        "斜边 ${fmtCm(diagLen.toDouble())}", offsetPx = -22f
    )
    // 下料长度（斜边 L = b/sin(α)）
    drawDim(
        expandShape[1], expandShape[2],
        "下料 ${fmtCm(hypL)}", offsetPx = 20f,
        labelColor = LABEL_GREEN
    )
}

// ─── 水平弯头展开图 ───────────────────────────────────────

private fun DrawScope.drawHorizontalDiagram(params: CalcParams, results: List<CalcResult>) {
    val W = params.width.toFloat()
    val b = params.distance.toFloat()
    val angle = params.angle
    val aRad = angle * PI.toFloat() / 180f

    val baseX = (b * tan(aRad / 2f)).toDouble()
    val hypL = (b / sin(aRad)).toDouble()
    val cutW = (hypL * sin(aRad / 2f)).toDouble()

    val cw = size.width
    val ch = size.height
    val centerX = cw / 2f
    val centerY = ch / 2f
    val pad = 55f

    val maxDim = maxOf(W, b).toFloat()
    val sc = calcScale(minOf(cw - 60f, ch - pad * 2) * 0.85f, maxDim)

    val shapeW = W * sc
    val shapeH = (b * 0.5f) * sc  // 半高
    val cutPx = baseX.toFloat() * sc * 0.5f

    // 左梯形展开图
    val leftTrap = listOf(
        Offset(centerX - shapeW * 0.15f - shapeW / 2f, centerY - shapeH),
        Offset(centerX - shapeW * 0.15f, centerY - shapeH),
        Offset(centerX - shapeW * 0.15f, centerY + shapeH),
        Offset(centerX - shapeW * 0.15f - shapeW / 2f + cutPx, centerY + shapeH)
    )
    drawShape(leftTrap)

    // 右梯形展开图（镜像）
    val rightTrap = listOf(
        Offset(centerX + shapeW * 0.15f, centerY - shapeH),
        Offset(centerX + shapeW * 0.15f + shapeW / 2f, centerY - shapeH),
        Offset(centerX + shapeW * 0.15f + shapeW / 2f - cutPx, centerY + shapeH),
        Offset(centerX + shapeW * 0.15f, centerY + shapeH)
    )
    drawShape(rightTrap)

    // 中间连接线
    drawLine(SHAPE_BLUE, Offset(centerX - shapeW * 0.15f, centerY), Offset(centerX + shapeW * 0.15f, centerY), strokeWidth = 2f)

    // 顶部"下料"标签
    val topY = centerY - shapeH - 32f * density
    drawTopLabel("下料", centerX - shapeW * 0.15f - shapeW / 4f, topY)
    drawTopLabel("下料", centerX + shapeW * 0.15f + shapeW / 4f, topY)

    // 底部标注
    drawAnnotation("水平弯展开图", centerX, centerY + shapeH + 22f * density)

    // 尺寸标注
    drawDim(
        Offset(centerX - shapeW * 0.15f - shapeW / 2f, centerY - shapeH),
        Offset(centerX - shapeW * 0.15f, centerY - shapeH),
        fmtCm(W.toDouble()), offsetPx = -22f
    )
    drawDim(
        Offset(centerX - shapeW * 0.15f, centerY - shapeH),
        Offset(centerX - shapeW * 0.15f, centerY + shapeH),
        fmtCm(b.toDouble()), offsetPx = -22f
    )
    drawDim(
        leftTrap[3], leftTrap[0],
        "斜边 ${fmtCm(hypL)}", offsetPx = 20f
    )
}

// ─── 三通展开图 ───────────────────────────────────────────

private fun DrawScope.drawTeeDiagram(params: CalcParams, results: List<CalcResult>) {
    val W = params.width.toFloat()
    val H = params.height.toFloat()

    val cw = size.width
    val ch = size.height
    val centerX = cw / 2f
    val centerY = ch / 2f
    val pad = 55f

    val maxDim = maxOf(W, H * 2f)
    val sc = calcScale(minOf(cw - 60f, ch - pad * 2) * 0.8f, maxDim)

    val mainW = W * sc
    val mainH = H * 0.3f * sc
    val branchW = W * 0.5f * sc
    val funnelH = H * 0.6f * sc
    val funnelW = W * 0.75f * sc

    // 主管（水平矩形）
    val mainRect = listOf(
        Offset(centerX - mainW / 2f, centerY - mainH),
        Offset(centerX + mainW / 2f, centerY - mainH),
        Offset(centerX + mainW / 2f, centerY + mainH),
        Offset(centerX - mainW / 2f, centerY + mainH)
    )
    drawShape(mainRect)

    // 支路切口线
    drawLine(DIM_RED, Offset(centerX - branchW / 2f, centerY - mainH), Offset(centerX - branchW / 2f, centerY + mainH), strokeWidth = 2.5f)
    drawLine(DIM_RED, Offset(centerX + branchW / 2f, centerY - mainH), Offset(centerX + branchW / 2f, centerY + mainH), strokeWidth = 2.5f)

    // 漏斗（下方展开）
    val funnel = listOf(
        Offset(centerX - funnelW / 2f, centerY + mainH),
        Offset(centerX + funnelW / 2f, centerY + mainH),
        Offset(centerX + branchW / 2f, centerY + mainH + funnelH),
        Offset(centerX - branchW / 2f, centerY + mainH + funnelH)
    )
    drawShape(funnel)

    // 顶部"下料"标签
    drawTopLabel("下料", centerX, centerY - mainH - 32f * density)
    drawAnnotation("三通展开图", centerX, centerY + mainH + funnelH + 22f * density)

    // 尺寸标注
    drawDim(
        mainRect[0], mainRect[1],
        "主管 ${fmtCm(W.toDouble())}", offsetPx = -22f
    )
    drawDim(
        Offset(centerX - branchW / 2f, centerY + mainH + funnelH),
        Offset(centerX + branchW / 2f, centerY + mainH + funnelH),
        "支管 ${fmtCm(W * 0.5)}", offsetPx = 22f
    )
    drawDim(
        funnel[0], funnel[3],
        "漏斗 ${fmtCm(funnelH / sc)}", offsetPx = -22f
    )
}

// ─── 变径展开图 ───────────────────────────────────────────

private fun DrawScope.drawReducingDiagram(params: CalcParams, results: List<CalcResult>) {
    val w1 = params.beforeWidth.toFloat()
    val w2 = params.afterWidth.toFloat()
    val H = params.height.toFloat()
    val deltaW = abs(w2 - w1)
    val hypL = sqrt((deltaW * deltaW + H * H).toDouble())

    val cw = size.width
    val ch = size.height
    val centerX = cw / 2f
    val centerY = ch / 2f
    val pad = 55f

    val maxDim = maxOf(maxOf(w1, w2), H)
    val sc = calcScale(minOf(cw - 60f, ch - pad * 2) * 0.8f, maxDim)

    val w1px = w1 * sc
    val w2px = w2 * sc
    val hpx = H * sc

    // 梯形
    val shape = if (w1 >= w2) {
        listOf(
            Offset(centerX - w1px / 2f, centerY - hpx / 2f),
            Offset(centerX + w1px / 2f, centerY - hpx / 2f),
            Offset(centerX + w2px / 2f, centerY + hpx / 2f),
            Offset(centerX - w2px / 2f, centerY + hpx / 2f)
        )
    } else {
        listOf(
            Offset(centerX - w2px / 2f, centerY - hpx / 2f),
            Offset(centerX + w2px / 2f, centerY - hpx / 2f),
            Offset(centerX + w1px / 2f, centerY + hpx / 2f),
            Offset(centerX - w1px / 2f, centerY + hpx / 2f)
        )
    }
    drawShape(shape)

    drawTopLabel("下料", centerX, centerY - hpx / 2f - 32f * density)
    drawAnnotation("变径展开图", centerX, centerY + hpx / 2f + 22f * density)

    // 尺寸标注
    drawDim(shape[0], shape[1], "宽 ${fmtCm(w1.toDouble())}", offsetPx = -22f)
    drawDim(shape[2], shape[3], "宽 ${fmtCm(w2.toDouble())}", offsetPx = 22f)
    drawDim(shape[0], shape[3], "高 ${fmtCm(H.toDouble())}", offsetPx = -22f)
    drawDim(shape[1], shape[2], "斜边 ${fmtCm(hypL)}", offsetPx = -22f)
}

// ─── 组合翻弯展开图 ───────────────────────────────────────

private fun DrawScope.drawCompositeDiagram(params: CalcParams, results: List<CalcResult>) {
    val b1 = params.distance1.toFloat()
    val a1 = params.angle1
    val b2 = params.distance2.toFloat()
    val a2 = params.angle2
    val a1Rad = a1 * PI.toFloat() / 180f
    val a2Rad = a2 * PI.toFloat() / 180f

    val h1 = (b1 * tan(a1Rad)).toDouble()
    val l1 = (b1 / sin(a1Rad)).toDouble()
    val h2 = (b2 * tan(a2Rad)).toDouble()
    val l2 = (b2 / sin(a2Rad)).toDouble()

    val cw = size.width
    val ch = size.height
    val centerX = cw / 2f
    val centerY = ch / 2f + 20f
    val pad = 55f

    val maxDim = maxOf(b1 + b2, maxOf(h1, h2)).toFloat()
    val sc = calcScale(minOf(cw - 60f, ch - pad * 2) * 0.8f, maxDim)

    val s1w = b1 * sc
    val s1h = h1.toFloat() * sc
    val s2w = b2 * sc
    val s2h = h2.toFloat() * sc

    // 第一段矩形
    val seg1 = listOf(
        Offset(centerX - s1w / 2f, centerY),
        Offset(centerX, centerY),
        Offset(centerX, centerY - s1h),
        Offset(centerX - s1w / 2f, centerY - s1h)
    )
    drawShape(seg1)

    // 第二段矩形
    val seg2 = listOf(
        Offset(centerX, centerY),
        Offset(centerX + s2w / 2f, centerY),
        Offset(centerX + s2w / 2f, centerY - s2h),
        Offset(centerX, centerY - s2h)
    )
    drawShape(seg2)

    // 中间折弯线
    drawLine(DIM_RED, Offset(centerX, centerY), Offset(centerX, centerY - maxOf(s1h, s2h)), strokeWidth = 2.5f)

    drawTopLabel("下料", centerX - s1w / 4f, centerY - s1h - 32f * density)
    drawTopLabel("下料", centerX + s2w / 4f, centerY - s2h - 32f * density)
    drawAnnotation("组合翻弯展开图", centerX, centerY + 22f * density)

    // 尺寸标注
    drawDim(seg1[0], seg1[1], "第1段 ${fmtCm(b1.toDouble())}", offsetPx = 22f)
    drawDim(seg2[0], seg2[1], "第2段 ${fmtCm(b2.toDouble())}", offsetPx = 22f)
    drawDim(
        Offset(centerX, centerY), Offset(centerX, centerY - s1h),
        "高 ${fmtCm(h1)}", offsetPx = -22f
    )
    drawDim(
        seg1[3], seg1[1],
        "斜边 ${fmtCm(l1)}", offsetPx = 20f
    )
}

// ─── 折角展开图 ───────────────────────────────────────────

private fun DrawScope.drawFoldedDiagram(params: CalcParams, results: List<CalcResult>) {
    val b = params.distance.toFloat()
    val angle = params.angle
    val aRad = angle * PI.toFloat() / 180f

    val baseX = (b * tan(aRad / 2f)).toDouble()
    val hypL = (b / sin(aRad)).toDouble()
    val riseH = (b * tan(aRad)).toDouble()

    val cw = size.width
    val ch = size.height
    val centerX = cw / 2f
    val centerY = ch / 2f
    val pad = 55f

    val maxDim = maxOf(b, riseH).toFloat()
    val sc = calcScale(minOf(cw - 60f, ch - pad * 2) * 0.8f, maxDim)

    val bw = b * sc
    val hw = riseH.toFloat() * sc
    val xw = baseX.toFloat() * sc

    // L 形折角
    val shape = listOf(
        Offset(centerX - bw / 2f, centerY + hw / 2f),
        Offset(centerX + bw / 2f, centerY + hw / 2f),
        Offset(centerX + bw / 2f, centerY),
        Offset(centerX, centerY),
        Offset(centerX, centerY - hw / 2f),
        Offset(centerX - bw / 2f, centerY - hw / 2f)
    )
    drawShape(shape)

    // 折弯线
    drawLine(DIM_RED, Offset(centerX, centerY + hw / 2f), Offset(centerX, centerY), strokeWidth = 2.5f)
    drawLine(DIM_RED, Offset(centerX, centerY), Offset(centerX - bw / 2f, centerY - hw / 2f), strokeWidth = 2.5f)

    drawTopLabel("下料", centerX, centerY - hw / 2f - 32f * density)
    drawAnnotation("折角展开图", centerX, centerY + hw / 2f + 22f * density)

    // 尺寸标注
    drawDim(shape[0], shape[1], "下料 ${fmtCm(b.toDouble())}", offsetPx = 22f)
    drawDim(shape[0], shape[5], fmtCm(riseH), offsetPx = -22f)
    drawDim(
        Offset(centerX, centerY), Offset(centerX + bw / 2f, centerY),
        "底边 ${fmtCm(baseX)}", offsetPx = -18f, labelColor = DIM_RED
    )
}
