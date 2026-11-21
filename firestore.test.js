const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const crypto = require("node:crypto");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PARENT_A = "parent_alice_1";
const PARENT_B = "parent_bob_2";
const ADMIN_UID = "admin_fekr_1";
const ADMIN_EMAIL = "amadkor8686@gmail.com";

function sha256Hex(raw) {
  return crypto.createHash("sha256").update(raw).digest("hex");
}

const BOOTSTRAP_ADMIN_HASH = sha256Hex("admin:fekr2026");

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 10000) {
  return new Date(Date.now() - offsetMs);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("admins").doc("a").get());
  await assertFails(unauth.collection("access_codes").get());
  await assertFails(unauth.collection("students").get());
  await assertFails(unauth.collection("appointments").get());
  await assertFails(unauth.collection("evaluations").get());
  await assertFails(unauth.collection("payments").get());
  await assertFails(unauth.collection("chats").get());
  await assertFails(unauth.collection("notifications").get());
});

test("Prevents privilege escalation, fake admin claim, and access_codes enumeration", async () => {
  const attackerDb = testEnv.authenticatedContext(PARENT_B, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  // Attacker cannot list access_codes
  await assertFails(attackerDb.collection("access_codes").get());

  // Attacker cannot create /admins/{uid} with a guessed/wrong hash
  await assertFails(
    attackerDb.collection("admins").doc(PARENT_B).set({
      userId: PARENT_B,
      credentialHash: sha256Hex("admin:wrongpass"),
      createdAt: ts,
    })
  );

  // Attacker cannot escalate role in /users/{uid}
  await assertSucceeds(
    attackerDb.collection("users").doc(PARENT_B).set({
      userId: PARENT_B,
      displayName: "مستخدم",
      email: "bob@example.com",
      phone: "01001234567",
      role: "parent",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    attackerDb.collection("users").doc(PARENT_B).update({
      role: "admin",
      updatedAt: pastDate(1000),
    })
  );

  // Spoofed email_verified=false cannot bypass admin check
  const spoofAdmin = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  await assertFails(spoofAdmin.collection("students").get());
});

test("Admin logs in via admin username+password hash, creates client credentials, and parent claims & follows child", async () => {
  const staffDb = testEnv.authenticatedContext("staff_phone_uid", { email: "staff@other.com", email_verified: true }).firestore();
  const momDb = testEnv.authenticatedContext("mom_device_uid", { email: "mom_any@other.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(PARENT_B, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  // 1. Staff claims admin access with valid bootstrap hash (admin:fekr2026)
  await assertSucceeds(
    staffDb.collection("admins").doc("staff_phone_uid").set({
      userId: "staff_phone_uid",
      credentialHash: BOOTSTRAP_ADMIN_HASH,
      createdAt: ts,
    })
  );

  // 2. Admin adds student + auto-generated username/password access_code
  const clientUsername = "fekr_1042";
  const clientPass = "849201";
  const clientHash = sha256Hex(`${clientUsername}:${clientPass}`);

  await assertSucceeds(
    staffDb.collection("students").doc("stu_center_1").set({
      parentUid: "pending_link",
      parentName: "والدة حمزة",
      parentPhone: "01005556677",
      childName: "حمزة خالد",
      ageYears: 5,
      primaryService: "تخاطب",
      specialistName: "أ / أحمد عوف",
      caseStatus: "active",
      progressPercent: 55,
      totalSessions: 1,
      attendedSessions: 1,
      clinicalNotes: "استجابة ممتازة",
      currentGoals: "تكوين جمل من 3 كلمات",
      clientUsername,
      clientPassword: clientPass,
      credentialHash: clientHash,
      pendingSyncRefs: ["evaluations/eval_center_1", "appointments/appt_center_1"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("access_codes").doc(clientHash).set({
      username: clientUsername,
      role: "parent",
      studentId: "stu_center_1",
      displayName: "والدة حمزة",
      linkedUid: "",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("evaluations").doc("eval_center_1").set({
      studentId: "stu_center_1",
      parentUid: "pending_link",
      childName: "حمزة خالد",
      monthLabel: "نوفمبر 2026",
      specialistName: "أ / أحمد عوف",
      speechScore: 82,
      attentionScore: 76,
      behaviorScore: 88,
      cognitiveScore: 79,
      motorScore: 84,
      overallScore: 82,
      summary: "تحسن مستمر في النطق",
      homeRecommendations: "تشجيع الطفل على القراءة المصورة",
      parentAcknowledged: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("appointments").doc("appt_center_1").set({
      parentUid: "pending_link",
      studentId: "stu_center_1",
      childName: "حمزة خالد",
      serviceName: "تخاطب",
      specialistName: "أ / أحمد عوف",
      dateLabel: "2026-11-24",
      timeSlot: "04:00 م",
      scheduledEpochMs: Date.now() + 86400000,
      status: "confirmed",
      paymentStatus: "unpaid",
      feeAmount: 250,
      notes: "جلسة متابعة",
      reminderMinutesBefore: 120,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("case_files").doc("file_center_1").set({
      studentId: "stu_center_1",
      parentUid: "pending_link",
      childName: "حمزة خالد",
      title: "تقرير تقييم التخاطب المبدئي",
      category: "report",
      notes: "يرجى الاطلاع قبل الجلسة القادمة",
      fileName: "speech_report.pdf",
      mimeType: "application/pdf",
      fileSizeBytes: 1200,
      contentBase64: "JVBERi0xLjQK",
      externalUrl: "",
      uploadedByName: "أ / أحمد عوف",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    staffDb.collection("students").doc("stu_center_1").update({
      pendingSyncRefs: ["evaluations/eval_center_1", "appointments/appt_center_1", "case_files/file_center_1"],
      updatedAt: ts,
    })
  );

  // Bob (without knowing username:password) CANNOT claim stu_center_1 directly
  await assertFails(
    bobDb.collection("students").doc("stu_center_1").update({
      parentUid: PARENT_B,
      updatedAt: pastDate(500),
    })
  );

  // 3. Mom logs in on her phone using fekr_1042 + 849201 (no Gmail matching needed!)
  await assertSucceeds(momDb.collection("access_codes").doc(clientHash).get());
  await assertSucceeds(
    momDb.collection("access_codes").doc(clientHash).update({
      linkedUid: "mom_device_uid",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    momDb.collection("students").doc("stu_center_1").update({
      parentUid: "mom_device_uid",
      updatedAt: pastDate(400),
    })
  );
  await assertSucceeds(momDb.collection("students").doc("stu_center_1").get());
  await assertSucceeds(
    momDb.collection("evaluations").doc("eval_center_1").update({
      parentUid: "mom_device_uid",
      updatedAt: pastDate(300),
    })
  );
  await assertSucceeds(
    momDb.collection("appointments").doc("appt_center_1").update({
      parentUid: "mom_device_uid",
      updatedAt: pastDate(300),
    })
  );
  await assertSucceeds(
    momDb.collection("case_files").doc("file_center_1").update({
      parentUid: "mom_device_uid",
      updatedAt: pastDate(300),
    })
  );
  await assertSucceeds(
    momDb.collection("students").doc("stu_center_1").update({
      pendingSyncRefs: [],
      updatedAt: pastDate(200),
    })
  );

  // Mom sees her child, evaluation, appointment, case_files, and acknowledges evaluation
  await assertSucceeds(momDb.collection("students").where("parentUid", "==", "mom_device_uid").get());
  await assertSucceeds(momDb.collection("evaluations").where("parentUid", "==", "mom_device_uid").get());
  await assertSucceeds(momDb.collection("case_files").where("parentUid", "==", "mom_device_uid").get());
  await assertSucceeds(
    momDb.collection("evaluations").doc("eval_center_1").update({
      parentAcknowledged: true,
      updatedAt: pastDate(100),
    })
  );

  // Parent CANNOT create or delete case_files directly
  await assertFails(
    momDb.collection("case_files").doc("file_fake_parent").set({
      studentId: "stu_center_1",
      parentUid: "mom_device_uid",
      childName: "حمزة خالد",
      title: "ملف مزيف",
      category: "other",
      notes: "",
      fileName: "test.txt",
      mimeType: "text/plain",
      fileSizeBytes: 10,
      contentBase64: "SGVsbG8=",
      externalUrl: "",
      uploadedByName: "ولي أمر",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(momDb.collection("case_files").doc("file_center_1").delete());

  // Parent cannot tamper with immutable credential fields on student
  await assertFails(
    momDb.collection("students").doc("stu_center_1").update({
      clientUsername: "hacked_user",
      updatedAt: pastDate(50),
    })
  );

  // Bob cannot read Mom's records or case files
  await assertFails(bobDb.collection("students").doc("stu_center_1").get());
  await assertFails(bobDb.collection("evaluations").doc("eval_center_1").get());
  await assertFails(bobDb.collection("case_files").doc("file_center_1").get());
});

test("Parent self-registers with username+password, books appointment, pays, and isolates from other parents", async () => {
  const aliceDb = testEnv.authenticatedContext(PARENT_A, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(PARENT_B, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  const aliceUser = "um_younes";
  const alicePass = "552211";
  const aliceHash = sha256Hex(`${aliceUser}:${alicePass}`);

  await assertSucceeds(
    aliceDb.collection("students").doc("stu_alice").set({
      parentUid: PARENT_A,
      parentName: "والدة يونس",
      parentPhone: "01001112233",
      childName: "يونس أحمد",
      ageYears: 6,
      primaryService: "تخاطب",
      specialistName: "أ / أحمد عوف",
      caseStatus: "active",
      progressPercent: 45,
      totalSessions: 4,
      attendedSessions: 3,
      clinicalNotes: "تحسن ملحوظ",
      currentGoals: "نطق حرف الراء",
      clientUsername: aliceUser,
      clientPassword: alicePass,
      credentialHash: aliceHash,
      pendingSyncRefs: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("access_codes").doc(aliceHash).set({
      username: aliceUser,
      role: "parent",
      studentId: "stu_alice",
      displayName: "والدة يونس",
      linkedUid: PARENT_A,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("students").doc("stu_alice").get());
  await assertSucceeds(
    aliceDb.collection("appointments").doc("appt_1").set({
      parentUid: PARENT_A,
      studentId: "stu_alice",
      childName: "يونس أحمد",
      serviceName: "تخاطب",
      specialistName: "أ / أحمد عوف",
      dateLabel: "2026-11-24",
      timeSlot: "04:00 م",
      scheduledEpochMs: Date.now() + 86400000,
      status: "pending",
      paymentStatus: "unpaid",
      feeAmount: 250,
      notes: "جلسة أسبوعية",
      reminderMinutesBefore: 120,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_1").set({
      parentUid: PARENT_A,
      appointmentId: "appt_1",
      childName: "يونس أحمد",
      description: "سداد جلسة تخاطب",
      amountEgp: 250,
      method: "vodafone_cash",
      referenceCode: "FKR-1001",
      status: "completed",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(aliceDb.collection("payments").doc("pay_1").delete());
});

test("Admin can update center_config while parents can read only and cannot modify", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const staffDb = testEnv.authenticatedContext("staff_cfg_uid", { email: "staff@other.com", email_verified: true }).firestore();
  const momDb = testEnv.authenticatedContext(PARENT_A, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    staffDb.collection("admins").doc("staff_cfg_uid").set({
      userId: "staff_cfg_uid",
      credentialHash: BOOTSTRAP_ADMIN_HASH,
      createdAt: ts,
    })
  );

  const configPayload = {
    centerName: "مركز فكر المطور",
    centerSlogan: "ابنك معانا بطل",
    centerPhone: "01009998877",
    centerAddress: "مدينة نصر - شارع عباس العقاد",
    centerHours: "يومياً 10 ص - 10 م",
    centerLat: 30.0561,
    centerLng: 31.3301,
    welcomeNote: "أهلاً بكم في الفرع الجديد",
    bookButtonLabel: "احجز الآن",
    posterBannerTitle: "رؤيتنا: ابنك معانا بطل",
    showPosterBanner: true,
    services: [
      {
        id: "srv_speech",
        title: "تخاطب",
        subtitle: "تأهيل النطق",
        description: "جلسات فردية",
        sessionDurationMin: 45,
        feeEgp: 300,
        categoryTag: "أساسي",
      },
    ],
    specialists: [
      {
        id: "spec_ahmad",
        name: "أ / أحمد عوف",
        roleTitle: "مدير المركز",
        specialties: ["تخاطب"],
        experienceYears: 12,
      },
    ],
    packages: [
      {
        id: "pkg_1",
        title: "باقة 4 جلسات",
        priceEgp: 1000,
        savingText: "وفر 200 ج.م",
      },
    ],
    timeSlots: ["10:00 ص", "04:00 م"],
    customButtons: [
      {
        id: "btn_wa",
        label: "تواصل واتساب",
        actionType: "whatsapp",
        targetValue: "01009998877",
        style: "secondary",
      },
    ],
    updatedAt: ts,
  };

  await assertFails(unauth.collection("center_config").doc("main").get());
  await assertSucceeds(staffDb.collection("center_config").doc("main").set(configPayload));
  await assertSucceeds(momDb.collection("center_config").doc("main").get());
  await assertFails(
    momDb.collection("center_config").doc("main").update({
      centerPhone: "01234567890",
      updatedAt: pastDate(50),
    })
  );
});
