package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BidangDesa
import com.example.model.RabItem
import com.example.model.RkpStatus
import com.example.model.RkpdesActivity
import com.example.model.RkpdesMeta
import com.example.model.RpjmdesProgram
import com.example.model.SshCategory
import com.example.model.SshItem
import com.example.model.SumberPendapatan
import com.example.model.VillageProfile
import com.example.model.VillageRevenue
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong

class VillageRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireActiveUid(expectedUid: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan akun Google terlebih dahulu.")
        check(uid == expectedUid) { "Sesi akun berubah; operasi dibatalkan." }
        return uid
    }

    private fun <T> Flow<T>.logFirestoreErrors(userId: String, path: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == userId) {
                handleFirestoreError(toLog, OperationType.LIST, path)
            }
            throw error
        }

    fun observeProfile(userId: String): Flow<VillageProfile?> {
        val path = "users/$userId/village_profiles/main"
        return db.collection("users").document(userId)
            .collection("village_profiles").document("main")
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(VillageProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(userId, path)
    }

    fun observeRpjmdesPrograms(userId: String): Flow<List<RpjmdesProgram>> {
        val path = "users/$userId/rpjmdes_programs"
        return db.collection("users").document(userId)
            .collection("rpjmdes_programs")
            .snapshots()
            .map { snap ->
                snap.toObjects(RpjmdesProgram::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<RpjmdesProgram> { it.tahunKe }.thenBy { it.bidangEnum.ordinal }.thenBy { it.programName })
            }
            .logFirestoreErrors(userId, path)
    }

    fun observeRkpdesActivities(userId: String): Flow<List<RkpdesActivity>> {
        val path = "users/$userId/rkpdes_activities"
        return db.collection("users").document(userId)
            .collection("rkpdes_activities")
            .snapshots()
            .map { snap ->
                snap.toObjects(RkpdesActivity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<RkpdesActivity> { it.fiscalYear }.thenBy { it.bidangEnum.ordinal }.thenBy { it.activityName })
            }
            .logFirestoreErrors(userId, path)
    }

    fun observeRkpdesMeta(userId: String): Flow<List<RkpdesMeta>> {
        val path = "users/$userId/rkpdes_meta"
        return db.collection("users").document(userId)
            .collection("rkpdes_meta")
            .snapshots()
            .map { snap ->
                snap.toObjects(RkpdesMeta::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(userId, path)
    }

    fun observeRabItems(userId: String): Flow<List<RabItem>> {
        val path = "users/$userId/rab_items"
        return db.collection("users").document(userId)
            .collection("rab_items")
            .snapshots()
            .map { snap ->
                snap.toObjects(RabItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.uraian }
            }
            .logFirestoreErrors(userId, path)
    }

    fun observeSshItems(userId: String): Flow<List<SshItem>> {
        val path = "users/$userId/ssh_items"
        return db.collection("users").document(userId)
            .collection("ssh_items")
            .snapshots()
            .map { snap ->
                snap.toObjects(SshItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<SshItem> { it.kategori }.thenBy { it.kodeItem }.thenBy { it.namaItem })
            }
            .logFirestoreErrors(userId, path)
    }

    fun observeRevenues(userId: String): Flow<List<VillageRevenue>> {
        val path = "users/$userId/revenues"
        return db.collection("users").document(userId)
            .collection("revenues")
            .snapshots()
            .map { snap ->
                snap.toObjects(VillageRevenue::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<VillageRevenue> { it.fiscalYear }.thenBy { it.kodeRekening }.thenBy { it.uraian })
            }
            .logFirestoreErrors(userId, path)
    }

    suspend fun saveProfile(userId: String, profile: VillageProfile): Result<Unit> {
        val docPath = "users/$userId/village_profiles/main"
        return try {
            val uid = requireActiveUid(userId)
            val cleanDesa = profile.desaName.trim().take(120).ifBlank { "Desa" }
            val startYear = profile.periodeStartYear.coerceIn(2000L, 2100L)
            val endYear = (startYear + 7L).coerceIn(startYear, 2110L)
            val activeYear = profile.activeFiscalYear.coerceIn(2000L, 2110L)
            val docRef = db.collection("users").document(uid)
                .collection("village_profiles").document("main")
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "desaName" to cleanDesa,
                        "kecamatan" to profile.kecamatan.trim().take(120),
                        "kabupaten" to profile.kabupaten.trim().take(120),
                        "provinsi" to profile.provinsi.trim().take(120),
                        "kepalaDesa" to profile.kepalaDesa.trim().take(120),
                        "sekretarisDesa" to profile.sekretarisDesa.trim().take(120),
                        "periodeStartYear" to startYear,
                        "periodeEndYear" to endYear,
                        "activeFiscalYear" to activeYear,
                        "visi" to profile.visi.trim().take(1500),
                        "misi" to profile.misi.trim().take(3000),
                        "tujuan" to profile.tujuan.trim().take(3000),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "desaName" to cleanDesa,
                        "kecamatan" to profile.kecamatan.trim().take(120),
                        "kabupaten" to profile.kabupaten.trim().take(120),
                        "provinsi" to profile.provinsi.trim().take(120),
                        "kepalaDesa" to profile.kepalaDesa.trim().take(120),
                        "sekretarisDesa" to profile.sekretarisDesa.trim().take(120),
                        "periodeStartYear" to startYear,
                        "periodeEndYear" to endYear,
                        "activeFiscalYear" to activeYear,
                        "visi" to profile.visi.trim().take(1500),
                        "misi" to profile.misi.trim().take(3000),
                        "tujuan" to profile.tujuan.trim().take(3000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertRpjmdesProgram(userId: String, item: RpjmdesProgram): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("rpjmdes_programs")
        val docRef = if (item.id.isBlank()) colRef.document() else colRef.document(item.id)
        return try {
            requireActiveUid(userId)
            val programName = item.programName.trim().take(200)
            val activityName = item.activityName.trim().take(250)
            require(programName.isNotBlank() && activityName.isNotBlank()) { "Nama program dan kegiatan wajib diisi." }
            val tahunKe = item.tahunKe.coerceIn(1L, 8L)
            val pagu = item.paguIndikatif.coerceAtLeast(0L)
            val fields = mapOf(
                "bidang" to BidangDesa.fromRaw(item.bidang).name,
                "subBidang" to item.subBidang.trim().take(160),
                "programName" to programName,
                "activityName" to activityName,
                "indikatorKinerja" to item.indikatorKinerja.trim().take(300),
                "target" to item.target.trim().take(160),
                "lokasi" to item.lokasi.trim().take(160),
                "tahunKe" to tahunKe,
                "paguIndikatif" to pagu,
                "sumberDana" to SumberPendapatan.fromRaw(item.sumberDana).name,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (item.id.isBlank()) {
                docRef.set(fields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(fields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRpjmdesProgram(userId: String, programId: String): Result<Unit> {
        val path = "users/$userId/rpjmdes_programs/$programId"
        return try {
            requireActiveUid(userId)
            require(programId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun pullRpjmdesToRkpdes(
        userId: String,
        fiscalYear: Long,
        programs: List<RpjmdesProgram>,
        existingRkpdes: List<RkpdesActivity>,
    ): Result<Int> {
        val colRef = db.collection("users").document(userId).collection("rkpdes_activities")
        return try {
            requireActiveUid(userId)
            val existingSourceIds = existingRkpdes
                .filter { it.fiscalYear == fiscalYear && it.rpjmdesProgramId.isNotBlank() }
                .map { it.rpjmdesProgramId }
                .toSet()
            val toImport = programs.filter { it.id.isNotBlank() && it.id !in existingSourceIds }
            if (toImport.isEmpty()) return Result.success(0)

            val batch = db.batch()
            toImport.forEach { prog ->
                val docRef = colRef.document()
                batch.set(
                    docRef,
                    mapOf(
                        "fiscalYear" to fiscalYear.coerceIn(2000L, 2110L),
                        "rpjmdesProgramId" to prog.id.take(128),
                        "bidang" to prog.bidangEnum.name,
                        "activityName" to prog.activityName.trim().take(250),
                        "indikator" to prog.indikatorKinerja.trim().take(300),
                        "target" to prog.target.trim().take(160),
                        "lokasi" to prog.lokasi.trim().take(160),
                        "pelaksana" to "TPK Desa",
                        "waktuPelaksanaan" to "Tahun $fiscalYear",
                        "paguAnggaran" to prog.paguIndikatif.coerceAtLeast(0L),
                        "sumberDana" to prog.sumberEnum.name,
                        "status" to RkpStatus.DRAFT.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(toImport.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertRkpdesActivity(userId: String, item: RkpdesActivity): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("rkpdes_activities")
        val docRef = if (item.id.isBlank()) colRef.document() else colRef.document(item.id)
        return try {
            requireActiveUid(userId)
            val name = item.activityName.trim().take(250)
            require(name.isNotBlank()) { "Nama kegiatan RKPDes wajib diisi." }
            val fields = mapOf(
                "fiscalYear" to item.fiscalYear.coerceIn(2000L, 2110L),
                "rpjmdesProgramId" to item.rpjmdesProgramId.take(128),
                "bidang" to BidangDesa.fromRaw(item.bidang).name,
                "activityName" to name,
                "indikator" to item.indikator.trim().take(300),
                "target" to item.target.trim().take(160),
                "lokasi" to item.lokasi.trim().take(160),
                "pelaksana" to item.pelaksana.trim().take(160),
                "waktuPelaksanaan" to item.waktuPelaksanaan.trim().take(120),
                "paguAnggaran" to item.paguAnggaran.coerceAtLeast(0L),
                "sumberDana" to SumberPendapatan.fromRaw(item.sumberDana).name,
                "status" to RkpStatus.fromRaw(item.status).name,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (item.id.isBlank()) {
                docRef.set(fields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(fields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRkpdesActivity(userId: String, activityId: String): Result<Unit> {
        val path = "users/$userId/rkpdes_activities/$activityId"
        return try {
            requireActiveUid(userId)
            require(activityId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveRkpdesMeta(userId: String, meta: RkpdesMeta): Result<Unit> {
        val yearKey = meta.fiscalYear.coerceIn(2000L, 2110L).toString()
        val docRef = db.collection("users").document(userId).collection("rkpdes_meta").document(yearKey)
        return try {
            requireActiveUid(userId)
            val existing = docRef.get().await()
            val fields = mapOf(
                "fiscalYear" to meta.fiscalYear.coerceIn(2000L, 2110L),
                "nomorSkTim" to meta.nomorSkTim.trim().take(120),
                "ketuaTim" to meta.ketuaTim.trim().take(120),
                "sekretarisTim" to meta.sekretarisTim.trim().take(120),
                "anggotaTim" to meta.anggotaTim.trim().take(2000),
                "nomorBaMusrenbang" to meta.nomorBaMusrenbang.trim().take(120),
                "tanggalMusrenbang" to meta.tanggalMusrenbang.trim().take(80),
                "tempatMusrenbang" to meta.tempatMusrenbang.trim().take(160),
                "pimpinanRapat" to meta.pimpinanRapat.trim().take(120),
                "jumlahPeserta" to meta.jumlahPeserta.coerceIn(0L, 10000L),
                "ringkasanKesepakatan" to meta.ringkasanKesepakatan.trim().take(3000),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (existing.exists()) {
                docRef.update(fields).await()
            } else {
                docRef.set(fields + ("createdAt" to FieldValue.serverTimestamp())).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertRabItem(userId: String, item: RabItem): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("rab_items")
        val docRef = if (item.id.isBlank()) colRef.document() else colRef.document(item.id)
        return try {
            requireActiveUid(userId)
            val uraian = item.uraian.trim().take(250)
            val satuan = item.satuan.trim().take(50)
            require(item.rkpdesActivityId.isNotBlank()) { "Kegiatan RKPDes harus dipilih." }
            require(uraian.isNotBlank() && satuan.isNotBlank()) { "Uraian dan satuan wajib diisi." }
            require(item.volume > 0.0) { "Volume harus lebih dari 0." }
            val unitPrice = item.hargaSatuan.coerceAtLeast(0L)
            val total = (item.volume * unitPrice.toDouble()).roundToLong().coerceAtLeast(0L)
            val fields = mapOf(
                "fiscalYear" to item.fiscalYear.coerceIn(2000L, 2110L),
                "rkpdesActivityId" to item.rkpdesActivityId.take(128),
                "rkpdesActivityName" to item.rkpdesActivityName.trim().take(250),
                "sshItemId" to item.sshItemId.take(128),
                "uraian" to uraian,
                "spesifikasi" to item.spesifikasi.trim().take(250),
                "volume" to item.volume,
                "satuan" to satuan,
                "hargaSatuan" to unitPrice,
                "sshMaxPrice" to item.sshMaxPrice.coerceAtLeast(0L),
                "jumlahTotal" to total,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (item.id.isBlank()) {
                docRef.set(fields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(fields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRabItem(userId: String, rabId: String): Result<Unit> {
        val path = "users/$userId/rab_items/$rabId"
        return try {
            requireActiveUid(userId)
            require(rabId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertSshItem(userId: String, item: SshItem): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("ssh_items")
        val docRef = if (item.id.isBlank()) colRef.document() else colRef.document(item.id)
        return try {
            requireActiveUid(userId)
            val kode = item.kodeItem.trim().take(60)
            val nama = item.namaItem.trim().take(200)
            val satuan = item.satuan.trim().take(50)
            require(kode.isNotBlank() && nama.isNotBlank() && satuan.isNotBlank()) {
                "Kode item, nama item, dan satuan SSH wajib diisi."
            }
            val fields = mapOf(
                "kodeItem" to kode,
                "kategori" to SshCategory.fromRaw(item.kategori).name,
                "namaItem" to nama,
                "spesifikasi" to item.spesifikasi.trim().take(250),
                "satuan" to satuan,
                "hargaSatuan" to item.hargaSatuan.coerceAtLeast(0L),
                "tahunBerlaku" to item.tahunBerlaku.coerceIn(2000L, 2110L),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (item.id.isBlank()) {
                docRef.set(fields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(fields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSshItem(userId: String, sshId: String): Result<Unit> {
        val path = "users/$userId/ssh_items/$sshId"
        return try {
            requireActiveUid(userId)
            require(sshId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStandardSshCatalog(userId: String, fiscalYear: Long): Result<Int> {
        val colRef = db.collection("users").document(userId).collection("ssh_items")
        return try {
            requireActiveUid(userId)
            val defaults = listOf(
                Triple("SSH-BRG-01", SshCategory.BARANG, Triple("Semen Portland (PC) 50 kg", "SNI 15-2049-2004", "Zak" to 68000L)),
                Triple("SSH-BRG-02", SshCategory.BARANG, Triple("Pasir Beton / Pasir Pasang", "Pasir sungai bersih ayak", "m3" to 265000L)),
                Triple("SSH-BRG-03", SshCategory.BARANG, Triple("Batu Split / Koral 2-3 cm", "Batu pecah mesin", "m3" to 320000L)),
                Triple("SSH-BRG-04", SshCategory.BARANG, Triple("Besi Beton Polos Ø 10 mm", "SNI panjang 12 m", "Batang" to 78000L)),
                Triple("SSH-BRG-05", SshCategory.BARANG, Triple("Kertas HVS F4 75 gsm", "Folio 215 x 330 mm", "Rim" to 58000L)),
                Triple("SSH-JSA-01", SshCategory.JASA, Triple("Sewa Molen / Concrete Mixer", "Kapasitas 0.35 m3 + BBM", "Hari" to 350000L)),
                Triple("SSH-JSA-02", SshCategory.JASA, Triple("Konsumsi Rapat Musyawarah Desa", "Nasi kotak + snack peserta", "Kotak" to 35000L)),
                Triple("SSH-HON-01", SshCategory.HONORARIUM, Triple("Upah Pekerja / Laden Konstruksi", "Standar Harian Desa", "OH" to 105000L)),
                Triple("SSH-HON-02", SshCategory.HONORARIUM, Triple("Upah Tukang Batu / Kayu", "Standar Harian Desa", "OH" to 135000L)),
                Triple("SSH-HON-03", SshCategory.HONORARIUM, Triple("Honorarium Tim Pelaksana Kegiatan (TPK)", "Per kegiatan sesuai Perbup", "OB" to 450000L)),
            )
            val batch = db.batch()
            defaults.forEach { (kode, cat, detail) ->
                val (nama, spek, unitPrice) = detail
                val docRef = colRef.document()
                batch.set(
                    docRef,
                    mapOf(
                        "kodeItem" to kode,
                        "kategori" to cat.name,
                        "namaItem" to nama,
                        "spesifikasi" to spek,
                        "satuan" to unitPrice.first,
                        "hargaSatuan" to unitPrice.second,
                        "tahunBerlaku" to fiscalYear.coerceIn(2000L, 2110L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(defaults.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertRevenue(userId: String, item: VillageRevenue): Result<Unit> {
        val colRef = db.collection("users").document(userId).collection("revenues")
        val docRef = if (item.id.isBlank()) colRef.document() else colRef.document(item.id)
        return try {
            requireActiveUid(userId)
            val uraian = item.uraian.trim().take(250)
            val kode = item.kodeRekening.trim().take(40).ifBlank { item.sumberEnum.kodeRekening }
            require(uraian.isNotBlank()) { "Uraian pendapatan wajib diisi." }
            val fields = mapOf(
                "fiscalYear" to item.fiscalYear.coerceIn(2000L, 2110L),
                "sumber" to SumberPendapatan.fromRaw(item.sumber).name,
                "kodeRekening" to kode,
                "subKategori" to item.subKategori.trim().take(160),
                "uraian" to uraian,
                "nomorDokumen" to item.nomorDokumen.trim().take(120),
                "paguAnggaran" to item.paguAnggaran.coerceAtLeast(0L),
                "realisasi" to item.realisasi.coerceAtLeast(0L),
                "keterangan" to item.keterangan.trim().take(500),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (item.id.isBlank()) {
                docRef.set(fields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                docRef.update(fields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRevenue(userId: String, revenueId: String): Result<Unit> {
        val path = "users/$userId/revenues/$revenueId"
        return try {
            requireActiveUid(userId)
            require(revenueId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
