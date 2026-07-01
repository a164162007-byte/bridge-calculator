package com.bridge.calculator.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
 * 6 种核心弯头用程序化几何（CubeNode）拼装：
 *  - RAMP 爬坡弯头
 *  - HORIZONTAL 水平弯头
 *  - TEE 三通
 *  - REDUCING 变径
 *  - COMPOSITE 组合翻弯
 *  - FOLDED 折角
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

    val centerNode = rememberNode(engine) { name = "center" }
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0.0f, y = 0.4f, z = 1.8f)
        lookAt(centerNode)
    }

    val childNodes = rememberNodes {
        add(centerNode)
        addAll(buildElbowNodes(engine, materialLoader, params, modelType))
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
 * 根据 ModelType 构造 3D 节点集合
 */
private fun buildElbowNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams,
    modelType: ModelType
): List<Node> {
    return when (modelType) {
        ModelType.RAMP -> buildRampNodes(engine, materialLoader, params)
        ModelType.HORIZONTAL -> buildHorizontalNodes(engine, materialLoader, params)
        ModelType.TEE -> buildTeeNodes(engine, materialLoader, params)
        ModelType.REDUCING -> buildReducingNodes(engine, materialLoader, params)
        ModelType.COMPOSITE -> buildCompositeNodes(engine, materialLoader, params)
        ModelType.FOLDED -> buildFoldedNodes(engine, materialLoader, params)
        ModelType.CUSTOM -> buildRampNodes(engine, materialLoader, params)
    }
}

/**
 * 创建桥架管段（CubeNode）
 * @param size 三维尺寸（米）
 * @param center 中心位置（米）
 * @param rotation 欧拉角（度）
 */
private fun makeBridgeSegment(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    size: Size,
    center: Position,
    rotation: Rotation = Rotation()
): CubeNode {
    val material = materialLoader.createColorInstance(Color(0.55f, 0.56f, 0.58f, 1.0f))
    return CubeNode(
        engine = engine,
        size = size,
        center = center,
        materialInstance = material
    ).apply {
        this.rotation = rotation
        name = "bridge_segment"
    }
}

/** 1. 爬坡弯头 (RAMP) - 绕 X 轴俯仰 */
private fun buildRampNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.35f

    val entrySize = Size(w, h, segLen)
    val entryCenter = Position(0.0f, 0.0f, -segLen / 2.0f)

    val exitSize = Size(w, h, segLen)
    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitCenter = Position(
        0.0f,
        (segLen / 2.0f) * sinA,
        -segLen + (segLen / 2.0f) * cosA
    )
    val exitRotation = Rotation(x = -alphaRad * 180.0f / PI.toFloat())

    val entry = makeBridgeSegment(engine, materialLoader, entrySize, entryCenter).apply { name = "ramp_entry" }
    val exit = makeBridgeSegment(engine, materialLoader, exitSize, exitCenter, exitRotation).apply { name = "ramp_exit" }

    return listOf(entry, exit)
}

/** 2. 水平弯头 (HORIZONTAL) - 绕 Y 轴偏航 */
private fun buildHorizontalNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.35f

    val entrySize = Size(w, h, segLen)
    val entryCenter = Position(segLen / 2.0f, 0.0f, 0.0f)

    val exitSize = Size(w, h, segLen)
    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitCenter = Position(
        segLen + (segLen / 2.0f) * cosA,
        0.0f,
        (segLen / 2.0f) * sinA
    )
    val exitRotation = Rotation(y = -alphaRad * 180.0f / PI.toFloat())

    val entry = makeBridgeSegment(engine, materialLoader, entrySize, entryCenter).apply { name = "horiz_entry" }
    val exit = makeBridgeSegment(engine, materialLoader, exitSize, exitCenter, exitRotation).apply { name = "horiz_exit" }

    return listOf(entry, exit)
}

/** 3. 三通 (TEE) - 主路 + 支路 */
private fun buildTeeNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val mainLen = 0.7f
    val branchLen = 0.4f
    val branchW = w * 0.55f
    val branchH = h * 0.55f

    val mainSize = Size(w, h, mainLen)
    val mainCenter = Position(0.0f, 0.0f, 0.0f)
    val main = makeBridgeSegment(engine, materialLoader, mainSize, mainCenter).apply { name = "tee_main" }

    val branchSize = Size(branchW, branchH, branchLen)
    val branchCenter = Position(-w / 2.0f + branchW / 2.0f, h / 2.0f + branchLen / 2.0f, 0.0f)
    val branchRotation = Rotation(x = 90.0f)
    val branch = makeBridgeSegment(engine, materialLoader, branchSize, branchCenter, branchRotation).apply { name = "tee_branch" }

    return listOf(main, branch)
}

/** 4. 变径 (REDUCING) - 梯形（先用 2 段 Cube 简化） */
private fun buildReducingNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w1 = (params.beforeWidth / 1000.0).toFloat()
    val h1 = (params.beforeHeight / 1000.0).toFloat()
    val w2 = (params.afterWidth / 1000.0).toFloat()
    val h2 = (params.afterHeight / 1000.0).toFloat()
    val segLen = 0.3f

    val entrySize = Size(w1, h1, segLen)
    val entryCenter = Position(0.0f, 0.0f, -segLen / 2.0f)
    val entry = makeBridgeSegment(engine, materialLoader, entrySize, entryCenter).apply { name = "red_entry" }

    val exitSize = Size(w2, h2, segLen)
    val exitCenter = Position(0.0f, 0.0f, -segLen - segLen / 2.0f)
    val exit = makeBridgeSegment(engine, materialLoader, exitSize, exitCenter).apply { name = "red_exit" }

    val midSize = Size((w1 + w2) / 2.0f, (h1 + h2) / 2.0f, 0.05f)
    val midCenter = Position(0.0f, 0.0f, -segLen - 0.025f)
    val mid = makeBridgeSegment(engine, materialLoader, midSize, midCenter).apply { name = "red_mid" }

    return listOf(entry, mid, exit)
}

/** 5. 组合翻弯 (COMPOSITE) - 多段折线 */
private fun buildCompositeNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val a1Rad = (params.angle1 * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val a2Rad = (params.angle2 * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.25f

    val seg0Size = Size(w, h, segLen)
    val seg0Center = Position(0.0f, 0.0f, -segLen / 2.0f)
    val seg0 = makeBridgeSegment(engine, materialLoader, seg0Size, seg0Center).apply { name = "comp_seg0" }

    val seg1Size = Size(w, h, segLen)
    val seg1Center = Position(0.0f, 0.0f, -segLen - (segLen / 2.0f) * cos(a1Rad) + 0.001f)
    val seg1Rotation = Rotation(x = -a1Rad * 180.0f / PI.toFloat())
    val seg1 = makeBridgeSegment(engine, materialLoader, seg1Size, seg1Center, seg1Rotation).apply { name = "comp_seg1" }

    val seg2Size = Size(w, h, segLen)
    val totalZ = -segLen * 2.0f
    val seg2Center = Position(0.0f, 0.001f + (segLen / 2.0f) * sin(a2Rad), totalZ)
    val seg2Rotation = Rotation(x = (a1Rad + a2Rad) * 180.0f / PI.toFloat())
    val seg2 = makeBridgeSegment(engine, materialLoader, seg2Size, seg2Center, seg2Rotation).apply { name = "comp_seg2" }

    return listOf(seg0, seg1, seg2)
}

/** 6. 折角 (FOLDED) - 较小角度的 2 段折线 */
private fun buildFoldedNodes(
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    params: CalcParams
): List<Node> {
    val w = (params.width / 1000.0).toFloat()
    val h = (params.height / 1000.0).toFloat()
    val alphaRad = (params.angle * PI / 180.0).toFloat().coerceAtLeast(0.001f)
    val segLen = 0.4f

    val entrySize = Size(w, h, segLen)
    val entryCenter = Position(0.0f, 0.0f, -segLen / 2.0f)
    val entry = makeBridgeSegment(engine, materialLoader, entrySize, entryCenter).apply { name = "fold_entry" }

    val exitSize = Size(w, h, segLen)
    val sinA = sin(alphaRad)
    val cosA = cos(alphaRad)
    val exitCenter = Position(
        0.0f,
        (segLen / 2.0f) * sinA,
        -segLen + (segLen / 2.0f) * cosA
    )
    val exitRotation = Rotation(x = -alphaRad * 180.0f / PI.toFloat())
    val exit = makeBridgeSegment(engine, materialLoader, exitSize, exitCenter, exitRotation).apply { name = "fold_exit" }

    return listOf(entry, exit)
}
