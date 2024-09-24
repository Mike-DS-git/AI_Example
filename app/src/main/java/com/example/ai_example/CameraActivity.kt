package com.example.ai_example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : ComponentActivity() {
	private lateinit var cameraExecutor: ExecutorService

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			CameraCaptureScreen()
		}

		cameraExecutor = Executors.newSingleThreadExecutor()
	}

	override fun onDestroy() {
		super.onDestroy()
		cameraExecutor.shutdown()
	}

	@Composable
	fun CameraCaptureScreen() {
		val context = LocalContext.current
		var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
		var hasCameraPermission by remember {
			mutableStateOf(
				ContextCompat.checkSelfPermission(
					context,
					Manifest.permission.CAMERA
				) == PackageManager.PERMISSION_GRANTED
			)
		}

		val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

		// Register permission request launcher
		val permissionLauncher = rememberLauncherForActivityResult(
			contract = ActivityResultContracts.RequestPermission(),
			onResult = { granted -> hasCameraPermission = granted }
		)

		// Request camera permission if not already granted
		LaunchedEffect(Unit) {
			if (!hasCameraPermission) {
				permissionLauncher.launch(Manifest.permission.CAMERA)
			}
		}

		if (hasCameraPermission) {
			Box(modifier = Modifier.fillMaxSize()) {
				AndroidView(
					modifier = Modifier.fillMaxSize(),
					factory = { ctx ->
						val previewView = PreviewView(ctx)

						val cameraProvider = cameraProviderFuture.get()
						val preview = Preview.Builder().build().also {
							it.setSurfaceProvider(previewView.surfaceProvider)
						}

						imageCapture = ImageCapture.Builder().build()

						val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

						try {
							// Unbind before rebinding
							cameraProvider.unbindAll()

							// Bind use cases to camera
							cameraProvider.bindToLifecycle(
								ctx as LifecycleOwner,
								cameraSelector,
								preview,
								imageCapture
							)
						} catch (exc: Exception) {
							Log.e("CameraCaptureScreen", "Binding failed", exc)
						}

						previewView
					}
				)

				// Square border box
				Box(
					modifier = Modifier
						.size(300.dp) // Size of the square box
						.align(Alignment.Center)
						.border(2.dp, Color.Red)
				)

				Button(
					onClick = { takePicture(context, imageCapture) },
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.padding(16.dp)
				) {
					Text("Capture Image")
				}
			}
		}
	}

	private fun takePicture(context: android.content.Context, imageCapture: ImageCapture?) {
		val mediaDir = context.getExternalFilesDir(null)?.let {
			File(it, "images").apply { mkdirs() }
		}

		val child = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"

		val file = if (mediaDir != null && mediaDir.exists()) File(mediaDir, child) else File(context.filesDir, child)

		val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

		imageCapture?.takePicture(
			outputOptions,
			ContextCompat.getMainExecutor(context),
			object : ImageCapture.OnImageSavedCallback {
				override fun onError(exception: ImageCaptureException) {
					Log.e("CameraActivity", "Error saving image: ${exception.message}", exception)
					Toast.makeText(this@CameraActivity, "Error capturing image: ${exception.message}", Toast.LENGTH_SHORT).show()
				}

				override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
					val uri = outputFileResults.savedUri.toString()
					Log.d("CameraActivity", "Image saved: $uri")
					Toast.makeText(this@CameraActivity, "Image saved", Toast.LENGTH_SHORT).show()
					setResult(RESULT_OK, Intent().apply {
						putExtra("data", uri)
					})
					finish()
				}
			}
		)
	}
}