package com.example.ai_example

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModel(private val context: Context) {
	private var tflite: Interpreter? = null

	init {
		tflite = Interpreter(loadModelFile("mobilenet_v2_1.0_224.tflite")) // Change "model.tflite" to your model's file name
	}

	private fun loadModelFile(modelFile: String): MappedByteBuffer {
		val fileDescriptor = context.assets.openFd(modelFile)
		val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
		val fileChannel = inputStream.channel
		val startOffset = fileDescriptor.startOffset
		val declaredLength = fileDescriptor.declaredLength
		return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
	}

	fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
		val inputSize = 224
		val numChannels = 3
		val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * numChannels * 4)  // 4 bytes per float
		inputBuffer.order(ByteOrder.nativeOrder())

		val intValues = IntArray(inputSize * inputSize)

		// Get the pixel values from the Bitmap
		bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

		// Convert pixel values to floats, normalize them (0-1), and store them in the input buffer
		var pixelIndex = 0
		for (i in 0 until inputSize) {
			for (j in 0 until inputSize) {
				val pixelValue = intValues[pixelIndex++]

				// Extract RGB values and normalize them to [0, 1]
				val red = (pixelValue shr 16 and 0xFF) / 255.0f
				val green = (pixelValue shr 8 and 0xFF) / 255.0f
				val blue = (pixelValue and 0xFF) / 255.0f

				// Add the normalized values to the input buffer
				inputBuffer.putFloat(red)
				inputBuffer.putFloat(green)
				inputBuffer.putFloat(blue)
			}
		}

		return inputBuffer
	}

	fun convertByteBufferToBitmap(byteBuffer: ByteBuffer, width: Int = 224, height: Int = 224): Bitmap {
		val intValues = IntArray(width * height)  // Array to hold pixel values

		byteBuffer.rewind()  // Make sure the buffer is at the start

		for (i in 0 until width) {
			for (j in 0 until height) {
				// Extract normalized RGB values from the buffer
				val red = (byteBuffer.float * 255.0f).toInt()  // Denormalize the value
				val green = (byteBuffer.float * 255.0f).toInt()
				val blue = (byteBuffer.float * 255.0f).toInt()

				// Make sure the values are within the valid range [0, 255]
				val r = red.coerceIn(0, 255)
				val g = green.coerceIn(0, 255)
				val b = blue.coerceIn(0, 255)

				// Combine RGB into a single integer pixel value
				intValues[i * width + j] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
			}
		}

		// Create a bitmap from the int array
		val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		bitmap.setPixels(intValues, 0, width, 0, 0, width, height)

		return bitmap
	}

	fun predictImage(inputBuffer: ByteBuffer, labelResult: (String) -> Unit) {
		// Define the input and output sizes
		val outputSize = 1001  // 1001 classes for MobileNetV2
		val outputBuffer = Array(1) { FloatArray(outputSize) }

		// Run the inference
		tflite?.run(inputBuffer, outputBuffer)

		val maxProbability = outputBuffer[0].maxOrNull()
		val maxProbabilityIndex = outputBuffer[0].toList().indexOf(maxProbability)

		val maxProbabilityLabel = MobileNetLabels.labels[maxProbabilityIndex] + "\nProbability: " + maxProbability
		labelResult(maxProbabilityLabel)
	}
}