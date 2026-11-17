package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VillageRepository
import com.example.model.RabItem
import com.example.model.RkpdesActivity
import com.example.model.RkpdesMeta
import com.example.model.RpjmdesProgram
import com.example.model.SshItem
import com.example.model.SumberPendapatan
import com.example.model.VillageProfile
import com.example.model.VillageRevenue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class VillageDashboardBundle(
    val profile: VillageProfile,
    val rpjmdesPrograms: List<RpjmdesProgram>,
    val rkpdesActivities: List<RkpdesActivity>,
    val rkpdesMetaList: List<RkpdesMeta>,
    val rabItems: List<RabItem>,
    val sshItems: List<SshItem>,
    val revenues: List<VillageRevenue>,
) {
    val activeYear: Long get() = profile.activeFiscalYear
    val activeYearRpjmdesIndex: Int get() = profile.currentRpjmdesYearIndex()
    val yearRkpdes: List<RkpdesActivity> get() = rkpdesActivities.filter { it.fiscalYear == activeYear }
    val yearRab: List<RabItem> get() = rabItems.filter { it.fiscalYear == activeYear }
    val yearRevenues: List<VillageRevenue> get() = revenues.filter { it.fiscalYear == activeYear }
    val activeYearMeta: RkpdesMeta? get() = rkpdesMetaList.firstOrNull { it.fiscalYear == activeYear }

    val totalRevenuePagu: Long get() = yearRevenues.sumOf { it.paguAnggaran }
    val totalRevenueRealized: Long get() = yearRevenues.sumOf { it.realisasi }
    val totalRkpdesPagu: Long get() = yearRkpdes.sumOf { it.paguAnggaran }
    val totalRabBelanja: Long get() = yearRab.sumOf { it.jumlahTotal }
    val totalRpjmdes8Tahun: Long get() = rpjmdesPrograms.sumOf { it.paguIndikatif }

    fun revenueBySource(source: SumberPendapatan): Long =
        yearRevenues.filter { it.sumberEnum == source }.sumOf { it.paguAnggaran }
}

class VillageViewModel(
    private val repository: VillageRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private fun <T> Flow<T>.unwrapFirestoreCatch(label: String): Flow<Result<T>> =
        map { Result.success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(Result.failure(rootError))
            }

    val bundleState: StateFlow<UiState<VillageDashboardBundle>> = combine(
        repository.observeProfile(currentUserId).unwrapFirestoreCatch("profile"),
        repository.observeRpjmdesPrograms(currentUserId).unwrapFirestoreCatch("rpjmdes"),
        repository.observeRkpdesActivities(currentUserId).unwrapFirestoreCatch("rkpdes"),
        repository.observeRkpdesMeta(currentUserId).unwrapFirestoreCatch("rkpdes_meta"),
        repository.observeRabItems(currentUserId).unwrapFirestoreCatch("rab"),
        repository.observeSshItems(currentUserId).unwrapFirestoreCatch("ssh"),
        repository.observeRevenues(currentUserId).unwrapFirestoreCatch("revenues"),
    ) { results ->
        val firstError = results.firstOrNull { it.isFailure }?.exceptionOrNull()
        if (firstError != null) {
            UiState.Error(firstError.localizedMessage ?: "Gagal memuat data desa dari Firestore")
        } else {
            @Suppress("UNCHECKED_CAST")
            val profile = (results[0].getOrNull() as? VillageProfile) ?: VillageProfile()
            @Suppress("UNCHECKED_CAST")
            val rpjmdes = results[1].getOrNull() as? List<RpjmdesProgram> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val rkpdes = results[2].getOrNull() as? List<RkpdesActivity> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val rkpdesMeta = results[3].getOrNull() as? List<RkpdesMeta> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val rab = results[4].getOrNull() as? List<RabItem> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val ssh = results[5].getOrNull() as? List<SshItem> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val revenues = results[6].getOrNull() as? List<VillageRevenue> ?: emptyList()

            UiState.Success(
                VillageDashboardBundle(
                    profile = profile,
                    rpjmdesPrograms = rpjmdes,
                    rkpdesActivities = rkpdes,
                    rkpdesMetaList = rkpdesMeta,
                    rabItems = rab,
                    sshItems = ssh,
                    revenues = revenues,
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
    )

    private fun runMutation(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = block()
                res.onSuccess {
                    _events.emit(successMessage)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Operasi gagal disimpan")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveProfile(profile: VillageProfile) =
        runMutation("Profil desa & visi misi disimpan") {
            repository.saveProfile(currentUserId, profile)
        }

    fun setActiveFiscalYear(currentProfile: VillageProfile, newYear: Long) =
        runMutation("Tahun anggaran diubah ke $newYear") {
            repository.saveProfile(currentUserId, currentProfile.copy(activeFiscalYear = newYear))
        }

    fun saveRpjmdesProgram(program: RpjmdesProgram) =
        runMutation("Program RPJMDes berhasil disimpan") {
            repository.upsertRpjmdesProgram(currentUserId, program)
        }

    fun deleteRpjmdesProgram(id: String) =
        runMutation("Program RPJMDes dihapus") {
            repository.deleteRpjmdesProgram(currentUserId, id)
        }

    fun pullFromRpjmdes(
        fiscalYear: Long,
        matchingRpjmdes: List<RpjmdesProgram>,
        existingRkpdes: List<RkpdesActivity>,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.pullRpjmdesToRkpdes(
                    currentUserId,
                    fiscalYear,
                    matchingRpjmdes,
                    existingRkpdes,
                ).onSuccess { count ->
                    val msg = if (count > 0) {
                        "$count kegiatan ditarik dari RPJMDes ke RKPDes $fiscalYear"
                    } else {
                        "Semua kegiatan RPJMDes tahun tersebut sudah ada di RKPDes $fiscalYear"
                    }
                    _events.emit(msg)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Gagal menarik data RPJMDes")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveRkpdesActivity(activity: RkpdesActivity) =
        runMutation("Kegiatan RKPDes berhasil disimpan") {
            repository.upsertRkpdesActivity(currentUserId, activity)
        }

    fun deleteRkpdesActivity(id: String) =
        runMutation("Kegiatan RKPDes dihapus") {
            repository.deleteRkpdesActivity(currentUserId, id)
        }

    fun saveRkpdesMeta(meta: RkpdesMeta) =
        runMutation("Tim Penyusun & BA Musrenbangdes disimpan") {
            repository.saveRkpdesMeta(currentUserId, meta)
        }

    fun saveRabItem(item: RabItem) =
        runMutation("Item RAB berhasil disimpan") {
            repository.upsertRabItem(currentUserId, item)
        }

    fun deleteRabItem(id: String) =
        runMutation("Item RAB dihapus") {
            repository.deleteRabItem(currentUserId, id)
        }

    fun saveSshItem(item: SshItem) =
        runMutation("Standar Harga (SSH) disimpan") {
            repository.upsertSshItem(currentUserId, item)
        }

    fun deleteSshItem(id: String) =
        runMutation("Item SSH dihapus") {
            repository.deleteSshItem(currentUserId, id)
        }

    fun loadStandardSshCatalog(fiscalYear: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.seedStandardSshCatalog(currentUserId, fiscalYear)
                    .onSuccess { count -> _events.emit("$count referensi SSH/SBB standar dimuat") }
                    .onFailure { err -> _events.emit(err.localizedMessage ?: "Gagal memuat referensi SSH") }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveRevenue(revenue: VillageRevenue) =
        runMutation("Data pendapatan desa disimpan") {
            repository.upsertRevenue(currentUserId, revenue)
        }

    fun deleteRevenue(id: String) =
        runMutation("Data pendapatan desa dihapus") {
            repository.deleteRevenue(currentUserId, id)
        }

    private companion object {
        const val TAG = "VillageVM"
    }
}
