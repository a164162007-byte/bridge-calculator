package com.bridge.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bridge.calculator.core.elbow.CalcParams
import com.bridge.calculator.core.elbow.ModelType
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberNodes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 真 3D 弯头场景 — 视觉风格严格对齐"陈工桥架计算器"
 *
 * 陈工风格关键特征：
 *  - 明亮天蓝色 U 形桥架（底板 + 两侧板，无外折翼缘）
 *  - 侧板圆孔：每侧板沿长度方向均匀排列 3 个圆孔
 *  - 斜接切口面：深灰色薄板表示切割/拼接面
 *  - 混凝土墙 + 浅灰地面作为场景参照
 *  - 6 种核心弯头用程序化几何拼装
 *
 * 单位约定：1mm = 0.001f（Filament 世界单位，米）
 */
@Composable
fun BridgeElbowScene(
    params: CalcParams,
    modelType: ModelType,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val centerNode = rememberNode(engine) { name = "scene_center" }
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0.6f, y = 0.5f, z = 0.8f)
        lookAt(centerNode)
    }

    val childNodes = rememberNodes {
        add(centerNode)
        addAll(buildSceneNodes(engine, materialLoader, params, modelType))
    }

    Scene(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFECEFF1)),
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        environmentLoader = environmentLoader,
        cameraNode = cameraNode,
        cameraManipulator = rememberCameraManipulator(
            orbitHomePosition = cameraNode.worldPosition,
            targetPosition = centerNode.worldPosition
        ),
        childNodes = childNodes,
        environment = rememberEnvironment(environmentLoader)
    )
}

/**
 * 构造 3D 场景节点集合：环境 + 6 种弯头之一
 */
private fun buildSceneNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams,
    modelType: ModelType
): List<Node> {
    val scene = mutableListOf<Node>()
    scene.addAll(buildEnvironment(engine, materialLoader))
    scene.addAll(
        when (modelType) {
            ModelType.RAMP -> buildRampNodes(engine, materialLoader, params)
            ModelType.HORIZONTAL -> buildHorizontalNodes(engine, materialLoader, params)
            ModelType.TEE -> buildTeeNodes(engine, materialLoader, params)
            ModelType.REDUCING -> buildReducingNodes(engine, materialLoader, params)
            ModelType.COMPOSITE -> buildCompositeNodes(engine, materialLoader, params)
            ModelType.FOLDED -> buildFoldedNodes(engine, materialLoader, params)
            ModelType.CUSTOM -> buildRampNodes(engine, materialLoader, params)
        }
    )
    return scene
}

// ─── 桥架横截面常量（米） ──────────────────────────────────
private const val WALL_T = 0.0025f      // 桥架钢板厚度 2.5mm

// ─── 颜色（对齐陈工桥架图风格） ───────────────────────────
private val BRIDGE_BLUE = Color(0.70f, 0.85f, 0.95f)       // 明亮天蓝色（陈工主色）
private val BRIDGE_DARK = Color(0.40f, 0.50f, 0.60f)       // 暗面 / 远端段
private val CUT_COLOR = Color(0.28f, 0.30f, 0.33f)         // 斜接切口 / 拼接面深灰
private val HOLE_COLOR = Color(0.10f, 0.10f, 0.12f)        // 圆孔（深灰黑）
private val HIGHLIGHT_RED = Color(0.90f, 0.15f, 0.15f)     // 折弯高亮红
private val HIGHLIGHT_YELLOW = Color(0.95f, 0.80f, 0.15f)  // 变径过渡黄
private val GROUND_COLOR = Color(0.88f, 0.88f, 0.90f)      // 浅灰地面
private val WALL_COLOR = Color(0.65f, 0.65f, 0.68f)        // 混凝土墙

// ─── 工具函数 ─────────────────────────────────────────────

/**
 * 创建单个 cube 段（钢板 / 板材 / 圆孔模拟块）
 */
private fun makePiece(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    size: Size,
    center: Position,
    rotation: Rotation = Rotation(),
    color: Color = BRIDGE_BLUE,
    name: String = "piece"
): CubeNode {
    val material = materialLoader.createColorInstance(color)
    return CubeNode(
        engine = engine,
        size = size,
        center = center,
        materialInstance = material
    ).apply {
        this.rotation = rotation
        this.name = name
    }
}

/**
 * 创建 U 形（C 形槽）桥架段。
 *
 * 几何构成（3 件拼装，无外折翼缘 — 对齐陈工风格）：
 *  - 底板：宽 W × 厚 t × 长 L，置于底部
 *  - 左 / 右侧板：高 (H-t) × 厚 t × 长 L，立于底板两侧
 *  - 侧板圆孔：每侧板沿长度方向均匀排列 3 个深色小方块模拟
 *
 * @return U 形段的子 Node 列表（含底板、两侧板、圆孔）
 */
private fun makeBridgeUSection(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    width: Float,
    height: Float,
    length: Float,
    color: Color = BRIDGE_BLUE,
    namePrefix: String = "u_seg"
): List<Node> {
    val w = width
    val h = height
    val t = WALL_T
    val l = length
    val nodes = mutableListOf<Node>()

    // 底板（在底部，居中）
    nodes.add(makePiece(
        engine, materialLoader,
        size = Size(w, t, l),
        center = Position(0.0f, -h / 2.0f + t / 2.0f, 0.0f),
        color = color,
        name = "${namePrefix}_base"
    ))

    // 左侧板
    nodes.add(makePiece(
        engine, materialLoader,
        size = Size(t, h - t, l),
        center = Position(-w / 2.0f + t / 2.0f, 0.0f, 0.0f),
        color = color,
        name = "${namePrefix}_leftSide"
    ))

    // 右侧板
    nodes.add(makePiece(
        engine, materialLoader,
        size = Size(t, h - t, l),
        center = Position(w / 2.0f - t / 2.0f, 0.0f, 0.0f),
        color = color,
        name = "${namePrefix}_rightSide"
    ))

    // 侧板圆孔（每侧 3 个，沿 Z 方向均匀分布）
    val holeSize = Size(0.008f, 0.008f, 0.002f)  // 8mm×8mm×2mm 深色小方块
    val holeOffsets = listOf(-l / 4.0f, 0.0f, l / 4.0f)
    for (zOff in holeOffsets) {
        // 左侧板圆孔（贴在左侧板外侧）
        nodes.add(makePiece(
            engine, materialLoader,
            size = holeSize,
            center = Position(-w / 2.0f - 0.001f, 0.0f, zOff),
            color = HOLE_COLOR,
            name = "${namePrefix}_holeL_${zOff}"
        ))
        // 右侧板圆孔（贴在右侧板外侧）
        nodes.add(makePiece(
            engine, materialLoader,
            size = holeSize,
            center = Position(w / 2.0f + 0.001f, 0.0f, zOff),
            color = HOLE_COLOR,
            name = "${namePrefix}_holeR_${zOff}"
        ))
    }

    return nodes
}

/**
 * 把 U 形段装到一个父 Node 上，父 Node 设置 position / rotation。
 * 多段拼装时数学更简单：每段局部坐标里居中 + 长度沿 -Z 方向。
 */
private fun placeSegment(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    width: Float,
    height: Float,
    length: Float,
    position: Position,
    rotation: Rotation = Rotation(),
    color: Color = BRIDGE_BLUE,
    namePrefix: String = "seg"
): Node {
    val parent = Node(engine).apply {
        this.position = position
        this.rotation = rotation
        this.name = namePrefix
    }
    val pieces = makeBridgeUSection(engine, materialLoader, width, height, length, color, namePrefix)
    pieces.forEach { parent.addChildNode(it) }
    return parent
}

/**
 * 在两段之间添加斜接切口面（深灰色薄板）。
 * 位置为两段交界处，旋转角度取两段角度的平均值。
 */
private fun addCutFace(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    width: Float,
    height: Float,
    position: Position,
    rotation: Rotation = Rotation(),
    thickness: Float = 0.003f
): CubeNode {
    return makePiece(
        engine, materialLoader,
        size = Size(width, height, thickness),
        center = position,
        rotation = rotation,
        color = CUT_COLOR,
        name = "cut_face"
    )
}

// ─── 场景参照（地面 + 墙） ─────────────────────────────────

private fun buildEnvironment(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader
): List<Node> {
    // 地面（XZ 平面，朝上）
    val ground = makePiece(
        engine, materialLoader,
        size = Size(4.0f, 0.001f, 4.0f),
        center = Position(0.0f, -0.40f, 0.0f),
        color = GROUND_COLOR,
        name = "ground"
    )

    // 后墙（XY 平面，朝 +Z）
    val backWall = makePiece(
        engine, materialLoader,
        size = Size(4.0f, 3.0f, 0.02f),
        center = Position(0.0f, 0.6f, -1.5f),
        color = WALL_COLOR,
        name = "back_wall"
    )

    return listOf(ground, backWall)
}

// ─── 6 种核心弯头 builder ─────────────────────────────────

/** 1. 爬坡弯头 (RAMP) — 绕 X 轴俯仰，对齐陈工 1.webp */
private fun buildRampNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.60f

    val nodes = mutableListOf<Node>()

    // 入口段（水平）
    val entry = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(0.0f, 0.0f, -segLen / 2.0f),
        namePrefix = "ramp_entry"
    )
    nodes.add(entry)

    // 斜接切口面（两段交界处）
    val cutAlpha = -alphaRad / 2.0f
    nodes.add(addCutFace(
        engine, materialLoader,
        width = w, height = h,
        position = Position(0.0f, 0.0f, 0.0f),
        rotation = Rotation(x = cutAlpha * 180.0f / PI.toFloat())
    ))

    // 出口段（倾斜）
    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitPos = Position(
        0.0f,
        (segLen / 2.0f) * sinA,
        -(segLen / 2.0f) * cosA
    )
    val exitRot = Rotation(x = -alphaRad * 180.0f / PI.toFloat())
    val exit = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = exitPos, rotation = exitRot,
        color = BRIDGE_DARK,
        namePrefix = "ramp_exit"
    )
    nodes.add(exit)

    return nodes
}

/** 2. 水平弯头 (HORIZONTAL) — 绕 Y 轴偏航，对齐陈工 4.webp */
private fun buildHorizontalNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.60f

    val nodes = mutableListOf<Node>()

    // 入口段
    val entry = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(segLen / 2.0f, 0.0f, 0.0f),
        namePrefix = "horiz_entry"
    )
    nodes.add(entry)

    // 斜接切口面
    val cutBeta = -alphaRad / 2.0f
    nodes.add(addCutFace(
        engine, materialLoader,
        width = w, height = h,
        position = Position(0.0f, 0.0f, 0.0f),
        rotation = Rotation(y = cutBeta * 180.0f / PI.toFloat())
    ))

    // 出口段
    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitPos = Position(
        (segLen / 2.0f) * cosA,
        0.0f,
        (segLen / 2.0f) * sinA
    )
    val exitRot = Rotation(y = -alphaRad * 180.0f / PI.toFloat())
    val exit = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = exitPos, rotation = exitRot,
        color = BRIDGE_DARK,
        namePrefix = "horiz_exit"
    )
    nodes.add(exit)

    return nodes
}

/** 3. 三通 (TEE) — 主管 + 支路，支路从侧面伸出，对齐陈工 22.webp */
private fun buildTeeNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val mainLen = 0.8f
    val branchLen = 0.45f
    val branchW = w * 0.55f
    val branchH = h * 0.55f

    val nodes = mutableListOf<Node>()

    // 主管（沿 Z 轴延伸，U 形开口朝 +Y）
    val main = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = mainLen,
        position = Position(0.0f, 0.0f, 0.0f),
        namePrefix = "tee_main"
    )
    nodes.add(main)

    // 支路（从主管右侧面伸出，沿 +X 轴延伸，U 形开口仍朝 +Y）
    // rotation.y = -90° 让段的长度方向从 -Z 转到 +X
    val branchPos = Position(w / 2.0f + branchLen / 2.0f, 0.0f, 0.0f)
    val branchRot = Rotation(y = -90.0f)
    val branch = placeSegment(
        engine, materialLoader,
        width = branchW, height = branchH, length = branchLen,
        position = branchPos, rotation = branchRot,
        color = BRIDGE_DARK,
        namePrefix = "tee_branch"
    )
    nodes.add(branch)

    // 支路与主管连接处的斜接面
    nodes.add(addCutFace(
        engine, materialLoader,
        width = branchW, height = branchH,
        position = Position(w / 2.0f, 0.0f, 0.0f),
        rotation = Rotation(y = -45.0f)
    ))

    return nodes
}

/** 4. 变径 (REDUCING) — 大 U → 小 U，中间黄色过渡，对齐陈工 25.webp / 26.webp */
private fun buildReducingNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w1 = (params.beforeWidth / 1000.0).toFloat()
    val h1 = (params.beforeHeight / 1000.0).toFloat()
    val w2 = (params.afterWidth / 1000.0).toFloat()
    val h2 = (params.afterHeight / 1000.0).toFloat()
    val segLen = 0.30f
    val midLen = 0.20f

    val nodes = mutableListOf<Node>()

    // 入口段（大）
    val entry = placeSegment(
        engine, materialLoader,
        width = w1, height = h1, length = segLen,
        position = Position(0.0f, 0.0f, -segLen / 2.0f - midLen / 2.0f),
        namePrefix = "red_entry"
    )
    nodes.add(entry)

    // 中间过渡段（黄色高亮）
    val midW = (w1 + w2) / 2.0f
    val midH = (h1 + h2) / 2.0f
    val mid = placeSegment(
        engine, materialLoader,
        width = midW, height = midH, length = midLen,
        position = Position(0.0f, 0.0f, 0.0f),
        color = HIGHLIGHT_YELLOW,
        namePrefix = "red_mid"
    )
    nodes.add(mid)

    // 斜接面 1（入口→中间）
    nodes.add(addCutFace(
        engine, materialLoader,
        width = (w1 + midW) / 2.0f, height = (h1 + midH) / 2.0f,
        position = Position(0.0f, 0.0f, -midLen / 2.0f)
    ))

    // 出口段（小）
    val exit = placeSegment(
        engine, materialLoader,
        width = w2, height = h2, length = segLen,
        position = Position(0.0f, 0.0f, midLen / 2.0f + segLen / 2.0f),
        color = BRIDGE_DARK,
        namePrefix = "red_exit"
    )
    nodes.add(exit)

    // 斜接面 2（中间→出口）
    nodes.add(addCutFace(
        engine, materialLoader,
        width = (midW + w2) / 2.0f, height = (midH + h2) / 2.0f,
        position = Position(0.0f, 0.0f, midLen / 2.0f)
    ))

    return nodes
}

/** 5. 组合翻弯 (COMPOSITE) — 多段折线，红色折弯高亮，对齐陈工 11.webp / 67.webp */
private fun buildCompositeNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val a1Rad = (params.angle1 * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val a2Rad = (params.angle2 * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.30f

    val nodes = mutableListOf<Node>()

    // 第 0 段（水平入口）
    val seg0 = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(0.0f, 0.0f, -segLen - segLen / 2.0f),
        namePrefix = "comp_seg0"
    )
    nodes.add(seg0)

    // 斜接面 1
    val cut1Alpha = -a1Rad / 2.0f
    nodes.add(addCutFace(
        engine, materialLoader,
        width = w, height = h,
        position = Position(0.0f, 0.0f, -segLen / 2.0f),
        rotation = Rotation(x = cut1Alpha * 180.0f / PI.toFloat())
    ))

    // 第 1 段（第一次折弯，红色高亮）
    val seg1Pos = Position(0.0f, 0.0f, -segLen / 2.0f)
    val seg1Rot = Rotation(x = -a1Rad * 180.0f / PI.toFloat())
    val seg1 = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = seg1Pos, rotation = seg1Rot,
        color = HIGHLIGHT_RED,
        namePrefix = "comp_seg1"
    )
    nodes.add(seg1)

    // 斜接面 2
    val cut2Alpha = (-a1Rad + a2Rad) / 2.0f
    nodes.add(addCutFace(
        engine, materialLoader,
        width = w, height = h,
        position = Position(0.0f, (segLen / 2.0f) * sin(a1Rad), segLen / 2.0f),
        rotation = Rotation(x = cut2Alpha * 180.0f / PI.toFloat())
    ))

    // 第 2 段（第二次折弯，红色高亮）
    val seg2Pos = Position(
        0.0f,
        (segLen / 2.0f) * sin(a1Rad) + (segLen / 2.0f) * sin(a2Rad - a1Rad),
        (segLen / 2.0f) * cos(a1Rad) + (segLen / 2.0f) * cos(a2Rad - a1Rad)
    )
    val seg2Rot = Rotation(x = -(a1Rad - a2Rad) * 180.0f / PI.toFloat())
    val seg2 = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = seg2Pos, rotation = seg2Rot,
        color = HIGHLIGHT_RED,
        namePrefix = "comp_seg2"
    )
    nodes.add(seg2)

    return nodes
}

/** 6. 折角 (FOLDED) — 较小角度的 2 段折线，对齐陈工 7-10.webp */
private fun buildFoldedNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.45f

    val nodes = mutableListOf<Node>()

    // 入口段
    val entry = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(0.0f, 0.0f, -segLen / 2.0f - segLen / 2.0f),
        namePrefix = "fold_entry"
    )
    nodes.add(entry)

    // 斜接面
    val cutAlpha = -alphaRad / 2.0f
    nodes.add(addCutFace(
        engine, materialLoader,
        width = w, height = h,
        position = Position(0.0f, 0.0f, -segLen / 2.0f),
        rotation = Rotation(x = cutAlpha * 180.0f / PI.toFloat())
    ))

    // 出口段
    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitPos = Position(
        0.0f,
        (segLen / 2.0f) * sinA,
        (segLen / 2.0f) * cosA
    )
    val exitRot = Rotation(x = -alphaRad * 180.0f / PI.toFloat())
    val exit = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = exitPos, rotation = exitRot,
        color = BRIDGE_DARK,
        namePrefix = "fold_exit"
    )
    nodes.add(exit)

    return nodes
}
