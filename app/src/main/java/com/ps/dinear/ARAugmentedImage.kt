package com.ps.dinear;

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ARAugmentedImage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modelName = intent.getStringExtra("modelName") ?: return
        val modelUrl = intent.getStringExtra("modelUrl") ?: return
        val modelFile = File(getExternalFilesDir("models"), modelName)

        // Only download the 3D model if it doesn't exist locally yet
        if (modelFile.exists()) {
            launchAR(modelFile)
        } else {
            Toast.makeText(this, "Downloading 3D Dish...", Toast.LENGTH_SHORT).show()
            downloadModelOnly(modelUrl, modelFile) {
                launchAR(modelFile)
            }
        }
    }

    private fun launchAR(modelFile: File) {
        setContent {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)

            var rotationDegrees by remember { mutableFloatStateOf(0f) }
            var activeSelectedNode by remember { mutableStateOf<ModelNode?>(null) }
            var trackedImage by remember { mutableStateOf<AugmentedImage?>(null) }

            // Pre-load the model instance to avoid lag when the marker is first detected
            val modelInstance = rememberModelInstance(
                modelLoader = modelLoader,
                fileLocation = Uri.fromFile(modelFile).toString()
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

                ARSceneView(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    planeRenderer = false,
                    onSessionCreated = { sessionCreated ->
                        sessionCreated.configure(sessionCreated.config.apply {
                            lightEstimationMode = com.google.ar.core.Config.LightEstimationMode.DISABLED
                            focusMode = com.google.ar.core.Config.FocusMode.AUTO
                            updateMode = com.google.ar.core.Config.UpdateMode.LATEST_CAMERA_IMAGE
                        })

                        val dbFile = File(getExternalFilesDir(null), "marker_database.imgdb")
                        val imageDatabase = try {
                            if (dbFile.exists()) {
                                FileInputStream(dbFile).use { AugmentedImageDatabase.deserialize(sessionCreated, it) }
                            } else {
                                val markerBitmap = BitmapFactory.decodeResource(resources, R.drawable.ar_marker)
                                val db = AugmentedImageDatabase(sessionCreated)
                                db.addImage("universal_marker", markerBitmap, 0.15f)
                                FileOutputStream(dbFile).use { db.serialize(it) }
                                db
                            }
                        } catch (_: Exception) {
                            // Fallback if deserialization fails
                            val markerBitmap = BitmapFactory.decodeResource(resources, R.drawable.ar_marker)
                            val db = AugmentedImageDatabase(sessionCreated)
                            db.addImage("universal_marker", markerBitmap, 0.15f)
                            db
                        }

                        sessionCreated.configure(sessionCreated.config.apply {
                            augmentedImageDatabase = imageDatabase
                        })
                    },
                    onSessionUpdated = { _, frame ->
                        val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                        val marker = updatedImages.find { it.name == "universal_marker" }

                        if (marker != null) {
                            if (marker.trackingState == TrackingState.TRACKING) {
                                trackedImage = marker
                            } else if (marker.trackingState == TrackingState.STOPPED) {
                                trackedImage = null
                                activeSelectedNode = null
                            }
                        }
                    }
                ) {
                    trackedImage?.let { image ->
                        val anchor = remember(image) { image.createAnchor(image.centerPose) }

                        AnchorNode(anchor = anchor) {
                            modelInstance?.let { instance ->
                                ModelNode(
                                    modelInstance = instance,
                                    scaleToUnits = 0.12f,
                                    centerOrigin = Position(0f, 0f, 0f),
                                    rotation = Rotation(0f, rotationDegrees, 0f),
                                    apply = {
                                        activeSelectedNode = this
                                    }
                                )
                            }
                        }
                    }
                }

                // UI Rotation Control Slider Overlay
                if (trackedImage != null && activeSelectedNode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Rotate Dish: ${rotationDegrees.toInt()}°", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = rotationDegrees,
                            onValueChange = { value ->
                                rotationDegrees = value
                                activeSelectedNode?.rotation = Rotation(0f, rotationDegrees, 0f)
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    private fun downloadModelOnly(modelUrl: String, modelFile: File, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                modelFile.parentFile?.mkdirs()

                val request = Request.Builder().url(modelUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful && response.body != null) {
                    FileOutputStream(modelFile).use { response.body!!.byteStream().copyTo(it) }
                    withContext(Dispatchers.Main) { onComplete() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ARAugmentedImage, "Failed to download model: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}