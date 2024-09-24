package com.example.ai_example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
	private val _bitmapLiveData = MutableLiveData<Bitmap>()
	val bitmapLiveData: LiveData<Bitmap> get() = _bitmapLiveData

	private val _labelLiveData = MutableLiveData<String>()
	val labelLiveData: LiveData<String> get() = _labelLiveData

	fun detectImage(context: Context, @DrawableRes drawableId: Int) {
		val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
		detectImage(context, bitmap)
	}

	fun detectImage(context: Context, bitmap: Bitmap) {
		val model = TFLiteModel(context)
		val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
		val inputBuffer = model.convertBitmapToByteBuffer(resizedBitmap)
		model.predictImage(inputBuffer) { labelName ->
			_labelLiveData.postValue(labelName)
		}
		_bitmapLiveData.postValue(model.convertByteBufferToBitmap(inputBuffer))
	}

	companion object {
		private const val INPUT_SIZE = 224
	}
}