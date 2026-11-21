package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Appointment
import com.example.data.model.CaseFile
import com.example.data.model.CenterCatalog
import com.example.data.model.CenterConfig
import com.example.data.model.CenterPackageItem
import com.example.data.model.CenterServiceItem
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.CreatedClientCredentials
import com.example.data.model.CustomActionButton
import com.example.data.model.MonthlyEvaluation
import com.example.data.model.PaymentRecord
import com.example.data.model.SmartNotification
import com.example.data.model.SpecialistProfile
import com.example.data.model.StudentCase
import com.example.data.preferences.CenterSession
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class FekrRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "يرجى تفعيل الاتصال السحابي أولاً." }
        return uid
    }

    private fun <T> observeQuery(
        path: String,
        queryProvider: (String) -> com.google.firebase.firestore.Query,
        mapper: (List<DocumentSnapshot>) -> List<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        emitAll(
            queryProvider(uid)
                .snapshots()
                .map { snapshot -> mapper(snapshot.documents) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun loginWithUsernamePassword(
        expectedRole: String,
        username: String,
        password: String
    ): Result<CenterSession> {
        val cleanUser = CenterCatalog.normalizeUsername(username).take(50)
        val cleanPass = password.trim().take(50)
        if (cleanUser.length < 2 || cleanPass.length < 4) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم مستخدم وكلمة مرور صحيحين."))
        }
        val codeHash = CenterCatalog.sha256Hex(cleanUser, cleanPass)
        var path = "access_codes/$codeHash"
        return try {
            val uid = requireUserId()
            if (expectedRole == "admin") {
                var displayName = "إدارة مركز فكر"
                if (codeHash != CenterCatalog.BOOTSTRAP_ADMIN_HASH) {
                    val codeSnap = db.collection("access_codes").document(codeHash).get().await()
                    if (!codeSnap.exists()) {
                        return Result.failure(IllegalArgumentException("اسم المستخدم أو كلمة مرور الإدارة غير صحيحة."))
                    }
                    val docRole = codeSnap.getString("role").orEmpty()
                    val active = codeSnap.getBoolean("active") ?: false
                    if (docRole != "admin" || !active) {
                        return Result.failure(IllegalArgumentException("هذا الحساب غير مفعل لصلاحيات الإدارة."))
                    }
                    displayName = codeSnap.getString("displayName").orEmpty().ifBlank { "إدارة مركز فكر" }
                    db.collection("access_codes").document(codeHash).update(
                        mapOf(
                            "linkedUid" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                path = "admins/$uid"
                db.collection("admins").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "credentialHash" to codeHash,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                ensureUserProfile(uid, displayName, auth.currentUser?.email.orEmpty(), isAdmin = true)
                Result.success(
                    CenterSession(
                        isLoggedIn = true,
                        role = "admin",
                        username = cleanUser,
                        displayName = displayName,
                        studentId = ""
                    )
                )
            } else {
                val codeSnap = db.collection("access_codes").document(codeHash).get().await()
                if (!codeSnap.exists()) {
                    return Result.failure(IllegalArgumentException("اسم المستخدم أو كلمة المرور غير صحيحة."))
                }
                val docRole = codeSnap.getString("role").orEmpty()
                val active = codeSnap.getBoolean("active") ?: false
                val studentId = codeSnap.getString("studentId").orEmpty()
                val displayName = codeSnap.getString("displayName").orEmpty().ifBlank { "ولي أمر" }
                if (docRole != "parent" || !active || studentId.isBlank()) {
                    return Result.failure(IllegalArgumentException("بيانات الحساب غير مفعلة لحساب ولي أمر."))
                }

                path = "access_codes/$codeHash"
                db.collection("access_codes").document(codeHash).update(
                    mapOf(
                        "linkedUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                path = "students/$studentId"
                db.collection("students").document(studentId).update(
                    mapOf(
                        "parentUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                val stuSnap = db.collection("students").document(studentId).get().await()
                val pendingRefs = (stuSnap.get("pendingSyncRefs") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                if (pendingRefs.isNotEmpty()) {
                    for (refPath in pendingRefs) {
                        runCatching {
                            db.document(refPath).update(
                                mapOf(
                                    "parentUid" to uid,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                    }
                    db.collection("students").document(studentId).update(
                        mapOf(
                            "pendingSyncRefs" to emptyList<String>(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                ensureUserProfile(uid, displayName, auth.currentUser?.email.orEmpty(), isAdmin = false)
                Result.success(
                    CenterSession(
                        isLoggedIn = true,
                        role = "parent",
                        username = cleanUser,
                        displayName = displayName,
                        studentId = studentId
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun registerParentWithCredentials(
        parentName: String,
        childName: String,
        ageYears: Int,
        parentPhone: String,
        primaryService: String,
        specialistName: String,
        username: String,
        password: String,
        goals: String
    ): Result<CenterSession> {
        var path = "students"
        return try {
            val uid = requireUserId()
            val cleanUser = CenterCatalog.normalizeUsername(username).take(50)
            val cleanPass = password.trim().take(50)
            val cleanChild = childName.trim().take(80)
            val cleanParent = parentName.trim().ifBlank { "ولي أمر" }.take(80)
            require(cleanUser.length >= 3) { "اسم المستخدم يجب أن لا يقل عن 3 أحرف." }
            require(cleanPass.length >= 4) { "كلمة المرور يجب أن لا تقل عن 4 أحرف أو أرقام." }
            require(cleanChild.isNotEmpty()) { "يرجى إدخال اسم الطفل." }

            val codeHash = CenterCatalog.sha256Hex(cleanUser, cleanPass)
            val stuRef = db.collection("students").document()
            path = stuRef.path
            stuRef.set(
                mapOf(
                    "parentUid" to uid,
                    "parentName" to cleanParent,
                    "parentPhone" to parentPhone.trim().take(30),
                    "childName" to cleanChild,
                    "ageYears" to ageYears.coerceIn(1, 25).toLong(),
                    "primaryService" to primaryService.trim().ifBlank { "تخاطب" }.take(60),
                    "specialistName" to specialistName.trim().ifBlank { "أ / أحمد عوف" }.take(80),
                    "caseStatus" to "active",
                    "progressPercent" to 15L,
                    "totalSessions" to 0L,
                    "attendedSessions" to 0L,
                    "clinicalNotes" to "ملف مسجل - جاهز لبدء الجلسات والمتابعة",
                    "currentGoals" to goals.trim().ifBlank { "تقييم مبدئي وبناء الخطة العلاجية" }.take(500),
                    "clientUsername" to cleanUser,
                    "clientPassword" to cleanPass,
                    "credentialHash" to codeHash,
                    "pendingSyncRefs" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            path = "access_codes/$codeHash"
            db.collection("access_codes").document(codeHash).set(
                mapOf(
                    "username" to cleanUser,
                    "role" to "parent",
                    "studentId" to stuRef.id,
                    "displayName" to cleanParent,
                    "linkedUid" to uid,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            ensureUserProfile(uid, cleanParent, auth.currentUser?.email.orEmpty(), isAdmin = false)
            Result.success(
                CenterSession(
                    isLoggedIn = true,
                    role = "parent",
                    username = cleanUser,
                    displayName = cleanParent,
                    studentId = stuRef.id
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createAdminAccount(
        username: String,
        password: String,
        displayName: String
    ): Result<Unit> {
        var path = "access_codes"
        return try {
            requireUserId()
            val cleanUser = CenterCatalog.normalizeUsername(username).take(50)
            val cleanPass = password.trim().take(50)
            val cleanName = displayName.trim().ifBlank { "أخصائي / إدارة" }.take(80)
            require(cleanUser.length >= 3) { "اسم المستخدم يجب أن لا يقل عن 3 أحرف." }
            require(cleanPass.length >= 4) { "كلمة المرور يجب أن لا تقل عن 4 أحرف أو أرقام." }
            val codeHash = CenterCatalog.sha256Hex(cleanUser, cleanPass)
            path = "access_codes/$codeHash"
            db.collection("access_codes").document(codeHash).set(
                mapOf(
                    "username" to cleanUser,
                    "role" to "admin",
                    "studentId" to "",
                    "displayName" to cleanName,
                    "linkedUid" to "",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String,
        isAdmin: Boolean
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            requireUserId()
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "ولي أمر" }.take(80)
            val safeEmail = email.trim().lowercase().take(120)
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "phone" to "",
                    "role" to if (isAdmin) "admin" else "parent",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeStudents(
        parentUid: String,
        isAdminSession: Boolean,
        activeStudentId: String
    ): Flow<List<StudentCase>> =
        observeQuery(
            path = "students",
            queryProvider = {
                if (isAdminSession) db.collection("students")
                else db.collection("students").whereEqualTo("parentUid", parentUid)
            },
            mapper = { docs ->
                val list = docs.mapNotNull {
                    it.toObject(StudentCase::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                if (!isAdminSession && activeStudentId.isNotBlank()) {
                    list.filter { it.id == activeStudentId || it.parentUid == parentUid }
                } else {
                    list
                }
            }
        )

    fun observeAppointments(
        parentUid: String,
        isAdminSession: Boolean,
        activeStudentId: String
    ): Flow<List<Appointment>> =
        observeQuery(
            path = "appointments",
            queryProvider = {
                if (isAdminSession) db.collection("appointments")
                else db.collection("appointments").whereEqualTo("parentUid", parentUid)
            },
            mapper = { docs ->
                val list = docs.mapNotNull {
                    it.toObject(Appointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                if (!isAdminSession && activeStudentId.isNotBlank()) {
                    list.filter { it.studentId == activeStudentId || it.parentUid == parentUid }
                } else {
                    list
                }
            }
        )

    fun observeEvaluations(
        parentUid: String,
        isAdminSession: Boolean,
        activeStudentId: String
    ): Flow<List<MonthlyEvaluation>> =
        observeQuery(
            path = "evaluations",
            queryProvider = {
                if (isAdminSession) db.collection("evaluations")
                else db.collection("evaluations").whereEqualTo("parentUid", parentUid)
            },
            mapper = { docs ->
                val list = docs.mapNotNull {
                    it.toObject(MonthlyEvaluation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                if (!isAdminSession && activeStudentId.isNotBlank()) {
                    list.filter { it.studentId == activeStudentId || it.parentUid == parentUid }
                } else {
                    list
                }
            }
        )

    fun observePayments(parentUid: String, isAdminSession: Boolean): Flow<List<PaymentRecord>> =
        observeQuery(
            path = "payments",
            queryProvider = {
                if (isAdminSession) db.collection("payments")
                else db.collection("payments").whereEqualTo("parentUid", parentUid)
            },
            mapper = { docs ->
                docs.mapNotNull {
                    it.toObject(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
        )

    fun observeChats(parentUid: String, isAdminSession: Boolean): Flow<List<ChatThread>> =
        observeQuery(
            path = "chats",
            queryProvider = {
                if (isAdminSession) db.collection("chats")
                else db.collection("chats").whereEqualTo("parentUid", parentUid)
            },
            mapper = { docs ->
                docs.mapNotNull {
                    it.toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
        )

    fun observeChatMessages(chatId: String, parentUid: String, isAdminSession: Boolean): Flow<List<ChatMessage>> =
        observeQuery(
            path = "chats/$chatId/messages",
            queryProvider = {
                val col = db.collection("chats").document(chatId).collection("messages")
                if (isAdminSession) col else col.whereEqualTo("parentUid", parentUid)
            },
            mapper = { docs ->
                docs.mapNotNull {
                    it.toObject(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            }
        )

    fun observeNotifications(parentUid: String): Flow<List<SmartNotification>> =
        observeQuery(
            path = "notifications",
            queryProvider = { db.collection("notifications").whereEqualTo("parentUid", parentUid) },
            mapper = { docs ->
                docs.mapNotNull {
                    it.toObject(SmartNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
        )

    fun observeCaseFiles(
        parentUid: String,
        isAdminSession: Boolean,
        activeStudentId: String
    ): Flow<List<CaseFile>> =
        observeQuery(
            path = "case_files",
            queryProvider = {
                if (isAdminSession) db.collection("case_files")
                else db.collection("case_files").whereEqualTo("parentUid", parentUid)
            },
            mapper = { docs ->
                val list = docs.mapNotNull {
                    it.toObject(CaseFile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                if (!isAdminSession && activeStudentId.isNotBlank()) {
                    list.filter { it.studentId == activeStudentId || it.parentUid == parentUid }
                } else {
                    list
                }
            }
        )

    suspend fun createStudent(
        parentUid: String,
        parentName: String,
        parentPhone: String,
        childName: String,
        ageYears: Int,
        primaryService: String,
        specialistName: String,
        currentGoals: String
    ): Result<String> {
        var path = "students"
        return try {
            requireUserId()
            val cleanChild = childName.trim().take(80)
            require(cleanChild.isNotEmpty()) { "يرجى إدخال اسم الطفل" }
            require(ageYears in 1..25) { "العمر يجب أن يكون بين 1 و 25 سنة" }
            val docRef = db.collection("students").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "parentUid" to parentUid,
                    "parentName" to parentName.trim().ifBlank { "ولي أمر" }.take(80),
                    "parentPhone" to parentPhone.trim().take(30),
                    "childName" to cleanChild,
                    "ageYears" to ageYears.toLong(),
                    "primaryService" to primaryService.trim().ifBlank { "تخاطب" }.take(60),
                    "specialistName" to specialistName.trim().ifBlank { "أ / أحمد عوف" }.take(80),
                    "caseStatus" to "active",
                    "progressPercent" to 15L,
                    "totalSessions" to 0L,
                    "attendedSessions" to 0L,
                    "clinicalNotes" to "ملف مسجل - جاهز لبدء الجلسات والمتابعة",
                    "currentGoals" to currentGoals.trim().ifBlank { "تقييم مبدئي وبناء الخطة العلاجية" }.take(500),
                    "clientUsername" to "",
                    "clientPassword" to "",
                    "credentialHash" to "",
                    "pendingSyncRefs" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createStudentWithAutoCredentialsAdmin(
        childName: String,
        ageYears: Int,
        parentName: String,
        parentPhone: String,
        primaryService: String,
        specialistName: String,
        currentGoals: String,
        customUsername: String = "",
        customPassword: String = ""
    ): Result<CreatedClientCredentials> {
        var path = "students"
        return try {
            requireUserId()
            val cleanChild = childName.trim().take(80)
            val cleanParent = parentName.trim().ifBlank { "ولي أمر" }.take(80)
            require(cleanChild.isNotEmpty()) { "يرجى إدخال اسم الطفل" }
            require(ageYears in 1..25) { "العمر يجب أن يكون بين 1 و 25 سنة" }

            val username = CenterCatalog.normalizeUsername(
                customUsername.ifBlank { CenterCatalog.generateClientUsername() }
            ).take(50)
            val password = customPassword.trim().ifBlank { CenterCatalog.generateClientPassword() }.take(50)
            require(username.length >= 3) { "اسم المستخدم يجب أن لا يقل عن 3 أحرف" }
            require(password.length >= 4) { "كلمة المرور يجب أن لا تقل عن 4 أحرف أو أرقام" }

            val codeHash = CenterCatalog.sha256Hex(username, password)
            val stuRef = db.collection("students").document()
            path = stuRef.path
            stuRef.set(
                mapOf(
                    "parentUid" to "pending_link",
                    "parentName" to cleanParent,
                    "parentPhone" to parentPhone.trim().take(30),
                    "childName" to cleanChild,
                    "ageYears" to ageYears.toLong(),
                    "primaryService" to primaryService.trim().ifBlank { "تخاطب" }.take(60),
                    "specialistName" to specialistName.trim().ifBlank { "أ / أحمد عوف" }.take(80),
                    "caseStatus" to "active",
                    "progressPercent" to 15L,
                    "totalSessions" to 0L,
                    "attendedSessions" to 0L,
                    "clinicalNotes" to "تم تسجيل الملف وتوليد بيانات الدخول لولي الأمر",
                    "currentGoals" to currentGoals.trim().ifBlank { "تقييم مبدئي وبناء الخطة العلاجية" }.take(500),
                    "clientUsername" to username,
                    "clientPassword" to password,
                    "credentialHash" to codeHash,
                    "pendingSyncRefs" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            path = "access_codes/$codeHash"
            db.collection("access_codes").document(codeHash).set(
                mapOf(
                    "username" to username,
                    "role" to "parent",
                    "studentId" to stuRef.id,
                    "displayName" to cleanParent,
                    "linkedUid" to "",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                CreatedClientCredentials(
                    studentId = stuRef.id,
                    childName = cleanChild,
                    parentName = cleanParent,
                    username = username,
                    password = password
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentAdmin(
        studentId: String,
        caseStatus: String,
        progressPercent: Int,
        specialistName: String,
        clinicalNotes: String,
        currentGoals: String
    ): Result<Unit> {
        val path = "students/$studentId"
        return try {
            requireUserId()
            require(studentId.isNotBlank())
            val safeStatus = if (caseStatus in listOf("active", "assessment", "completed", "paused")) caseStatus else "active"
            val safeProgress = progressPercent.coerceIn(0, 100).toLong()
            db.collection("students").document(studentId).update(
                mapOf(
                    "caseStatus" to safeStatus,
                    "progressPercent" to safeProgress,
                    "specialistName" to specialistName.trim().take(80),
                    "clinicalNotes" to clinicalNotes.trim().take(1000),
                    "currentGoals" to currentGoals.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun bookAppointment(
        student: StudentCase,
        serviceName: String,
        feeAmount: Int,
        specialistName: String,
        dateLabel: String,
        timeSlot: String,
        scheduledEpochMs: Long,
        notes: String,
        reminderMinutesBefore: Int
    ): Result<String> {
        var path = "appointments"
        return try {
            val uid = requireUserId()
            require(student.id.isNotBlank()) { "يرجى اختيار ملف الطفل أولاً" }
            val apptRef = db.collection("appointments").document()
            path = apptRef.path
            val notifRef = db.collection("notifications").document()
            val targetParentUid = student.parentUid.ifBlank { uid }
            val batch = db.batch()
            val apptPayload = mapOf(
                "parentUid" to targetParentUid,
                "studentId" to student.id,
                "childName" to student.childName.take(80),
                "serviceName" to serviceName.trim().take(60),
                "specialistName" to specialistName.trim().take(80),
                "dateLabel" to dateLabel.trim().take(40),
                "timeSlot" to timeSlot.trim().take(30),
                "scheduledEpochMs" to scheduledEpochMs.coerceAtLeast(0L),
                "status" to "pending",
                "paymentStatus" to "unpaid",
                "feeAmount" to feeAmount.coerceIn(0, 10000).toLong(),
                "notes" to notes.trim().take(500),
                "reminderMinutesBefore" to reminderMinutesBefore.coerceIn(0, 1440).toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(apptRef, apptPayload)
            batch.set(
                notifRef,
                mapOf(
                    "parentUid" to targetParentUid,
                    "studentId" to student.id,
                    "title" to "تذكير ذكي مجدول: جلسة ${serviceName.take(30)}",
                    "body" to "موعد ${student.childName} يوم $dateLabel الساعة $timeSlot مع $specialistName. سنذكرك قبل الجلسة بـ $reminderMinutesBefore دقيقة.",
                    "category" to "reminder",
                    "read" to false,
                    "scheduledForLabel" to "$dateLabel • $timeSlot",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (targetParentUid == "pending_link") {
                batch.update(
                    db.collection("students").document(student.id),
                    mapOf(
                        "pendingSyncRefs" to FieldValue.arrayUnion(apptRef.path, notifRef.path),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(apptRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAppointmentStatusAdmin(
        appointment: Appointment,
        newStatus: String
    ): Result<Unit> {
        val path = "appointments/${appointment.id}"
        return try {
            requireUserId()
            require(newStatus in listOf("pending", "confirmed", "completed", "absent", "cancelled"))
            val batch = db.batch()
            val apptRef = db.collection("appointments").document(appointment.id)
            batch.update(
                apptRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (appointment.studentId.isNotBlank() && appointment.status != newStatus) {
                val stuRef = db.collection("students").document(appointment.studentId)
                if (newStatus == "completed" && appointment.status != "completed") {
                    batch.update(
                        stuRef,
                        mapOf(
                            "totalSessions" to FieldValue.increment(1),
                            "attendedSessions" to FieldValue.increment(1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (newStatus == "absent" && appointment.status != "absent") {
                    batch.update(
                        stuRef,
                        mapOf(
                            "totalSessions" to FieldValue.increment(1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelAppointmentParent(appointmentId: String): Result<Unit> {
        val path = "appointments/$appointmentId"
        return try {
            requireUserId()
            db.collection("appointments").document(appointmentId).update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun payForSessionOrPackage(
        parentUid: String,
        appointment: Appointment?,
        childName: String,
        description: String,
        amountEgp: Int,
        method: String
    ): Result<String> {
        var path = "payments"
        return try {
            requireUserId()
            require(amountEgp in 1..50000) { "المبلغ غير صالح" }
            require(method in listOf("card", "vodafone_cash", "instapay", "center_cash"))
            val payRef = db.collection("payments").document()
            path = payRef.path
            val refCode = "FKR-${Random.nextInt(100000, 999999)}"
            val batch = db.batch()
            batch.set(
                payRef,
                mapOf(
                    "parentUid" to parentUid,
                    "appointmentId" to (appointment?.id ?: "package"),
                    "childName" to childName.trim().take(80),
                    "description" to description.trim().take(150),
                    "amountEgp" to amountEgp.toLong(),
                    "method" to method,
                    "referenceCode" to refCode,
                    "status" to "completed",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (appointment != null && appointment.paymentStatus == "unpaid") {
                val apptRef = db.collection("appointments").document(appointment.id)
                batch.update(
                    apptRef,
                    mapOf(
                        "paymentStatus" to "paid",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val notifRef = db.collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "parentUid" to parentUid,
                    "studentId" to (appointment?.studentId ?: ""),
                    "title" to "تم سداد الرسوم بنجاح ($refCode)",
                    "body" to "تم استلام مبلغ $amountEgp ج.م لـ $description الخاص بالطفل $childName.",
                    "category" to "payment",
                    "read" to false,
                    "scheduledForLabel" to "إيصال إلكتروني",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(refCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishMonthlyEvaluation(
        student: StudentCase,
        monthLabel: String,
        specialistName: String,
        speechScore: Int,
        attentionScore: Int,
        behaviorScore: Int,
        cognitiveScore: Int,
        motorScore: Int,
        summary: String,
        homeRecommendations: String
    ): Result<Unit> {
        var path = "evaluations"
        return try {
            requireUserId()
            val s1 = speechScore.coerceIn(0, 100)
            val s2 = attentionScore.coerceIn(0, 100)
            val s3 = behaviorScore.coerceIn(0, 100)
            val s4 = cognitiveScore.coerceIn(0, 100)
            val s5 = motorScore.coerceIn(0, 100)
            val overall = (s1 + s2 + s3 + s4 + s5) / 5
            val evalRef = db.collection("evaluations").document()
            path = evalRef.path
            val notifRef = db.collection("notifications").document()
            val batch = db.batch()
            batch.set(
                evalRef,
                mapOf(
                    "studentId" to student.id,
                    "parentUid" to student.parentUid,
                    "childName" to student.childName.take(80),
                    "monthLabel" to monthLabel.trim().take(40),
                    "specialistName" to specialistName.trim().take(80),
                    "speechScore" to s1.toLong(),
                    "attentionScore" to s2.toLong(),
                    "behaviorScore" to s3.toLong(),
                    "cognitiveScore" to s4.toLong(),
                    "motorScore" to s5.toLong(),
                    "overallScore" to overall.toLong(),
                    "summary" to summary.trim().ifBlank { "تقييم شهري دوري لتقدم الطفل" }.take(1000),
                    "homeRecommendations" to homeRecommendations.trim().take(1000),
                    "parentAcknowledged" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "parentUid" to student.parentUid,
                    "studentId" to student.id,
                    "title" to "صدور التقييم الشهري: ${student.childName}",
                    "body" to "تقييم شهر $monthLabel جاهز للاطلاع (المتوسط العام $overall%).",
                    "category" to "evaluation",
                    "read" to false,
                    "scheduledForLabel" to monthLabel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (student.parentUid == "pending_link") {
                batch.update(
                    db.collection("students").document(student.id),
                    mapOf(
                        "progressPercent" to overall.toLong(),
                        "pendingSyncRefs" to FieldValue.arrayUnion(evalRef.path, notifRef.path),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    db.collection("students").document(student.id),
                    mapOf(
                        "progressPercent" to overall.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun acknowledgeEvaluation(evaluationId: String): Result<Unit> {
        val path = "evaluations/$evaluationId"
        return try {
            requireUserId()
            db.collection("evaluations").document(evaluationId).update(
                mapOf(
                    "parentAcknowledged" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        chatId: String,
        parentUid: String,
        parentName: String,
        specialistId: String,
        specialistName: String,
        senderName: String,
        senderRole: String,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.success(Unit)
        val chatRef = db.collection("chats").document(chatId)
        val path = "chats/$chatId/messages"
        return try {
            val uid = requireUserId()
            val chatSnap = chatRef.get().await()
            if (!chatSnap.exists()) {
                chatRef.set(
                    mapOf(
                        "parentUid" to parentUid,
                        "parentName" to parentName.trim().ifBlank { "ولي أمر" }.take(80),
                        "specialistId" to specialistId.take(60),
                        "specialistName" to specialistName.take(80),
                        "lastMessage" to cleanText.take(500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                chatRef.update(
                    mapOf(
                        "lastMessage" to cleanText.take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            chatRef.collection("messages").document().set(
                mapOf(
                    "parentUid" to parentUid,
                    "senderUid" to uid,
                    "senderName" to senderName.trim().ifBlank { "مستخدم" }.take(80),
                    "senderRole" to senderRole,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId).update(
                mapOf(
                    "read" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createSmartReminderLog(
        parentUid: String,
        studentId: String,
        title: String,
        body: String,
        scheduledForLabel: String
    ): Result<Unit> {
        var path = "notifications"
        return try {
            requireUserId()
            val ref = db.collection("notifications").document()
            path = ref.path
            ref.set(
                mapOf(
                    "parentUid" to parentUid,
                    "studentId" to studentId,
                    "title" to title.trim().take(120),
                    "body" to body.trim().take(400),
                    "category" to "reminder",
                    "read" to false,
                    "scheduledForLabel" to scheduledForLabel.trim().take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCaseFileAdmin(
        student: StudentCase,
        title: String,
        category: String,
        notes: String,
        fileName: String,
        mimeType: String,
        fileSizeBytes: Long,
        contentBase64: String,
        externalUrl: String,
        uploadedByName: String
    ): Result<String> {
        var path = "case_files"
        return try {
            requireUserId()
            require(student.id.isNotBlank()) { "يرجى اختيار الحالة أولاً" }
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "يرجى إدخال عنوان الملف" }
            val cleanBase64 = contentBase64.trim()
            val cleanUrl = externalUrl.trim().take(500)
            require(cleanBase64.isNotEmpty() || cleanUrl.isNotEmpty()) {
                "يرجى اختيار ملف من الجهاز أو إضافة رابط للملف"
            }
            require(cleanBase64.length <= 900_000) {
                "حجم الملف يتجاوز الحد المسموح (600 كيلوبايت)، يرجى استخدام رابط خارجي."
            }
            val safeCategory = if (category in listOf("report", "plan", "assessment", "worksheet", "other")) category else "other"
            val safeName = fileName.trim().ifBlank { "attachment" }.take(120)
            val safeMime = mimeType.trim().ifBlank { if (cleanUrl.isNotEmpty()) "text/uri-list" else "application/octet-stream" }.take(100)
            val safeUploader = uploadedByName.trim().ifBlank { student.specialistName.ifBlank { "إدارة مركز فكر" } }.take(80)

            val fileRef = db.collection("case_files").document()
            path = fileRef.path
            val notifRef = db.collection("notifications").document()
            val batch = db.batch()
            batch.set(
                fileRef,
                mapOf(
                    "studentId" to student.id,
                    "parentUid" to student.parentUid.ifBlank { "pending_link" },
                    "childName" to student.childName.take(80),
                    "title" to cleanTitle,
                    "category" to safeCategory,
                    "notes" to notes.trim().take(500),
                    "fileName" to safeName,
                    "mimeType" to safeMime,
                    "fileSizeBytes" to fileSizeBytes.coerceIn(0L, 20_000_000L),
                    "contentBase64" to cleanBase64,
                    "externalUrl" to cleanUrl,
                    "uploadedByName" to safeUploader,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "parentUid" to student.parentUid.ifBlank { "pending_link" },
                    "studentId" to student.id,
                    "title" to "ملف جديد لحالة ${student.childName}",
                    "body" to "تم إرفاق «$cleanTitle» (${CenterCatalog.caseFileCategoryLabel(safeCategory)}) في ملف الحالة للعرض.",
                    "category" to "general",
                    "read" to false,
                    "scheduledForLabel" to "ملفات الحالة",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (student.parentUid.isBlank() || student.parentUid == "pending_link") {
                batch.update(
                    db.collection("students").document(student.id),
                    mapOf(
                        "pendingSyncRefs" to FieldValue.arrayUnion(fileRef.path, notifRef.path),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(fileRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCaseFileAdmin(fileId: String): Result<Unit> {
        val path = "case_files/$fileId"
        return try {
            requireUserId()
            require(fileId.isNotBlank())
            db.collection("case_files").document(fileId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeCenterConfig(): Flow<CenterConfig> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("center_config").document("main")
                .snapshots()
                .map { snapshot -> snapshot.toCenterConfigOrDefault() }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, "center_config/main")
                    }
                    emit(CenterCatalog.defaultConfig())
                }
        )
    }

    suspend fun saveCenterConfigAdmin(config: CenterConfig): Result<Unit> {
        val path = "center_config/main"
        return try {
            requireUserId()
            val cleanName = config.centerName.trim().ifBlank { CenterCatalog.CENTER_NAME }.take(120)
            val cleanSlogan = config.centerSlogan.trim().take(120)
            val cleanPhone = config.centerPhone.trim().ifBlank { CenterCatalog.CENTER_PHONE }.take(30)
            val cleanAddress = config.centerAddress.trim().ifBlank { CenterCatalog.CENTER_ADDRESS }.take(200)
            val cleanHours = config.centerHours.trim().take(150)
            val cleanWelcome = config.welcomeNote.trim().take(400)
            val cleanBookLabel = config.bookButtonLabel.trim().ifBlank { "احجز جلسة" }.take(60)
            val cleanPosterTitle = config.posterBannerTitle.trim().take(120)

            val servicesList = config.services.take(30).mapIndexed { index, s ->
                mapOf(
                    "id" to s.id.trim().ifBlank { "srv_$index" }.take(60),
                    "title" to s.title.trim().ifBlank { "خدمة" }.take(60),
                    "subtitle" to s.subtitle.trim().take(120),
                    "description" to s.description.trim().take(400),
                    "sessionDurationMin" to s.sessionDurationMin.coerceIn(10, 240).toLong(),
                    "feeEgp" to s.feeEgp.coerceIn(0, 50000).toLong(),
                    "categoryTag" to s.categoryTag.trim().ifBlank { "عام" }.take(40),
                    "imageKey" to s.imageKey.trim().take(40)
                )
            }
            val specialistsList = config.specialists.take(30).mapIndexed { index, sp ->
                mapOf(
                    "id" to sp.id.trim().ifBlank { "spec_$index" }.take(60),
                    "name" to sp.name.trim().ifBlank { "أخصائي" }.take(80),
                    "roleTitle" to sp.roleTitle.trim().take(120),
                    "specialties" to sp.specialties.map { it.trim().take(50) }.filter { it.isNotEmpty() }.take(10),
                    "experienceYears" to sp.experienceYears.coerceIn(0, 60).toLong()
                )
            }
            val packagesList = config.packages.take(20).mapIndexed { index, pkg ->
                mapOf(
                    "id" to pkg.id.trim().ifBlank { "pkg_$index" }.take(60),
                    "title" to pkg.title.trim().ifBlank { "باقة جلسات" }.take(80),
                    "priceEgp" to pkg.priceEgp.coerceIn(1, 50000).toLong(),
                    "savingText" to pkg.savingText.trim().take(60)
                )
            }
            val slotsList = config.timeSlots
                .map { it.trim().take(30) }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(30)
            val buttonsList = config.customButtons.take(25).mapIndexed { index, btn ->
                val validType = if (btn.actionType in listOf("url", "phone", "whatsapp", "book", "chat", "payments", "map")) {
                    btn.actionType
                } else {
                    "url"
                }
                val validStyle = if (btn.style in listOf("primary", "secondary", "outlined")) btn.style else "secondary"
                mapOf(
                    "id" to btn.id.trim().ifBlank { "btn_$index" }.take(60),
                    "label" to btn.label.trim().ifBlank { "زر مخصص" }.take(60),
                    "actionType" to validType,
                    "targetValue" to btn.targetValue.trim().take(400),
                    "style" to validStyle
                )
            }

            val payload = mapOf(
                "centerName" to cleanName,
                "centerSlogan" to cleanSlogan,
                "centerPhone" to cleanPhone,
                "centerAddress" to cleanAddress,
                "centerHours" to cleanHours,
                "centerLat" to config.centerLat,
                "centerLng" to config.centerLng,
                "welcomeNote" to cleanWelcome,
                "bookButtonLabel" to cleanBookLabel,
                "posterBannerTitle" to cleanPosterTitle,
                "showPosterBanner" to config.showPosterBanner,
                "services" to servicesList,
                "specialists" to specialistsList,
                "packages" to packagesList,
                "timeSlots" to slotsList,
                "customButtons" to buttonsList,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("center_config").document("main").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun resetCenterConfigAdmin(): Result<Unit> =
        saveCenterConfigAdmin(CenterCatalog.defaultConfig())

    private fun DocumentSnapshot.toCenterConfigOrDefault(): CenterConfig {
        if (!exists()) return CenterCatalog.defaultConfig()
        val def = CenterCatalog.defaultConfig()

        val servicesRaw = (get("services") as? List<*>)?.mapIndexedNotNull { idx, item ->
            val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val title = (m["title"] as? String)?.trim().orEmpty()
            if (title.isBlank()) return@mapIndexedNotNull null
            CenterServiceItem(
                id = (m["id"] as? String)?.ifBlank { "srv_$idx" } ?: "srv_$idx",
                title = title,
                subtitle = (m["subtitle"] as? String).orEmpty(),
                description = (m["description"] as? String).orEmpty(),
                sessionDurationMin = (m["sessionDurationMin"] as? Number)?.toInt() ?: 45,
                feeEgp = (m["feeEgp"] as? Number)?.toInt() ?: 0,
                categoryTag = (m["categoryTag"] as? String)?.ifBlank { "عام" } ?: "عام",
                imageKey = (m["imageKey"] as? String).orEmpty()
            )
        }

        val specialistsRaw = (get("specialists") as? List<*>)?.mapIndexedNotNull { idx, item ->
            val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val name = (m["name"] as? String)?.trim().orEmpty()
            if (name.isBlank()) return@mapIndexedNotNull null
            val specs = (m["specialties"] as? List<*>)?.filterIsInstance<String>().orEmpty()
            SpecialistProfile(
                id = (m["id"] as? String)?.ifBlank { "spec_$idx" } ?: "spec_$idx",
                name = name,
                roleTitle = (m["roleTitle"] as? String).orEmpty(),
                specialties = specs,
                experienceYears = (m["experienceYears"] as? Number)?.toInt() ?: 5
            )
        }

        val packagesRaw = (get("packages") as? List<*>)?.mapIndexedNotNull { idx, item ->
            val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val title = (m["title"] as? String)?.trim().orEmpty()
            if (title.isBlank()) return@mapIndexedNotNull null
            CenterPackageItem(
                id = (m["id"] as? String)?.ifBlank { "pkg_$idx" } ?: "pkg_$idx",
                title = title,
                priceEgp = (m["priceEgp"] as? Number)?.toInt() ?: 500,
                savingText = (m["savingText"] as? String).orEmpty()
            )
        }

        val slotsRaw = (get("timeSlots") as? List<*>)
            ?.filterIsInstance<String>()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }

        val buttonsRaw = (get("customButtons") as? List<*>)?.mapIndexedNotNull { idx, item ->
            val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val label = (m["label"] as? String)?.trim().orEmpty()
            if (label.isBlank()) return@mapIndexedNotNull null
            CustomActionButton(
                id = (m["id"] as? String)?.ifBlank { "btn_$idx" } ?: "btn_$idx",
                label = label,
                actionType = (m["actionType"] as? String)?.ifBlank { "url" } ?: "url",
                targetValue = (m["targetValue"] as? String).orEmpty(),
                style = (m["style"] as? String)?.ifBlank { "secondary" } ?: "secondary"
            )
        }

        return CenterConfig(
            centerName = getString("centerName")?.ifBlank { def.centerName } ?: def.centerName,
            centerSlogan = getString("centerSlogan") ?: def.centerSlogan,
            centerPhone = getString("centerPhone")?.ifBlank { def.centerPhone } ?: def.centerPhone,
            centerAddress = getString("centerAddress")?.ifBlank { def.centerAddress } ?: def.centerAddress,
            centerHours = getString("centerHours") ?: def.centerHours,
            centerLat = (get("centerLat") as? Number)?.toDouble() ?: def.centerLat,
            centerLng = (get("centerLng") as? Number)?.toDouble() ?: def.centerLng,
            welcomeNote = getString("welcomeNote") ?: def.welcomeNote,
            bookButtonLabel = getString("bookButtonLabel")?.ifBlank { def.bookButtonLabel } ?: def.bookButtonLabel,
            posterBannerTitle = getString("posterBannerTitle") ?: def.posterBannerTitle,
            showPosterBanner = getBoolean("showPosterBanner") ?: def.showPosterBanner,
            services = if (servicesRaw != null && servicesRaw.isNotEmpty()) servicesRaw else def.services,
            specialists = if (specialistsRaw != null && specialistsRaw.isNotEmpty()) specialistsRaw else def.specialists,
            packages = packagesRaw ?: def.packages,
            timeSlots = if (slotsRaw != null && slotsRaw.isNotEmpty()) slotsRaw else def.timeSlots,
            customButtons = buttonsRaw ?: def.customButtons
        )
    }

    suspend fun sendFeeNoticeAdmin(
        student: StudentCase,
        existingAppointment: Appointment?,
        description: String,
        amountEgp: Int,
        notes: String
    ): Result<Unit> {
        var path = "appointments"
        return try {
            requireUserId()
            require(student.id.isNotBlank()) { "يرجى اختيار الحالة أولاً" }
            val safeAmount = amountEgp.coerceIn(1, 10000)
            val cleanDesc = description.trim().ifBlank { student.primaryService.ifBlank { "رسوم جلسات" } }.take(60)
            val cleanNotes = notes.trim().take(500)
            val targetParentUid = student.parentUid.ifBlank { "pending_link" }

            val batch = db.batch()
            val notifRef = db.collection("notifications").document()
            val targetApptPath: String

            if (existingAppointment != null) {
                val apptRef = db.collection("appointments").document(existingAppointment.id)
                path = apptRef.path
                targetApptPath = apptRef.path
                batch.update(
                    apptRef,
                    mapOf(
                        "feeAmount" to safeAmount.toLong(),
                        "paymentStatus" to "unpaid",
                        "notes" to cleanNotes.ifBlank { existingAppointment.notes }.take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val apptRef = db.collection("appointments").document()
                path = apptRef.path
                targetApptPath = apptRef.path
                val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                batch.set(
                    apptRef,
                    mapOf(
                        "parentUid" to targetParentUid,
                        "studentId" to student.id,
                        "childName" to student.childName.take(80),
                        "serviceName" to cleanDesc,
                        "specialistName" to student.specialistName.ifBlank { "إدارة المركز" }.take(80),
                        "dateLabel" to dateLabel,
                        "timeSlot" to "مطالبة مالية",
                        "scheduledEpochMs" to System.currentTimeMillis(),
                        "status" to "confirmed",
                        "paymentStatus" to "unpaid",
                        "feeAmount" to safeAmount.toLong(),
                        "notes" to cleanNotes,
                        "reminderMinutesBefore" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                notifRef,
                mapOf(
                    "parentUid" to targetParentUid,
                    "studentId" to student.id,
                    "title" to "إشعار بسعر ومستحقات: $safeAmount ج.م",
                    "body" to "أرسلت إدارة المركز تفاصيل الرسوم لـ ${student.childName}: $cleanDesc بمبلغ $safeAmount ج.م.${if (cleanNotes.isNotBlank()) " ملاحظة: $cleanNotes" else ""}",
                    "category" to "payment",
                    "read" to false,
                    "scheduledForLabel" to "$safeAmount ج.م",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (targetParentUid == "pending_link") {
                val refsToSync = if (existingAppointment == null) {
                    arrayOf(targetApptPath, notifRef.path)
                } else {
                    arrayOf(notifRef.path)
                }
                batch.update(
                    db.collection("students").document(student.id),
                    mapOf(
                        "pendingSyncRefs" to FieldValue.arrayUnion(*refsToSync),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
