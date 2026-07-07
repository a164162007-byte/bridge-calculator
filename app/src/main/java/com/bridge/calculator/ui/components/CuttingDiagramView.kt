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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bridge.calculator.core.elbow.CalcParams
import com.bridge.calculator.core.elbow.CalcResult
import com.bridge.calculator.core.elbow.FormulaEngine
import com.bridge.calculator.core.elbow.ModelType
import kotlin.math.*

/**
 * 爬坡计算视图 — 对齐陈工桥架计算器
 * 砖墙障碍物 + 直角三角形 + 尺寸标注 + 角度标注 + 下料参数
 */
@Composable
fun CalculationView(
    params: CalcParams,
    modifier: Modifier = Modifier
) {
    val W = params.width.toFloat()
    val b = params.distance.toFloat()
    val angleDeg = params.angle.toFloat()
    val aRad = angleDeg * PI.toFloat() / 180f

    val baseX = b * tan(aRad / 2f)
    val hypL = b / sin(aRad)
    val riseH = b * tan(aRad)
    val cutW = W * tan(aRad / 2f)
    val cutHalf = cutW / 2f

    val angleA = 90f
    val angleB = angleDeg
    val angleC = maxOf(0f, 180f - angleA - angleB)

    Canvas(modifier = modifier.fillMaxSize().padding(8.dp)) {
        val cw = size.width
        val ch = size.height
        val margin = 30f
        val brickW = cw * 0.12f
        val bottomMargin = 60f

        val availW = cw - brickW - margin * 2f
        val availH = ch - bottomMargin - margin
        val maxDim = maxOf(b, riseH)
        val sc = minOf(availW / maxDim, availH / maxDim)

        val triW = b * sc
        val triH = riseH * sc
        val triX = b * sc - baseX * sc

        val ox = brickW + margin
        val oy = ch - bottomMargin

        val pA = Offset(ox, oy - triH)
        val pB = Offset(ox, oy)
        val pC = Offset(ox + triW, oy)

        drawBrickWall(ox, oy, triH, brickW)

        val rampPath = Path().apply {
            moveTo(pB.x, pB.y)
            lineTo(pC.x, pC.y)
            lineTo(pA.x, pA.y)
            close()
        }
        drawPath(rampPath, Color(0x3042A5F5))
        drawPath(rampPath, Color(0xFF1565C0), style = Stroke(width = 3f))

        // 桥架宽度小方块
        val boxSize = 28f
        drawRect(Color(0xFF1565C0), Offset(pB.x - boxSize - 4f, pB.y - boxSize / 2f),
            androidx.compose.ui.geometry.Size(boxSize, boxSize))
        drawText("L", pB.x - boxSize / 2f - 4f, pB.y + 5f, Color.White, 11f * density, true)

        // 三角形内部标注
        val midAB = Offset((pA.x + pB.x) / 2f, (pA.y + pB.y) / 2f)
        val midBC = Offset((pB.x + pC.x) / 2f, (pB.y + pC.y) / 2f)
        val midAC = Offset((pA.x + pC.x) / 2f, (pA.y + pC.y) / 2f)

        drawDimV(midAB.x - 24f * density, midAB.y, "a=%.1f".format(riseH / 10f))
        drawDimH(midBC.x, midBC.y + 22f * density, "b=%.1f".format(b / 10f))
        drawText("c=%.1f".format(hypL / 10f), midAC.x + 16f * density, midAC.y - 8f * density,
            Color(0xFFC62828), 14f * density, true)

        drawText("∠A=%.0f°".format(angleA), pA.x + 14f * density, pA.y + 18f * density,
            Color(0xFFC62828), 11f * density)
        drawText("∠B=%.0f°".format(angleB), pB.x + 10f * density, pB.y - 10f * density,
            Color(0xFFC62828), 11f * density)
        drawText("∠C=%.0f°".format(angleC), pC.x - 50f * density, pC.y - 10f * density,
            Color(0xFFC62828), 11f * density)

        // 下料参数
        drawText("下料x: %.2f cm".format(cutW / 10f), cw - margin, margin + 10f * density,
            Color(0xFF212121), 14f * density, true)
        drawText("x/2: %.3f cm".format(cutHalf / 10f), cw - margin, margin + 32f * density,
            Color(0xFF212121), 13f * density)
    }
}

private fun DrawScope.drawBrickWall(x: Float, bottomY: Float, height: Float, width: Float) {
    val brickH = 14f
    val brickW = width * 0.7f
    val rows = (height / brickH).toInt()
    for (row in 0 until rows) {
        val y = bottomY - (row + 1) * brickH
        val offset = if (row % 2 == 0) 0f else brickW * 0.5f
        var bx = x - width + offset
        while (bx < x) {
            val bw = minOf(brickW, x - bx)
            drawRect(Color(0xFFBDBDBD), Offset(bx, y),
                androidx.compose.ui.geometry.Size(bw - 1.5f, brickH - 1.5f))
            bx += brickW
        }
    }
    drawRect(Color(0xFF757575), Offset(x - width, bottomY - height),
        androidx.compose.ui.geometry.Size(width, height),
        style = Stroke(width = 1f))
}

private fun DrawScope.drawDimV(x: Float, y: Float, label: String) {
    drawText(label, x, y, Color(0xFF212121), 13f * density, true)
}

private fun DrawScope.drawDimH(x: Float, y: Float, label: String) {
    drawText(label, x, y, Color(0xFF212121), 13f * density, true)
}

// ================================================================
// 2D 下料展开图 v3 — 完全对齐陈工桥架计算器
// 桥架侧面视图 + 红色切口三角标记 + 划线尺寸标注 + 3D线框
// ================================================================

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
            .background(Color(0xFFF5F5F5))
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
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            withTransform({ rotate(rotationZ, center) }) {
                when (modelType) {
                    ModelType.RAMP, ModelType.CUSTOM -> drawRampCutting(params, results)
                    ModelType.HORIZONTAL -> drawHorizontalCutting(params, results)
                    ModelType.TEE -> drawTeeCutting(params, results)
                    ModelType.REDUCING -> drawReducingCutting(params, results)
                    ModelType.COMPOSITE -> drawCompositeCutting(params, results)
                    ModelType.FOLDED -> drawFoldedCutting(params, results)
                }
            }
        }
    }
}

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

// ── 颜色 ─
private val TRAY_FILL = Color(0xFFD6EAF8)
private val TRAY_STROKE = Color(0xFF2980B9)
private val CUT_RED = Color(0xFFE74C3C)
private val CUT_FILL = Color(0x40E74C3C)
private val DIM_COLOR = Color(0xFF2C3E50)
private val LABEL_GREEN = Color(0xFF27AE60)
private val BG_COLOR = Color(0xFFF5F5F5)
private val WIRE_COLOR = Color(0xFF7F8C8D)

// ── 工具函数 ──
private fun DrawScope.drawText(text: String, x: Float, y: Float, color: Color,
    textSizePx: Float, bold: Boolean = false) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setColor(color.toArgb())
        this.textSize = textSizePx
        textAlign = Paint.Align.CENTER
        if (bold) isFakeBoldText = true
    }
    canvas.drawText(text, x, y, paint)
}

private fun DrawScope.drawDimLineH(x1: Float, x2: Float, y: Float, label: String,
    color: Color = DIM_COLOR, above: Boolean = true) {
    val arrowY = if (above) y - 12f else y + 12f
    val lineY = y
    // 两端短竖线
    drawLine(color, Offset(x1, lineY - 6f), Offset(x1, lineY + 6f), strokeWidth = 2f)
    drawLine(color, Offset(x2, lineY - 6f), Offset(x2, lineY + 6f), strokeWidth = 2f)
    // 主横线
    drawLine(color, Offset(x1, lineY), Offset(x2, lineY), strokeWidth = 1.5f)
    // 箭头
    drawArrowHead(Offset(x1, lineY), Offset(x2, lineY), color, 8f)
    drawArrowHead(Offset(x2, lineY), Offset(x1, lineY), color, 8f)
    // 标签
    drawText(label, (x1 + x2) / 2f, arrowY, color, 13f * density, true)
}

private fun DrawScope.drawDimLineV(y1: Float, y2: Float, x: Float, label: String,
    color: Color = DIM_COLOR, left: Boolean = true) {
    val arrowX = if (left) x - 12f else x + 12f
    val lineX = x
    drawLine(color, Offset(lineX - 6f, y1), Offset(lineX + 6f, y1), strokeWidth = 2f)
    drawLine(color, Offset(lineX - 6f, y2), Offset(lineX + 6f, y2), strokeWidth = 2f)
    drawLine(color, Offset(lineX, y1), Offset(lineX, y2), strokeWidth = 1.5f)
    drawArrowHead(Offset(lineX, y1), Offset(lineX, y2), color, 8f)
    drawArrowHead(Offset(lineX, y2), Offset(lineX, y1), color, 8f)
    drawText(label, arrowX, (y1 + y2) / 2f + 5f * density, color, 12f * density, true)
}

private fun DrawScope.drawArrowHead(from: Offset, to: Offset, color: Color, size: Float) {
    val dx = to.x - from.x; val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy); if (len < 1f) return
    val ux = dx / len; val uy = dy / len
    val px = -uy; val py = ux
    val tip = to
    val l = Offset(to.x - ux * size + px * size * 0.4f, to.y - uy * size + py * size * 0.4f)
    val r = Offset(to.x - ux * size - px * size * 0.4f, to.y - uy * size - py * size * 0.4f)
    val path = Path().apply { moveTo(tip.x, tip.y); lineTo(l.x, l.y); lineTo(r.x, r.y); close() }
    drawPath(path, color)
}

private fun DrawScope.drawTrayRect(x: Float, y: Float, w: Float, h: Float) {
    drawRect(TRAY_FILL, Offset(x, y), androidx.compose.ui.geometry.Size(w, h))
    drawRect(TRAY_STROKE, Offset(x, y), androidx.compose.ui.geometry.Size(w, h),
        style = Stroke(width = 2.5f))
    // 中间横线（表示桥架底板）
    drawLine(TRAY_STROKE.copy(alpha = 0.4f), Offset(x, y + h * 0.5f),
        Offset(x + w, y + h * 0.5f), strokeWidth = 1f)
}

private fun DrawScope.drawCutTriangle(x: Float, topY: Float, depth: Float,
    width: Float, pointingDown: Boolean = true) {
    val path = Path()
    if (pointingDown) {
        // 底边在顶部，顶点向下
        path.moveTo(x - width / 2f, topY)
        path.lineTo(x + width / 2f, topY)
        path.lineTo(x, topY + depth)
    } else {
        // 底边在底部，顶点向上
        path.moveTo(x - width / 2f, topY + depth)
        path.lineTo(x + width / 2f, topY + depth)
        path.lineTo(x, topY)
    }
    path.close()
    drawPath(path, CUT_FILL)
    drawPath(path, CUT_RED, style = Stroke(width = 2f))
}

/** 绘制3D线框桥架 */
private fun DrawScope.drawWireframeTray(cx: Float, cy: Float, scale: Float,
    length: Float, width: Float, height: Float, rampAngle: Float) {
    val aRad = rampAngle * PI.toFloat() / 180f
    val rampH = length * sin(aRad) * scale
    val rampL = length * cos(aRad) * scale
    val w = width * scale
    val h = height * scale

    // 简化：画一个带坡度的桥架线框
    val baseY = cy + h
    // 底面四个点
    val p1 = Offset(cx - rampL / 2f, baseY)
    val p2 = Offset(cx + rampL / 2f, baseY)
    val p3 = Offset(cx + rampL / 2f - rampH * 0.3f, baseY - rampH)
    val p4 = Offset(cx - rampL / 2f - rampH * 0.3f, baseY - rampH)

    // 底面
    val bottom = Path().apply {
        moveTo(p1.x, p1.y); lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
    }
    drawPath(bottom, WIRE_COLOR, style = Stroke(width = 1.2f))

    // 顶面（偏移h）
    val top = Path().apply {
        val t1 = Offset(p1.x, p1.y - h); val t2 = Offset(p2.x, p2.y - h)
        val t3 = Offset(p3.x, p3.y - h); val t4 = Offset(p4.x, p4.y - h)
        moveTo(t1.x, t1.y); lineTo(t2.x, t2.y)
        lineTo(t3.x, t3.y); lineTo(t4.x, t4.y); close()
    }
    drawPath(top, WIRE_COLOR, style = Stroke(width = 1.2f))

    // 连接竖线
    drawLine(WIRE_COLOR, p1, Offset(p1.x, p1.y - h), strokeWidth = 1f)
    drawLine(WIRE_COLOR, p2, Offset(p2.x, p2.y - h), strokeWidth = 1f)
    drawLine(WIRE_COLOR, p3, Offset(p3.x, p3.y - h), strokeWidth = 1f)
    drawLine(WIRE_COLOR, p4, Offset(p4.x, p4.y - h), strokeWidth = 1f)
}

// ================================================================
// 爬坡弯头 — 划线展开图
// ================================================================
private fun DrawScope.drawRampCutting(params: CalcParams, results: List<CalcResult>) {
    val W = params.width.toFloat()
    val H = params.height.toFloat()
    val b = params.distance.toFloat()
    val angleDeg = params.angle.toFloat()
    val aRad = angleDeg * PI.toFloat() / 180f

    val baseX = b * tan(aRad / 2f)
    val hypL = b / sin(aRad)
    val riseH = b * tan(aRad)
    // 切口深度 = W * tan(α/2)
    val cutDepth = W * tan(aRad / 2f)

    val cw = size.width
    val ch = size.height
    val margin = 50f
    val trayH = 40f  // 侧面视图高度（固定像素，表示桥架边高）
    val topY = ch * 0.35f  // 桥架顶部Y
    val bottomY = topY + trayH

    // 总长度 = 水平距离 b (mm)
    val totalLen = b
    // 起坡位置 = baseX (mm) — 从左侧到切口的距离
    val rampStart = baseX
    // 切口位置在 rampStart 处
    val cutX = rampStart

    val availW = cw - margin * 2f
    val sc = availW / totalLen.coerceAtLeast(1f)

    val trayLeft = margin
    val trayRight = margin + totalLen * sc
    val trayW = trayRight - trayLeft

    // 切口像素位置
    val cutPx = trayLeft + cutX * sc
    // 切口深度像素
    val cutDepthPx = cutDepth * sc

    // 切口三角宽度（用桥架边高H作为三角底边宽）
    val triBaseW = (H * sc * 0.6f).coerceAtLeast(20f)

    // ── 绘制桥架侧面（长矩形）──
    drawTrayRect(trayLeft, topY, trayW, trayH)

    // ── 绘制切口三角（在切口位置，红色三角）──
    // 第一个切口：在 rampStart 处，三角顶点向下
    drawCutTriangle(cutPx, topY, cutDepthPx, triBaseW, pointingDown = true)

    // 第二个切口：在 rampStart + 某距离处（对称切口）
    // 对于简单爬坡，两个切口间距 = b - 2*baseX 的水平投影
    // 简化：第二个切口在距左端 (b - baseX) 处
    val cut2X = totalLen - baseX
    val cut2Px = trayLeft + cut2X * sc
    drawCutTriangle(cut2Px, topY, cutDepthPx, triBaseW, pointingDown = true)

    // ── 尺寸标注（上方：起坡距离 + 中段距离）──
    val dimY = topY - 30f
    // 起坡距离标注
    drawDimLineH(trayLeft, cutPx, dimY, "%.2fcm".format(rampStart / 10f), above = true)
    // 中段距离（两切口之间）
    val midLen = cut2X - cutX
    drawDimLineH(cutPx, cut2Px, dimY, "%.2fcm".format(midLen / 10f), above = true)

    // ── 尺寸标注（下方：切口深度）──
    val dimY2 = bottomY + 30f
    drawDimLineH(cutPx - triBaseW / 2f, cutPx + triBaseW / 2f, dimY2,
        "%.2fcm".format(cutDepth / 10f), above = false)
    drawDimLineH(cut2Px - triBaseW / 2f, cut2Px + triBaseW / 2f, dimY2,
        "%.2fcm".format(cutDepth / 2f / 10f), above = false)

    // ── 文字标签 ──
    drawText("起坡: %.2f cm".format(rampStart / 10f), cw / 2f, 20f * density,
        DIM_COLOR, 13f * density, true)
    drawText("下料x: %.2f cm".format(cutDepth / 10f), cw / 2f, 38f * density,
        LABEL_GREEN, 12f * density, true)
    drawText("下料一半: %.2f cm".format(cutDepth / 2f / 10f), cw / 2f, 54f * density,
        LABEL_GREEN, 11f * density)

    // ── 3D 线框（下方）──
    val wfY = ch * 0.72f
    val wfScale = (cw * 0.4f) / totalLen.coerceAtLeast(1f)
    drawWireframeTray(cw / 2f, wfY, wfScale, totalLen, W, H, angleDeg)
}

// ================================================================
// 水平弯头 — 划线展开图
// ================================================================
private fun DrawScope.drawHorizontalCutting(params: CalcParams, results: List<CalcResult>) {
    val W = params.width.toFloat()
    val b = params.distance.toFloat()
    val angleDeg = params.angle.toFloat()
    val aRad = angleDeg * PI.toFloat() / 180f

    val baseX = b * tan(aRad / 2f)
    val hypL = b / sin(aRad)
    val cutDepth = W * tan(aRad / 2f)

    val cw = size.width
    val ch = size.height
    val margin = 50f
    val trayH = 40f
    val topY = ch * 0.35f
    val bottomY = topY + trayH

    val totalLen = b
    val cutX = baseX
    val availW = cw - margin * 2f
    val sc = availW / totalLen.coerceAtLeast(1f)

    val trayLeft = margin
    val trayRight = margin + totalLen * sc
    val trayW = trayRight - trayLeft
    val cutPx = trayLeft + cutX * sc
    val cutDepthPx = cutDepth * sc
    val triBaseW = (params.height.toFloat() * sc * 0.6f).coerceAtLeast(20f)

    drawTrayRect(trayLeft, topY, trayW, trayH)
    drawCutTriangle(cutPx, topY, cutDepthPx, triBaseW, pointingDown = true)

    val cut2X = totalLen - baseX
    val cut2Px = trayLeft + cut2X * sc
    drawCutTriangle(cut2Px, topY, cutDepthPx, triBaseW, pointingDown = true)

    val dimY = topY - 30f
    drawDimLineH(trayLeft, cutPx, dimY, "%.2fcm".format(cutX / 10f), above = true)
    val midLen = cut2X - cutX
    drawDimLineH(cutPx, cut2Px, dimY, "%.2fcm".format(midLen / 10f), above = true)

    val dimY2 = bottomY + 30f
    drawDimLineH(cutPx - triBaseW / 2f, cutPx + triBaseW / 2f, dimY2,
        "%.2fcm".format(cutDepth / 10f), above = false)

    drawText("下料x: %.2f cm".format(cutDepth / 10f), cw / 2f, 20f * density,
        DIM_COLOR, 13f * density, true)

    val wfY = ch * 0.72f
    val wfScale = (cw * 0.4f) / totalLen.coerceAtLeast(1f)
    drawWireframeTray(cw / 2f, wfY, wfScale, totalLen, W, params.height.toFloat(), angleDeg)
}

// ================================================================
// 三通 — 划线展开图
// ================================================================
private fun DrawScope.drawTeeCutting(params: CalcParams, results: List<CalcResult>) {
    val W = params.width.toFloat()
    val H = params.height.toFloat()

    val cw = size.width
    val ch = size.height
    val margin = 40f
    val trayH = 40f
    val topY = ch * 0.3f
    val bottomY = topY + trayH

    val totalLen = W * 2f
    val availW = cw - margin * 2f
    val sc = availW / totalLen.coerceAtLeast(1f)

    val trayLeft = margin
    val trayRight = margin + totalLen * sc
    val trayW = trayRight - trayLeft
    val centerX = (trayLeft + trayRight) / 2f

    drawTrayRect(trayLeft, topY, trayW, trayH)

    // 支路切口（中间位置）
    val branchW = W * 0.5f
    val cutDepthPx = H * 0.5f * sc
    val triBaseW = (branchW * sc * 0.5f).coerceAtLeast(15f)
    drawCutTriangle(centerX, topY, cutDepthPx, triBaseW, pointingDown = true)

    // 标注
    drawDimLineH(trayLeft, centerX, topY - 25f, "主管 %.1fcm".format(W / 10f), above = true)
    drawDimLineH(centerX, trayRight, topY - 25f, "支管 %.1fcm".format(branchW / 10f), above = true)
    drawDimLineH(centerX - triBaseW / 2f, centerX + triBaseW / 2f, bottomY + 25f,
        "%.2fcm".format(cutDepthPx / sc / 10f), above = false)

    drawText("三通展开图", cw / 2f, 20f * density, DIM_COLOR, 13f * density, true)
}

// ================================================================
// 变径 — 划线展开图
// ================================================================
private fun DrawScope.drawReducingCutting(params: CalcParams, results: List<CalcResult>) {
    val w1 = params.beforeWidth.toFloat()
    val w2 = params.afterWidth.toFloat()
    val H = params.height.toFloat()
    val deltaW = abs(w2 - w1)
    val hypL = sqrt(deltaW * deltaW + H * H)

    val cw = size.width
    val ch = size.height
    val margin = 40f
    val trayH = 40f
    val topY = ch * 0.35f
    val bottomY = topY + trayH

    val totalLen = maxOf(w1, w2)
    val availW = cw - margin * 2f
    val sc = availW / totalLen.coerceAtLeast(1f)

    val trayLeft = margin
    val trayRight = margin + totalLen * sc
    val trayW = trayRight - trayLeft

    // 梯形
    val w1px = w1 * sc
    val w2px = w2 * sc
    val centerX = (trayLeft + trayRight) / 2f

    val path = Path()
    if (w1 >= w2) {
        path.moveTo(centerX - w1px / 2f, topY)
        path.lineTo(centerX + w1px / 2f, topY)
        path.lineTo(centerX + w2px / 2f, bottomY)
        path.lineTo(centerX - w2px / 2f, bottomY)
    } else {
        path.moveTo(centerX - w2px / 2f, topY)
        path.lineTo(centerX + w2px / 2f, topY)
        path.lineTo(centerX + w1px / 2f, bottomY)
        path.lineTo(centerX - w1px / 2f, bottomY)
    }
    path.close()
    drawPath(path, TRAY_FILL)
    drawPath(path, TRAY_STROKE, style = Stroke(width = 2.5f))

    drawDimLineH(centerX - w1px / 2f, centerX + w1px / 2f, topY - 25f,
        "宽 %.1fcm".format(w1 / 10f), above = true)
    drawDimLineH(centerX - w2px / 2f, centerX + w2px / 2f, bottomY + 25f,
        "宽 %.1fcm".format(w2 / 10f), above = false)

    drawText("变径展开图", cw / 2f, 20f * density, DIM_COLOR, 13f * density, true)
}

// ================================================================
// 组合翻弯 — 划线展开图
// ================================================================
private fun DrawScope.drawCompositeCutting(params: CalcParams, results: List<CalcResult>) {
    val b1 = params.distance1.toFloat()
    val a1Rad = params.angle1.toFloat() * PI.toFloat() / 180f
    val b2 = params.distance2.toFloat()
    val a2Rad = params.angle2.toFloat() * PI.toFloat() / 180f

    val W = params.width.toFloat()
    val cutDepth1 = W * tan(a1Rad / 2f)
    val cutDepth2 = W * tan(a2Rad / 2f)

    val cw = size.width
    val ch = size.height
    val margin = 50f
    val trayH = 40f
    val topY = ch * 0.35f
    val bottomY = topY + trayH

    val totalLen = b1 + b2
    val availW = cw - margin * 2f
    val sc = availW / totalLen.coerceAtLeast(1f)

    val trayLeft = margin
    val trayRight = margin + totalLen * sc
    val trayW = trayRight - trayLeft

    drawTrayRect(trayLeft, topY, trayW, trayH)

    // 第一个切口
    val cut1X = b1 * tan(a1Rad / 2f)
    val cut1Px = trayLeft + cut1X * sc
    val cut1DepthPx = cutDepth1 * sc
    val triBaseW = (params.height.toFloat() * sc * 0.6f).coerceAtLeast(20f)
    drawCutTriangle(cut1Px, topY, cut1DepthPx, triBaseW, pointingDown = true)

    // 第二个切口
    val cut2Pos = b1 + b2 - b2 * tan(a2Rad / 2f)
    val cut2Px = trayLeft + cut2Pos * sc
    val cut2DepthPx = cutDepth2 * sc
    drawCutTriangle(cut2Px, topY, cut2DepthPx, triBaseW, pointingDown = true)

    // 标注
    val dimY = topY - 30f
    drawDimLineH(trayLeft, cut1Px, dimY, "%.2fcm".format(cut1X / 10f), above = true)
    drawDimLineH(cut1Px, cut2Px, dimY - 20f, "%.2fcm".format((cut2Pos - cut1X) / 10f), above = true)

    val dimY2 = bottomY + 30f
    drawDimLineH(cut1Px - triBaseW / 2f, cut1Px + triBaseW / 2f, dimY2,
        "%.2fcm".format(cutDepth1 / 10f), above = false)
    drawDimLineH(cut2Px - triBaseW / 2f, cut2Px + triBaseW / 2f, dimY2 + 20f,
        "%.2fcm".format(cutDepth2 / 10f), above = false)

    drawText("组合翻弯展开图", cw / 2f, 20f * density, DIM_COLOR, 13f * density, true)
}

// ================================================================
// 折角 — 划线展开图
// ================================================================
private fun DrawScope.drawFoldedCutting(params: CalcParams, results: List<CalcResult>) {
    val W = params.width.toFloat()
    val b = params.distance.toFloat()
    val angleDeg = params.angle.toFloat()
    val aRad = angleDeg * PI.toFloat() / 180f

    val baseX = b * tan(aRad / 2f)
    val hypL = b / sin(aRad)
    val riseH = b * tan(aRad)
    val cutDepth = W * tan(aRad / 2f)

    val cw = size.width
    val ch = size.height
    val margin = 50f
    val trayH = 40f
    val topY = ch * 0.35f
    val bottomY = topY + trayH

    val totalLen = b
    val availW = cw - margin * 2f
    val sc = availW / totalLen.coerceAtLeast(1f)

    val trayLeft = margin
    val trayRight = margin + totalLen * sc
    val trayW = trayRight - trayLeft

    drawTrayRect(trayLeft, topY, trayW, trayH)

    val cutPx = trayLeft + baseX * sc
    val cutDepthPx = cutDepth * sc
    val triBaseW = (params.height.toFloat() * sc * 0.6f).coerceAtLeast(20f)
    drawCutTriangle(cutPx, topY, cutDepthPx, triBaseW, pointingDown = true)

    val cut2Pos = totalLen - baseX
    val cut2Px = trayLeft + cut2Pos * sc
    drawCutTriangle(cut2Px, topY, cutDepthPx, triBaseW, pointingDown = true)

    val dimY = topY - 30f
    drawDimLineH(trayLeft, cutPx, dimY, "%.2fcm".format(baseX / 10f), above = true)
    drawDimLineH(cutPx, cut2Px, dimY, "%.2fcm".format((cut2Pos - baseX) / 10f), above = true)

    val dimY2 = bottomY + 30f
    drawDimLineH(cutPx - triBaseW / 2f, cutPx + triBaseW / 2f, dimY2,
        "%.2fcm".format(cutDepth / 10f), above = false)

    drawText("折角展开图", cw / 2f, 20f * density, DIM_COLOR, 13f * density, true)
}
