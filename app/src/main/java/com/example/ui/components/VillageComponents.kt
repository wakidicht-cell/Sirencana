package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BidangDesa
import com.example.model.SumberPendapatan
import com.example.model.VillageProfile

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp),
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BidangPill(bidang: BidangDesa) {
    val containerColor = when (bidang) {
        BidangDesa.PEMERINTAHAN -> MaterialTheme.colorScheme.primaryContainer
        BidangDesa.PEMBANGUNAN -> MaterialTheme.colorScheme.secondaryContainer
        BidangDesa.PEMBINAAN -> MaterialTheme.colorScheme.tertiaryContainer
        BidangDesa.PEMBERDAYAAN -> MaterialTheme.colorScheme.primaryContainer
        BidangDesa.BENCANA -> MaterialTheme.colorScheme.errorContainer
    }
    val textColor = when (bidang) {
        BidangDesa.PEMERINTAHAN -> MaterialTheme.colorScheme.onPrimaryContainer
        BidangDesa.PEMBANGUNAN -> MaterialTheme.colorScheme.onSecondaryContainer
        BidangDesa.PEMBINAAN -> MaterialTheme.colorScheme.onTertiaryContainer
        BidangDesa.PEMBERDAYAAN -> MaterialTheme.colorScheme.onPrimaryContainer
        BidangDesa.BENCANA -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = "${bidang.code} · ${bidang.shortLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun SumberDanaBadge(sumber: SumberPendapatan) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Text(
            text = "${sumber.kodeRekening} · ${sumber.shortLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun EmptyStatePanel(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.testTag("empty_state_action"),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

@Composable
fun ProfileEditDialog(
    initial: VillageProfile,
    onSave: (VillageProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var desaName by remember { mutableStateOf(initial.desaName) }
    var kecamatan by remember { mutableStateOf(initial.kecamatan) }
    var kabupaten by remember { mutableStateOf(initial.kabupaten) }
    var provinsi by remember { mutableStateOf(initial.provinsi) }
    var kepalaDesa by remember { mutableStateOf(initial.kepalaDesa) }
    var sekretarisDesa by remember { mutableStateOf(initial.sekretarisDesa) }
    var startYear by remember { mutableStateOf(initial.periodeStartYear.toString()) }
    var activeYear by remember { mutableStateOf(initial.activeFiscalYear.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil Desa & Periode 8 Tahun") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = desaName,
                    onValueChange = { desaName = it },
                    label = { Text("Nama Desa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = kecamatan,
                    onValueChange = { kecamatan = it },
                    label = { Text("Kecamatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = kabupaten,
                    onValueChange = { kabupaten = it },
                    label = { Text("Kabupaten / Kota") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = provinsi,
                    onValueChange = { provinsi = it },
                    label = { Text("Provinsi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = kepalaDesa,
                    onValueChange = { kepalaDesa = it },
                    label = { Text("Nama Kepala Desa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sekretarisDesa,
                    onValueChange = { sekretarisDesa = it },
                    label = { Text("Nama Sekretaris Desa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startYear,
                        onValueChange = { startYear = it.filter(Char::isDigit).take(4) },
                        label = { Text("Awal Jabatan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = activeYear,
                        onValueChange = { activeYear = it.filter(Char::isDigit).take(4) },
                        label = { Text("Tahun Anggaran") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                val startParsed = startYear.toLongOrNull() ?: initial.periodeStartYear
                Text(
                    text = "Periode 8 tahun otomatis: $startParsed – ${startParsed + 7}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sYear = (startYear.toLongOrNull() ?: initial.periodeStartYear).coerceIn(2000L, 2100L)
                    val aYear = (activeYear.toLongOrNull() ?: initial.activeFiscalYear).coerceIn(2000L, 2110L)
                    onSave(
                        initial.copy(
                            desaName = desaName.ifBlank { "Desa" },
                            kecamatan = kecamatan,
                            kabupaten = kabupaten,
                            provinsi = provinsi,
                            kepalaDesa = kepalaDesa,
                            sekretarisDesa = sekretarisDesa,
                            periodeStartYear = sYear,
                            periodeEndYear = sYear + 7L,
                            activeFiscalYear = aYear,
                        )
                    )
                },
                modifier = Modifier.testTag("save_profile_button"),
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
