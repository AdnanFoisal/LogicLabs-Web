package com.logiclabs.app.ui.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.designsystem.component.DesktopModalDialog
import com.logiclabs.core.designsystem.component.DesktopVerticalScrollbar
import com.logiclabs.core.designsystem.component.LogicIcons
import com.logiclabs.core.designsystem.component.SheetScaffold
import com.logiclabs.core.designsystem.component.keyboardPageScroll
import com.logiclabs.core.designsystem.theme.Dimens
import com.logiclabs.feature.tools.oscilloscope.ScopePanel
import com.logiclabs.feature.tools.oscilloscope.rememberScopeSettings

@Composable
fun ScopeSheet(
    circuit: BreadboardCircuit,
    onDismiss: () -> Unit,
    onSample: () -> Unit
) {
    val settings = rememberScopeSettings()
    val scrollState = rememberScrollState()

    DesktopModalDialog(
        onDismissRequest = onDismiss,
        maxWidth = 820.dp,
        maxHeight = 700.dp
    ) {
        SheetScaffold(
            title = "DUAL-TRACE OSCILLOSCOPE",
            subtitle = "8x10 graticule · phosphor CRT · live sweep · Esc to close",
            titleIcon = LogicIcons.Scope,
            onDismiss = onDismiss
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .keyboardPageScroll(scrollState)
            ) {
                ScopePanel(
                    circuit = circuit,
                    settings = settings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(end = Dimens.Space3),
                    onSample = onSample
                )
                DesktopVerticalScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}
