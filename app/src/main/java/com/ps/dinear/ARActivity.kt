package com.ps.dinear

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.res.painterResource
import com.ps.dinear.data.model.CartItem
import com.ps.dinear.data.model.Order
import com.ps.dinear.data.model.OrderRequest
import com.ps.dinear.data.model.RestaurantMenuResponse
import com.ps.dinear.data.model.SearchResponse
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
    val rotationY: MutableState<Float> = mutableStateOf(0f),
    val scale: MutableState<Float> = mutableStateOf(1f)
)

class ARViewModel : ViewModel() {
    val httpClient: OkHttpClient by lazy { OkHttpClient() }

    private val _loadedModels = mutableStateMapOf<String, Model>()
    val loadedModels: Map<String, Model> = _loadedModels

    private val _downloadStates = mutableStateMapOf<String, DownloadState>()
    val downloadStates: Map<String, DownloadState> = _downloadStates

    private val _menuItems = mutableStateListOf<MenuItem>()
    val menuItems: List<MenuItem> = _menuItems

    private val _orders = mutableStateListOf<Order>()
    val orders: List<Order> = _orders

    private val _favoriteFoods = mutableStateListOf<MenuItem>()
    val favoriteFoods: List<MenuItem> = _favoriteFoods
    
    var cartItemCount by mutableIntStateOf(CartManager.getInstance().itemCount)
        private set
        
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> = _cartItems

    private val cartListener = CartManager.CartListener {
        cartItemCount = CartManager.getInstance().itemCount
        _cartItems.clear()
        _cartItems.addAll(CartManager.getInstance().items)
    }

    init {
        CartManager.getInstance().addListener(cartListener)
        _cartItems.addAll(CartManager.getInstance().items)
    }

    override fun onCleared() {
        super.onCleared()
        CartManager.getInstance().removeListener(cartListener)
    }

    fun fetchMenu(context: android.content.Context, restaurantSlug: String) {
        if (_menuItems.isNotEmpty()) return

        val api = RetrofitClient.getClient(context).create(ApiService::class.java)
        api.getMenu(restaurantSlug).enqueue(object : retrofit2.Callback<RestaurantMenuResponse> {
            override fun onResponse(call: retrofit2.Call<RestaurantMenuResponse>, response: retrofit2.Response<RestaurantMenuResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val flattenedList = mutableListOf<MenuItem>()
                    response.body()?.categories?.forEach { group ->
                        group.menuItems?.forEach { item ->
                            item.category = group.name
                            flattenedList.add(item)
                        }
                    }
                    _menuItems.clear()
                    _menuItems.addAll(flattenedList)
                }
            }
            override fun onFailure(call: retrofit2.Call<RestaurantMenuResponse>, t: Throwable) {
            }
        })
    }

    fun fetchOrders(context: android.content.Context) {
        val token = SharedPrefManager.getAccessToken(context) ?: return
        val api = RetrofitClient.getClient(context).create(ApiService::class.java)
        api.getOrders("Bearer $token").enqueue(object : retrofit2.Callback<List<Order>> {
            override fun onResponse(call: retrofit2.Call<List<Order>>, response: retrofit2.Response<List<Order>>) {
                if (response.isSuccessful && response.body() != null) {
                    _orders.clear()
                    _orders.addAll(response.body()!!)
                }
            }
            override fun onFailure(call: retrofit2.Call<List<Order>>, t: Throwable) {
            }
        })
    }

    fun fetchFavorites(context: android.content.Context) {
        val token = SharedPrefManager.getAccessToken(context) ?: return
        val api = RetrofitClient.getClient(context).create(ApiService::class.java)
        api.getFavoriteDetails("Bearer $token").enqueue(object : retrofit2.Callback<SearchResponse> {
            override fun onResponse(call: retrofit2.Call<SearchResponse>, response: retrofit2.Response<SearchResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    _favoriteFoods.clear()
                    _favoriteFoods.addAll(response.body()!!.foodItems)
                }
            }
            override fun onFailure(call: retrofit2.Call<SearchResponse>, t: Throwable) {
            }
        })
    }

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

        val selectedItem = intent.getSerializableExtra("selectedItem") as? MenuItem
        val restaurantSlug = intent.getStringExtra("restaurantSlug")
        var restaurantId = intent.getIntExtra("restaurantId", -1)

        if (selectedItem == null) {
            Toast.makeText(this, "Error: Food item not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Fallback: Try to get restaurant ID from the selected item if not in intent
        if (restaurantId == -1) {
            restaurantId = selectedItem.restaurantId ?: -1
        }

        restaurantSlug?.let { slug ->
            viewModel.fetchMenu(this, slug)
        }

        setContent {
            ARScreen(
                initialMenuItem = selectedItem,
                menuList = viewModel.menuItems,
                restaurantId = restaurantId,
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

    fun reset() {
        rotationDegrees = 0f
        currentScale = 1.0f
        selectedNodeId = null
        selectedModelNode = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ARScreen(
    initialMenuItem: MenuItem,
    menuList: List<MenuItem>,
    restaurantId: Int,
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
        val rawUrl = item.modelUrl ?: ""
        if (rawUrl.isEmpty()) {
            viewModel.setError("", "No 3D model available")
            return
        }

        val url = RetrofitClient.getFullUrl(context, rawUrl) ?: ""
        if (url.isEmpty() || viewModel.loadedModels.containsKey(url) || viewModel.downloadStates[url] is DownloadState.Downloading) return

        val filename = "${item.id}_${item.modelName ?: "dish.glb"}"
        val modelFile = File(context.getExternalFilesDir("models"), filename)

        if (modelFile.exists()) {
            activity.lifecycleScope.launch {
                val model = withContext(Dispatchers.IO) {
                    modelLoader.loadModel("file://${modelFile.absolutePath}")
                }
                if (model != null) {
                    viewModel.setModelReady(url, model)
                } else {
                    modelFile.delete()
                    viewModel.setError(url, "Corrupted local file deleted. Retrying.")
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
                        viewModel.setError(url, "Load failed after download")
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
        transform.selectedNodeId = dishId ?: node?.name
        selectedDishId = dishId

        if (node != null) {
            if (dishId != "marker_dish") {
                placedDishes.find { it.id == dishId }?.let { dish ->
                    transform.rotationDegrees = dish.rotationY.value
                    transform.currentScale = dish.scale.value
                }
            }
        }
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
            loadedModel = viewModel.loadedModels[RetrofitClient.getFullUrl(context, currentMenuItem.modelUrl)],
            onNodeTapped = { node, dishId -> selectNode(node, dishId) },
            onEmptyTap = { hit ->
                if (isMenuOpen) {
                    isMenuOpen = false
                } else if (transform.selectedNodeId != null) {
                    clearSelection()
                } else if (!isMarkerMode) {
                    val model = viewModel.loadedModels[RetrofitClient.getFullUrl(context, currentMenuItem.modelUrl)]
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear Table Button (Left Side)
            if (placedDishes.isNotEmpty()) {
                Surface(
                    onClick = { clearAllDishes() },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Table",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }

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
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isMarkerMode) DINEAR_ORANGE else Color.Transparent)
                            .clickable {
                                if (isMarkerMode) {
                                    isMarkerMode = false
                                    clearAllDishes()
                                    transform.reset()
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

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isMarkerMode) DINEAR_ORANGE else Color.Transparent)
                            .clickable {
                                if (!isMarkerMode) {
                                    isMarkerMode = true
                                    clearAllDishes()
                                    transform.reset()
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
                        // Keeps zero-latency feedback while a node reference is live (dragging).
                        transform.selectedModelNode?.rotation = Rotation(0f, newRotation, 0f)
                        // Writes to the State-backed value so it stays correct after deselecting too.
                        if (selectedDishId != "marker_dish") {
                            placedDishes.find { it.id == selectedDishId }?.rotationY?.value = newRotation
                        }
                    },
                    onScaleChange = { newScale ->
                        transform.currentScale = newScale
                        transform.selectedModelNode?.scale = Scale(newScale, newScale, newScale)
                        if (selectedDishId != "marker_dish") {
                            placedDishes.find { it.id == selectedDishId }?.scale?.value = newScale
                        }
                    },
                    onClose = { clearSelection() },
                    onReset = {
                        transform.rotationDegrees = 0f
                        transform.currentScale = 1f
                        transform.selectedModelNode?.let {
                            it.rotation = Rotation(0f, 0f, 0f)
                            it.scale = Scale(1f, 1f, 1f)
                        }
                        if (selectedDishId != "marker_dish") {
                            placedDishes.find { it.id == selectedDishId }?.let {
                                it.rotationY.value = 0f
                                it.scale.value = 1f
                            }
                        }
                    },
                    onDelete = if (selectedDishId != "marker_dish") {
                        {
                            placedDishes.find { it.id == selectedDishId }?.let { dish ->
                                dish.anchor.detach()
                                placedDishes.remove(dish)
                            }
                            clearSelection()
                        }
                    } else null
                )
            }
        }

        if (isMenuOpen) {
            MenuOverlay(
                menuList = menuList,
                restaurantId = restaurantId,
                downloadStates = viewModel.downloadStates,
                viewModel = viewModel,
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
    val onSessionUpdatedStable = remember(isMarkerMode, markerAnchor) {
        { s: Session, frame: Frame ->
            if (isMarkerMode) {
                val images = s.getAllTrackables(AugmentedImage::class.java)
                val marker = images.find { it.name == "universal_marker" && it.trackingState == TrackingState.TRACKING }

                if (marker != null) {
                    val currentAnchor = markerAnchor
                    if (currentAnchor == null || currentAnchor.trackingState == TrackingState.STOPPED) {
                        Log.d("DineAR", "Marker found! Creating anchor at: ${marker.centerPose}")
                        stableOnMarkerAnchorFound.value(marker.createAnchor(marker.centerPose))
                    }
                }
            }
        }
    }

    val stableOnEmptyTap = rememberUpdatedState(onEmptyTap)
    val stableOnNodeTapped = rememberUpdatedState(onNodeTapped)
    val stableOnClearSelection = rememberUpdatedState(onClearSelection)

    val gestureListener = rememberOnGestureListener(
        onSingleTapConfirmed = { _, tappedNode ->
            if (tappedNode is ModelNode) {
                if (tappedNode.name == "marker_dish") {
                    stableOnNodeTapped.value(tappedNode, "marker_dish")
                } else {
                    val dishId = placedDishes.find { it.id == tappedNode.name }?.id
                    stableOnNodeTapped.value(tappedNode, dishId)
                }
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
                        var dishNode by remember(dish.id) { mutableStateOf<ModelNode?>(null) }
                        AnchorNode(anchor = dish.anchor) {
                            ModelNode(
                                modelInstance = dish.modelInstance,
                                scaleToUnits = null,
                                rotation = Rotation(0f, dish.rotationY.value, 0f),
                                scale = Scale(MODEL_SCALE_INITIAL * dish.scale.value),
                                apply = {
                                    name = dish.id
                                    dishNode = this
                                    if (transform.selectedModelNode == null && selectedDishId == dish.id) {
                                        transform.selectedModelNode = this
                                    }
                                }
                            )
                            if (selectedDishId == dish.id) {
                                CylinderNode(
                                    radius = 0.18f, // Static radius
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
                        val instance = remember(model) {
                            Log.d("DineAR", "Creating model instance for marker mode")
                            modelLoader.assetLoader.createInstance(model)
                        }
                        key("marker_dish_node") {
                            AnchorNode(anchor = anchor) {
                                instance?.let { inst ->
                                    ModelNode(
                                        modelInstance = inst,
                                        scaleToUnits = null,
                                        rotation = Rotation(0f, transform.rotationDegrees, 0f),
                                        scale = Scale(MODEL_SCALE_INITIAL * transform.currentScale),
                                        apply = {
                                            name = "marker_dish"
                                            if (selectedDishId == "marker_dish") {
                                                transform.selectedModelNode = this
                                            }
                                        }
                                    )
                                    if (selectedDishId == "marker_dish") {
                                        CylinderNode(
                                            radius = 0.18f, // Static radius
                                            height = 0.002f,
                                            position = Position(0f, 0.001f, 0f),
                                            materialInstance = selectionMaterial
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
}

@Composable
private fun MenuOverlay(
    menuList: List<MenuItem>,
    restaurantId: Int,
    downloadStates: Map<String, DownloadState>,
    viewModel: ARViewModel,
    onClose: () -> Unit,
    onItemSelected: (MenuItem) -> Unit
) {
    var activeTab by remember { mutableStateOf("MENU") }
    val context = LocalContext.current
    
    LaunchedEffect(activeTab) {
        if (activeTab == "CART") {
            viewModel.fetchOrders(context)
        } else if (activeTab == "FAVORITES") {
            viewModel.fetchFavorites(context)
        }
    }

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
                            "CART" -> "Cart & Orders"
                            "FAVORITES" -> "My Favorites"
                            else -> "Menu"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Box {
                        IconButton(onClick = { activeTab = "CART" }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cart_premium),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (activeTab == "CART") DINEAR_ORANGE else Color.Black
                            )
                        }
                        
                        val cartCount = viewModel.cartItemCount
                        if (cartCount > 0) {
                            Surface(
                                modifier = Modifier.size(18.dp).align(Alignment.TopEnd),
                                color = Color.Red,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = cartCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        "MENU" -> MenuTabContent(menuList, restaurantId, downloadStates, onItemSelected)
                        "FAVORITES" -> FavoritesTabContent(viewModel.favoriteFoods, downloadStates, onItemSelected)
                        "CART" -> CartOrdersTabContent(viewModel, viewModel.orders, onClose)
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
                            modifier = Modifier.clickable { activeTab = "FAVORITES" }
                        ) {
                            Icon(Icons.Default.Star, null, tint = if (activeTab == "FAVORITES") DINEAR_ORANGE else Color.Gray)
                            Text("FAVORITE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (activeTab == "FAVORITES") DINEAR_ORANGE else Color.Gray)
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
    restaurantId: Int,
    downloadStates: Map<String, DownloadState>,
    onItemSelected: (MenuItem) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(menuList) {
        val cats = mutableListOf("All")
        val uniqueCats = menuList.mapNotNull { it.category }.distinct().sorted()
        cats.addAll(uniqueCats)
        cats
    }

    val filteredList = remember(menuList, searchQuery, selectedCategory) {
        menuList.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                    (item.getDescription() != null && item.getDescription().contains(searchQuery, ignoreCase = true))
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            placeholder = { Text("Search culinary delights...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        Spacer(Modifier.height(16.dp))

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

        if (filteredList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No items match your search", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    VerticalMenuItemRow(
                        item = item,
                        restaurantId = restaurantId,
                        downloadState = downloadStates[RetrofitClient.getFullUrl(context, item.modelUrl)] ?: DownloadState.Idle,
                        onClick = { onItemSelected(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalMenuItemRow(
    item: MenuItem,
    restaurantId: Int,
    downloadState: DownloadState,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onClick() },
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
                    model = RetrofitClient.getFullUrl(context, item.imageUrl),
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
                onClick = {
                    val finalResId = if (restaurantId != -1) restaurantId else (item.restaurantId ?: -1)
                    if (finalResId != -1) {
                        CartManager.getInstance().addItem(item, finalResId)
                        Toast.makeText(context, "Added to cart!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error: Restaurant ID unknown", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(40.dp).background(Color(0xFFD4AF37), CircleShape)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun FavoritesTabContent(
    favoriteList: List<MenuItem>,
    downloadStates: Map<String, DownloadState>,
    onItemSelected: (MenuItem) -> Unit
) {
    val context = LocalContext.current
    if (favoriteList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favorite foods yet", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(favoriteList, key = { it.id }) { item ->
                VerticalMenuItemRow(
                    item = item,
                    restaurantId = item.restaurantId ?: -1,
                    downloadState = downloadStates[RetrofitClient.getFullUrl(context, item.modelUrl)] ?: DownloadState.Idle,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun CartOrdersTabContent(
    viewModel: ARViewModel,
    orders: List<Order>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val cartItems = viewModel.cartItems

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (cartItems.isNotEmpty()) {
            item {
                Text(
                    "Active Cart",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(cartItems) { item ->
                VerticalCartItemRow(item) {
                    CartManager.getInstance().removeItem(item.menuItem.id)
                }
            }
            item {
                Button(
                    onClick = {
                        val intent = android.content.Intent(context, CartActivity::class.java)
                        context.startActivity(intent)
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DINEAR_ORANGE)
                ) {
                    Text("Checkout (Rs. ${CartManager.getInstance().totalPrice.toInt()})", color = Color.White)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
            }
        }

        item {
            Text(
                "Order History",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (orders.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No past orders", color = Color.Gray)
                }
            }
        } else {
            items(orders) { order ->
                OrderRow(order)
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun VerticalCartItemRow(item: CartItem, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.menuItem.name, fontWeight = FontWeight.Bold)
                Text("Qty: ${item.quantity}", fontSize = 12.sp, color = Color.Gray)
            }
            Text("Rs. ${(item.menuItem.price * item.quantity).toInt()}", fontWeight = FontWeight.Bold)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order #${order.id}", fontWeight = FontWeight.Bold)
                Text(order.status, color = DINEAR_ORANGE, fontWeight = FontWeight.Bold)
            }
            Text("Date: ${order.createdAt.split("T")[0]}", fontSize = 12.sp, color = Color.Gray)
            Text("Total: Rs. ${order.totalPrice.toInt()}", fontWeight = FontWeight.Bold)
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
    onReset: () -> Unit = {},
    onDelete: (() -> Unit)? = null
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

                IconButton(onClick = onReset, modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape)) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))

                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.background(Color(0xFFFFEBEE), CircleShape)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
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