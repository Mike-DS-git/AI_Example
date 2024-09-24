package com.example.ai_example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.squareup.picasso.Picasso
import java.io.IOException
import java.util.regex.Pattern

class ImageDownloader {
	// Function to download image, crop center and resize
	fun downloadAndProcessImage(url: String, targetSize: Int, callback: (Bitmap?) -> Unit) {
		Thread {
			try {
				if (isHttpsUrl(url)) {
					// Download the image using Picasso
					val originalBitmap = Picasso.get().load(url).get()

					// Crop the center and resize
					val processedBitmap = cropAndResizeCenter(originalBitmap, targetSize)

					// Pass the processed bitmap back to the main thread
					callback(processedBitmap)
				} else if (isBase64Url(url)) {
					// Decode the Base64 string
					val imageBytes = Base64.decode(url.split(",")[1], Base64.DEFAULT)
					val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

					// Crop the center and resize
					val processedBitmap = cropAndResizeCenter(originalBitmap, targetSize)
					callback(processedBitmap)
				}
			} catch (e: IOException) {
				e.printStackTrace()
				callback(null) // Handle error case
			}
		}.start()
	}

	// Function to crop the center and resize to target size (224x224)
	private fun cropAndResizeCenter(bitmap: Bitmap, targetSize: Int): Bitmap {
		val width = bitmap.width
		val height = bitmap.height

		// Calculate the size and position of the square crop
		val newWidth = Math.min(width, height)
		val cropX = (width - newWidth) / 2
		val cropY = (height - newWidth) / 2

		// Crop the center of the image
		val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, newWidth, newWidth)

		// Resize the cropped bitmap to target size (224x224)
		return Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true)
	}

	private fun isHttpsUrl(url: String): Boolean {
		return url.startsWith("https://", ignoreCase = true)
	}

	private fun isBase64Url(url: String): Boolean {
		val base64Pattern = Pattern.compile("^data:(\\w+)/(\\w+);base64,([a-zA-Z0-9+/=]+)$")
		return base64Pattern.matcher(url).matches()
	}
}