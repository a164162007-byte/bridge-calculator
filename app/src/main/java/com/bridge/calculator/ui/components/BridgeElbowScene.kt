package com.bridge.calculator.ui.components

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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * 真 3D 弯头场景（基于 SceneView 2.2.1 + Google Filament 引擎）
 *
 * 视觉风格对齐"陈工桥架计算器"：
 *  - U 形（C 形槽）桥架横截面：底板 + 两侧板 + 翼缘外折
 *  - 浅蓝色金属漆，带阴影投影
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
        position = Position(x = 1.2f, y = 0.8f, z = 1.6f)
        lookAt(centerNode)
    }

    val childNodes = rememberNodes {
        add(centerNode)
        addAll(buildSceneNodes(engine, materialLoader, params, modelType))
    }

    Scene(
        modifier = modifier.fillMaxSize(),
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
private const val FLANGE_W = 0.018f     // 翼缘外折宽度 18mm

// ─── 颜色（接近陈工桥架图风格） ───────────────────────────
private val BRIDGE_BLUE = Color(0.74f, 0.80f, 0.86f)   // 浅蓝灰金属漆
private val BRIDGE_DARK = Color(0.45f, 0.50f, 0.56f)   // 切口/暗面
private val GROUND_COLOR = Color(0.88f, 0.88f, 0.90f)  // 浅灰地面
private val WALL_COLOR = Color(0.62f, 0.62f, 0.65f)    // 混凝土墙
private val HIGHLIGHT_RED = Color(0.86f, 0.20f, 0.20f) // 切口红色（陈工常用红虚线）
private val HIGHLIGHT_YELLOW = Color(0.95f, 0.78f, 0.20f) // 底板变径高亮

// ─── 工具函数 ─────────────────────────────────────────────

/**
 * 创建单个 cube 段（钢板/板材）
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
 * 几何构成（横截面，沿 Y 方向站立，沿 Z 方向延伸长度 L）：
 *  - 底板：宽 W × 厚 t × 长 L，置于底部
 *  - 左/右翼：高 (H-t) × 厚 t × 长 L，置于两侧
 *  - 左/右翼缘（外折）：宽 FLANGE_W × 厚 t × 长 L，置于顶部外侧（陈工风格关键）
 *
 * @return U 形段的 5 个子 Node
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
    val fl = FLANGE_W

    // 底板（在底部，居中）
    val base = makePiece(
        engine, materialLoader,
        size = Size(w, t, l),
        center = Position(0.0f, -h / 2.0f + t / 2.0f, 0.0f),
        color = color,
        name = "${namePrefix}_base"
    )

    // 左侧板（高度 h - t，从底板顶面到翼缘底面）
    val leftSide = makePiece(
        engine, materialLoader,
        size = Size(t, h - t, l),
        center = Position(-w / 2.0f + t / 2.0f, 0.0f, 0.0f),
        color = color,
        name = "${namePrefix}_leftSide"
    )

    // 右侧板
    val rightSide = makePiece(
        engine, materialLoader,
        size = Size(t, h - t, l),
        center = Position(w / 2.0f - t / 2.0f, 0.0f, 0.0f),
        color = color,
        name = "${namePrefix}_rightSide"
    )

    // 左翼缘（顶部外折）
    val leftFlange = makePiece(
        engine, materialLoader,
        size = Size(fl, t, l),
        center = Position(-w / 2.0f - fl / 2.0f, h / 2.0f - t / 2.0f, 0.0f),
        color = color,
        name = "${namePrefix}_leftFlange"
    )

    // 右翼缘
    val rightFlange = makePiece(
        engine, materialLoader,
        size = Size(fl, t, l),
        center = Position(w / 2.0f + fl / 2.0f, h / 2.0f - t / 2.0f, 0.0f),
        color = color,
        name = "${namePrefix}_rightFlange"
    )

    return listOf(base, leftSide, rightSide, leftFlange, rightFlange)
}

/**
 * 把 U 形段（5 个子 piece）装到一个父 Node 上，父 Node 设置 position/rotation。
 * 这样多段拼装时数学更简单：每段局部坐标里居中 + 长度沿 -Z 方向。
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

/** 1. 爬坡弯头 (RAMP) - 绕 X 轴俯仰，对齐陈工 1.webp */
private fun buildRampNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.40f

    val entry = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(0.0f, 0.0f, -segLen / 2.0f),
        namePrefix = "ramp_entry"
    )

    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitPos = Position(
        0.0f,
        (segLen / 2.0f) * sinA,
        -segLen + (segLen / 2.0f) * cosA
    )
    val exitRot = Rotation(x = -alphaRad * 180.0f / PI.toFloat())
    val exit = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = exitPos, rotation = exitRot,
        color = BRIDGE_DARK,  // 出口段暗一档，呼应陈工的"切割后"效果
        namePrefix = "ramp_exit"
    )

    return listOf(entry, exit)
}

/** 2. 水平弯头 (HORIZONTAL) - 绕 Y 轴偏航，对齐陈工 4.webp */
private fun buildHorizontalNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.40f

    val entry = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(segLen / 2.0f, 0.0f, 0.0f),
        namePrefix = "horiz_entry"
    )

    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitPos = Position(
        segLen + (segLen / 2.0f) * cosA,
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

    return listOf(entry, exit)
}

/** 3. 三通 (TEE) - 主路 + 支路，支路带斜切口，对齐陈工 22.webp / 47.webp */
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

    val main = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = mainLen,
        position = Position(0.0f, 0.0f, 0.0f),
        namePrefix = "tee_main"
    )

    // 支路沿 +Y 方向（顶部开口朝上），从主路顶部延伸
    val branchPos = Position(
        0.0f,
        h / 2.0f + branchLen / 2.0f,
        0.0f
    )
    val branchRot = Rotation(x = 90.0f)  // 局部 Z 转向 -Y（实际想让段沿 +Y 延伸）
    val branch = placeSegment(
        engine, materialLoader,
        width = branchW, height = branchH, length = branchLen,
        position = branchPos, rotation = branchRot,
        color = BRIDGE_DARK,
        namePrefix = "tee_branch"
    )

    return listOf(main, branch)
}

/** 4. 变径 (REDUCING) - 大 U → 小 U，对齐陈工 25.webp / 26.webp（梯形漏斗过渡） */
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

    val entry = placeSegment(
        engine, materialLoader,
        width = w1, height = h1, length = segLen,
        position = Position(0.0f, 0.0f, -segLen / 2.0f - midLen / 2.0f),
        namePrefix = "red_entry"
    )

    // 中间段：底板用黄色高亮（陈工风格），侧板用蓝色
    val midW = (w1 + w2) / 2.0f
    val midH = (h1 + h2) / 2.0f
    val mid = placeSegment(
        engine, materialLoader,
        width = midW, height = midH, length = midLen,
        position = Position(0.0f, 0.0f, 0.0f),
        color = HIGHLIGHT_YELLOW,
        namePrefix = "red_mid"
    )

    val exit = placeSegment(
        engine, materialLoader,
        width = w2, height = h2, length = segLen,
        position = Position(0.0f, 0.0f, segLen / 2.0f + midLen / 2.0f),
        color = BRIDGE_DARK,
        namePrefix = "red_exit"
    )

    return listOf(entry, mid, exit)
}

/** 5. 组合翻弯 (COMPOSITE) - 多段折线，对齐陈工 11.webp (S型) / 67.webp (一体弯) */
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

    val seg0 = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(0.0f, 0.0f, -segLen / 2.0f - segLen),
        namePrefix = "comp_seg0"
    )

    val seg1Pos = Position(0.0f, 0.0f, -segLen / 2.0f)
    val seg1Rot = Rotation(x = -a1Rad * 180.0f / PI.toFloat())
    val seg1 = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = seg1Pos, rotation = seg1Rot,
        color = HIGHLIGHT_RED,  // 折弯段红色（陈工风格切口线）
        namePrefix = "comp_seg1"
    )

    val seg2Pos = Position(0.0f, (segLen / 2.0f) * sin(a2Rad), segLen / 2.0f)
    val seg2Rot = Rotation(x = a2Rad * 180.0f / PI.toFloat())
    val seg2 = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = seg2Pos, rotation = seg2Rot,
        color = HIGHLIGHT_RED,
        namePrefix = "comp_seg2"
    )

    return listOf(seg0, seg1, seg2)
}

/** 6. 折角 (FOLDED) - 较小角度的 2 段折线，对齐陈工 7-10.webp (45°组成90°) */
private fun buildFoldedNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.45f

    val entry = placeSegment(
        engine, materialLoader,
        width = w, height = h, length = segLen,
        position = Position(0.0f, 0.0f, -segLen / 2.0f - segLen / 2.0f),
        namePrefix = "fold_entry"
    )

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

    return listOf(entry, exit)
}
