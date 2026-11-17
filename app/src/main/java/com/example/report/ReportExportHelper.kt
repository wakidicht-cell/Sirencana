package com.example.report

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import com.example.model.F4ReportType
import com.example.model.RabItem
import com.example.model.RkpdesActivity
import com.example.model.RkpdesMeta
import com.example.model.RpjmdesProgram
import com.example.model.VillageProfile
import com.example.model.VillageRevenue

object ReportExportHelper {

    // Official F4 / Folio physical paper dimensions in mils (1/1000 inch):
    // 210 mm = 8.268 in = 8268 mils
    // 330 mm = 12.992 in = 12992 mils
    private val MEDIA_SIZE_F4_PORTRAIT = PrintAttributes.MediaSize(
        "ISO_F4_FOLIO",
        "F4 Folio (210 x 330 mm)",
        8268,
        12992,
    )

    fun printF4Report(
        context: Context,
        webView: WebView,
        reportType: F4ReportType,
        landscape: Boolean,
        fiscalYear: Long,
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "SiRencanaDesa_${reportType.name}_${fiscalYear}_F4"
        val adapter = webView.createPrintDocumentAdapter(jobName)
        val mediaSize = if (landscape) MEDIA_SIZE_F4_PORTRAIT.asLandscape() else MEDIA_SIZE_F4_PORTRAIT.asPortrait()
        val attributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, adapter, attributes)
    }

    fun generateCsv(
        reportType: F4ReportType,
        profile: VillageProfile,
        rpjmdes: List<RpjmdesProgram>,
        rkpdes: List<RkpdesActivity>,
        rkpdesMeta: RkpdesMeta?,
        rabItems: List<RabItem>,
        revenues: List<VillageRevenue>,
    ): String {
        val sb = StringBuilder("\uFEFF") // UTF-8 BOM for Indonesian Microsoft Excel compatibility
        fun q(v: Any?): String {
            val s = v?.toString().orEmpty().replace("\"", "\"\"")
            return "\"$s\""
        }
        val year = profile.activeFiscalYear
        when (reportType) {
            F4ReportType.RPJMDES -> {
                sb.appendLine("No,Bidang,SubBidang,Program,Kegiatan,Indikator,Target,Lokasi,TahunKe,PaguIndikatif,SumberDana")
                rpjmdes.forEachIndexed { idx, p ->
                    sb.appendLine(
                        listOf(
                            idx + 1, q(p.bidangEnum.fullLabel), q(p.subBidang), q(p.programName),
                            q(p.activityName), q(p.indikatorKinerja), q(p.target), q(p.lokasi),
                            p.tahunKe, p.paguIndikatif, q(p.sumberEnum.shortLabel),
                        ).joinToString(",")
                    )
                }
            }
            F4ReportType.RKPDES -> {
                sb.appendLine("TahunAnggaran,No,Bidang,Kegiatan,Indikator,Target,Lokasi,Pelaksana,Waktu,PaguAnggaran,SumberDana,Status")
                if (rkpdesMeta != null) {
                    sb.appendLine("# Tim Penyusun SK: ${q(rkpdesMeta.nomorSkTim)} | Ketua: ${q(rkpdesMeta.ketuaTim)} | BA Musrenbangdes: ${q(rkpdesMeta.nomorBaMusrenbang)}")
                }
                rkpdes.filter { it.fiscalYear == year }.forEachIndexed { idx, a ->
                    sb.appendLine(
                        listOf(
                            a.fiscalYear, idx + 1, q(a.bidangEnum.fullLabel), q(a.activityName),
                            q(a.indikator), q(a.target), q(a.lokasi), q(a.pelaksana),
                            q(a.waktuPelaksanaan), a.paguAnggaran, q(a.sumberEnum.shortLabel), q(a.statusEnum.label),
                        ).joinToString(",")
                    )
                }
            }
            F4ReportType.RAB -> {
                sb.appendLine("TahunAnggaran,No,Kegiatan,Uraian,Spesifikasi,Volume,Satuan,HargaSatuan,BatasSSH,JumlahTotal,StatusSSH")
                rabItems.filter { it.fiscalYear == year }.forEachIndexed { idx, r ->
                    val status = when {
                        r.sshMaxPrice <= 0L -> "Manual"
                        r.exceedsSsh -> "Melebihi SSH"
                        else -> "Sesuai SSH"
                    }
                    sb.appendLine(
                        listOf(
                            r.fiscalYear, idx + 1, q(r.rkpdesActivityName), q(r.uraian), q(r.spesifikasi),
                            r.volume, q(r.satuan), r.hargaSatuan, r.sshMaxPrice, r.jumlahTotal, q(status),
                        ).joinToString(",")
                    )
                }
            }
            F4ReportType.PENDAPATAN -> {
                sb.appendLine("TahunAnggaran,No,KodeRekening,SumberPendapatan,SubKategori,Uraian,NomorDokumen,PaguAnggaran,Realisasi,Keterangan")
                revenues.filter { it.fiscalYear == year }.forEachIndexed { idx, rev ->
                    sb.appendLine(
                        listOf(
                            rev.fiscalYear, idx + 1, q(rev.kodeRekening), q(rev.sumberEnum.fullLabel),
                            q(rev.subKategori), q(rev.uraian), q(rev.nomorDokumen),
                            rev.paguAnggaran, rev.realisasi, q(rev.keterangan),
                        ).joinToString(",")
                    )
                }
            }
        }
        return sb.toString()
    }
}
