package com.logiclabs.feature.tools.diagram

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.designsystem.component.DesktopModalDialog
import com.logiclabs.core.designsystem.component.SheetScaffold
import kotlinx.coroutines.isActive

@Composable
fun BooleanDiagramSheet(
    circuit: BreadboardCircuit,
    onDismiss: () -> Unit
) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(circuit) {
        var lastSig = Int.MIN_VALUE
        while (isActive) {
            withFrameNanos {
                val sig = liveSignature(circuit)
                if (sig != lastSig) {
                    lastSig = sig
                    frame++
                }
            }
        }
    }

    DesktopModalDialog(
        onDismissRequest = onDismiss,
        maxWidth = 860.dp,
        maxHeight = 680.dp
    ) {
        SheetScaffold(
            title = "BOOLEAN LOGIC DIAGRAM",
            subtitle = "gate-level equivalent of the breadboard circuit",
            onDismiss = onDismiss
        ) {
            BooleanDiagramView(
                circuit = circuit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 560.dp),
                renderVersion = frame
            )
        }
    }
}

private fun liveSignature(circuit: BreadboardCircuit): Int {
    var sig = if (circuit.masterPower) 1 else 0
    for (b in circuit.switches) sig = sig * 2 + if (b) 1 else 0
    for (b in circuit.ledValues) sig = sig * 2 + if (b) 1 else 0
    sig = sig * 3 + if (circuit.clockRunning) 1 else 0
    sig = sig * 3 + if (circuit.clockState) 1 else 0
    sig = sig * 3 + if (circuit.pulserAPressed) 1 else 0
    sig = sig * 3 + if (circuit.pulserBPressed) 1 else 0
    sig = sig * 31 + circuit.netLevels.size
    sig = sig * 31 + circuit.netLevels.values.hashCode()
    return sig
}
