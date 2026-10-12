package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DjSkinColors
import com.example.ui.theme.DjSkinType
import com.example.ui.theme.DjSkins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinSelectorSheet(
    activeSkin: DjSkinType,
    skinColors: DjSkinColors,
    onSelectSkin: (DjSkinType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = skinColors.surfaceChassis,
        contentColor = skinColors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Skin Customization",
                    tint = skinColors.deckAColor
                )
                Text(
                    text = "CUSTOM PERFORMANCE SKINS",
                    color = skinColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                items(DjSkinType.values().toList()) { skinType ->
                    val skinPreview = DjSkins.getSkin(skinType)
                    val isSelected = activeSkin == skinType

                    Card(
                        colors = CardDefaults.cardColors(containerColor = skinPreview.surfaceChassis),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) skinPreview.deckAColor else skinPreview.surfaceChassisAlt,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onSelectSkin(skinType)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = skinType.displayName,
                                    color = skinPreview.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = skinType.description,
                                    color = skinPreview.textSecondary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                // Color swatches preview
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(skinPreview.deckAColor))
                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(skinPreview.deckBColor))
                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(skinPreview.jogRimColor))
                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(skinPreview.vuMeterNormal))
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = skinPreview.deckAColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
