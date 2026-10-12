package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CrossfaderCurve
import com.example.ui.theme.DjSkinColors

@Composable
fun CrossfaderSection(
    crossfaderPosition: Float, // -1.0 to +1.0
    crossfaderCurve: CrossfaderCurve,
    isHamster: Boolean,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onPositionChange: (Float) -> Unit,
    onCurveChange: (CrossfaderCurve) -> Unit,
    onToggleHamster: () -> Unit,
    onAutoTransition: (toRight: Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .testTag("crossfader_section")
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassis),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Top Bar: Curve selector, Hamster toggle, Auto Transition buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Auto transition Left
                OutlinedButton(
                    onClick = { onAutoTransition(false) },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = skinColors.deckAColor)
                ) {
                    Text("◀ AUTO A", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Curve selector pill buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        CrossfaderCurve.SMOOTH to "SMOOTH",
                        CrossfaderCurve.SHARP_CUT to "CUT",
                        CrossfaderCurve.CONSTANT_POWER to "PWR"
                    ).forEach { (curve, label) ->
                        val isSelected = crossfaderCurve == curve
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) skinColors.deckAColor else skinColors.surfaceChassisAlt)
                                .clickable { onCurveChange(curve) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else skinColors.textSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Hamster reverse toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isHamster) Color(0xFFF59E0B) else skinColors.surfaceChassisAlt)
                            .clickable { onToggleHamster() }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isHamster) "REV ON" else "REV OFF",
                            color = if (isHamster) Color.Black else skinColors.textSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Auto transition Right
                OutlinedButton(
                    onClick = { onAutoTransition(true) },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = skinColors.deckBColor)
                ) {
                    Text("AUTO B ▶", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Crossfader Rail with Quick Cut Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quick Cut A (slam to left)
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(skinColors.surfaceChassisAlt)
                        .clickable { onPositionChange(-1.0f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CUT A",
                        color = skinColors.deckAColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Horizontal Crossfader Slider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .padding(horizontal = 8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val normalized = (change.position.x / size.width).coerceIn(0f, 1f)
                                val pos = normalized * 2.0f - 1.0f
                                onPositionChange(pos)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val centerY = height / 2f

                        // Crossfader track slot
                        drawRoundRect(
                            color = skinColors.crossfaderTrack,
                            topLeft = Offset(8f, centerY - 4f),
                            size = Size(width - 16f, 8f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )

                        // Center Detent notch
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(width / 2f, centerY - 10f),
                            end = Offset(width / 2f, centerY + 10f),
                            strokeWidth = 2f
                        )

                        // 1/4 and 3/4 ticks
                        drawLine(
                            color = skinColors.textSecondary.copy(alpha = 0.3f),
                            start = Offset(width * 0.25f, centerY - 6f),
                            end = Offset(width * 0.25f, centerY + 6f),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = skinColors.textSecondary.copy(alpha = 0.3f),
                            start = Offset(width * 0.75f, centerY - 6f),
                            end = Offset(width * 0.75f, centerY + 6f),
                            strokeWidth = 1.5f
                        )

                        // Thumb Cap Position
                        val norm = (crossfaderPosition + 1.0f) * 0.5f
                        val thumbX = 16f + norm * (width - 32f)

                        // Fader Cap Body
                        drawRoundRect(
                            color = skinColors.crossfaderThumb,
                            topLeft = Offset(thumbX - 12f, centerY - 16f),
                            size = Size(24f, 32f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )

                        // Center groove line on cap
                        drawLine(
                            color = Color.Black,
                            start = Offset(thumbX, centerY - 12f),
                            end = Offset(thumbX, centerY + 12f),
                            strokeWidth = 2.5f
                        )
                    }
                }

                // Quick Cut B (slam to right)
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(skinColors.surfaceChassisAlt)
                        .clickable { onPositionChange(1.0f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CUT B",
                        color = skinColors.deckBColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
