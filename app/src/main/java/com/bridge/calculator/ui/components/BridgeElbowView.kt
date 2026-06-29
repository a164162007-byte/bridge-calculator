package com.bridge.calculator.ui.components

import android.content.Context
import android.graphics.*
import android.view.View
import com.bridge.calculator.core.elbow.CalcParams
import com.bridge.calculator.core.elbow.ModelType
import kotlin.math.cos
import kotlin.math.sin

class BridgeElbowView @JvmOverloads constructor(
    context: Context,
    private var params: CalcParams = CalcParams(),
    private var modelType: ModelType = ModelType.RAMP
) : View(context) {
    
    private var scale = 1.5f
    private var rotationY = -30f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    private val bridgeColor = Color.parseColor("#607D8B")
    private val cutColor = Color.parseColor("#FF7043")
    
    private val bodyPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
        color = Color.parseColor("#37474F")
    }
    private val cutLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        color = cutColor
    }
    private val textPaint = Paint().apply {
        textSize = 28f
        isAntiAlias = true
        color = Color.parseColor("#212121")
    }
    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#E0E0E0")
    }
    
    fun updateModel(newParams: CalcParams, newModelType: ModelType) {
        params = newParams
        modelType = newModelType
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        canvas.save()
        canvas.translate(width / 2f + translateX, height / 2f + translateY)
        canvas.scale(scale, scale)
        when (modelType) {
            ModelType.RAMP -> drawRampElbow(canvas)
            ModelType.HORIZONTAL -> drawHorizontalElbow(canvas)
            ModelType.TEE -> drawTeeElbow(canvas)
            ModelType.REDUCING -> drawReducingElbow(canvas)
            else -> drawRampElbow(canvas)
        }
        canvas.restore()
        canvas.drawText("W=${params.width.toInt()} H=${params.height.toInt()} α=${params.angle.toInt()}°", 20f, 40f, textPaint)
    }
    
    private fun drawRampElbow(canvas: Canvas) {
        val w = params.width.toFloat()
        val h = params.height.toFloat()
        val alpha = params.angle.toFloat()
        val b = params.distance.toFloat()
        val alphaRad = Math.toRadians(alpha.toDouble()).toFloat()
        val x = (b * sin(alphaRad / 2f)).toFloat()
        val rampLen = b.toFloat()
        val heightGain = (b * sin(alphaRad)).toFloat()
        val hw = w / 2f
        val path = Path()
        bodyPaint.color = bridgeColor
        val entryLen = rampLen * 0.3f
        path.moveTo(-hw, 0f); path.lineTo(hw, 0f); path.lineTo(hw, -h); path.lineTo(-hw, -h); path.close()
        canvas.drawPath(path, bodyPaint); canvas.drawPath(path, strokePaint)
        val rampStartX = hw
        val rampEndX = hw + rampLen * 0.4f
        path.reset()
        path.moveTo(-hw, 0f); path.lineTo(hw, 0f); path.lineTo(rampEndX + hw, -heightGain); path.lineTo(rampStartX - hw, -heightGain); path.close()
        canvas.drawPath(path, bodyPaint); canvas.drawPath(path, strokePaint)
        path.reset()
        path.moveTo(-hw, -heightGain); path.lineTo(hw, -heightGain); path.lineTo(hw, -h - heightGain); path.lineTo(-hw, -h - heightGain); path.close()
        canvas.drawPath(path, bodyPaint); canvas.drawPath(path, strokePaint)
        cutLinePaint.color = cutColor
        canvas.drawLine(-hw + x, 0f, -hw + x, -h, cutLinePaint)
        canvas.drawLine(hw - x, 0f, hw - x, -h, cutLinePaint)
    }
    
    private fun drawHorizontalElbow(canvas: Canvas) {
        val w = params.width.toFloat()
        val h = params.height.toFloat()
        val alpha = params.angle.toFloat()
        val b = params.distance.toFloat()
        val alphaRad = Math.toRadians(alpha.toDouble()).toFloat()
        val radius = (b / (2 * sin(alphaRad / 2f))).toFloat()
        val hw = w / 2f
        bodyPaint.color = bridgeColor
        val path = Path()
        val startAngle = -alphaRad / 2f * (180f / Math.PI.toFloat())
        val sweepAngle = alphaRad * (180f / Math.PI.toFloat())
        path.moveTo((radius + hw) * cos(-alphaRad / 2f), (radius + hw) * sin(-alphaRad / 2f))
        path.arcTo(-radius - hw, -radius - hw, radius + hw, radius + hw, startAngle, sweepAngle, false)
        canvas.drawPath(path, bodyPaint); canvas.drawPath(path, strokePaint)
        path.reset()
        path.moveTo((radius - hw) * cos(-alphaRad / 2f), (radius - hw) * sin(-alphaRad / 2f))
        path.arcTo(-radius + hw, -radius + hw, radius - hw, radius - hw, startAngle, sweepAngle, false)
        canvas.drawPath(path, bodyPaint); canvas.drawPath(path, strokePaint)
        canvas.drawLine((radius + hw) * cos(-alphaRad / 2f), (radius + hw) * sin(-alphaRad / 2f), (radius - hw) * cos(-alphaRad / 2f), (radius - hw) * sin(-alphaRad / 2f), strokePaint)
        canvas.drawLine((radius + hw) * cos(alphaRad / 2f), (radius + hw) * sin(alphaRad / 2f), (radius - hw) * cos(alphaRad / 2f), (radius - hw) * sin(alphaRad / 2f), strokePaint)
    }
    
    private fun drawTeeElbow(canvas: Canvas) {
        val w = params.width.toFloat()
        val h = params.height.toFloat()
        val hw = w / 2f
        val mainLen = w * 2f
        val branchLen = w
        bodyPaint.color = bridgeColor
        val mainPath = Path()
        mainPath.moveTo(-mainLen / 2, -h); mainPath.lineTo(mainLen / 2, -h); mainPath.lineTo(mainLen / 2, 0f); mainPath.lineTo(-mainLen / 2, 0f); mainPath.close()
        canvas.drawPath(mainPath, bodyPaint); canvas.drawPath(mainPath, strokePaint)
        val branchPath = Path()
        branchPath.moveTo(-hw, -h); branchPath.lineTo(0f, -h); branchPath.lineTo(0f, -h - branchLen); branchPath.lineTo(-hw, -h - branchLen); branchPath.close()
        canvas.drawPath(branchPath, bodyPaint); canvas.drawPath(branchPath, strokePaint)
    }
    
    private fun drawReducingElbow(canvas: Canvas) {
        val bw = params.beforeWidth.toFloat()
        val bh = params.beforeHeight.toFloat()
        val aw = params.afterWidth.toFloat()
        val ah = params.afterHeight.toFloat()
        val hwB = bw / 2f; val hwA = aw / 2f; val len = bh
        bodyPaint.color = bridgeColor
        val path = Path()
        path.moveTo(-hwB, 0f); path.lineTo(hwB, 0f); path.lineTo(hwA, -len); path.lineTo(-hwA, -len); path.close()
        canvas.drawPath(path, bodyPaint); canvas.drawPath(path, strokePaint)
    }
    
    private fun drawGrid(canvas: Canvas) {
        val gridSize = 40f
        val centerX = width / 2f; val centerY = height / 2f
        gridPaint.color = Color.parseColor("#EEEEEE")
        var x = centerX % gridSize
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint); x += gridSize }
        var y = centerY % gridSize
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, gridPaint); y += gridSize }
    }
    
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> { lastTouchX = event.x; lastTouchY = event.y; return true }
            android.view.MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX; val dy = event.y - lastTouchY
                rotationY += dx * 0.5f; translateX += dx; translateY += dy
                lastTouchX = event.x; lastTouchY = event.y; invalidate(); return true
            }
        }
        return super.onTouchEvent(event)
    }
}
