package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.engine.DjMidiService
import com.example.ui.components.midi.MidiMappingScreen
import com.example.ui.theme.DjSkinColors
import com.example.ui.viewmodel.DjViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidiSettingsDialog(
    viewModel: DjViewModel,
    skinColors: DjSkinColors,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = skinColors.surfaceChassis,
        contentColor = skinColors.textPrimary
    ) {
        MidiMappingScreen(
            viewModel = viewModel,
            skinColors = skinColors,
            onDismiss = onDismiss
        )
    }
}

/**
 * Backward compatibility overload
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidiSettingsDialog(
    midiService: DjMidiService,
    skinColors: DjSkinColors,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = skinColors.surfaceChassis,
        contentColor = skinColors.textPrimary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
