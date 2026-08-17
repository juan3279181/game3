package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryViolet,
  onPrimary = Color.White,
  primaryContainer = PrimaryVioletDark,
  onPrimaryContainer = Color.White,
  secondary = SecondaryGold,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF4A3800),
  onSecondaryContainer = GoldLight,
  tertiary = TertiaryEmerald,
  background = BackgroundDark,
  onBackground = Color(0xFFF3F0F9),
  surface = SurfaceDark,
  onSurface = Color(0xFFF3F0F9),
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = Color(0xFFD1C7E8)
)

private val LightColorScheme = lightColorScheme(
  primary = PrimaryVioletDark,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFEDE7F6),
  onPrimaryContainer = PrimaryVioletDark,
  secondary = Color(0xFFD97706),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFEF3C7),
  onSecondaryContainer = Color(0xFF92400E),
  tertiary = TertiaryEmerald,
  background = BackgroundLight,
  onBackground = Color(0xFF1E192B),
  surface = SurfaceLight,
  onSurface = Color(0xFF1E192B),
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun FollowMeTheme(
  darkTheme: Boolean = true, // Default to sleek dark mode for high-contrast social experience
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

