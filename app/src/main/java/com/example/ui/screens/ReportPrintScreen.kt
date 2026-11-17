package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.F4ReportType
import com.example.report.F4ReportHtmlGenerator
import com.example.report.ReportExportHelper
import com.example.ui.viewmodel.VillageDashboardBundle

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReportPrintScreen(
    bundle: VillageDashboardBundle,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf(F4ReportType.RPJMDES) }
    var isLandscape by remember { mutableStateOf(F4ReportType.RPJMDES.defaultLandscape) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val htmlContent = remember(
        selectedReport,
        isLandscape,
        bundle.profile,
        bundle.rpjmdesPrograms,
        bundle.rkpdesActivities,
        bundle.activeYearMeta,
        bundle.rabItems,
        bundle.revenues,
    ) {
        F4ReportHtmlGenerator.generateHtml(
            reportType = selectedReport,
            landscape = isLandscape,
            profile = bundle.profile,
            rpjmdes = bundle.rpjmdesPrograms,
            rkpdes = bundle.rkpdesActivities,
            rkpdesMeta = bundle.activeYearMeta,
            rabItems = bundle.rabItems,
            revenues = bundle.revenues,
        )
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = ReportExportHelper.generateCsv(
                    reportType = selectedReport,
                    profile = bundle.profile,
                    rpjmdes = bundle.rpjmdesPrograms,
                    rkpdes = bundle.rkpdesActivities,
                    rkpdesMeta = bundle.activeYearMeta,
                    rabItems = bundle.rabItems,
                    revenues = bundle.revenues,
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                onNotify("Berhasil export Excel/CSV: ${selectedReport.title}")
            } catch (e: Exception) {
                onNotify("Gagal mengekspor CSV: ${e.localizedMessage}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Cetak Laporan Resmi Kertas F4",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "F4 · 210 × 330 mm",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    F4ReportType.entries.forEach { report ->
                        FilterChip(
                            selected = selectedReport == report,
                            onClick = {
                                selectedReport = report
                                isLandscape = report.defaultLandscape
                            },
                            label = { Text(report.title) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Orientasi:", style = MaterialTheme.typography.labelMedium)
                    FilterChip(
                        selected = !isLandscape,
                        onClick = { isLandscape = false },
                        label = { Text("Portrait") },
                    )
                    FilterChip(
                        selected = isLandscape,
                        onClick = { isLandscape = true },
                        label = { Text("Landscape") },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val wv = webViewRef
                            if (wv != null) {
                                ReportExportHelper.printF4Report(
                                    context = context,
                                    webView = wv,
                                    reportType = selectedReport,
                                    landscape = isLandscape,
                                    fiscalYear = bundle.activeYear,
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_f4_pdf_button"),
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cetak / PDF F4")
                    }

                    OutlinedButton(
                        onClick = {
                            val fileName = "${selectedReport.name.lowercase()}_${bundle.activeYear}.csv"
                            csvLauncher.launch(fileName)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_csv_button"),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export Excel")
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
