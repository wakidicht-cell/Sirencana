package com.example.report

import android.text.Html
import com.example.model.BidangDesa
import com.example.model.F4ReportType
import com.example.model.RabItem
import com.example.model.RkpdesActivity
import com.example.model.RkpdesMeta
import com.example.model.RpjmdesProgram
import com.example.model.RupiahFormatter
import com.example.model.SumberPendapatan
import com.example.model.VillageProfile
import com.example.model.VillageRevenue

object F4ReportHtmlGenerator {

    private fun esc(text: String): String =
        Html.escapeHtml(text).replace("\n", "<br/>")

    private fun commonCss(landscape: Boolean): String {
        val orientation = if (landscape) "landscape" else "portrait"
        val sheetWidth = if (landscape) "330mm" else "210mm"
        return """
            <style>
              @page {
                size: 210mm 330mm $orientation;
                margin: 13mm 14mm 15mm 14mm;
              }
              * { box-sizing: border-box; }
              html, body {
                margin: 0;
                padding: 0;
                font-family: 'Times New Roman', Times, serif;
                font-size: 10pt;
                color: #111111;
                background: #e5e7eb;
              }
              .f4-sheet {
                width: $sheetWidth;
                min-height: ${if (landscape) "210mm" else "330mm"};
                margin: 8px auto;
                padding: 12mm 13mm;
                background: #ffffff;
                box-shadow: 0 2px 10px rgba(0,0,0,0.18);
              }
              @media print {
                html, body { background: #ffffff; }
                .f4-sheet {
                  width: auto;
                  min-height: auto;
                  margin: 0;
                  padding: 0;
                  box-shadow: none;
                }
                .page-break { page-break-before: always; }
                tr { page-break-inside: avoid; }
              }
              .kop {
                text-align: center;
                border-bottom: 3px double #000000;
                padding-bottom: 6px;
                margin-bottom: 10px;
              }
              .kop .instansi { font-size: 12pt; font-weight: bold; text-transform: uppercase; letter-spacing: 0.4px; }
              .kop .desa { font-size: 14pt; font-weight: bold; text-transform: uppercase; margin-top: 2px; }
              .kop .sub { font-size: 9pt; color: #333333; margin-top: 2px; }
              .doc-title {
                text-align: center;
                font-size: 11.5pt;
                font-weight: bold;
                text-transform: uppercase;
                margin: 10px 0 3px 0;
              }
              .doc-subtitle {
                text-align: center;
                font-size: 9.5pt;
                margin-bottom: 10px;
              }
              .section-box {
                border: 1px solid #333333;
                padding: 7px 9px;
                margin-bottom: 10px;
                font-size: 9.5pt;
                line-height: 1.4;
              }
              .section-title {
                font-weight: bold;
                text-transform: uppercase;
                font-size: 9.5pt;
                margin: 8px 0 4px 0;
              }
              table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 10px;
                font-size: 8.8pt;
              }
              th, td {
                border: 1px solid #222222;
                padding: 4px 5px;
                vertical-align: top;
              }
              th {
                background-color: #ecfdf5;
                font-weight: bold;
                text-align: center;
              }
              .num { text-align: right; white-space: nowrap; font-variant-numeric: tabular-nums; }
              .center { text-align: center; }
              .bold { font-weight: bold; }
              .total-row { background-color: #f3f4f6; font-weight: bold; }
              .warn-ssh { color: #b91c1c; font-weight: bold; }
              .ok-ssh { color: #065f46; }
              .ttd-grid {
                display: table;
                width: 100%;
                margin-top: 18px;
                font-size: 9.5pt;
              }
              .ttd-col {
                display: table-cell;
                width: 50%;
                text-align: center;
                vertical-align: top;
              }
              .ttd-space { height: 52px; }
            </style>
        """.trimIndent()
    }

    private fun kopHeader(profile: VillageProfile): String = """
        <div class="kop">
          <div class="instansi">PEMERINTAH KABUPATEN ${esc(profile.kabupaten.ifBlank { "-" })}</div>
          <div class="instansi" style="font-size:10.5pt;">KECAMATAN ${esc(profile.kecamatan.ifBlank { "-" })}</div>
          <div class="desa">PEMERINTAH ${esc(profile.desaName.ifBlank { "DESA" })}</div>
          <div class="sub">Provinsi ${esc(profile.provinsi.ifBlank { "-" })} &bull; Ukuran Cetak Resmi Kertas F4 (210 &times; 330 mm)</div>
        </div>
    """.trimIndent()

    private fun signatureFooter(profile: VillageProfile, leftTitle: String, leftName: String): String = """
        <div class="ttd-grid">
          <div class="ttd-col">
            <div>Mengetahui,</div>
            <div class="bold">$leftTitle</div>
            <div class="ttd-space"></div>
            <div class="bold" style="text-decoration: underline;">${esc(leftName.ifBlank { "............................" })}</div>
          </div>
          <div class="ttd-col">
            <div>${esc(profile.desaName.ifBlank { "Desa" })}, Tahun Anggaran ${profile.activeFiscalYear}</div>
            <div class="bold">Kepala Desa</div>
            <div class="ttd-space"></div>
            <div class="bold" style="text-decoration: underline;">${esc(profile.kepalaDesa.ifBlank { "............................" })}</div>
          </div>
        </div>
    """.trimIndent()

    fun generateHtml(
        reportType: F4ReportType,
        landscape: Boolean,
        profile: VillageProfile,
        rpjmdes: List<RpjmdesProgram>,
        rkpdes: List<RkpdesActivity>,
        rkpdesMeta: RkpdesMeta?,
        rabItems: List<RabItem>,
        revenues: List<VillageRevenue>,
    ): String {
        val body = when (reportType) {
            F4ReportType.RPJMDES -> buildRpjmdesBody(profile, rpjmdes)
            F4ReportType.RKPDES -> buildRkpdesBody(profile, rkpdes, rkpdesMeta)
            F4ReportType.RAB -> buildRabBody(profile, rkpdes, rabItems)
            F4ReportType.PENDAPATAN -> buildRevenueBody(profile, revenues, rabItems)
        }
        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>${esc(reportType.title)}</title>
              ${commonCss(landscape)}
            </head>
            <body>
              <div class="f4-sheet">
                ${kopHeader(profile)}
                $body
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildRpjmdesBody(profile: VillageProfile, programs: List<RpjmdesProgram>): String {
        val totalPagu = programs.sumOf { it.paguIndikatif }
        val yearTotals = (1..8).associateWith { yr ->
            programs.filter { it.tahunKe.toInt() == yr }.sumOf { it.paguIndikatif }
        }
        val rows = if (programs.isEmpty()) {
            """<tr><td colspan="15" class="center">Belum ada data program RPJMDes 8 tahun.</td></tr>"""
        } else {
            programs.mapIndexed { idx, p ->
                val yrCells = (1..8).joinToString("") { yr ->
                    val mark = if (p.tahunKe.toInt() == yr) "&#10003;" else ""
                    """<td class="center">$mark</td>"""
                }
                """
                <tr>
                  <td class="center">${idx + 1}</td>
                  <td>${esc(p.bidangEnum.shortLabel)}<br/><small>${esc(p.subBidang)}</small></td>
                  <td><span class="bold">${esc(p.programName)}</span><br/>${esc(p.activityName)}</td>
                  <td>${esc(p.indikatorKinerja)}<br/><small>Target: ${esc(p.target)}</small></td>
                  <td>${esc(p.lokasi)}</td>
                  $yrCells
                  <td class="num">${RupiahFormatter.formatPlain(p.paguIndikatif)}</td>
                  <td class="center">${esc(p.sumberEnum.name)}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }
        val yearSumCells = (1..8).joinToString("") { yr ->
            """<td class="num" style="font-size:7.5pt;">${RupiahFormatter.formatCompact(yearTotals[yr] ?: 0L)}</td>"""
        }
        return """
            <div class="doc-title">BUKU RENCANA PEMBANGUNAN JANGKA MENENGAH DESA (RPJMDES)</div>
            <div class="doc-subtitle">Masa Jabatan Kepala Desa 8 Tahun &bull; Periode ${profile.periodeStartYear} &ndash; ${profile.periodeEndYear}</div>
            <div class="section-box">
              <div><span class="bold">VISI:</span> ${esc(profile.visi.ifBlank { "Belum diisi" })}</div>
              <div style="margin-top:4px;"><span class="bold">MISI:</span><br/>${esc(profile.misi.ifBlank { "Belum diisi" })}</div>
              <div style="margin-top:4px;"><span class="bold">TUJUAN:</span><br/>${esc(profile.tujuan.ifBlank { "Belum diisi" })}</div>
            </div>
            <div class="section-title">Matriks Rencana Program &amp; Kegiatan Pembangunan Desa (Tahun ke-1 s/d Tahun ke-8)</div>
            <table>
              <thead>
                <tr>
                  <th rowspan="2" style="width:24px;">No</th>
                  <th rowspan="2">Bidang / Sub-Bidang</th>
                  <th rowspan="2">Program &amp; Kegiatan</th>
                  <th rowspan="2">Indikator &amp; Target</th>
                  <th rowspan="2">Lokasi</th>
                  <th colspan="8">Tahun Pelaksanaan</th>
                  <th rowspan="2">Pagu Indikatif (Rp)</th>
                  <th rowspan="2">Sumber</th>
                </tr>
                <tr>
                  <th>1</th><th>2</th><th>3</th><th>4</th><th>5</th><th>6</th><th>7</th><th>8</th>
                </tr>
              </thead>
              <tbody>
                $rows
                <tr class="total-row">
                  <td colspan="5" class="center">TOTAL PAGU INDIKATIF RPJMDES (8 TAHUN)</td>
                  $yearSumCells
                  <td class="num">${RupiahFormatter.formatPlain(totalPagu)}</td>
                  <td></td>
                </tr>
              </tbody>
            </table>
            ${signatureFooter(profile, "Sekretaris Desa", profile.sekretarisDesa)}
        """.trimIndent()
    }

    private fun buildRkpdesBody(
        profile: VillageProfile,
        activities: List<RkpdesActivity>,
        meta: RkpdesMeta?,
    ): String {
        val year = profile.activeFiscalYear
        val list = activities.filter { it.fiscalYear == year }
        val total = list.sumOf { it.paguAnggaran }
        val rows = if (list.isEmpty()) {
            """<tr><td colspan="10" class="center">Belum ada kegiatan RKPDes untuk Tahun Anggaran $year.</td></tr>"""
        } else {
            list.mapIndexed { idx, act ->
                """
                <tr>
                  <td class="center">${idx + 1}</td>
                  <td>${esc(act.bidangEnum.code)} - ${esc(act.bidangEnum.shortLabel)}</td>
                  <td class="bold">${esc(act.activityName)}</td>
                  <td>${esc(act.indikator)}<br/><small>Target: ${esc(act.target)}</small></td>
                  <td>${esc(act.lokasi)}</td>
                  <td>${esc(act.pelaksana)}</td>
                  <td>${esc(act.waktuPelaksanaan)}</td>
                  <td class="num">${RupiahFormatter.formatPlain(act.paguAnggaran)}</td>
                  <td class="center">${esc(act.sumberEnum.shortLabel)}</td>
                  <td class="center">${esc(act.statusEnum.label)}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }
        val timHtml = if (meta != null) {
            """
            <div class="section-box">
              <div class="bold">I. TIM PENYUSUN RKPDES (SK No: ${esc(meta.nomorSkTim.ifBlank { "-" })})</div>
              <div>Ketua: ${esc(meta.ketuaTim.ifBlank { "-" })} &bull; Sekretaris: ${esc(meta.sekretarisTim.ifBlank { "-" })}</div>
              <div>Anggota: ${esc(meta.anggotaTim.ifBlank { "-" })}</div>
              <div class="bold" style="margin-top:6px;">II. BERITA ACARA MUSRENBANGDES (Nomor: ${esc(meta.nomorBaMusrenbang.ifBlank { "-" })})</div>
              <div>Tanggal: ${esc(meta.tanggalMusrenbang.ifBlank { "-" })} &bull; Tempat: ${esc(meta.tempatMusrenbang.ifBlank { "-" })} &bull; Pimpinan: ${esc(meta.pimpinanRapat.ifBlank { "-" })} (${meta.jumlahPeserta} peserta)</div>
              <div>Kesepakatan: ${esc(meta.ringkasanKesepakatan.ifBlank { "-" })}</div>
            </div>
            """.trimIndent()
        } else {
            """<div class="section-box">Data Tim Penyusun &amp; Berita Acara Musrenbangdes Tahun $year belum dilengkapi.</div>"""
        }
        return """
            <div class="doc-title">BUKU RENCANA KERJA PEMERINTAH DESA (RKPDES)</div>
            <div class="doc-subtitle">Tahun Anggaran $year &bull; Tahun ke-${profile.currentRpjmdesYearIndex()} RPJMDes</div>
            $timHtml
            <div class="section-title">Matriks Rencana Kegiatan &amp; Anggaran Tahunan (5 Bidang)</div>
            <table>
              <thead>
                <tr>
                  <th style="width:24px;">No</th>
                  <th>Bidang</th>
                  <th>Nama Kegiatan</th>
                  <th>Indikator &amp; Target</th>
                  <th>Lokasi</th>
                  <th>Pelaksana</th>
                  <th>Waktu</th>
                  <th>Pagu Anggaran (Rp)</th>
                  <th>Sumber</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                $rows
                <tr class="total-row">
                  <td colspan="7" class="center">JUMLAH TOTAL RENCANA BELANJA RKPDES TAHUN $year</td>
                  <td class="num">${RupiahFormatter.formatPlain(total)}</td>
                  <td colspan="2"></td>
                </tr>
              </tbody>
            </table>
            ${signatureFooter(profile, "Ketua Tim Penyusun RKPDes", meta?.ketuaTim ?: profile.sekretarisDesa)}
        """.trimIndent()
    }

    private fun buildRabBody(
        profile: VillageProfile,
        activities: List<RkpdesActivity>,
        rabItems: List<RabItem>,
    ): String {
        val year = profile.activeFiscalYear
        val yearRkp = activities.filter { it.fiscalYear == year }
        val yearRab = rabItems.filter { it.fiscalYear == year }
        val grandTotal = yearRab.sumOf { it.jumlahTotal }

        val sections = if (yearRab.isEmpty()) {
            """<div class="section-box">Belum ada rincian item Rencana Anggaran Biaya (RAB) pada Tahun Anggaran $year.</div>"""
        } else {
            val grouped = yearRab.groupBy { it.rkpdesActivityId }
            grouped.entries.joinToString("\n") { (actId, items) ->
                val activity = yearRkp.firstOrNull { it.id == actId }
                val actTitle = activity?.activityName ?: items.firstOrNull()?.rkpdesActivityName ?: "Kegiatan Desa"
                val bidangLabel = activity?.bidangEnum?.fullLabel ?: "-"
                val subTotal = items.sumOf { it.jumlahTotal }
                val rows = items.mapIndexed { idx, item ->
                    val sshNote = when {
                        item.sshMaxPrice <= 0L -> "Manual"
                        item.exceedsSsh -> """<span class="warn-ssh">Melebihi SSH (${RupiahFormatter.formatPlain(item.sshMaxPrice)})</span>"""
                        else -> """<span class="ok-ssh">Sesuai SSH (&le; ${RupiahFormatter.formatPlain(item.sshMaxPrice)})</span>"""
                    }
                    """
                    <tr>
                      <td class="center">${idx + 1}</td>
                      <td><span class="bold">${esc(item.uraian)}</span><br/><small>${esc(item.spesifikasi)}</small></td>
                      <td class="center">${item.volume}</td>
                      <td class="center">${esc(item.satuan)}</td>
                      <td class="num">${RupiahFormatter.formatPlain(item.hargaSatuan)}</td>
                      <td class="num bold">${RupiahFormatter.formatPlain(item.jumlahTotal)}</td>
                      <td class="center" style="font-size:8pt;">$sshNote</td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")

                """
                <div class="section-title">Kegiatan: ${esc(actTitle)} (${esc(bidangLabel)})</div>
                <table>
                  <thead>
                    <tr>
                      <th style="width:24px;">No</th>
                      <th>Uraian Belanja &amp; Spesifikasi</th>
                      <th>Vol</th>
                      <th>Satuan</th>
                      <th>Harga Satuan (Rp)</th>
                      <th>Jumlah (Rp)</th>
                      <th>Acuan SBB/SSH</th>
                    </tr>
                  </thead>
                  <tbody>
                    $rows
                    <tr class="total-row">
                      <td colspan="5" class="center">SUBTOTAL RAB KEGIATAN</td>
                      <td class="num">${RupiahFormatter.formatPlain(subTotal)}</td>
                      <td></td>
                    </tr>
                  </tbody>
                </table>
                """.trimIndent()
            }
        }
        return """
            <div class="doc-title">BUKU RENCANA ANGGARAN BIAYA (RAB) DESA</div>
            <div class="doc-subtitle">Tahun Anggaran $year &bull; Tervalidasi Standar Satuan Harga (SBB / SSH Desa)</div>
            $sections
            <table>
              <tbody>
                <tr class="total-row">
                  <td class="center" style="width:70%;">TOTAL KESELURUHAN RAB TAHUN ANGGARAN $year</td>
                  <td class="num" style="font-size:10.5pt;">${RupiahFormatter.format(grandTotal)}</td>
                </tr>
              </tbody>
            </table>
            ${signatureFooter(profile, "Pelaksana Kegiatan Anggaran (TPK)", profile.sekretarisDesa)}
        """.trimIndent()
    }

    private fun buildRevenueBody(
        profile: VillageProfile,
        revenues: List<VillageRevenue>,
        rabItems: List<RabItem>,
    ): String {
        val year = profile.activeFiscalYear
        val list = revenues.filter { it.fiscalYear == year }
        val totalPagu = list.sumOf { it.paguAnggaran }
        val totalRealisasi = list.sumOf { it.realisasi }
        val totalBelanjaRab = rabItems.filter { it.fiscalYear == year }.sumOf { it.jumlahTotal }
        val surplusDefisit = totalPagu - totalBelanjaRab

        val summaryRows = SumberPendapatan.entries.mapIndexed { idx, src ->
            val srcItems = list.filter { it.sumberEnum == src }
            val pagu = srcItems.sumOf { it.paguAnggaran }
            val real = srcItems.sumOf { it.realisasi }
            val pct = if (totalPagu > 0) String.format("%.1f%%", (pagu * 100.0) / totalPagu) else "0.0%"
            """
            <tr>
              <td class="center">${idx + 1}</td>
              <td class="center">${esc(src.kodeRekening)}</td>
              <td class="bold">${esc(src.fullLabel)}</td>
              <td class="center">${srcItems.size} item</td>
              <td class="num">${RupiahFormatter.formatPlain(pagu)}</td>
              <td class="num">${RupiahFormatter.formatPlain(real)}</td>
              <td class="center">$pct</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        val detailRows = if (list.isEmpty()) {
            """<tr><td colspan="7" class="center">Belum ada rincian pendapatan untuk Tahun Anggaran $year.</td></tr>"""
        } else {
            list.mapIndexed { idx, r ->
                """
                <tr>
                  <td class="center">${idx + 1}</td>
                  <td class="center">${esc(r.kodeRekening)}</td>
                  <td><span class="bold">${esc(r.sumberEnum.shortLabel)}</span><br/><small>${esc(r.subKategori)}</small></td>
                  <td>${esc(r.uraian)}<br/><small>${esc(r.keterangan)}</small></td>
                  <td class="center">${esc(r.nomorDokumen.ifBlank { "-" })}</td>
                  <td class="num">${RupiahFormatter.formatPlain(r.paguAnggaran)}</td>
                  <td class="num">${RupiahFormatter.formatPlain(r.realisasi)}</td>
                </tr>
                """.trimIndent()
            }.joinToString("\n")
        }

        return """
            <div class="doc-title">BUKU REKAPITULASI SUMBER PENDAPATAN DESA</div>
            <div class="doc-subtitle">Tahun Anggaran $year &bull; Struktur APBDes Lengkap (ADD, DD, PAD, BKK, BHP &amp; Lainnya)</div>
            <div class="section-title">I. Rekapitulasi per Sumber Pendapatan</div>
            <table>
              <thead>
                <tr>
                  <th style="width:24px;">No</th>
                  <th>Kode Rek.</th>
                  <th>Sumber Pendapatan Desa</th>
                  <th>Rincian</th>
                  <th>Pagu Anggaran (Rp)</th>
                  <th>Realisasi (Rp)</th>
                  <th>Porsi</th>
                </tr>
              </thead>
              <tbody>
                $summaryRows
                <tr class="total-row">
                  <td colspan="4" class="center">TOTAL PENDAPATAN DESA TAHUN $year</td>
                  <td class="num">${RupiahFormatter.formatPlain(totalPagu)}</td>
                  <td class="num">${RupiahFormatter.formatPlain(totalRealisasi)}</td>
                  <td class="center">100%</td>
                </tr>
              </tbody>
            </table>

            <div class="section-box">
              <div class="bold">RINGKASAN KESEIMBANGAN APBDES TAHUN $year</div>
              <div>Total Pagu Pendapatan Desa: <span class="bold">${RupiahFormatter.format(totalPagu)}</span></div>
              <div>Total Rencana Belanja (RAB RKPDes): <span class="bold">${RupiahFormatter.format(totalBelanjaRab)}</span></div>
              <div>Selisih (Surplus / Defisit): <span class="bold">${RupiahFormatter.format(surplusDefisit)}</span></div>
            </div>

            <div class="section-title">II. Rincian Dokumen Pendapatan Desa</div>
            <table>
              <thead>
                <tr>
                  <th style="width:24px;">No</th>
                  <th>Kode</th>
                  <th>Sumber &amp; Sub-Kategori</th>
                  <th>Uraian &amp; Keterangan</th>
                  <th>No. Dokumen / SK</th>
                  <th>Pagu (Rp)</th>
                  <th>Realisasi (Rp)</th>
                </tr>
              </thead>
              <tbody>
                $detailRows
              </tbody>
            </table>
            ${signatureFooter(profile, "Sekretaris Desa / Bendahara", profile.sekretarisDesa)}
        """.trimIndent()
    }
}
