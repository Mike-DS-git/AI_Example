package com.example.ai_example.objectdetection


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define your Material3 colors for both light and dark themes
private val DarkColorScheme = darkColorScheme(
	primary = Color(0xFFBB86FC),
	secondary = Color(0xFF03DAC5),
	tertiary = Color(0xFF6200EE)
)

private val LightColorScheme = lightColorScheme(
	primary = Color(0xFF6200EE),
	secondary = Color(0xFF03DAC5),
	tertiary = Color(0xFFBB86FC)
)

@Composable
fun ObjectDetectionTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colorScheme = if (darkTheme) {
		DarkColorScheme
	} else {
		LightColorScheme
	}

	MaterialTheme(
		colorScheme = colorScheme,
		typography = com.example.ai_example.ui.theme.Typography,
		content = content
	)
}