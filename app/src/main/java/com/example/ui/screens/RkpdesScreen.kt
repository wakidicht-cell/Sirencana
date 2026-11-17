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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.model.RabItem
import com.example.model.RkpStatus
import com.example.model.RkpdesActivity
import com.example.model.RkpdesMeta
import com.example.model.RpjmdesProgram
import com.example.model.RupiahFormatter
import com.example.model.SshItem
import com.example.model.SumberPendapatan
import com.example.ui.components.BidangPill
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.SumberDanaBadge
import com.example.ui.viewmodel.VillageDashboardBundle
import kotlin.math.roundToLong

@Composable
fun RkpdesScreen(
    bundle: VillageDashboardBundle,
    onPullFromRpjmdes: (Long, List<RpjmdesProgram>, List<RkpdesActivity>) -> Unit,
    onSaveActivity: (RkpdesActivity) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onSaveMeta: (RkpdesMeta) -> Unit,
    onSaveRabItem: (RabItem) -> Unit,
    onDeleteRabItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedBidang by remember { mutableStateOf<BidangDesa?>(null) }
    var showAddActivity by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<RkpdesActivity?>(null) }
    var deletingActivity by remember { mutableStateOf<RkpdesActivity?>(null) }

    var showAddRab by remember { mutableStateOf(false) }
    var editingRab by remember { mutableStateOf<RabItem?>(null) }
    var deletingRab by remember { mutableStateOf<RabItem?>(null) }
    var selectedActivityForRabId by remember { mutableStateOf<String?>(null) }

    val year = bundle.activeYear
    val yearRpjmdesIndex = bundle.activeYearRpjmdesIndex
    val matchingRpjmdes = remember(bundle.rpjmdesPrograms, yearRpjmdesIndex) {
        bundle.rpjmdesPrograms.filter { it.tahunKe.toInt() == yearRpjmdesIndex }
    }
    val filteredActivities = remember(bundle.yearRkpdes, selectedBidang) {
        bundle.yearRkpdes.filter { selectedBidang == null || it.bidangEnum == selectedBidang }
    }
    val filteredRab = remember(bundle.yearRab, selectedActivityForRabId) {
        bundle.yearRab.filter { selectedActivityForRabId == null || it.rkpdesActivityId == selectedActivityForRabId }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddActivity = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_rkpdes_fab"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah kegiatan RKPDes")
                }
            } else if (selectedTab == 1 && bundle.yearRkpdes.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddRab = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("add_rab_fab"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah item RAB")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Kegiatan (${bundle.yearRkpdes.size})") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("RAB & SSH (${bundle.yearRab.size})") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tim & Musrenbang") },
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "Penarikan Otomatis RPJMDes Tahun ke-$yearRpjmdesIndex (TA $year)",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = "Terdapat ${matchingRpjmdes.size} kegiatan di RPJMDes tahun ke-$yearRpjmdesIndex yang dapat ditarik ke RKPDes $year.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Button(
                                        onClick = {
                                            onPullFromRpjmdes(year, matchingRpjmdes, bundle.rkpdesActivities)
                                        },
                                        enabled = matchingRpjmdes.isNotEmpty(),
                                        modifier = Modifier.testTag("pull_rpjmdes_button"),
                                    ) {
                                        Icon(Icons.Filled.CloudDownload, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Tarik Kegiatan RPJMDes ke RKPDes")
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FilterChip(
                                    selected = selectedBidang == null,
                                    onClick = { selectedBidang = null },
                                    label = { Text("Semua Bidang") },
                                )
                                BidangDesa.entries.forEach { b ->
                                    FilterChip(
                                        selected = selectedBidang == b,
                                        onClick = { selectedBidang = b },
                                        label = { Text(b.shortLabel) },
                                    )
                                }
                            }
                        }

                        if (filteredActivities.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    icon = Icons.Filled.Assignment,
                                    title = "Belum Ada Kegiatan RKPDes TA $year",
                                    description = "Tarik kegiatan dari RPJMDes tahun ke-$yearRpjmdesIndex atau tambahkan usulan prioritas hasil Musrenbangdes.",
                                    actionLabel = "Tambah Kegiatan RKPDes",
                                    onAction = { showAddActivity = true },
                                )
                            }
                        } else {
                            items(filteredActivities, key = { it.id }) { act ->
                                val rabSum = bundle.yearRab.filter { it.rkpdesActivityId == act.id }.sumOf { it.jumlahTotal }
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
                                            BidangPill(act.bidangEnum)
                                            Row {
                                                IconButton(onClick = { editingActivity = act }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit kegiatan")
                                                }
                                                IconButton(onClick = { deletingActivity = act }) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "Hapus kegiatan",
                                                        tint = MaterialTheme.colorScheme.error,
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = act.activityName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "Lokasi: ${act.lokasi.ifBlank { "-" }} · Pelaksana: ${act.pelaksana.ifBlank { "-" }} · Status: ${act.statusEnum.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            SumberDanaBadge(act.sumberEnum)
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Pagu: ${RupiahFormatter.format(act.paguAnggaran)}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                Text(
                                                    text = "RAB Terinci: ${RupiahFormatter.format(rabSum)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                selectedActivityForRabId = act.id
                                                selectedTab = 1
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text("Kelola RAB Kegiatan Ini")
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Filter RAB per Kegiatan RKPDes TA $year",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    FilterChip(
                                        selected = selectedActivityForRabId == null,
                                        onClick = { selectedActivityForRabId = null },
                                        label = { Text("Semua Kegiatan") },
                                    )
                                    bundle.yearRkpdes.forEach { act ->
                                        FilterChip(
                                            selected = selectedActivityForRabId == act.id,
                                            onClick = { selectedActivityForRabId = act.id },
                                            label = { Text(act.activityName.take(22)) },
                                        )
                                    }
                                }
                                val rabTotal = filteredRab.sumOf { it.jumlahTotal }
                                Text(
                                    text = "${filteredRab.size} Item RAB · Total RAB: ${RupiahFormatter.format(rabTotal)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        if (bundle.yearRkpdes.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    icon = Icons.Filled.ReceiptLong,
                                    title = "Belum Ada Kegiatan RKPDes",
                                    description = "Tambahkan atau tarik kegiatan di tab Kegiatan terlebih dahulu sebelum menyusun Rencana Anggaran Biaya (RAB).",
                                    actionLabel = "Ke Tab Kegiatan",
                                    onAction = { selectedTab = 0 },
                                )
                            }
                        } else if (filteredRab.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    icon = Icons.Filled.ReceiptLong,
                                    title = "Belum Ada Rincian RAB",
                                    description = "Susun rincian belanja barang, jasa, atau honorarium kegiatan dengan mengacu pada Standar Harga (SBB/SSH Desa).",
                                    actionLabel = "Tambah Item RAB",
                                    onAction = { showAddRab = true },
                                )
                            }
                        } else {
                            items(filteredRab, key = { it.id }) { rab ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        if (rab.exceedsSsh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    ),
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
                                            Text(
                                                text = rab.rkpdesActivityName,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Row {
                                                IconButton(onClick = { editingRab = rab }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit RAB")
                                                }
                                                IconButton(onClick = { deletingRab = rab }) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "Hapus RAB",
                                                        tint = MaterialTheme.colorScheme.error,
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = rab.uraian,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (rab.spesifikasi.isNotBlank()) {
                                            Text(
                                                text = rab.spesifikasi,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "${rab.volume} ${rab.satuan} × ${RupiahFormatter.format(rab.hargaSatuan)}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            Text(
                                                text = RupiahFormatter.format(rab.jumlahTotal),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        if (rab.sshMaxPrice > 0L) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (rab.exceedsSsh) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                                                    contentDescription = "Validasi SSH",
                                                    tint = if (rab.exceedsSsh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (rab.exceedsSsh) {
                                                        "Melebihi batas HPS SSH (${RupiahFormatter.format(rab.sshMaxPrice)})"
                                                    } else {
                                                        "Sesuai Standar Harga SSH (≤ ${RupiahFormatter.format(rab.sshMaxPrice)})"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (rab.exceedsSsh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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

                2 -> {
                    TimDanMusrenbangForm(
                        fiscalYear = year,
                        initial = bundle.activeYearMeta ?: RkpdesMeta(fiscalYear = year),
                        onSave = onSaveMeta,
                    )
                }
            }
        }
    }

    if (showAddActivity || editingActivity != null) {
        RkpdesActivityDialog(
            initial = editingActivity ?: RkpdesActivity(
                fiscalYear = year,
                bidang = (selectedBidang ?: BidangDesa.PEMBANGUNAN).name,
            ),
            onSave = { act ->
                showAddActivity = false
                editingActivity = null
                onSaveActivity(act)
            },
            onDismiss = {
                showAddActivity = false
                editingActivity = null
            },
        )
    }

    deletingActivity?.let { target ->
        DeleteConfirmDialog(
            title = "Hapus Kegiatan RKPDes?",
            message = "Kegiatan \"${target.activityName}\" akan dihapus dari RKPDes TA $year.",
            onConfirm = {
                deletingActivity = null
                onDeleteActivity(target.id)
            },
            onDismiss = { deletingActivity = null },
        )
    }

    if (showAddRab || editingRab != null) {
        RabItemDialog(
            initial = editingRab ?: RabItem(
                fiscalYear = year,
                rkpdesActivityId = selectedActivityForRabId ?: bundle.yearRkpdes.firstOrNull()?.id.orEmpty(),
                rkpdesActivityName = bundle.yearRkpdes.firstOrNull {
                    it.id == (selectedActivityForRabId ?: bundle.yearRkpdes.firstOrNull()?.id)
                }?.activityName.orEmpty(),
            ),
            activities = bundle.yearRkpdes,
            sshItems = bundle.sshItems,
            onSave = { item ->
                showAddRab = false
                editingRab = null
                onSaveRabItem(item)
            },
            onDismiss = {
                showAddRab = false
                editingRab = null
            },
        )
    }

    deletingRab?.let { target ->
        DeleteConfirmDialog(
            title = "Hapus Item RAB?",
            message = "Rincian \"${target.uraian}\" akan dihapus dari RAB.",
            onConfirm = {
                deletingRab = null
                onDeleteRabItem(target.id)
            },
            onDismiss = { deletingRab = null },
        )
    }
}

@Composable
private fun TimDanMusrenbangForm(
    fiscalYear: Long,
    initial: RkpdesMeta,
    onSave: (RkpdesMeta) -> Unit,
) {
    var nomorSkTim by remember(initial.id, fiscalYear) { mutableStateOf(initial.nomorSkTim) }
    var ketuaTim by remember(initial.id, fiscalYear) { mutableStateOf(initial.ketuaTim) }
    var sekretarisTim by remember(initial.id, fiscalYear) { mutableStateOf(initial.sekretarisTim) }
    var anggotaTim by remember(initial.id, fiscalYear) { mutableStateOf(initial.anggotaTim) }
    var nomorBa by remember(initial.id, fiscalYear) { mutableStateOf(initial.nomorBaMusrenbang) }
    var tanggalBa by remember(initial.id, fiscalYear) { mutableStateOf(initial.tanggalMusrenbang) }
    var tempatBa by remember(initial.id, fiscalYear) { mutableStateOf(initial.tempatMusrenbang) }
    var pimpinan by remember(initial.id, fiscalYear) { mutableStateOf(initial.pimpinanRapat) }
    var jumlahPeserta by remember(initial.id, fiscalYear) { mutableStateOf(if (initial.jumlahPeserta > 0) initial.jumlahPeserta.toString() else "") }
    var ringkasan by remember(initial.id, fiscalYear) { mutableStateOf(initial.ringkasanKesepakatan) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Penetapan Tim Penyusun RKPDes TA $fiscalYear",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = nomorSkTim,
            onValueChange = { nomorSkTim = it },
            label = { Text("Nomor SK Kepala Desa Tim Penyusun") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = ketuaTim,
            onValueChange = { ketuaTim = it },
            label = { Text("Ketua Tim (Sekretaris Desa)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = sekretarisTim,
            onValueChange = { sekretarisTim = it },
            label = { Text("Sekretaris Tim") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = anggotaTim,
            onValueChange = { anggotaTim = it },
            label = { Text("Daftar Anggota Tim (Perangkat, LPMD, Tokoh Masyarakat)") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Berita Acara Musrenbangdes TA $fiscalYear",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = nomorBa,
            onValueChange = { nomorBa = it },
            label = { Text("Nomor Berita Acara Musrenbangdes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = tanggalBa,
                onValueChange = { tanggalBa = it },
                label = { Text("Tanggal Pelaksanaan") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = jumlahPeserta,
                onValueChange = { jumlahPeserta = it.filter(Char::isDigit) },
                label = { Text("Jml Peserta") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.7f),
            )
        }
        OutlinedTextField(
            value = tempatBa,
            onValueChange = { tempatBa = it },
            label = { Text("Tempat Pelaksanaan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pimpinan,
            onValueChange = { pimpinan = it },
            label = { Text("Pimpinan Rapat Musrenbangdes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = ringkasan,
            onValueChange = { ringkasan = it },
            label = { Text("Ringkasan Kesepakatan Prioritas Kegiatan") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                onSave(
                    initial.copy(
                        fiscalYear = fiscalYear,
                        nomorSkTim = nomorSkTim,
                        ketuaTim = ketuaTim,
                        sekretarisTim = sekretarisTim,
                        anggotaTim = anggotaTim,
                        nomorBaMusrenbang = nomorBa,
                        tanggalMusrenbang = tanggalBa,
                        tempatMusrenbang = tempatBa,
                        pimpinanRapat = pimpinan,
                        jumlahPeserta = jumlahPeserta.toLongOrNull() ?: 0L,
                        ringkasanKesepakatan = ringkasan,
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_rkpdes_meta_button"),
        ) {
            Text("Simpan Tim Penyusun & Berita Acara")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RkpdesActivityDialog(
    initial: RkpdesActivity,
    onSave: (RkpdesActivity) -> Unit,
    onDismiss: () -> Unit,
) {
    var bidang by remember { mutableStateOf(initial.bidangEnum) }
    var bidangExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initial.activityName) }
    var indikator by remember { mutableStateOf(initial.indikator) }
    var target by remember { mutableStateOf(initial.target) }
    var lokasi by remember { mutableStateOf(initial.lokasi) }
    var pelaksana by remember { mutableStateOf(initial.pelaksana.ifBlank { "TPK Desa" }) }
    var waktu by remember { mutableStateOf(initial.waktuPelaksanaan.ifBlank { "Triwulan II" }) }
    var paguStr by remember { mutableStateOf(if (initial.paguAnggaran > 0) initial.paguAnggaran.toString() else "") }
    var sumber by remember { mutableStateOf(initial.sumberEnum) }
    var sumberExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(initial.statusEnum) }
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Tambah Kegiatan RKPDes" else "Edit Kegiatan RKPDes") },
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
                        label = { Text("Bidang") },
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kegiatan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = indikator,
                    onValueChange = { indikator = it },
                    label = { Text("Indikator Keluaran") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target / Volume") },
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pelaksana,
                        onValueChange = { pelaksana = it },
                        label = { Text("Pelaksana") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = waktu,
                        onValueChange = { waktu = it },
                        label = { Text("Waktu") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = paguStr,
                    onValueChange = { paguStr = it.filter(Char::isDigit) },
                    label = { Text("Pagu Anggaran (Rp)") },
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
                        label = { Text("Sumber Dana") },
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
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded },
                ) {
                    OutlinedTextField(
                        value = status.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Penetapan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                    ) {
                        RkpStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.label) },
                                onClick = {
                                    status = st
                                    statusExpanded = false
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
                            activityName = name,
                            indikator = indikator,
                            target = target,
                            lokasi = lokasi,
                            pelaksana = pelaksana,
                            waktuPelaksanaan = waktu,
                            paguAnggaran = RupiahFormatter.parseRupiahDigits(paguStr),
                            sumberDana = sumber.name,
                            status = status.name,
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_rkpdes_button"),
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RabItemDialog(
    initial: RabItem,
    activities: List<RkpdesActivity>,
    sshItems: List<SshItem>,
    onSave: (RabItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedActivity by remember {
        mutableStateOf(activities.firstOrNull { it.id == initial.rkpdesActivityId } ?: activities.first())
    }
    var activityExpanded by remember { mutableStateOf(false) }
    var selectedSshId by remember { mutableStateOf(initial.sshItemId) }
    var sshExpanded by remember { mutableStateOf(false) }
    var uraian by remember { mutableStateOf(initial.uraian) }
    var spesifikasi by remember { mutableStateOf(initial.spesifikasi) }
    var volumeStr by remember { mutableStateOf(if (initial.volume > 0) initial.volume.toString() else "1") }
    var satuan by remember { mutableStateOf(initial.satuan) }
    var hargaStr by remember { mutableStateOf(if (initial.hargaSatuan > 0) initial.hargaSatuan.toString() else "") }
    var sshMaxPrice by remember { mutableStateOf(initial.sshMaxPrice) }

    val volParsed = volumeStr.replace(',', '.').toDoubleOrNull() ?: 0.0
    val hargaParsed = RupiahFormatter.parseRupiahDigits(hargaStr)
    val totalPreview = (volParsed * hargaParsed.toDouble()).roundToLong().coerceAtLeast(0L)
    val exceedsSsh = sshMaxPrice > 0L && hargaParsed > sshMaxPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Tambah Item RAB (Acuan SSH)" else "Edit Item RAB") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = !activityExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedActivity.activityName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kegiatan RKPDes") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = activityExpanded,
                        onDismissRequest = { activityExpanded = false },
                    ) {
                        activities.forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act.activityName) },
                                onClick = {
                                    selectedActivity = act
                                    activityExpanded = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = sshExpanded,
                    onExpandedChange = { sshExpanded = !sshExpanded },
                ) {
                    val activeSsh = sshItems.firstOrNull { it.id == selectedSshId }
                    OutlinedTextField(
                        value = activeSsh?.let { "${it.kodeItem} - ${it.namaItem}" } ?: "Pilih dari Standar Harga (SSH) — Opsional",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Acuan SBB / SSH Desa") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sshExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = sshExpanded,
                        onDismissRequest = { sshExpanded = false },
                    ) {
                        sshItems.forEach { ssh ->
                            DropdownMenuItem(
                                text = { Text("${ssh.kodeItem} · ${ssh.namaItem} (${RupiahFormatter.format(ssh.hargaSatuan)}/${ssh.satuan})") },
                                onClick = {
                                    selectedSshId = ssh.id
                                    uraian = ssh.namaItem
                                    spesifikasi = ssh.spesifikasi
                                    satuan = ssh.satuan
                                    hargaStr = ssh.hargaSatuan.toString()
                                    sshMaxPrice = ssh.hargaSatuan
                                    sshExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uraian,
                    onValueChange = { uraian = it },
                    label = { Text("Uraian Belanja *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = spesifikasi,
                    onValueChange = { spesifikasi = it },
                    label = { Text("Spesifikasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = volumeStr,
                        onValueChange = { volumeStr = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                        label = { Text("Volume *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = satuan,
                        onValueChange = { satuan = it },
                        label = { Text("Satuan *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = hargaStr,
                    onValueChange = { hargaStr = it.filter(Char::isDigit) },
                    label = { Text("Harga Satuan (Rp)") },
                    isError = exceedsSsh,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (sshMaxPrice > 0L) {
                    Surface(
                        color = if (exceedsSsh) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (exceedsSsh) {
                                "Peringatan: Harga melebihi batas SSH (${RupiahFormatter.format(sshMaxPrice)})!"
                            } else {
                                "Validasi SSH OK · Batas HPS: ${RupiahFormatter.format(sshMaxPrice)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (exceedsSsh) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                Text(
                    text = "Jumlah Total: ${RupiahFormatter.format(totalPreview)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            rkpdesActivityId = selectedActivity.id,
                            rkpdesActivityName = selectedActivity.activityName,
                            sshItemId = selectedSshId,
                            uraian = uraian,
                            spesifikasi = spesifikasi,
                            volume = volParsed,
                            satuan = satuan,
                            hargaSatuan = hargaParsed,
                            sshMaxPrice = sshMaxPrice,
                            jumlahTotal = totalPreview,
                        )
                    )
                },
                enabled = uraian.isNotBlank() && satuan.isNotBlank() && volParsed > 0.0,
                modifier = Modifier.testTag("save_rab_button"),
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
