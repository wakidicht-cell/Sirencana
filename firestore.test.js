const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_kades";
const BOB_UID = "bob_sekdes";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write village data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("revenues").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("rpjmdes_programs").doc("prog_1").set({
      bidang: "PEMBANGUNAN",
      subBidang: "Infrastruktur",
      programName: "Jalan Desa",
      activityName: "Rabat Beton",
      indikatorKinerja: "Panjang jalan",
      target: "500 m",
      lokasi: "Dusun I",
      tahunKe: 1,
      paguIndikatif: 150000000,
      sumberDana: "DD",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can CRUD all 7 village planning collections with Kotlin-mirrored payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("village_profiles").doc("main").set({
      desaName: "Desa Kemuning",
      kecamatan: "Tegalombo",
      kabupaten: "Pacitan",
      provinsi: "Jawa Timur",
      kepalaDesa: "Suryanto",
      sekretarisDesa: "Hendra Wijaya",
      periodeStartYear: 2024,
      periodeEndYear: 2031,
      activeFiscalYear: 2026,
      visi: "Desa Mandiri dan Sejahtera",
      misi: "1. Tata kelola transparan",
      tujuan: "Meningkatkan infrastruktur dasar",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rpjmdes_programs").doc("rpjm_1").set({
      bidang: "PEMBANGUNAN",
      subBidang: "Pekerjaan Umum",
      programName: "Pembangunan Jalan Usaha Tani",
      activityName: "Pengerasan Jalan Dusun Krajan",
      indikatorKinerja: "Panjang jalan terbangun",
      target: "400 Meter",
      lokasi: "Dusun Krajan RT 02",
      tahunKe: 3,
      paguIndikatif: 120000000,
      sumberDana: "DD",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("rpjmdes_programs").doc("rpjm_bad").set({
      bidang: "PEMBANGUNAN",
      subBidang: "PU",
      programName: "Jalan",
      activityName: "Jalan 9",
      indikatorKinerja: "Panjang",
      target: "100m",
      lokasi: "Desa",
      tahunKe: 9,
      paguIndikatif: 1000000,
      sumberDana: "DD",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rkpdes_activities").doc("rkp_1").set({
      fiscalYear: 2026,
      rpjmdesProgramId: "rpjm_1",
      bidang: "PEMBANGUNAN",
      activityName: "Pengerasan Jalan Dusun Krajan",
      indikator: "Panjang jalan terbangun",
      target: "400 Meter",
      lokasi: "Dusun Krajan RT 02",
      pelaksana: "TPK Desa",
      waktuPelaksanaan: "Triwulan II",
      paguAnggaran: 120000000,
      sumberDana: "DD",
      status: "MUSRENBANG",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rkpdes_activities").doc("rkp_1").update({
      status: "DITETAPKAN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rkpdes_meta").doc("2026").set({
      fiscalYear: 2026,
      nomorSkTim: "141/05/SK/2026",
      ketuaTim: "Hendra Wijaya",
      sekretarisTim: "Siti Aminah",
      anggotaTim: "Budi Santoso\nRina Wati",
      nomorBaMusrenbang: "141/12/BA/2026",
      tanggalMusrenbang: "18 Januari 2026",
      tempatMusrenbang: "Balai Desa Kemuning",
      pimpinanRapat: "Ketua BPD",
      jumlahPeserta: 48,
      ringkasanKesepakatan: "Menetapkan prioritas pembangunan jalan tani.",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ssh_items").doc("ssh_1").set({
      kodeItem: "SSH-BRG-01",
      kategori: "BARANG",
      namaItem: "Semen Portland 50kg",
      spesifikasi: "SNI 50kg per zak",
      satuan: "Zak",
      hargaSatuan: 68000,
      tahunBerlaku: 2026,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rab_items").doc("rab_1").set({
      fiscalYear: 2026,
      rkpdesActivityId: "rkp_1",
      rkpdesActivityName: "Pengerasan Jalan Dusun Krajan",
      sshItemId: "ssh_1",
      uraian: "Semen Portland 50kg",
      spesifikasi: "SNI 50kg per zak",
      volume: 100.5,
      satuan: "Zak",
      hargaSatuan: 68000,
      sshMaxPrice: 68000,
      jumlahTotal: 6834000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("revenues").doc("rev_1").set({
      fiscalYear: 2026,
      sumber: "DD",
      kodeRekening: "4.2.1",
      subKategori: "Dana Desa (APBN)",
      uraian: "Dana Desa Tahap I & II",
      nomorDokumen: "PMK-145/2025",
      paguAnggaran: 950000000,
      realisasi: 475000000,
      keterangan: "Penerimaan Dana Desa Pusat",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("revenues").doc("rev_bad").set({
      id: "rev_bad",
      fiscalYear: 2026,
      sumber: "ADD",
      kodeRekening: "4.2.3",
      subKategori: "ADD Kab",
      uraian: "ADD",
      nomorDokumen: "SK-01",
      paguAnggaran: 400000000,
      realisasi: 0,
      keterangan: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("revenues").doc("rev_a").set({
      fiscalYear: 2026,
      sumber: "ADD",
      kodeRekening: "4.2.3",
      subKategori: "ADD Kabupaten",
      uraian: "Alokasi Dana Desa",
      nomorDokumen: "Perbup 12/2026",
      paguAnggaran: 420000000,
      realisasi: 210000000,
      keterangan: "Tahap I",
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("revenues").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("revenues").doc("rev_a").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("revenues").doc("rev_a").update({
      paguAnggaran: 999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
