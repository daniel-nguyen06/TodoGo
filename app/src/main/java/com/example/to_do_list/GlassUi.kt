package com.example.to_do_list

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism 2.0:
 *  - Nền "mesh gradient" có các đốm sáng chuyển động chậm (aurora)
 *  - Kính mờ nhiều lớp: gradient trong suốt + viền sáng chéo + bóng mềm
 *  - Chữ vẫn tối trên nền kính sáng để dễ đọc
 */

// Màu chữ dùng chung
val GlassTextPrimary = Color(0xFF1E293B)
val GlassTextSecondary = Color(0xFF64748B)

/** Nền aurora động, đổi màu theo themeColor của người dùng. */
@Composable
fun GlassBackground(
    themeColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        themeColor.copy(alpha = 0.18f).compositeOver(Color(0xFFF4F7FB)),
                        Color(0xFFEEF2FF),
                        Color(0xFFFDF2F8)
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            fun blob(color: Color, center: Offset, radius: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color, color.copy(alpha = 0f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            blob(themeColor.copy(alpha = 0.55f), Offset(w * (0.15f + 0.25f * t1), h * (0.12f + 0.10f * t2)), w * 0.7f)
            blob(Color(0xFFC084FC).copy(alpha = 0.40f), Offset(w * (0.85f - 0.25f * t2), h * (0.35f + 0.12f * t1)), w * 0.65f)
            blob(Color(0xFF67E8F9).copy(alpha = 0.40f), Offset(w * (0.30f + 0.30f * t2), h * (0.80f - 0.12f * t1)), w * 0.75f)
            blob(Color(0xFFFDA4AF).copy(alpha = 0.30f), Offset(w * (0.90f - 0.20f * t1), h * (0.92f - 0.06f * t2)), w * 0.55f)
        }
        content()
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f
    )
}

/**
 * Modifier tạo hiệu ứng kính:
 * bóng mềm -> cắt bo góc -> lớp kính gradient -> viền sáng chéo (specular edge)
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White,
    alpha: Float = 0.45f,
    borderAlpha: Float = 0.9f,
    elevation: Dp = 10.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x14000000),
        spotColor = Color(0x2A1E293B)
    )
    .clip(shape)
    .background(
        Brush.linearGradient(
            colors = listOf(
                tint.copy(alpha = alpha),
                tint.copy(alpha = alpha * 0.45f)
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 900f)
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = borderAlpha * 0.5f)
            ),
            start = Offset(0f, 0f),
            end = Offset(500f, 800f)
        ),
        shape = shape
    )

/** Kính nhuộm màu theme (dùng cho task quan trọng, nút chính, ô ngày được chọn). */
fun Modifier.glassTinted(
    themeColor: Color,
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.35f
): Modifier = glass(shape = shape, tint = themeColor, alpha = alpha, borderAlpha = 0.7f, elevation = 8.dp)