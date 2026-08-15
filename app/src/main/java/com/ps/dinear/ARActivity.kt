package com.ps.dinear

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.*
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.node.ReticleNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.model.Model
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val MAX_PLACED_OBJECTS = 8
private const val MODEL_SCALE_INITIAL = 1.0f
private val DINEAR_ORANGE = Color(0xFFF45905)

data class PlacedDish(
    val id: String = UUID.randomUUID().toString(),
    val anchor: Anchor,
    var rotationY: Float = 0f,
    var scale: Float = 1.0f
)

class ARViewModel : ViewModel() {
    val httpClient: OkHttpClient by lazy { OkHttpClient() }
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun setDownloading() { _downloadState.value = DownloadState.Downloading }
    fun setReady()       { _downloadState.value = DownloadState.Ready }
    fun setError(msg: String) { _downloadState.value = DownloadState.Error(msg) }
}

sealed class DownloadState {
    object Idle        : DownloadState()
    object Downloading : DownloadState()
    object Ready       : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ARActivity : ComponentActivity() {
    private val viewModel: ARViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modelName = intent.getStringExtra("modelName") ?: run { finish(); return }
        val modelUrl  = intent.getStringExtra("modelUrl")  ?: run { finish(); return }
        val modelFile = File(getExternalFilesDir("models"), modelName)

        lifecycleScope.launch {
            viewModel.downloadState.collect { state ->
                when (state) {
                    is DownloadState.Ready       -> launchAR(modelFile)
                    is DownloadState.Error       -> {
                        Toast.makeText(this@ARActivity, state.message, Toast.LENGTH_LONG).show()
                        finish()
                    }
                    else -> Unit
                }
            }
        }

        if (modelFile.exists()) {
            viewModel.setReady()
        } else {
            downloadModel(modelUrl, modelFile)
        }
    }

    private fun downloadModel(modelUrl: String, destinationFile: File) {
        viewModel.setDownloading()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request  = Request.Builder().url(modelUrl).build()
                val response = viewModel.httpClient.newCall(request).execute()
                if (!response.isSuccessful || response.body == null) throw Exception("Server returned ${response.code}")
                response.body!!.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output -> input.copyTo(output) }
                }
                withContext(Dispatchers.Main) { viewModel.setReady() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { viewModel.setError("Download failed: ${e.message}") }
            }
        }
    }

    private fun launchAR(modelFile: File) {
        setContent { ARScreen(modelFile = modelFile, activity = this) }
    }
}

@Composable
private fun ARScreen(modelFile: File, activity: ARActivity) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    var session by remember { mutableStateOf<Session?>(null) }
    var lastHitResult by remember { mutableStateOf<HitResult?>(null) }

    var isMarkerMode by remember { mutableStateOf(false) }
    var markerAnchor by remember { mutableStateOf<Anchor?>(null) }
    var markerDatabase by remember { mutableStateOf<AugmentedImageDatabase?>(null) }

    val placedDishes = remember { mutableStateListOf<PlacedDish>() }
    var selectedNode by remember { mutableStateOf<ModelNode?>(null) }
    var selectedDish by remember { mutableStateOf<PlacedDish?>(null) }

    var rotationDegrees by remember { mutableStateOf(0f) }
    var currentScale by remember { mutableStateOf(1.0f) }

    // Load the model once
    val model by produceState<Model?>(initialValue = null, modelFile) {
        value = withContext(Dispatchers.IO) {
            modelLoader.loadModel("file://${modelFile.absolutePath}")
        }
    }

    LaunchedEffect(model) {
        if (model != null) {
            Toast.makeText(context, "Model loaded! Move camera to find a surface.", Toast.LENGTH_SHORT).show()
        }
    }

    val reticleMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF4CAF50).copy(alpha = 0.6f))
    }
    val selectionMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Cyan.copy(alpha = 0.4f))
    }

    // Initialize Marker Database
    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val dbFile = File(context.getExternalFilesDir(null), "marker_database.imgdb")
            val db = try {
                if (dbFile.exists()) {
                    dbFile.inputStream().use { AugmentedImageDatabase.deserialize(s, it) }
                } else {
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ar_marker)
                    val newDb = AugmentedImageDatabase(s)
                    newDb.addImage("universal_marker", bitmap, 0.15f)
                    dbFile.outputStream().use { newDb.serialize(it) }
                    newDb
                }
            } catch (_: Exception) {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ar_marker)
                val newDb = AugmentedImageDatabase(s)
                newDb.addImage("universal_marker", bitmap, 0.15f)
                newDb
            }
            withContext(Dispatchers.Main) { markerDatabase = db }
        }
    }

    // Configure Session
    LaunchedEffect(isMarkerMode, session, markerDatabase) {
        session?.let { s ->
            if (isMarkerMode && markerDatabase == null) return@LaunchedEffect
            configureSession(s, isMarkerMode, markerDatabase)
        }
    }

    val gestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { _, tappedNode ->
            if (tappedNode is ModelNode) {
                selectedNode = tappedNode
                rotationDegrees = tappedNode.rotation.y
                currentScale = tappedNode.scale.x / MODEL_SCALE_INITIAL
                selectedDish = placedDishes.find { it.id == tappedNode.name }
            } else {
                if (!isMarkerMode) {
                    lastHitResult?.let { hit ->
                        if (placedDishes.size < MAX_PLACED_OBJECTS) {
                            placedDishes.add(PlacedDish(anchor = hit.createAnchor()))
                        } else {
                            Toast.makeText(context, "Max objects reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                selectedNode = null
                selectedDish = null
            }
        }
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val centerXPx = with(LocalDensity.current) { constraints.maxWidth / 2f }
        val centerYPx = with(LocalDensity.current) { constraints.maxHeight / 2f }

        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            planeRenderer = !isMarkerMode,
            onSessionCreated = { session = it },
            onSessionUpdated = { _, frame ->
                if (isMarkerMode) {
                    val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    updatedImages.find { it.name == "universal_marker" && it.trackingState == TrackingState.TRACKING }?.let {
                        if (markerAnchor == null) markerAnchor = it.createAnchor(it.centerPose)
                    }
                }
            },
            onGestureListener = gestureListener
        ) {
            if (!isMarkerMode) {
                ReticleNode(
                    xPx = centerXPx,
                    yPx = centerYPx,
                    point = true,
                    planePoseInPolygon = false,
                    onHitResultChanged = { lastHitResult = it }
                ) {
                    CylinderNode(
                        radius = 0.05f,
                        height = 0.01f,
                        materialInstance = reticleMaterial
                    )
                }

                placedDishes.forEach { dish ->
                    key(dish.id) {
                        val dishInstance = remember(model, dish.id) {
                            model?.let { modelLoader.assetLoader.createInstance(it) }
                        }
                        AnchorNode(anchor = dish.anchor) {
                            dishInstance?.let { instance ->
                                ModelNode(
                                    modelInstance = instance,
                                    scaleToUnits = null,
                                    centerOrigin = Position(0f, 0f, 0f),
                                    apply = {
                                        name = dish.id
                                        scale = Scale(MODEL_SCALE_INITIAL * dish.scale)
                                        rotation = Rotation(0f, dish.rotationY, 0f)
                                        if (selectedNode == null && selectedDish == dish) selectedNode = this
                                    }
                                )
                                if (selectedDish == dish) {
                                    CylinderNode(
                                        radius = 0.15f,
                                        height = 0.002f,
                                        position = Position(0f, 0.001f, 0f),
                                        materialInstance = selectionMaterial
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                markerAnchor?.let { anchor ->
                    val markerInstance = remember(model) {
                        model?.let { modelLoader.assetLoader.createInstance(it) }
                    }
                    AnchorNode(anchor = anchor) {
                        markerInstance?.let { instance ->
                            ModelNode(
                                modelInstance = instance,
                                scaleToUnits = null,
                                centerOrigin = Position(0f, 0f, 0f),
                                apply = {
                                    scale = Scale(MODEL_SCALE_INITIAL * currentScale)
                                    rotation = Rotation(0f, rotationDegrees, 0f)
                                    if (selectedNode == null) selectedNode = this
                                }
                            )
                        }
                    }
                }
            }
        }

        // Mode Toggle
        Button(
            onClick = {
                isMarkerMode = !isMarkerMode
                selectedNode = null
                selectedDish = null
                if (isMarkerMode) markerAnchor = null
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isMarkerMode) "Switch to Plane Mode" else "Switch to Marker Mode")
        }

        // Transform Panel
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TransformPanel(
                rotation = rotationDegrees,
                scale = currentScale,
                onRotationChange = {
                    rotationDegrees = it
                    selectedNode?.rotation = Rotation(0f, it, 0f)
                    selectedDish?.rotationY = it
                },
                onScaleChange = {
                    currentScale = it
                    selectedNode?.scale = Scale(MODEL_SCALE_INITIAL * it)
                    selectedDish?.scale = it
                },
                onDelete = {
                    selectedDish?.let { placedDishes.remove(it) }
                    selectedNode = null
                    selectedDish = null
                }
            )
        }
    }
}

private fun configureSession(session: Session, isMarkerMode: Boolean, markerDatabase: AugmentedImageDatabase?) {
    val config = session.config
    if (isMarkerMode && markerDatabase != null) {
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        config.augmentedImageDatabase = markerDatabase
    } else {
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
        config.setAugmentedImageDatabase(null)
    }
    config.focusMode = Config.FocusMode.AUTO
    session.configure(config)
}

@Composable
fun TransformPanel(
    rotation: Float,
    scale: Float,
    onRotationChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transform Object", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, null, tint = Color.Red)
                }
            }
            Text("Rotation: ${rotation.toInt()}°", fontSize = 12.sp, color = Color.LightGray)
            Slider(
                value = rotation,
                onValueChange = onRotationChange,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(thumbColor = DINEAR_ORANGE, activeTrackColor = DINEAR_ORANGE)
            )
            Text("Scale: ${"%.1f".format(scale)}x", fontSize = 12.sp, color = Color.LightGray)
            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = DINEAR_ORANGE, activeTrackColor = DINEAR_ORANGE)
            )
        }
    }
}