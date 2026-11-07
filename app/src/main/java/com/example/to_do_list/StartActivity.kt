package com.example.to_do_list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.to_do_list.ui.theme.To_do_listTheme

/**
 * Trang khởi đầu phong cách 3D Soft Realism (clay / soft 3D):
 *  - Khối bo tròn mềm, đổ bóng nhiều lớp, ánh sáng từ góc trên-trái
 *  - Tất cả hình minh họa được vẽ bằng Compose nên đổi màu theo theme được
 *  - Màu lấy từ SharedPreferences "app_settings" / "theme_color_key" giống MainActivity
 */
class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            To_do_listTheme {
                StartScreen(
                    onStart = {
                        // Nếu app có màn hình đăng nhập (LoginActivity...), đổi đích đến ở đây
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

// ---------- Tiện ích màu ----------
private fun Color.lighten(f: Float) = lerp(this, Color.White, f)
private fun Color.darken(f: Float) = lerp(this, Color.Black, f)

private fun Modifier.floating(v: Float, amplitudeDp: Float = 10f) =
    this.offset { IntOffset(0, (v * amplitudeDp * density).toInt()) }

// ---------- Màn hình chính ----------
@Composable
fun StartScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val themeColor = remember {
        Color(prefs.getInt("theme_color_key", Color(0xFF87B5E2).toArgb()))
    }
    val accent = remember(themeColor) { lerp(themeColor, Color(0xFFFFB74D), 0.65f) }
    val titleColor = themeColor.darken(0.72f)
    val subtitleColor = themeColor.darken(0.45f).copy(alpha = 0.8f)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val enter by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "enter"
    )

    val transition = rememberInfiniteTransition(label = "float")
    val f1 by transition.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f1"
    )
    val f2 by transition.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f2"
    )
    val f3 by transition.animateFloat(
        1f, -1f,
        infiniteRepeatable(tween(3900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        themeColor.lighten(0.9f),
                        themeColor.lighten(0.72f),
                        themeColor.lighten(0.55f)
                    )
                )
            )
    ) {
        // Vầng sáng nền tạo chiều sâu
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.1f), radius = w * 0.8f
                ),
                radius = w * 0.8f, center = Offset(w * 0.15f, h * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(themeColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.9f, h * 0.95f), radius = w * 0.9f
                ),
                radius = w * 0.9f, center = Offset(w * 0.9f, h * 0.95f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    alpha = enter
                    translationY = (1f - enter) * 60f
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.5f))

            // ===== Hình minh họa 3D =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.Center
            ) {
                // Bóng nền dưới sàn
                Canvas(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .width(230.dp)
                        .height(30.dp)
                ) {
                    drawOval(
                        brush = Brush.radialGradient(
                            listOf(themeColor.darken(0.5f).copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width / 2f
                        )
                    )
                }

                ClayCard(themeColor, Modifier.floating(f1, 6f))

                Sphere3D(
                    color = themeColor.lighten(0.05f),
                    diameter = 84.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 4.dp)
                        .floating(f2, 12f)
                ) {
                    Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }

                Sphere3D(
                    color = accent,
                    diameter = 70.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 40.dp, start = 2.dp)
                        .floating(f3, 14f)
                ) {
                    Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Sphere3D(
                    color = themeColor.lighten(0.4f),
                    diameter = 46.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 36.dp, start = 22.dp)
                        .floating(f3, 9f)
                ) {
                    Icon(Icons.Default.DateRange, null, tint = themeColor.darken(0.25f), modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.weight(0.4f))

            Text(
                text = "Mọi việc gọn gàng,\nmỗi ngày nhẹ nhàng",
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Lên kế hoạch, đặt nhắc hẹn và thêm công việc bằng giọng nói cùng trợ lý AI.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = subtitleColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(0.4f))

            ClayButton(text = "Bắt đầu ngay", themeColor = themeColor, onClick = onStart)

            Spacer(Modifier.height(28.dp))
        }
    }
}

// ---------- Quả cầu 3D ----------
@Composable
fun Sphere3D(
    color: Color,
    diameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = minOf(w, h) / 2f
            val bodyCenter = Offset(w / 2f, h * 0.45f)
            val bodyRadius = r * 0.9f

            // Bóng đổ dưới quả cầu
            drawOval(
                brush = Brush.radialGradient(
                    listOf(color.darken(0.5f).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.96f), radius = r * 0.8f
                ),
                topLeft = Offset(w * 0.12f, h * 0.86f),
                size = Size(w * 0.76f, h * 0.2f)
            )

            // Thân cầu: sáng góc trên-trái, tối góc dưới-phải
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.lighten(0.6f), color, color.darken(0.4f)),
                    center = Offset(w * 0.33f, h * 0.28f),
                    radius = r * 1.55f
                ),
                radius = bodyRadius,
                center = bodyCenter
            )

            // Ánh sáng phản chiếu từ nền (góc dưới-phải)
            val body = Path().apply { addOval(Rect(center = bodyCenter, radius = bodyRadius)) }
            clipPath(body) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(w * 0.7f, h * 0.85f), radius = r * 0.7f
                    ),
                    radius = r * 0.7f,
                    center = Offset(w * 0.7f, h * 0.85f)
                )
            }

            // Điểm sáng bóng (specular)
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(w * 0.32f, h * 0.2f), radius = r * 0.3f
                ),
                topLeft = Offset(w * 0.2f, h * 0.08f),
                size = Size(w * 0.24f, h * 0.14f)
            )
        }
        content()
    }
}

// ---------- Thẻ clay nghiêng 3D ----------
@Composable
fun ClayCard(themeColor: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier = modifier
            .width(220.dp)
            .height(270.dp)
            .graphicsLayer {
                rotationY = -14f
                rotationX = 6f
                rotationZ = -3f
                cameraDistance = 16f * density
            }
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = themeColor.darken(0.4f),
                spotColor = themeColor.darken(0.4f)
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White, themeColor.lighten(0.88f), themeColor.lighten(0.7f)),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 900f)
                ),
                shape
            )
            .border(
                2.dp,
                Brush.linearGradient(
                    listOf(Color.White, Color.White.copy(alpha = 0.2f), themeColor.darken(0.1f).copy(alpha = 0.3f))
                ),
                shape
            )
            .padding(20.dp)
    ) {
        // Thanh tiêu đề
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(themeColor.copy(alpha = 0.85f))
        )
        Spacer(Modifier.height(20.dp))
        TaskRowClay(themeColor, checked = true, lineFraction = 0.85f)
        Spacer(Modifier.height(12.dp))
        TaskRowClay(themeColor, checked = true, lineFraction = 0.6f)
        Spacer(Modifier.height(12.dp))
        TaskRowClay(themeColor, checked = false, lineFraction = 0.75f)
    }
}

@Composable
private fun TaskRowClay(themeColor: Color, checked: Boolean, lineFraction: Float) {
    val rowShape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                6.dp, rowShape,
                ambientColor = themeColor.darken(0.3f),
                spotColor = themeColor.darken(0.3f)
            )
            .background(
                Brush.verticalGradient(listOf(Color.White, themeColor.lighten(0.85f))),
                rowShape
            )
            .border(1.dp, Color.White, rowShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Sphere3D(
            color = if (checked) themeColor else Color(0xFFCBD5E1),
            diameter = 30.dp
        ) {
            if (checked) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(lineFraction)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(themeColor.lighten(0.45f).copy(alpha = if (checked) 0.9f else 0.6f))
        )
    }
}

// ---------- Nút "gối" 3D ----------
@Composable
fun ClayButton(text: String, themeColor: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "pressScale"
    )
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (pressed) 6.dp else 16.dp,
                shape = shape,
                ambientColor = themeColor.darken(0.35f),
                spotColor = themeColor.darken(0.25f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(themeColor.lighten(0.25f), themeColor, themeColor.darken(0.18f))
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Transparent)),
                shape
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Vệt sáng phía trên nút
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .fillMaxWidth(0.78f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent))
                )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}