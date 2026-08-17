package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.GoldLight
import com.example.ui.theme.SecondaryGold
import com.example.ui.theme.TertiaryEmerald
import kotlinx.coroutines.launch

@Composable
fun LuckyWheelDialog(
    freeSpinsLeft: Int,
    isAdLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSpinWin: (Int) -> Unit,
    onWatchAdForSpins: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var lastWonAmount by remember { mutableIntStateOf(0) }
    val rotationAnim = remember { Animatable(0f) }

    val wheelSlices = remember {
        listOf(
            Pair(20, Color(0xFF8B5CF6)),
            Pair(50, Color(0xFFEC4899)),
            Pair(100, Color(0xFFF59E0B)),
            Pair(30, Color(0xFF3B82F6)),
            Pair(200, Color(0xFF10B981)),
            Pair(500, Color(0xFFEF4444)),
            Pair(50, Color(0xFF6366F1)),
            Pair(1000, Color(0xFFFFB800))
        )
    }

    fun spinWheel() {
        if (isSpinning || freeSpinsLeft <= 0) return
        isSpinning = true
        lastWonAmount = 0

        val targetIndex = (0 until wheelSlices.size).random()
        val sliceAngle = 360f / wheelSlices.size
        // Land in the middle of target slice (pointing upwards at 270 degrees)
        val targetSliceCenter = targetIndex * sliceAngle + (sliceAngle / 2f)
        
        // Calculate the rotation to land on the target slice, always spinning forward
        val currentRotation = rotationAnim.value
        val extraSpins = 360f * 5 // Spin 5 times for effect
        val currentDegrees = currentRotation % 360f
        val targetDegrees = (360f - targetSliceCenter + 270f) % 360f
        val rotationNeeded = if (targetDegrees > currentDegrees) {
            targetDegrees - currentDegrees
        } else {
            360f - currentDegrees + targetDegrees
        }
        val targetRotation = currentRotation + extraSpins + rotationNeeded

        coroutineScope.launch {
            rotationAnim.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing)
            )
            val won = wheelSlices[targetIndex].first
            lastWonAmount = won
            isSpinning = false
            onSpinWin(won)
        }
    }

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = SecondaryGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fortune Lucky Wheel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSpinning,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wheel Canvas
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                            .rotate(rotationAnim.value)
                    ) {
                        val canvasSize = size.minDimension
                        val radius = canvasSize / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        val sliceAngle = 360f / wheelSlices.size

                        wheelSlices.forEachIndexed { index, slice ->
                            val startAngle = index * sliceAngle
                            drawArc(
                                color = slice.second,
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                size = Size(canvasSize, canvasSize),
                                topLeft = Offset(0f, 0f)
                            )
                        }

                        // Outer border
                        drawCircle(
                            color = Color(0xFFFFB800),
                            radius = radius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
                        )
                    }

                    // Center pin indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1035))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = SecondaryGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Top Pointer needle
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 16.dp, height = 24.dp)
                            .background(Color.White, shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result display
                if (lastWonAmount > 0) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SecondaryGold.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎉 You Won +$lastWonAmount Coins!",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldLight,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Spins Counter Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Free Spins Available: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$freeSpinsLeft",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (freeSpinsLeft > 0) SecondaryGold else Color(0xFFEF4444),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (freeSpinsLeft > 0) {
                    // Main Spin Button
                    Button(
                        onClick = { spinWheel() },
                        enabled = !isSpinning,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("spin_wheel_button")
                    ) {
                        Text(
                            text = if (isSpinning) "Spinning..." else "SPIN THE WHEEL ($freeSpinsLeft Left)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Out of spins state - Prompt rewarded video ad
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Spins Left!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Watch a quick sponsored video ad to immediately earn +3 more spins!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onWatchAdForSpins() },
                                enabled = !isSpinning && !isAdLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("watch_ad_for_3_spins_button")
                            ) {
                                if (isAdLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading Ad...", fontSize = 14.sp, color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "WATCH AD (+3 SPINS)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
