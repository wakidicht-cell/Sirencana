package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.RupiahFormatter
import com.example.model.SumberPendapatan
import com.example.model.VillageRevenue
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.SumberDanaBadge
import com.example.ui.viewmodel.VillageDashboardBundle

@Composable
fun RevenueScreen(
    bundle: VillageDashboardBundle,
    onSaveRevenue: (VillageRevenue) -> Unit,
    onDeleteRevenue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSource by remember { mutableStateOf<SumberPendapatan?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRevenue by remember { mutableStateOf<VillageRevenue?>(null) }
    var deletingRevenue by remember { mutableStateOf<VillageRevenue?>(null) }

    val year = bundle.activeYear
    val filtered = remember(bundle.yearRevenues, selectedSource) {
        bundle.yearRevenues.filter { selectedSource == null || it.sumberEnum == selectedSource }
    }
    val totalPagu = bundle.totalRevenuePagu
    val totalReal = bundle.totalRevenueRealized
    val realizationRatio = if (totalPagu > 0L) (totalReal.toFloat() / totalPagu.toFloat()).coerceIn(0f, 1f) else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_revenue_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah pendapatan desa")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Total Pendapatan Desa TA $year",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = RupiahFormatter.format(totalPagu),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Realisasi: ${RupiahFormatter.format(totalReal)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${(realizationRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { realizationRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filter 7 Sumber Pendapatan Resmi APBDes",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = selectedSource == null,
                            onClick = { selectedSource = null },
                            label = { Text("Semua (${bundle.yearRevenues.size})") },
                        )
                        SumberPendapatan.entries.forEach { src ->
                            FilterChip(
                                selected = selectedSource == src,
                                onClick = { selectedSource = src },
                                label = { Text("${src.kodeRekening} · ${src.shortLabel}") },
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "Belum Ada Data Pendapatan",
                        description = "Catat penerimaan ADD, Dana Desa (DD), PAD (Hasil Usaha, Aset, Swadaya/Gotong Royong), BK Kabupaten, BK Provinsi, BHP & Retribusi, serta Pendapatan Lain-lain.",
                        actionLabel = "Input Pendapatan Desa",
                        onAction = { showAddDialog = true },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { rev ->
                    val pct = if (rev.paguAnggaran > 0L) ((rev.realisasi * 100.0) / rev.paguAnggaran).toInt() else 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SumberDanaBadge(rev.sumberEnum)
                                Row {
                                    IconButton(onClick = { editingRevenue = rev }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit pendapatan")
                                    }
                                    IconButton(onClick = { deletingRevenue = rev }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Hapus pendapatan",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = rev.uraian,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${rev.subKategori.ifBlank { rev.sumberEnum.fullLabel }} · Rek. ${rev.kodeRekening}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (rev.nomorDokumen.isNotBlank() || rev.keterangan.isNotBlank()) {
                                Text(
                                    text = "SK/Dok: ${rev.nomorDokumen.ifBlank { "-" }} · ${rev.keterangan}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Pagu: ${RupiahFormatter.format(rev.paguAnggaran)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Realisasi: ${RupiahFormatter.format(rev.realisasi)} ($pct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = "TA ${rev.fiscalYear}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog || editingRevenue != null) {
        RevenueDialog(
            initial = editingRevenue ?: VillageRevenue(
                fiscalYear = year,
                sumber = (selectedSource ?: SumberPendapatan.DD).name,
                kodeRekening = (selectedSource ?: SumberPendapatan.DD).kodeRekening,
                subKategori = (selectedSource ?: SumberPendapatan.DD).subCategories.first(),
            ),
            onSave = { item ->
                showAddDialog = false
                editingRevenue = null
                onSaveRevenue(item)
            },
            onDismiss = {
                showAddDialog = false
                editingRevenue = null
            },
        )
    }

    deletingRevenue?.let { target ->
        DeleteConfirmDialog(
            title = "Hapus Data Pendapatan?",
            message = "Pendapatan \"${target.uraian}\" akan dihapus dari TA $year.",
            onConfirm = {
                deletingRevenue = null
                onDeleteRevenue(target.id)
            },
            onDismiss = { deletingRevenue = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RevenueDialog(
    initial: VillageRevenue,
    onSave: (VillageRevenue) -> Unit,
    onDismiss: () -> Unit,
) {
    var sumber by remember { mutableStateOf(initial.sumberEnum) }
    var sumberExpanded by remember { mutableStateOf(false) }
    var subKategori by remember { mutableStateOf(initial.subKategori.ifBlank { sumber.subCategories.first() }) }
    var subExpanded by remember { mutableStateOf(false) }
    var kodeRekening by remember { mutableStateOf(initial.kodeRekening.ifBlank { sumber.kodeRekening }) }
    var uraian by remember { mutableStateOf(initial.uraian) }
    var nomorDok by remember { mutableStateOf(initial.nomorDokumen) }
    var paguStr by remember { mutableStateOf(if (initial.paguAnggaran > 0) initial.paguAnggaran.toString() else "") }
    var realStr by remember { mutableStateOf(if (initial.realisasi > 0) initial.realisasi.toString() else "") }
    var keterangan by remember { mutableStateOf(initial.keterangan) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Input Pendapatan Desa" else "Edit Pendapatan Desa") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = sumberExpanded,
                    onExpandedChange = { sumberExpanded = !sumberExpanded },
                ) {
                    OutlinedTextField(
                        value = "${sumber.kodeRekening} · ${sumber.fullLabel}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sumber Pendapatan *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sumberExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = sumberExpanded,
                        onDismissRequest = { sumberExpanded = false },
                    ) {
                        SumberPendapatan.entries.forEach { src ->
                            DropdownMenuItem(
                                text = { Text("${src.kodeRekening} · ${src.fullLabel}") },
                                onClick = {
                                    sumber = src
                                    kodeRekening = src.kodeRekening
                                    subKategori = src.subCategories.first()
                                    sumberExpanded = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = subExpanded,
                    onExpandedChange = { subExpanded = !subExpanded },
                ) {
                    OutlinedTextField(
                        value = subKategori,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rincian / Sub-Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = subExpanded,
                        onDismissRequest = { subExpanded = false },
                    ) {
                        sumber.subCategories.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    subKategori = sub
                                    subExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kodeRekening,
                        onValueChange = { kodeRekening = it },
                        label = { Text("Kode Rekening") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                    )
                    OutlinedTextField(
                        value = nomorDok,
                        onValueChange = { nomorDok = it },
                        label = { Text("No. SK / Perbup / PMK") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                    )
                }

                OutlinedTextField(
                    value = uraian,
                    onValueChange = { uraian = it },
                    label = { Text("Uraian Pendapatan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = paguStr,
                    onValueChange = { paguStr = it.filter(Char::isDigit) },
                    label = { Text("Pagu Anggaran (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = realStr,
                    onValueChange = { realStr = it.filter(Char::isDigit) },
                    label = { Text("Realisasi Diterima (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan / Catatan") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            sumber = sumber.name,
                            kodeRekening = kodeRekening.ifBlank { sumber.kodeRekening },
                            subKategori = subKategori,
                            uraian = uraian,
                            nomorDokumen = nomorDok,
                            paguAnggaran = RupiahFormatter.parseRupiahDigits(paguStr),
                            realisasi = RupiahFormatter.parseRupiahDigits(realStr),
                            keterangan = keterangan,
                        )
                    )
                },
                enabled = uraian.isNotBlank(),
                modifier = Modifier.testTag("save_revenue_button"),
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
