package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkCinemaColorScheme = darkColorScheme(
    primary = CelluloidAmber,
    onPrimary = ObsidianBg,
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,
    secondary = ConfirmedTeal,
    onSecondary = ObsidianBg,
    secondaryContainer = ConfirmedTealContainer,
    onSecondaryContainer = Color(0xFFB4F8F0),
    tertiary = ReelCrimson,
    onTertiary = Color.White,
    tertiaryContainer = CrimsonContainer,
    onTertiaryContainer = OnCrimsonContainer,
    background = ObsidianBg,
    onBackground = NitrateWhite,
    surface = VelvetSurface,
    onSurface = NitrateWhite,
    surfaceVariant = SlateCard,
    onSurfaceVariant = SilverAsh,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = VelvetSurface,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = ElevatedSlate,
    surfaceContainerHighest = Color(0xFF2B3042),
    outline = HairlineSlate,
    outlineVariant = Color(0xFF232736),
    error = ReelCrimson,
    onError = Color.White
)

private val LightMatineeColorScheme = lightColorScheme(
    primary = MatineeAmber,
    onPrimary = Color.White,
    primaryContainer = MatineeAmberContainer,
    onPrimaryContainer = Color(0xFF3E2600),
    secondary = Color(0xFF0E7C73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F2ED),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFFB92E34),
    onTertiary = Color.White,
    background = MatineeCream,
    onBackground = MatineeInk,
    surface = MatineeSurface,
    onSurface = MatineeInk,
    surfaceVariant = MatineeCard,
    onSurfaceVariant = MatineeMuted,
    outline = Color(0xFFD6D0C4),
    outlineVariant = Color(0xFFE7E2D8)
)

val FrameMemoryShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FrameMemoryTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkCinemaColorScheme else LightMatineeColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FrameMemoryTypography,
        shapes = FrameMemoryShapes,
        content = content
    )
}
