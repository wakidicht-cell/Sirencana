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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.RupiahFormatter
import com.example.model.SshCategory
import com.example.model.SshItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.viewmodel.VillageDashboardBundle

@Composable
fun SshScreen(
    bundle: VillageDashboardBundle,
    onSaveSsh: (SshItem) -> Unit,
    onDeleteSsh: (String) -> Unit,
    onLoadStandardSsh: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SshCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SshItem?>(null) }
    var deletingItem by remember { mutableStateOf<SshItem?>(null) }

    val filtered = remember(bundle.sshItems, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        bundle.sshItems.filter { item ->
            val matchCat = selectedCategory == null || item.categoryEnum == selectedCategory
            val matchText = q.isBlank() ||
                item.namaItem.lowercase().contains(q) ||
                item.kodeItem.lowercase().contains(q) ||
                item.spesifikasi.lowercase().contains(q)
            matchCat && matchText
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_ssh_fab"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah item SSH")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari kode, nama barang/jasa/honorarium...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ssh_search_input"),
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Semua (${bundle.sshItems.size})") },
                    )
                    SshCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                        )
                    }
                }
            }

            if (bundle.sshItems.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Filled.Inventory2,
                        title = "Standar Harga (SBB / SSH) Kosong",
                        description = "Tambahkan item Standar Satuan Harga untuk barang, jasa, dan honorarium sebagai batas maksimal (HPS) saat menyusun RAB kegiatan RKPDes.",
                        actionLabel = "Muat Referensi SSH Standar",
                        onAction = { onLoadStandardSsh(bundle.activeYear) },
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${filtered.size} Standar Harga Ditemukan",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        OutlinedButton(onClick = { onLoadStandardSsh(bundle.activeYear) }) {
                            Icon(Icons.Filled.PlaylistAddCheck, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Tambah Template")
                        }
                    }
                }

                items(filtered, key = { it.id }) { item ->
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
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp),
                                    ) {
                                        Text(
                                            text = item.categoryEnum.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                    Text(
                                        text = item.kodeItem,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editingItem = item }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit SSH")
                                    }
                                    IconButton(onClick = { deletingItem = item }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Hapus SSH",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.namaItem,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (item.spesifikasi.isNotBlank()) {
                                Text(
                                    text = "Spesifikasi: ${item.spesifikasi}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Satuan: ${item.satuan} · Berlaku TA ${item.tahunBerlaku}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "HPS: ${RupiahFormatter.format(item.hargaSatuan)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog || editingItem != null) {
        SshItemDialog(
            initial = editingItem ?: SshItem(tahunBerlaku = bundle.activeYear),
            onSave = { item ->
                showAddDialog = false
                editingItem = null
                onSaveSsh(item)
            },
            onDismiss = {
                showAddDialog = false
                editingItem = null
            },
        )
    }

    deletingItem?.let { target ->
        DeleteConfirmDialog(
            title = "Hapus Standar Harga?",
            message = "Item SSH \"${target.namaItem}\" akan dihapus.",
            onConfirm = {
                deletingItem = null
                onDeleteSsh(target.id)
            },
            onDismiss = { deletingItem = null },
        )
    }
}

@Composable
private fun SshItemDialog(
    initial: SshItem,
    onSave: (SshItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var kode by remember { mutableStateOf(initial.kodeItem) }
    var kategori by remember { mutableStateOf(initial.categoryEnum) }
    var nama by remember { mutableStateOf(initial.namaItem) }
    var spek by remember { mutableStateOf(initial.spesifikasi) }
    var satuan by remember { mutableStateOf(initial.satuan) }
    var hargaStr by remember { mutableStateOf(if (initial.hargaSatuan > 0) initial.hargaSatuan.toString() else "") }
    var tahunStr by remember { mutableStateOf(initial.tahunBerlaku.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Tambah Standar Harga (SSH)" else "Edit Standar Harga") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SshCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = kategori == cat,
                            onClick = { kategori = cat },
                            label = { Text(cat.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = kode,
                    onValueChange = { kode = it },
                    label = { Text("Kode Item (mis. SSH-BRG-01) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Barang / Jasa / Honorarium *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = spek,
                    onValueChange = { spek = it },
                    label = { Text("Spesifikasi Teknis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = satuan,
                        onValueChange = { satuan = it },
                        label = { Text("Satuan *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = tahunStr,
                        onValueChange = { tahunStr = it.filter(Char::isDigit).take(4) },
                        label = { Text("Tahun") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                    )
                }
                OutlinedTextField(
                    value = hargaStr,
                    onValueChange = { hargaStr = it.filter(Char::isDigit) },
                    label = { Text("Harga Satuan / HPS (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            kodeItem = kode,
                            kategori = kategori.name,
                            namaItem = nama,
                            spesifikasi = spek,
                            satuan = satuan,
                            hargaSatuan = RupiahFormatter.parseRupiahDigits(hargaStr),
                            tahunBerlaku = tahunStr.toLongOrNull() ?: initial.tahunBerlaku,
                        )
                    )
                },
                enabled = kode.isNotBlank() && nama.isNotBlank() && satuan.isNotBlank(),
                modifier = Modifier.testTag("save_ssh_button"),
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
