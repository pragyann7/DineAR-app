package com.ps.dinear

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.ar.core.*
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.configure
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.node.ReticleNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.Dispatchers
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
    val menuItem: MenuItem,
    val model: Model,
    val modelInstance: ModelInstance,
    var rotationY: Float = 0f,
    var scale: Float = 1.0f
)

class ARViewModel : ViewModel() {
    val httpClient: OkHttpClient by lazy { OkHttpClient() }

    private val _loadedModels = mutableStateMapOf<String, Model>()
    val loadedModels: Map<String, Model> = _loadedModels

    private val _downloadStates = mutableStateMapOf<String, DownloadState>()
    val downloadStates: Map<String, DownloadState> = _downloadStates

    fun setModelReady(url: String, model: Model) {
        _loadedModels[url] = model
        _downloadStates[url] = DownloadState.Ready
    }

    fun setDownloading(url: String) {
        _downloadStates[url] = DownloadState.Downloading
    }

    fun setError(url: String, msg: String) {
        _downloadStates[url] = DownloadState.Error(msg)
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    object Ready : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ARActivity : ComponentActivity() {
    private val viewModel: ARViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val menuList = intent.getSerializableExtra("menuList") as? ArrayList<MenuItem> ?: arrayListOf()
        val initialItem = MenuItem(
            0,
            intent.getStringExtra("foodName") ?: "Dish",
            0,
            "",
            intent.getStringExtra("modelUrl") ?: "",
            intent.getStringExtra("modelName") ?: "",
            intent.getStringExtra("modelVersion") ?: "v1"
        )

        setContent {
            ARScreen(
                initialMenuItem = initialItem,
                menuList = menuList,
                activity = this,
                viewModel = viewModel
            )
        }
    }
}

private class ArTransformState {
    var selectedNodeId by mutableStateOf<String?>(null)
    var rotationDegrees by mutableStateOf(0f)
    var currentScale by mutableStateOf(1.0f)
    var selectedModelNode: ModelNode? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ARScreen(
    initialMenuItem: MenuItem,
    menuList: List<MenuItem>,
    activity: ARActivity,
    viewModel: ARViewModel
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    var session by remember { mutableStateOf<Session?>(null) }

    var isMarkerMode by remember { mutableStateOf(false) }
    var markerAnchor by remember { mutableStateOf<Anchor?>(null) }
    var markerDatabase by remember { mutableStateOf<AugmentedImageDatabase?>(null) }

    val placedDishes = remember { mutableStateListOf<PlacedDish>() }
    val transform = remember { ArTransformState() }
    var selectedDishId by remember { mutableStateOf<String?>(null) }

    var currentMenuItem by remember { mutableStateOf(initialMenuItem) }
    var isMenuOpen by remember { mutableStateOf(false) }

    fun downloadModel(item: MenuItem) {
        val url = item.modelUrl
        if (viewModel.loadedModels.containsKey(url) || viewModel.downloadStates[url] is DownloadState.Downloading) return

        val modelFile = File(context.getExternalFilesDir("models"), item.modelName)

        if (modelFile.exists()) {
            activity.lifecycleScope.launch {
                val model = withContext(Dispatchers.IO) {
                    modelLoader.loadModel("file://${modelFile.absolutePath}")
                }
                if (model != null) {
                    viewModel.setModelReady(url, model)
                } else {
                    viewModel.setError(url, "Failed to load model from disk")
                }
            }
            return
        }

        viewModel.setDownloading(url)
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = viewModel.httpClient.newCall(request).execute()
                if (!response.isSuccessful || response.body == null) throw Exception("Server error ${response.code}")

                FileOutputStream(modelFile).use { output ->
                    response.body!!.byteStream().use { input -> input.copyTo(output) }
                }

                val model = withContext(Dispatchers.Main) {
                    modelLoader.loadModel("file://${modelFile.absolutePath}")
                }

                withContext(Dispatchers.Main) {
                    if (model != null) {
                        viewModel.setModelReady(url, model)
                    } else {
                        viewModel.setError(url, "Failed to load downloaded model")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    viewModel.setError(url, "Download failed: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        downloadModel(currentMenuItem)
    }

    // Initialize Marker Database (unchanged from before — already off the main thread)
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

    LaunchedEffect(isMarkerMode, session, markerDatabase) {
        session?.let { s ->
            s.configure { config ->
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                config.focusMode = Config.FocusMode.AUTO
                if (isMarkerMode) {
                    markerDatabase?.let { config.augmentedImageDatabase = it }
                    config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                } else {
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    config.setAugmentedImageDatabase(null)
                }
            }
        }
    }

    fun selectNode(node: ModelNode?, dishId: String?) {
        transform.selectedModelNode = node
        transform.selectedNodeId = node?.name
        transform.rotationDegrees = node?.rotation?.y ?: 0f
        transform.currentScale = (node?.scale?.x ?: MODEL_SCALE_INITIAL) / MODEL_SCALE_INITIAL
        selectedDishId = dishId
    }

    fun clearSelection() = selectNode(null, null)

    fun clearAllDishes() {
        placedDishes.forEach { it.anchor.detach() }
        placedDishes.clear()
        clearSelection()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ArSceneContent(
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            session = session,
            onSessionCreated = { session = it },
            isMarkerMode = isMarkerMode,
            isMenuOpen = isMenuOpen,
            markerAnchor = markerAnchor,
            onMarkerAnchorFound = { markerAnchor = it },
            placedDishes = placedDishes,
            transform = transform,
            selectedDishId = selectedDishId,
            currentMenuItem = currentMenuItem,
            loadedModel = viewModel.loadedModels[currentMenuItem.modelUrl],
            onNodeTapped = { node, dishId -> selectNode(node, dishId) },
            onEmptyTap = { hit ->
                if (isMenuOpen) {
                    isMenuOpen = false
                } else if (transform.selectedNodeId != null) {
                    clearSelection()
                } else if (!isMarkerMode) {
                    val model = viewModel.loadedModels[currentMenuItem.modelUrl]
                    if (model != null) {
                        if (placedDishes.size < MAX_PLACED_OBJECTS) {
                            val instance = modelLoader.assetLoader.createInstance(model)
                            if (instance != null) {
                                placedDishes.add(
                                    PlacedDish(
                                        anchor = hit.createAnchor(),
                                        menuItem = currentMenuItem,
                                        model = model,
                                        modelInstance = instance
                                    )
                                )
                            }
                        } else {
                            Toast.makeText(context, "Max objects reached", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Model not ready yet", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onClearSelection = {
                if (isMenuOpen) {
                    isMenuOpen = false
                } else {
                    clearSelection()
                }
            }
        )

        // Guide Message Overlay
        if (!isMenuOpen && transform.selectedNodeId == null) {
            val guideText = if (isMarkerMode) {
                if (markerAnchor == null) "Scan the AR marker to view dish" else ""
            } else {
                if (placedDishes.isEmpty()) "Move device slowly and tap to place dish" else ""
            }

            if (guideText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = guideText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Top UI: Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                modifier = Modifier.wrapContentSize(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PLANE Mode Segment
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isMarkerMode) DINEAR_ORANGE else Color.Transparent)
                            .clickable {
                                if (isMarkerMode) {
                                    isMarkerMode = false
                                    clearAllDishes()
                                    markerAnchor?.detach()
                                    markerAnchor = null
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_3d),
                            contentDescription = null,
                            tint = if (!isMarkerMode) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Plane",
                            color = if (!isMarkerMode) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // MARKER Mode Segment
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isMarkerMode) DINEAR_ORANGE else Color.Transparent)
                            .clickable {
                                if (!isMarkerMode) {
                                    isMarkerMode = true
                                    clearAllDishes()
                                    markerAnchor = null
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_qr),
                            contentDescription = null,
                            tint = if (isMarkerMode) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Marker",
                            color = if (isMarkerMode) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom UI: Menu FAB and Transform Panel
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
            if (transform.selectedNodeId == null) {
                ExtendedFloatingActionButton(
                    onClick = { isMenuOpen = true },
                    containerColor = DINEAR_ORANGE,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Menu, null) },
                    text = { Text("Food Menu", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(28.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            }

            if (transform.selectedNodeId != null) {
                TransformPanel(
                    rotation = transform.rotationDegrees,
                    scale = transform.currentScale,
                    onRotationChange = { newRotation ->
                        transform.rotationDegrees = newRotation
                        transform.selectedModelNode?.rotation = Rotation(0f, newRotation, 0f)
                        placedDishes.find { it.id == selectedDishId }?.rotationY = newRotation
                    },
                    onScaleChange = { newScale ->
                        transform.currentScale = newScale
                        transform.selectedModelNode?.scale = Scale(MODEL_SCALE_INITIAL * newScale)
                        placedDishes.find { it.id == selectedDishId }?.scale = newScale
                    },
                    onClose = { clearSelection() },
                    onDelete = {
                        placedDishes.find { it.id == selectedDishId }?.let { dish ->
                            dish.anchor.detach()
                            placedDishes.remove(dish)
                        }
                        clearSelection()
                    }
                )
            }
        }

        if (isMenuOpen) {
            MenuOverlay(
                menuList = menuList,
                downloadStates = viewModel.downloadStates,
                onClose = { isMenuOpen = false },
                onItemSelected = { item ->
                    currentMenuItem = item
                    downloadModel(item)
                    isMenuOpen = false
                }
            )
        }
    }
}

@Composable
private fun ArSceneContent(
    engine: com.google.android.filament.Engine,
    modelLoader: io.github.sceneview.loaders.ModelLoader,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    session: Session?,
    onSessionCreated: (Session) -> Unit,
    isMarkerMode: Boolean,
    isMenuOpen: Boolean,
    markerAnchor: Anchor?,
    onMarkerAnchorFound: (Anchor) -> Unit,
    placedDishes: List<PlacedDish>,
    transform: ArTransformState,
    selectedDishId: String?,
    currentMenuItem: MenuItem,
    loadedModel: Model?,
    onNodeTapped: (ModelNode, String?) -> Unit,
    onEmptyTap: (HitResult) -> Unit,
    onClearSelection: () -> Unit
) {
    var lastHitResult by remember { mutableStateOf<HitResult?>(null) }

    val reticleMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF4CAF50).copy(alpha = 0.6f))
    }
    val selectionMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Cyan.copy(alpha = 0.4f))
    }

    val stableOnSessionCreated = rememberUpdatedState(onSessionCreated)
    val onSessionCreatedStable = remember { { s: Session -> stableOnSessionCreated.value(s) } }

    val stableOnMarkerAnchorFound = rememberUpdatedState(onMarkerAnchorFound)
    val onSessionUpdatedStable = remember(isMarkerMode) {
        { _: Any?, frame: Frame ->
            if (isMarkerMode) {
                val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                updatedImages.find { it.name == "universal_marker" && it.trackingState == TrackingState.TRACKING }
                    ?.let { stableOnMarkerAnchorFound.value(it.createAnchor(it.centerPose)) }
            }
        }
    }

    val stableOnEmptyTap = rememberUpdatedState(onEmptyTap)
    val stableOnNodeTapped = rememberUpdatedState(onNodeTapped)
    val stableOnClearSelection = rememberUpdatedState(onClearSelection)

    val gestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { _, tappedNode ->
            if (tappedNode is ModelNode) {
                val dishId = placedDishes.find { it.id == tappedNode.name }?.id
                stableOnNodeTapped.value(tappedNode, dishId)
            } else if (!isMarkerMode) {
                lastHitResult?.let { stableOnEmptyTap.value(it) }
                stableOnClearSelection.value()
            } else {
                stableOnClearSelection.value()
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
            planeRenderer = false,
            mainLightNode = rememberMainLightNode(engine) { intensity = 2000f },
            fillLightNode = rememberFillLightNode(engine) { intensity = 800f },
            onSessionCreated = onSessionCreatedStable,
            onSessionUpdated = onSessionUpdatedStable,
            onGestureListener = gestureListener
        ) {
            if (!isMarkerMode) {
                ReticleNode(
                    xPx = centerXPx,
                    yPx = centerYPx,
                    point = true,
                    planePoseInPolygon = false,
                    onHitResultChanged = { if (!isMenuOpen) lastHitResult = it }
                ) {
                    CylinderNode(radius = 0.05f, height = 0.01f, materialInstance = reticleMaterial)
                }

                placedDishes.forEach { dish ->
                    key(dish.id) {
                        AnchorNode(anchor = dish.anchor) {
                            ModelNode(
                                modelInstance = dish.modelInstance,
                                scaleToUnits = null,
                                centerOrigin = Position(0f, 0f, 0f),
                                apply = {
                                    name = dish.id
                                    scale = Scale(MODEL_SCALE_INITIAL * dish.scale)
                                    rotation = Rotation(0f, dish.rotationY, 0f)
                                    // Compare by id, not full structural equality.
                                    if (transform.selectedModelNode == null && selectedDishId == dish.id) {
                                        transform.selectedModelNode = this
                                    }
                                }
                            )
                            if (selectedDishId == dish.id) {
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
            } else {
                markerAnchor?.let { anchor ->
                    loadedModel?.let { model ->
                        val instance = remember(model, anchor) { modelLoader.assetLoader.createInstance(model) }
                        key(model) {
                            AnchorNode(anchor = anchor) {
                                instance?.let { inst ->
                                    ModelNode(
                                        modelInstance = inst,
                                        scaleToUnits = null,
                                        centerOrigin = Position(0f, 0f, 0f),
                                        apply = {
                                            name = "marker_dish"
                                            scale = Scale(MODEL_SCALE_INITIAL * transform.currentScale)
                                            rotation = Rotation(0f, transform.rotationDegrees, 0f)
                                            if (transform.selectedModelNode == null) {
                                                transform.selectedModelNode = this
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuOverlay(
    menuList: List<MenuItem>,
    downloadStates: Map<String, DownloadState>,
    onClose: () -> Unit,
    onItemSelected: (MenuItem) -> Unit
) {
    var activeTab by remember { mutableStateOf("MENU") } // MENU, ORDERS, CART

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
                    }

                    Text(
                        text = when (activeTab) {
                            "ORDERS" -> "My Orders"
                            "CART" -> "My Cart"
                            else -> "Menu"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Box {
                        IconButton(onClick = { activeTab = "CART" }) {
                            Icon(Icons.Outlined.ShoppingCart, null, tint = Color.Black)
                        }
                        Surface(
                            modifier = Modifier.size(18.dp).align(Alignment.TopEnd),
                            color = Color(0xFF6B6B00),
                            shape = CircleShape
                        ) {
                            Text(
                                "4",
                                color = Color.White,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        "MENU" -> MenuTabContent(menuList, downloadStates, onItemSelected)
                        "ORDERS" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Order History", color = Color.Gray)
                        }
                        "CART" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cart Items", color = Color.Gray)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { activeTab = "MENU" }
                        ) {
                            Icon(Icons.Default.Restaurant, null, tint = if (activeTab == "MENU") DINEAR_ORANGE else Color.Gray)
                            Text("MENU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (activeTab == "MENU") DINEAR_ORANGE else Color.Gray)
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(64.dp).background(Color(0xFF6B6B00), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { activeTab = "ORDERS" }
                        ) {
                            Icon(Icons.Default.ReceiptLong, null, tint = if (activeTab == "ORDERS") DINEAR_ORANGE else Color.Gray)
                            Text("ORDERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (activeTab == "ORDERS") DINEAR_ORANGE else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTabContent(
    menuList: List<MenuItem>,
    downloadStates: Map<String, DownloadState>,
    onItemSelected: (MenuItem) -> Unit
) {
    Column {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            placeholder = { Text("Search culinary delights...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        Spacer(Modifier.height(16.dp))

        val categories = listOf("Popular", "Italian", "Burgers", "Desserts", "Drinks")
        var selectedCategory by remember { mutableStateOf("Popular") }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isCatSelected = category == selectedCategory
                Surface(
                    modifier = Modifier.clickable { selectedCategory = category },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCatSelected) Color(0xFF6B6B00) else Color.White,
                    border = if (!isCatSelected) CardDefaults.outlinedCardBorder() else null
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isCatSelected) Color.White else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(menuList, key = { it.name }) { item ->
                VerticalMenuItemRow(
                    item = item,
                    downloadState = downloadStates[item.modelUrl] ?: DownloadState.Idle,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
fun VerticalMenuItemRow(
    item: MenuItem,
    downloadState: DownloadState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(80.dp)) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )

                if (downloadState is DownloadState.Downloading) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(
                    text = item.getDescription() ?: "Freshly prepared dish with premium ingredients.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(text = "₹${item.price}.00", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB8860B))
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp).background(Color(0xFFD4AF37), CircleShape)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun TransformPanel(
    rotation: Float,
    scale: Float,
    onRotationChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transform Object", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.background(Color(0xFFFFEBEE), CircleShape)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onClose, modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Rotation: ${rotation.toInt()}°", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Slider(
                value = rotation,
                onValueChange = onRotationChange,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = DINEAR_ORANGE,
                    activeTrackColor = DINEAR_ORANGE,
                    inactiveTrackColor = DINEAR_ORANGE.copy(alpha = 0.2f)
                )
            )

            Spacer(Modifier.height(8.dp))

            Text("Scale: ${"%.1f".format(scale)}x", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = 0.1f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = DINEAR_ORANGE,
                    activeTrackColor = DINEAR_ORANGE,
                    inactiveTrackColor = DINEAR_ORANGE.copy(alpha = 0.2f)
                )
            )
        }
    }
}