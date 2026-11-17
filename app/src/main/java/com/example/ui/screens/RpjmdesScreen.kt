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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BidangDesa
import com.example.model.RpjmdesProgram
import com.example.model.RupiahFormatter
import com.example.model.SumberPendapatan
import com.example.model.VillageProfile
import com.example.ui.components.BidangPill
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.SumberDanaBadge
import com.example.ui.viewmodel.VillageDashboardBundle

@Composable
fun RpjmdesScreen(
    bundle: VillageDashboardBundle,
    onSaveProfile: (VillageProfile) -> Unit,
    onSaveProgram: (RpjmdesProgram) -> Unit,
    onDeleteProgram: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedYearFilter by remember { mutableIntStateOf(0) } // 0 = Semua Tahun 1..8
    var selectedBidangFilter by remember { mutableStateOf<BidangDesa?>(null) }
    var editingVisiMisi by remember { mutableStateOf(false) }
    var editingProgram by remember { mutableStateOf<RpjmdesProgram?>(null) }
    var showAddProgram by remember { mutableStateOf(false) }
    var deletingProgram by remember { mutableStateOf<RpjmdesProgram?>(null) }

    val filteredPrograms = remember(bundle.rpjmdesPrograms, selectedYearFilter, selectedBidangFilter) {
        bundle.rpjmdesPrograms.filter { item ->
            val matchYear = selectedYearFilter == 0 || item.tahunKe.toInt() == selectedYearFilter
            val matchBidang = selectedBidangFilter == null || item.bidangEnum == selectedBidangFilter
            matchYear && matchBidang
        }
    }
    val totalFilteredPagu = remember(filteredPrograms) { filteredPrograms.sumOf { it.paguIndikatif } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProgram = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_rpjmdes_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah program RPJMDes")
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Visi, Misi & Tujuan Kepala Desa",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "RPJMDes Masa Jabatan 8 Tahun (${bundle.profile.periodeStartYear}–${bundle.profile.periodeEndYear})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            OutlinedButton(
                                onClick = { editingVisiMisi = true },
                                modifier = Modifier.testTag("edit_visi_misi_button"),
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("Edit")
                            }
                        }
                        Text(
                            text = "Visi: ${bundle.profile.visi.ifBlank { "Belum diisi. Tekan Edit untuk melengkapi Visi Kepala Desa." }}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (bundle.profile.misi.isNotBlank()) {
                            Text(
                                text = "Misi: ${bundle.profile.misi}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (bundle.profile.tujuan.isNotBlank()) {
                            Text(
                                text = "Tujuan: ${bundle.profile.tujuan}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filter Tahun Jabatan (Tahun ke-1 s/d ke-8)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = selectedYearFilter == 0,
                            onClick = { selectedYearFilter = 0 },
                            label = { Text("Semua (8 Thn)") },
                        )
                        (1..8).forEach { yr ->
                            val calYear = bundle.profile.periodeStartYear + yr - 1
                            FilterChip(
                                selected = selectedYearFilter == yr,
                                onClick = { selectedYearFilter = yr },
                                label = { Text("Thn $yr ($calYear)") },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = selectedBidangFilter == null,
                            onClick = { selectedBidangFilter = null },
                            label = { Text("Semua Bidang") },
                        )
                        BidangDesa.entries.forEach { b ->
                            FilterChip(
                                selected = selectedBidangFilter == b,
                                onClick = { selectedBidangFilter = b },
                                label = { Text(b.shortLabel) },
                            )
                        }
                    }
                    Text(
                        text = "${filteredPrograms.size} Program/Kegiatan · Total Pagu: ${RupiahFormatter.format(totalFilteredPagu)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (filteredPrograms.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Filled.DateRange,
                        title = "Belum Ada Program RPJMDes",
                        description = "Tambahkan rencana program dan kegiatan pembangunan 8 tahun (Tahun ke-1 sampai Tahun ke-8) lengkap dengan indikator, target, lokasi, dan pagu indikatif.",
                        actionLabel = "Tambah Program RPJMDes",
                        onAction = { showAddProgram = true },
                    )
                }
            } else {
                items(filteredPrograms, key = { it.id }) { item ->
                    val calYear = bundle.profile.periodeStartYear + item.tahunKe - 1
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
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BidangPill(item.bidangEnum)
                                    Text(
                                        text = "Tahun ke-${item.tahunKe} ($calYear)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editingProgram = item }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit program")
                                    }
                                    IconButton(onClick = { deletingProgram = item }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Hapus program",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.activityName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Program: ${item.programName}" + if (item.subBidang.isNotBlank()) " (${item.subBidang})" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Indikator: ${item.indikatorKinerja.ifBlank { "-" }} · Target: ${item.target.ifBlank { "-" }} · Lokasi: ${item.lokasi.ifBlank { "-" }}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SumberDanaBadge(item.sumberEnum)
                                Text(
                                    text = RupiahFormatter.format(item.paguIndikatif),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (editingVisiMisi) {
        VisiMisiDialog(
            profile = bundle.profile,
            onSave = { updated ->
                editingVisiMisi = false
                onSaveProfile(updated)
            },
            onDismiss = { editingVisiMisi = false },
        )
    }

    if (showAddProgram || editingProgram != null) {
        RpjmdesProgramDialog(
            initial = editingProgram ?: RpjmdesProgram(
                tahunKe = if (selectedYearFilter in 1..8) selectedYearFilter.toLong() else bundle.activeYearRpjmdesIndex.toLong(),
                bidang = (selectedBidangFilter ?: BidangDesa.PEMBANGUNAN).name,
            ),
            periodeStartYear = bundle.profile.periodeStartYear,
            onSave = { prog ->
                showAddProgram = false
                editingProgram = null
                onSaveProgram(prog)
            },
            onDismiss = {
                showAddProgram = false
                editingProgram = null
            },
        )
    }

    deletingProgram?.let { target ->
        DeleteConfirmDialog(
            title = "Hapus Program RPJMDes?",
            message = "Program \"${target.activityName}\" akan dihapus dari matriks RPJMDes 8 tahun.",
            onConfirm = {
                deletingProgram = null
                onDeleteProgram(target.id)
            },
            onDismiss = { deletingProgram = null },
        )
    }
}

@Composable
private fun VisiMisiDialog(
    profile: VillageProfile,
    onSave: (VillageProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var visi by remember { mutableStateOf(profile.visi) }
    var misi by remember { mutableStateOf(profile.misi) }
    var tujuan by remember { mutableStateOf(profile.tujuan) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Visi, Misi & Tujuan Kepala Desa") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = visi,
                    onValueChange = { visi = it },
                    label = { Text("Visi Kepala Desa (8 Tahun)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = misi,
                    onValueChange = { misi = it },
                    label = { Text("Misi Kepala Desa") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tujuan,
                    onValueChange = { tujuan = it },
                    label = { Text("Tujuan Pembangunan Desa") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(profile.copy(visi = visi, misi = misi, tujuan = tujuan)) },
                modifier = Modifier.testTag("save_visi_misi_button"),
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RpjmdesProgramDialog(
    initial: RpjmdesProgram,
    periodeStartYear: Long,
    onSave: (RpjmdesProgram) -> Unit,
    onDismiss: () -> Unit,
) {
    var bidang by remember { mutableStateOf(initial.bidangEnum) }
    var bidangExpanded by remember { mutableStateOf(false) }
    var subBidang by remember { mutableStateOf(initial.subBidang) }
    var programName by remember { mutableStateOf(initial.programName) }
    var activityName by remember { mutableStateOf(initial.activityName) }
    var indikator by remember { mutableStateOf(initial.indikatorKinerja) }
    var target by remember { mutableStateOf(initial.target) }
    var lokasi by remember { mutableStateOf(initial.lokasi) }
    var tahunKe by remember { mutableIntStateOf(initial.tahunKe.toInt().coerceIn(1, 8)) }
    var paguStr by remember { mutableStateOf(if (initial.paguIndikatif > 0) initial.paguIndikatif.toString() else "") }
    var sumber by remember { mutableStateOf(initial.sumberEnum) }
    var sumberExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Tambah Program RPJMDes" else "Edit Program RPJMDes") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = bidangExpanded,
                    onExpandedChange = { bidangExpanded = !bidangExpanded },
                ) {
                    OutlinedTextField(
                        value = "${bidang.code} - ${bidang.shortLabel}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bidang Pembangunan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bidangExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = bidangExpanded,
                        onDismissRequest = { bidangExpanded = false },
                    ) {
                        BidangDesa.entries.forEach { b ->
                            DropdownMenuItem(
                                text = { Text("${b.code} · ${b.fullLabel}") },
                                onClick = {
                                    bidang = b
                                    bidangExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = subBidang,
                    onValueChange = { subBidang = it },
                    label = { Text("Sub-Bidang") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = programName,
                    onValueChange = { programName = it },
                    label = { Text("Nama Program *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    label = { Text("Nama Kegiatan Pembangunan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = indikator,
                    onValueChange = { indikator = it },
                    label = { Text("Indikator Kinerja") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = lokasi,
                        onValueChange = { lokasi = it },
                        label = { Text("Lokasi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Tahun Pelaksanaan (ke-1 s/d ke-8):", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (1..8).forEach { yr ->
                        FilterChip(
                            selected = tahunKe == yr,
                            onClick = { tahunKe = yr },
                            label = { Text("Thn $yr (${periodeStartYear + yr - 1})") },
                        )
                    }
                }
                OutlinedTextField(
                    value = paguStr,
                    onValueChange = { paguStr = it.filter(Char::isDigit) },
                    label = { Text("Pagu Indikatif (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = sumberExpanded,
                    onExpandedChange = { sumberExpanded = !sumberExpanded },
                ) {
                    OutlinedTextField(
                        value = "${sumber.kodeRekening} · ${sumber.shortLabel}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sumber Dana Indikatif") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sumberExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = sumberExpanded,
                        onDismissRequest = { sumberExpanded = false },
                    ) {
                        SumberPendapatan.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.kodeRekening} · ${s.fullLabel}") },
                                onClick = {
                                    sumber = s
                                    sumberExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            bidang = bidang.name,
                            subBidang = subBidang,
                            programName = programName,
                            activityName = activityName,
                            indikatorKinerja = indikator,
                            target = target,
                            lokasi = lokasi,
                            tahunKe = tahunKe.toLong(),
                            paguIndikatif = RupiahFormatter.parseRupiahDigits(paguStr),
                            sumberDana = sumber.name,
                        )
                    )
                },
                enabled = programName.isNotBlank() && activityName.isNotBlank(),
                modifier = Modifier.testTag("save_rpjmdes_button"),
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
