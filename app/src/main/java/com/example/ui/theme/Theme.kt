package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
  primary = JarvisCyan,
  onPrimary = JarvisDarkBackground,
  primaryContainer = JarvisDeepBlue,
  onPrimaryContainer = JarvisCyan,
  secondary = JarvisBlue,
  onSecondary = TextPrimary,
  secondaryContainer = JarvisSurfaceCard,
  onSecondaryContainer = JarvisCyan,
  tertiary = JarvisAccentGold,
  background = JarvisDarkBackground,
  onBackground = TextPrimary,
  surface = JarvisSurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = JarvisSurfaceCard,
  onSurfaceVariant = TextSecondary,
  error = JarvisWarningRed,
  onError = TextPrimary
)

@Composable
fun JarvisTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = JarvisColorScheme,
    typography = Typography,
    content = content
  )
}
