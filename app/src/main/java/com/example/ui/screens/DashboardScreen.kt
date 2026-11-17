package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BidangDesa
import com.example.model.RupiahFormatter
import com.example.model.SumberPendapatan
import com.example.model.VillageProfile
import com.example.ui.components.MetricCard
import com.example.ui.components.ProfileEditDialog
import com.example.ui.viewmodel.VillageDashboardBundle

@Composable
fun DashboardScreen(
    bundle: VillageDashboardBundle,
    onSaveProfile: (VillageProfile) -> Unit,
    onNavigateModule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    val profile = bundle.profile
    val totalPendapatan = bundle.totalRevenuePagu
    val totalRab = bundle.totalRabBelanja
    val totalRkpdes = bundle.totalRkpdesPagu
    val surplusDefisit = totalPendapatan - totalRab

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_village_banner),
                        contentDescription = "Banner pemerintah desa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.20f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "TA ${bundle.activeYear} · RPJMDes Tahun ke-${bundle.activeYearRpjmdesIndex}/8",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                )
                            }
                            OutlinedButton(
                                onClick = { showProfileDialog = true },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("edit_profile_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit profil desa",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Profil Desa", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = profile.desaName.ifBlank { "Desa Kemuning" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                        Text(
                            text = "Kec. ${profile.kecamatan.ifBlank { "-" }}, Kab. ${profile.kabupaten.ifBlank { "-" }}, ${profile.provinsi.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Text(
                            text = "Kades: ${profile.kepalaDesa.ifBlank { "Belum diisi" }}  •  Periode ${profile.periodeStartYear}–${profile.periodeEndYear} (8 Tahun)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "Pendapatan ${bundle.activeYear}",
                    value = RupiahFormatter.formatCompact(totalPendapatan),
                    subtitle = "${bundle.yearRevenues.size} rincian sumber",
                    icon = Icons.Filled.AccountBalanceWallet,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Belanja RAB ${bundle.activeYear}",
                    value = RupiahFormatter.formatCompact(totalRab),
                    subtitle = "${bundle.yearRab.size} item tervalidasi",
                    icon = Icons.Filled.ReceiptLong,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    title = "Pagu RKPDes ${bundle.activeYear}",
                    value = RupiahFormatter.formatCompact(totalRkpdes),
                    subtitle = "${bundle.yearRkpdes.size} kegiatan tahunan",
                    icon = Icons.Filled.Assignment,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "RPJMDes 8 Tahun",
                    value = RupiahFormatter.formatCompact(bundle.totalRpjmdes8Tahun),
                    subtitle = "${bundle.rpjmdesPrograms.size} program · ${bundle.sshItems.size} SSH",
                    icon = Icons.Filled.DateRange,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Neraca Anggaran Desa TA ${bundle.activeYear}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total Pagu Pendapatan", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            RupiahFormatter.format(totalPendapatan),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total Rincian Belanja (RAB)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            RupiahFormatter.format(totalRab),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (surplusDefisit >= 0) "Sisa / Surplus Anggaran" else "Defisit Anggaran",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = RupiahFormatter.format(surplusDefisit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (surplusDefisit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Struktur 7 Sumber Pendapatan TA ${bundle.activeYear}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Kelola",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigateModule("pendapatan") }
                                .padding(4.dp),
                        )
                    }
                    SumberPendapatan.entries.forEach { src ->
                        val amount = bundle.revenueBySource(src)
                        val ratio = if (totalPendapatan > 0L) (amount.toFloat() / totalPendapatan.toFloat()).coerceIn(0f, 1f) else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${src.kodeRekening} · ${src.shortLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = RupiahFormatter.format(amount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Pagu RKPDes per 5 Bidang (TA ${bundle.activeYear})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    BidangDesa.entries.forEach { bidang ->
                        val sum = bundle.yearRkpdes.filter { it.bidangEnum == bidang }.sumOf { it.paguAnggaran }
                        val count = bundle.yearRkpdes.count { it.bidangEnum == bidang }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${bidang.code}. ${bidang.shortLabel} ($count)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = RupiahFormatter.format(sum),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Akses Cepat Modul & Cetak F4",
                    style = MaterialTheme.typography.titleMedium,
                )
                QuickModuleTile(
                    title = "Standar Harga (SBB / SSH Desa)",
                    subtitle = "${bundle.sshItems.size} item harga barang, jasa & honorarium acuan RAB",
                    icon = Icons.Filled.Inventory2,
                    tag = "quick_ssh_tile",
                    onClick = { onNavigateModule("ssh") },
                )
                QuickModuleTile(
                    title = "Cetak Laporan Resmi Kertas F4 (210 × 330 mm)",
                    subtitle = "Print Preview, Simpan PDF F4, & Export Excel (.CSV) 4 Buku Dokumen",
                    icon = Icons.Filled.Print,
                    tag = "quick_print_tile",
                    onClick = { onNavigateModule("cetak") },
                )
            }
        }
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            initial = profile,
            onSave = { updated ->
                showProfileDialog = false
                onSaveProfile(updated)
            },
            onDismiss = { showProfileDialog = false },
        )
    }
}

@Composable
private fun QuickModuleTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Buka $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
