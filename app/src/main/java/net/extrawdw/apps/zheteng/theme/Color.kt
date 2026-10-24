package net.extrawdw.apps.zheteng.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class AppColors(
    val paper: Color, val ink: Color, val muted: Color, val coral: Color,
    val paleCoral: Color, val line: Color, val green: Color, val paleGreen: Color,
    val panel: Color, val track: Color
)

internal val LightColors = AppColors(
    paper = Color(0xFFF8F6F0), ink = Color(0xFF252A26), muted = Color(0xFF767870),
    coral = Color(0xFFF3543D), paleCoral = Color(0xFFF9E7DB), line = Color(0xFFE4E2D8),
    green = Color(0xFF446D50), paleGreen = Color(0xFFE6ECDF), panel = Color(0xFFEFEEE6),
    track = Color(0xFFDADDD1)
)
internal val DarkColors = AppColors(
    paper = Color(0xFF151917), ink = Color(0xFFF1F1E8), muted = Color(0xFFA8ADA4),
    coral = Color(0xFFFF8C75), paleCoral = Color(0xFF362923), line = Color(0xFF373E37),
    green = Color(0xFFACD4AB), paleGreen = Color(0xFF27382C), panel = Color(0xFF222923),
    track = Color(0xFF485246)
)
internal val BrandPaper = Color(0xFFF8F6F0)
internal val BrandCoral = Color(0xFFF3543D)
