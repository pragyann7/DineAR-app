package com.ps.dinear;

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
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

private const val MAX_PLACED_OBJECTS = 8

private const val MODEL_SCALE = 0.2f

data class PlacedDish(
    val anchor: Anchor,
    var rotationY: Float = 0f
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

        when {
            modelFile.exists() -> viewModel.setReady()
            else -> {
                Toast.makeText(this, "Downloading model…", Toast.LENGTH_SHORT).show()
                downloadModel(modelUrl, modelFile)
            }
        }
    }

    private fun downloadModel(modelUrl: String, destinationFile: File) {
        viewModel.setDownloading()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request  = Request.Builder().url(modelUrl).build()
                val response = viewModel.httpClient.newCall(request).execute()

                if (!response.isSuccessful || response.body == null) {
                    throw Exception("Server returned ${response.code}")
                }

                // Stream to disk – avoids loading the full GLB into heap.
                response.body!!.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ARActivity, "Model ready", Toast.LENGTH_SHORT).show()
                    viewModel.setReady()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    viewModel.setError("Download failed: ${e.message}")
                }
            }
        }
    }

    private fun launchAR(modelFile: File) {
        setContent {
            ARScreen(modelFile = modelFile, activity = this)
        }
    }
}

@Composable
private fun ARScreen(modelFile: File, activity: ARActivity) {

    val context = LocalContext.current
    val engine        = rememberEngine()
    val modelLoader   = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    var session         by remember { mutableStateOf<Session?>(null) }
    var lastHitResult   by remember { mutableStateOf<HitResult?>(null) }
    var isTrackingPlane by remember { mutableStateOf(false) }

    var isMarkerMode    by remember { mutableStateOf(false) }
    var markerAnchor    by remember { mutableStateOf<Anchor?>(null) }

    // Optimization: Pre-load database from cache or create it once session is ready
    var markerDatabase by remember { mutableStateOf<AugmentedImageDatabase?>(null) }

    val placedDishes = remember { mutableStateListOf<PlacedDish>() }

    var selectedNode    by remember { mutableStateOf<ModelNode?>(null) }
    var selectedDish    by remember { mutableStateOf<PlacedDish?>(null) }
    var rotationDegrees by remember { mutableStateOf(0f) }

    val reticleMaterial   = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF4CAF50).copy(alpha = 0.6f))
    }
    val selectionMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Cyan.copy(alpha = 0.4f))
    }

    // Initialize database once session is ready
    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        if (markerDatabase == null) {
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
                withContext(Dispatchers.Main) {
                    markerDatabase = db
                }
            }
        }
    }

    // Reconfigure session when mode changes (or database is ready)
    LaunchedEffect(isMarkerMode, session, markerDatabase) {
        val s = session
        val db = markerDatabase
        if (s != null) {
            // If switching to marker mode, we need the database to be ready
            if (isMarkerMode && db == null) return@LaunchedEffect
            configureSession(s, isMarkerMode, db)
        }
    }

    val gestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { _, tappedNode ->
            if (isMarkerMode) {
                if (tappedNode is ModelNode) {
                    selectedNode = tappedNode
                    rotationDegrees = tappedNode.rotation.y
                    Toast.makeText(activity, "Marker dish selected", Toast.LENGTH_SHORT).show()
                } else {
                    selectedNode = null
                }
                return@rememberOnGestureListener
            }

            when {
                // Tap on an existing model → select it
                tappedNode is ModelNode -> {
                    selectedNode    = tappedNode
                    rotationDegrees = tappedNode.rotation.y
                    // Find which PlacedDish owns this node so we can mutate its rotationY
                    selectedDish = placedDishes.firstOrNull { dish ->
                        // The node association is set via the apply{} block below
                        dish.anchor.cloudAnchorId == tappedNode.name
                    }
                    Toast.makeText(activity, "Dish selected", Toast.LENGTH_SHORT).show()
                }

                // Tap on empty space while tracking → place a dish
                tappedNode == null && isTrackingPlane -> {
                    if (placedDishes.size >= MAX_PLACED_OBJECTS) {
                        Toast.makeText(
                            activity,
                            "Maximum dishes reached (${MAX_PLACED_OBJECTS})",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        lastHitResult?.let { hit ->
                            val anchor = hit.createAnchor()
                            placedDishes.add(PlacedDish(anchor = anchor))
                            Toast.makeText(activity, "Dish placed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Deselect on empty-space tap
                    selectedNode = null
                    selectedDish = null
                }

                // Tap on empty space, no plane → deselect
                else -> {
                    selectedNode = null
                    selectedDish = null
                }
            }
        }
    )

    // ── Layout ───────────────────────────────────────────────────────────────
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val centerXPx = with(LocalDensity.current) { constraints.maxWidth / 2f }
        val centerYPx = with(LocalDensity.current) { constraints.maxHeight / 2f }

        // ── AR scene ─────────────────────────────────────────────────────────
        ARSceneView(
            modifier       = Modifier.fillMaxSize(),
            engine         = engine,
            modelLoader    = modelLoader,
            materialLoader = materialLoader,
            planeRenderer  = false,
            onSessionCreated = { arSession ->
                session = arSession
            },
            onSessionUpdated = { _, frame ->
                if (isMarkerMode) {
                    val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    val marker = updatedImages.find { it.name == "universal_marker" }
                    if (marker != null) {
                        if (marker.trackingState == TrackingState.TRACKING && markerAnchor == null) {
                            markerAnchor = marker.createAnchor(marker.centerPose)
                        } else if (marker.trackingState == TrackingState.STOPPED) {
                            markerAnchor = null
                        }
                    }
                }
            },
            onGestureListener = gestureListener
        ) {

            if (!isMarkerMode) {
                ReticleNode(
                    xPx = centerXPx,
                    yPx = centerYPx,
                    onHitResultChanged = { hit ->
                        lastHitResult   = hit
                        isTrackingPlane = hit != null &&
                                hit.trackable is Plane &&
                                (hit.trackable as Plane).isPoseInPolygon(hit.hitPose)
                    }
                ) {
                    if (isTrackingPlane) {
                        CylinderNode(
                            radius           = 0.05f,
                            height           = 0.01f,
                            materialInstance = reticleMaterial   // reused, not recreated
                        )
                    }
                }

                placedDishes.forEach { dish ->
                    key(dish.anchor) {
                        AnchorNode(anchor = dish.anchor) {
                            rememberModelInstance(
                                modelLoader  = modelLoader,
                                fileLocation = Uri.fromFile(modelFile).toString()
                            )?.let { modelInstance ->
                                ModelNode(
                                    modelInstance = modelInstance,
                                    scaleToUnits  = MODEL_SCALE,
                                    centerOrigin  = Position(0f, 0f, 0f),
                                    apply         = {
                                        name     = dish.anchor.cloudAnchorId ?: dish.anchor.hashCode().toString()
                                        rotation = Rotation(0f, dish.rotationY, 0f)
                                        if (selectedNode == null && selectedDish == dish) {
                                            selectedNode = this
                                        }
                                    }
                                )
                                if (selectedDish == dish) {
                                    CylinderNode(
                                        radius           = 0.15f,
                                        height           = 0.002f,
                                        position         = Position(0f, 0.001f, 0f),
                                        materialInstance = selectionMaterial  // reused
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Marker Mode Rendering
                markerAnchor?.let { anchor ->
                    AnchorNode(anchor = anchor) {
                        rememberModelInstance(
                            modelLoader = modelLoader,
                            fileLocation = Uri.fromFile(modelFile).toString()
                        )?.let { modelInstance ->
                            ModelNode(
                                modelInstance = modelInstance,
                                scaleToUnits = MODEL_SCALE,
                                centerOrigin = Position(0f, 0f, 0f),
                                apply = {
                                    rotation = Rotation(0f, rotationDegrees, 0f)
                                    if (selectedNode == null) {
                                        selectedNode = this
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Mode Toggle Button
        Button(
            onClick = {
                isMarkerMode = !isMarkerMode
                selectedNode = null
                selectedDish = null
                if (isMarkerMode) markerAnchor = null
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isMarkerMode) "Switch to Plane Mode" else "Switch to Marker Mode")
        }

        AnimatedVisibility(
            visible = selectedNode != null,
            enter   = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit    = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RotationPanel(
                rotationDegrees = rotationDegrees,
                onRotationChange = { value ->
                    rotationDegrees = value
                    selectedNode?.rotation = Rotation(0f, value, 0f)
                    selectedDish?.rotationY = value   // persist into PlacedDish
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
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.setAugmentedImageDatabase(null)
    }
    
    config.depthMode            = Config.DepthMode.AUTOMATIC
    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
    config.lightEstimationMode  = Config.LightEstimationMode.AMBIENT_INTENSITY
    config.focusMode            = Config.FocusMode.AUTO
    config.updateMode           = Config.UpdateMode.LATEST_CAMERA_IMAGE
    
    session.configure(config)
}

@Composable
private fun RotationPanel(
    rotationDegrees: Float,
    onRotationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
            .background(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = "Rotate Dish  ${rotationDegrees.toInt()}°",
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value         = rotationDegrees,
            onValueChange = onRotationChange,
            valueRange    = 0f..360f,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor       = Color(0xFF4CAF50),
                activeTrackColor = Color(0xFF4CAF50)
            )
        )
    }
}