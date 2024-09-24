package com.example.ai_example

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ai_example.objectdetection.ObjectDetectionTheme
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
	private lateinit var cameraExecutor: ExecutorService

	private val mainViewModel: MainViewModel by viewModels()

	private val photoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == RESULT_OK) {
			val data: Intent? = result.data
			val imageUriString = data?.getStringExtra("data")
			val imageUri = Uri.parse(imageUriString)

			val bitmap = try {
				val inputStream = contentResolver.openInputStream(imageUri)
				BitmapFactory.decodeStream(inputStream)
			} catch (e: Exception) {
				e.printStackTrace()
				Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
				null
			} ?: return@registerForActivityResult

			try {
				mainViewModel.detectImage(this, bitmap)
			} catch (e: IOException) {
				e.printStackTrace()
				Toast.makeText(this, "Failed to detect image", Toast.LENGTH_SHORT).show()
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		cameraExecutor = Executors.newSingleThreadExecutor()

		setContent {
			ObjectDetectionTheme {
				Surface(
					modifier = Modifier.fillMaxSize(),
				) {
					ImagesListPreview()

					Column(
						modifier = Modifier.fillMaxSize(),
						horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
						verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
					) {
						ImagePreview()
						LabelPreview()
						DownloadSectionButton()
						TakePhotoButton()
					}
				}
			}
		}
	}

	@Preview
	@Composable
	fun ImagesListPreview() {
		val imgList = listOf(R.drawable.tiger, R.drawable.flamingo, R.drawable.rabbit, R.drawable.hartebeest)

		LazyRow(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 32.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between items
		) {
			items(imgList) { img ->
				Image(
					bitmap = BitmapFactory.decodeResource(resources, img).asImageBitmap(),  // Convert Bitmap to ImageBitmap
					modifier = Modifier
						.size(100.dp)  // Set the size for each image
						.clickable {
							try {
								mainViewModel.detectImage(this@MainActivity, img)
							} catch (e: IOException) {
								e.printStackTrace()
							}
						},
					contentDescription = "Displayed Image",
					contentScale = ContentScale.Crop // You can adjust to ContentScale.Fit if desired
				)
			}
		}
	}

	@Preview
	@Composable
	fun ImagePreview() {
		// Observe the LiveData as state
		val bitmap by mainViewModel.bitmapLiveData.observeAsState()
		Image(
			bitmap = bitmap?.asImageBitmap() ?: return,  // Convert Bitmap to ImageBitmap
			modifier = Modifier.wrapContentSize(),
			contentDescription = "Displayed Image",
			contentScale = ContentScale.Crop // Adjust how the image is scaled in the Image composable
		)
	}

	@Preview
	@Composable
	fun LabelPreview() {
		// Observe the LiveData as state
		val label by mainViewModel.labelLiveData.observeAsState()
		Text(
			modifier = Modifier.wrapContentSize(),
			text = label ?: "No label found",
			color = Color.Red
		)
	}

	@Preview
	@Composable
	fun DownloadSectionButton() {
		// State variable to hold the text input
		var imageUrl by rememberSaveable { mutableStateOf("") }
		var loading by rememberSaveable { mutableStateOf(false) }

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 32.dp),
			horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
		) {
			TextField(
				maxLines = 4,
				value = imageUrl,
				onValueChange = { newText ->
					imageUrl = newText
				},
				label = { Text("Enter URL here") },
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp)
			)

			Button(
				enabled = !loading && imageUrl.isNotEmpty(),
				onClick = {
					downloadImageFromUrl(imageUrl)
				},
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = Color.Blue,    // Button background color
					contentColor = Color.White      // Title (text) color
				)
			) {
				Text(text = "Download Image from URL")
			}
		}
	}

	private fun downloadImageFromUrl(imageUrl: String) {
		ImageDownloader().downloadAndProcessImage(imageUrl, 224) { bitmap ->
			if (bitmap != null) {
				try {
					mainViewModel.detectImage(this@MainActivity, bitmap)
				} catch (e: IOException) {
					e.printStackTrace()
				}
			} else {
				// Handle error case (e.g., show error message)
			}
		}
	}

	@Composable
	private fun TakePhotoButton() {
		Button(
			onClick = {
				photoResultLauncher.launch(Intent(this, CameraActivity::class.java))
			},
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = Color.Blue,    // Button background color
				contentColor = Color.White      // Title (text) color
			)
		) {
			Text(text = "Take photo")
		}
	}
}