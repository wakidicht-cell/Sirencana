package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

enum class BidangDesa(
    val code: String,
    val shortLabel: String,
    val fullLabel: String,
) {
    PEMERINTAHAN("01", "Pemerintahan", "Penyelenggaraan Pemerintahan Desa"),
    PEMBANGUNAN("02", "Pembangunan", "Pelaksanaan Pembangunan Desa"),
    PEMBINAAN("03", "Pembinaan", "Pembinaan Kemasyarakatan Desa"),
    PEMBERDAYAAN("04", "Pemberdayaan", "Pemberdayaan Masyarakat Desa"),
    BENCANA("05", "Bencana & Darurat", "Penanggulangan Bencana, Keadaan Darurat & Mendesak");

    companion object {
        fun fromRaw(raw: String?): BidangDesa =
            entries.firstOrNull { it.name == raw } ?: PEMBANGUNAN
    }
}

enum class SumberPendapatan(
    val kodeRekening: String,
    val shortLabel: String,
    val fullLabel: String,
    val subCategories: List<String>,
) {
    DD(
        kodeRekening = "4.2.1",
        shortLabel = "DD (Pusat)",
        fullLabel = "Dana Desa (APBN)",
        subCategories = listOf("Dana Desa Reguler", "Dana Desa Earmark (BLT/Ketahanan Pangan/Stunting)"),
    ),
    ADD(
        kodeRekening = "4.2.3",
        shortLabel = "ADD (Kabupaten)",
        fullLabel = "Alokasi Dana Desa (ADD)",
        subCategories = listOf("Siltap & Tunjangan Pemdes", "Operasional Pemerintahan Desa", "Operasional BPD & RT/RW"),
    ),
    PAD(
        kodeRekening = "4.1",
        shortLabel = "PAD Desa",
        fullLabel = "Pendapatan Asli Desa (PAD)",
        subCategories = listOf(
            "Hasil Usaha Desa (BUMDes)",
            "Hasil Aset Desa (Tanah Kas/Pasar/Sewa)",
            "Swadaya, Partisipasi & Gotong Royong",
            "Lain-lain Pendapatan Asli Desa",
        ),
    ),
    BK_KAB(
        kodeRekening = "4.2.5",
        shortLabel = "BK Kabupaten",
        fullLabel = "Bantuan Keuangan Kabupaten",
        subCategories = listOf("BKK Infrastruktur Kabupaten", "BKK Khusus/Program Kabupaten"),
    ),
    BK_PROV(
        kodeRekening = "4.2.4",
        shortLabel = "BK Provinsi",
        fullLabel = "Bantuan Keuangan Provinsi",
        subCategories = listOf("BKK Infrastruktur Provinsi", "BKK Pemberdayaan/Khusus Provinsi"),
    ),
    BHP(
        kodeRekening = "4.2.2",
        shortLabel = "BHP & Retribusi",
        fullLabel = "Bagi Hasil Pajak & Retribusi Daerah",
        subCategories = listOf("Bagi Hasil Pajak Daerah", "Bagi Hasil Retribusi Daerah"),
    ),
    LAINNYA(
        kodeRekening = "4.3",
        shortLabel = "Lain-lain Sah",
        fullLabel = "Pendapatan Lain-lain yang Sah",
        subCategories = listOf("Hibah & Sumbangan Pihak Ketiga", "Bunga Bank / Jasa Giro Kas Desa", "Lain-lain Pendapatan Sah"),
    );

    companion object {
        fun fromRaw(raw: String?): SumberPendapatan =
            entries.firstOrNull { it.name == raw } ?: DD
    }
}

enum class RkpStatus(val label: String) {
    DRAFT("Draft"),
    MUSRENBANG("Hasil Musrenbangdes"),
    DITETAPKAN("Ditetapkan Perdes");

    companion object {
        fun fromRaw(raw: String?): RkpStatus =
            entries.firstOrNull { it.name == raw } ?: DRAFT
    }
}

enum class SshCategory(val label: String) {
    BARANG("Barang / Material"),
    JASA("Jasa / Sewa"),
    HONORARIUM("Honorarium / Upah");

    companion object {
        fun fromRaw(raw: String?): SshCategory =
            entries.firstOrNull { it.name == raw } ?: BARANG
    }
}

enum class F4ReportType(
    val title: String,
    val subtitle: String,
    val defaultLandscape: Boolean,
) {
    RPJMDES("Buku RPJMDes (8 Tahun)", "Visi, Misi, Tujuan & Matriks Program Tahun ke-1 s/d ke-8", true),
    RKPDES("Buku RKPDes & Matriks Kegiatan", "Tim Penyusun, BA Musrenbangdes & Rencana Kerja Tahunan", true),
    RAB("Buku Rencana Anggaran Biaya (RAB)", "Rincian Biaya Kegiatan & Validasi Standar Harga (SSH)", false),
    PENDAPATAN("Buku Rekapitulasi Pendapatan Desa", "Rincian 7 Sumber Pendapatan & Neraca APBDes", false),
}

@Keep
@IgnoreExtraProperties
data class VillageProfile(
    @DocumentId val id: String = "",
    val desaName: String = "Desa Kemuning",
    val kecamatan: String = "Tegalombo",
    val kabupaten: String = "Pacitan",
    val provinsi: String = "Jawa Timur",
    val kepalaDesa: String = "",
    val sekretarisDesa: String = "",
    val periodeStartYear: Long = 2024L,
    val periodeEndYear: Long = 2031L,
    val activeFiscalYear: Long = 2026L,
    val visi: String = "",
    val misi: String = "",
    val tujuan: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun currentRpjmdesYearIndex(): Int {
        val diff = (activeFiscalYear - periodeStartYear).toInt() + 1
        return diff.coerceIn(1, 8)
    }
}

@Keep
@IgnoreExtraProperties
data class RpjmdesProgram(
    @DocumentId val id: String = "",
    val bidang: String = BidangDesa.PEMBANGUNAN.name,
    val subBidang: String = "",
    val programName: String = "",
    val activityName: String = "",
    val indikatorKinerja: String = "",
    val target: String = "",
    val lokasi: String = "",
    val tahunKe: Long = 1L,
    val paguIndikatif: Long = 0L,
    val sumberDana: String = SumberPendapatan.DD.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val bidangEnum: BidangDesa get() = BidangDesa.fromRaw(bidang)
    val sumberEnum: SumberPendapatan get() = SumberPendapatan.fromRaw(sumberDana)
}

@Keep
@IgnoreExtraProperties
data class RkpdesActivity(
    @DocumentId val id: String = "",
    val fiscalYear: Long = 2026L,
    val rpjmdesProgramId: String = "",
    val bidang: String = BidangDesa.PEMBANGUNAN.name,
    val activityName: String = "",
    val indikator: String = "",
    val target: String = "",
    val lokasi: String = "",
    val pelaksana: String = "",
    val waktuPelaksanaan: String = "",
    val paguAnggaran: Long = 0L,
    val sumberDana: String = SumberPendapatan.DD.name,
    val status: String = RkpStatus.DRAFT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val bidangEnum: BidangDesa get() = BidangDesa.fromRaw(bidang)
    val sumberEnum: SumberPendapatan get() = SumberPendapatan.fromRaw(sumberDana)
    val statusEnum: RkpStatus get() = RkpStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class RkpdesMeta(
    @DocumentId val id: String = "",
    val fiscalYear: Long = 2026L,
    val nomorSkTim: String = "",
    val ketuaTim: String = "",
    val sekretarisTim: String = "",
    val anggotaTim: String = "",
    val nomorBaMusrenbang: String = "",
    val tanggalMusrenbang: String = "",
    val tempatMusrenbang: String = "",
    val pimpinanRapat: String = "",
    val jumlahPeserta: Long = 0L,
    val ringkasanKesepakatan: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class RabItem(
    @DocumentId val id: String = "",
    val fiscalYear: Long = 2026L,
    val rkpdesActivityId: String = "",
    val rkpdesActivityName: String = "",
    val sshItemId: String = "",
    val uraian: String = "",
    val spesifikasi: String = "",
    val volume: Double = 1.0,
    val satuan: String = "",
    val hargaSatuan: Long = 0L,
    val sshMaxPrice: Long = 0L,
    val jumlahTotal: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val exceedsSsh: Boolean get() = sshMaxPrice > 0L && hargaSatuan > sshMaxPrice
}

@Keep
@IgnoreExtraProperties
data class SshItem(
    @DocumentId val id: String = "",
    val kodeItem: String = "",
    val kategori: String = SshCategory.BARANG.name,
    val namaItem: String = "",
    val spesifikasi: String = "",
    val satuan: String = "",
    val hargaSatuan: Long = 0L,
    val tahunBerlaku: Long = 2026L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val categoryEnum: SshCategory get() = SshCategory.fromRaw(kategori)
}

@Keep
@IgnoreExtraProperties
data class VillageRevenue(
    @DocumentId val id: String = "",
    val fiscalYear: Long = 2026L,
    val sumber: String = SumberPendapatan.DD.name,
    val kodeRekening: String = "4.2.1",
    val subKategori: String = "",
    val uraian: String = "",
    val nomorDokumen: String = "",
    val paguAnggaran: Long = 0L,
    val realisasi: Long = 0L,
    val keterangan: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val sumberEnum: SumberPendapatan get() = SumberPendapatan.fromRaw(sumber)
}

object RupiahFormatter {
    private val idLocale = Locale("id", "ID")
    private val numberFormat = NumberFormat.getNumberInstance(idLocale).apply {
        maximumFractionDigits = 0
    }

    fun format(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        val absVal = kotlin.math.abs(amount)
        return "${sign}Rp ${numberFormat.format(absVal)}"
    }

    fun formatPlain(amount: Long): String = numberFormat.format(amount)

    fun formatCompact(amount: Long): String {
        val absVal = kotlin.math.abs(amount).toDouble()
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000_000.0 -> String.format(idLocale, "%sRp %.2f M", sign, absVal / 1_000_000_000.0)
            absVal >= 1_000_000.0 -> String.format(idLocale, "%sRp %.1f Jt", sign, absVal / 1_000_000.0)
            absVal >= 1_000.0 -> String.format(idLocale, "%sRp %.0f Rb", sign, absVal / 1_000.0)
            else -> format(amount)
        }
    }

    fun parseRupiahDigits(raw: String): Long =
        raw.filter { it.isDigit() }.toLongOrNull() ?: 0L
}
