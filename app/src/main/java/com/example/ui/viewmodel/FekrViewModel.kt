package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Appointment
import com.example.data.model.CaseFile
import com.example.data.model.CenterCatalog
import com.example.data.model.CenterConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.CreatedClientCredentials
import com.example.data.model.MonthlyEvaluation
import com.example.data.model.PaymentRecord
import com.example.data.model.SmartNotification
import com.example.data.model.StudentCase
import com.example.data.model.UiState
import com.example.data.preferences.ReminderPreferences
import com.example.data.preferences.ReminderSettings
import com.example.data.repository.FekrRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FekrViewModel(
    private val repository: FekrRepository,
    private val reminderPreferences: ReminderPreferences,
    val currentUserId: String,
    val userDisplayName: String,
    val isAdminSession: Boolean,
    val activeStudentId: String
) : ViewModel() {

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _lastCreatedCredentials = MutableStateFlow<CreatedClientCredentials?>(null)
    val lastCreatedCredentials: StateFlow<CreatedClientCredentials?> = _lastCreatedCredentials.asStateFlow()

    val reminderSettings: StateFlow<ReminderSettings> = reminderPreferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ReminderSettings())

    private fun <T> Flow<List<T>>.toUiStateFlow(tag: String, fallbackMsg: String): StateFlow<UiState<List<T>>> =
        this.map<List<T>, UiState<List<T>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "$tag stream error", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMsg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val studentsState: StateFlow<UiState<List<StudentCase>>> =
        repository.observeStudents(currentUserId, isAdminSession, activeStudentId)
            .toUiStateFlow("students", "تعذر تحميل ملفات الحالات")

    val appointmentsState: StateFlow<UiState<List<Appointment>>> =
        repository.observeAppointments(currentUserId, isAdminSession, activeStudentId)
            .toUiStateFlow("appointments", "تعذر تحميل جدول المواعيد")

    val evaluationsState: StateFlow<UiState<List<MonthlyEvaluation>>> =
        repository.observeEvaluations(currentUserId, isAdminSession, activeStudentId)
            .toUiStateFlow("evaluations", "تعذر تحميل التقييمات الشهرية")

    val paymentsState: StateFlow<UiState<List<PaymentRecord>>> =
        repository.observePayments(currentUserId, isAdminSession)
            .toUiStateFlow("payments", "تعذر تحميل سجل المدفوعات")

    val chatsState: StateFlow<UiState<List<ChatThread>>> =
        repository.observeChats(currentUserId, isAdminSession)
            .toUiStateFlow("chats", "تعذر تحميل المحادثات")

    val notificationsState: StateFlow<UiState<List<SmartNotification>>> =
        repository.observeNotifications(currentUserId)
            .toUiStateFlow("notifications", "تعذر تحميل الإشعارات")

    val caseFilesState: StateFlow<UiState<List<CaseFile>>> =
        repository.observeCaseFiles(currentUserId, isAdminSession, activeStudentId)
            .toUiStateFlow("case_files", "تعذر تحميل ملفات الحالة المرفقة")

    val centerConfig: StateFlow<CenterConfig> =
        repository.observeCenterConfig()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "center_config stream error", error)
                emit(CenterCatalog.defaultConfig())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), CenterCatalog.defaultConfig())

    val chatMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        _selectedChatId.flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeChatMessages(chatId, currentUserId, isAdminSession)
            }
        }.toUiStateFlow("messages", "تعذر تحميل الرسائل")

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun clearCreatedCredentials() {
        _lastCreatedCredentials.value = null
    }

    fun createStudent(
        childName: String,
        ageYears: Int,
        parentPhone: String,
        primaryService: String,
        specialistName: String,
        currentGoals: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createStudent(
                parentUid = currentUserId,
                parentName = userDisplayName,
                parentPhone = parentPhone,
                childName = childName,
                ageYears = ageYears,
                primaryService = primaryService,
                specialistName = specialistName,
                currentGoals = currentGoals
            )
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم إضافة ملف الطفل $childName بنجاح" },
                onFailure = { it.localizedMessage ?: "تعذر حفظ ملف الطفل" }
            )
        }
    }

    fun createStudentAdmin(
        childName: String,
        ageYears: Int,
        parentName: String,
        parentPhone: String,
        primaryService: String,
        specialistName: String,
        currentGoals: String,
        customUsername: String,
        customPassword: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createStudentWithAutoCredentialsAdmin(
                childName = childName,
                ageYears = ageYears,
                parentName = parentName,
                parentPhone = parentPhone,
                primaryService = primaryService,
                specialistName = specialistName,
                currentGoals = currentGoals,
                customUsername = customUsername,
                customPassword = customPassword
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { creds ->
                    _lastCreatedCredentials.value = creds
                    _bannerMessage.value = "تم تسجيل ${creds.childName} وتوليد بيانات الدخول"
                },
                onFailure = {
                    _bannerMessage.value = it.localizedMessage ?: "تعذر تسجيل ملف العميل"
                }
            )
        }
    }

    fun createAdminAccount(username: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createAdminAccount(username, password, displayName)
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم إنشاء حساب الإدارة (@${username.trim().lowercase()}) بنجاح" },
                onFailure = { it.localizedMessage ?: "تعذر إنشاء حساب الإدارة" }
            )
        }
    }

    fun updateStudentAdmin(
        studentId: String,
        caseStatus: String,
        progressPercent: Int,
        specialistName: String,
        clinicalNotes: String,
        currentGoals: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateStudentAdmin(
                studentId = studentId,
                caseStatus = caseStatus,
                progressPercent = progressPercent,
                specialistName = specialistName,
                clinicalNotes = clinicalNotes,
                currentGoals = currentGoals
            )
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم تحديث ملف الحالة بنجاح" },
                onFailure = { it.localizedMessage ?: "تعذر تحديث ملف الحالة" }
            )
        }
    }

    fun bookAppointment(
        student: StudentCase,
        serviceName: String,
        feeAmount: Int,
        specialistName: String,
        dateLabel: String,
        timeSlot: String,
        scheduledEpochMs: Long,
        notes: String,
        reminderMinutesBefore: Int,
        onBooked: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.bookAppointment(
                student = student,
                serviceName = serviceName,
                feeAmount = feeAmount,
                specialistName = specialistName,
                dateLabel = dateLabel,
                timeSlot = timeSlot,
                scheduledEpochMs = scheduledEpochMs,
                notes = notes,
                reminderMinutesBefore = reminderMinutesBefore
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { id ->
                    _bannerMessage.value = "تم حجز موعد $serviceName وجدولة التذكير الذكي"
                    onBooked(id)
                },
                onFailure = {
                    _bannerMessage.value = it.localizedMessage ?: "تعذر حجز الموعد"
                }
            )
        }
    }

    fun updateAppointmentStatusAdmin(appointment: Appointment, newStatus: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateAppointmentStatusAdmin(appointment, newStatus)
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم تحديث حالة الحضور بنجاح" },
                onFailure = { it.localizedMessage ?: "تعذر تحديث الحالة" }
            )
        }
    }

    fun cancelAppointmentParent(appointmentId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.cancelAppointmentParent(appointmentId)
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم إلغاء الموعد" },
                onFailure = { it.localizedMessage ?: "تعذر إلغاء الموعد" }
            )
        }
    }

    fun payForSessionOrPackage(
        appointment: Appointment?,
        childName: String,
        description: String,
        amountEgp: Int,
        method: String,
        onPaid: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.payForSessionOrPackage(
                parentUid = currentUserId,
                appointment = appointment,
                childName = childName,
                description = description,
                amountEgp = amountEgp,
                method = method
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { refCode ->
                    _bannerMessage.value = "تم الدفع بنجاح - رقم المرجع $refCode"
                    onPaid(refCode)
                },
                onFailure = {
                    _bannerMessage.value = it.localizedMessage ?: "تعذر إتمام الدفع"
                }
            )
        }
    }

    fun publishMonthlyEvaluation(
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
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.publishMonthlyEvaluation(
                student = student,
                monthLabel = monthLabel,
                specialistName = specialistName,
                speechScore = speechScore,
                attentionScore = attentionScore,
                behaviorScore = behaviorScore,
                cognitiveScore = cognitiveScore,
                motorScore = motorScore,
                summary = summary,
                homeRecommendations = homeRecommendations
            )
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم نشر التقييم الشهري وإرسال إشعار لولي الأمر" },
                onFailure = { it.localizedMessage ?: "تعذر نشر التقييم" }
            )
        }
    }

    fun acknowledgeEvaluation(evaluationId: String) {
        viewModelScope.launch {
            val res = repository.acknowledgeEvaluation(evaluationId)
            _bannerMessage.value = res.fold(
                onSuccess = { "تم تأكيد اطلاعك على التقرير الشهري" },
                onFailure = { it.localizedMessage ?: "تعذر تأكيد الاطلاع" }
            )
        }
    }

    fun sendChatMessage(
        chatId: String,
        parentUid: String,
        parentName: String,
        specialistId: String,
        specialistName: String,
        text: String
    ) {
        viewModelScope.launch {
            val role = if (isAdminSession && parentUid != currentUserId) "admin" else "parent"
            val senderName = if (role == "admin") specialistName else userDisplayName
            val res = repository.sendChatMessage(
                chatId = chatId,
                parentUid = parentUid,
                parentName = parentName,
                specialistId = specialistId,
                specialistName = specialistName,
                senderName = senderName,
                senderRole = role,
                text = text
            )
            if (res.isFailure) {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "تعذر إرسال الرسالة"
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun logSmartReminder(title: String, body: String, scheduledForLabel: String) {
        viewModelScope.launch {
            repository.createSmartReminderLog(currentUserId, activeStudentId, title, body, scheduledForLabel)
        }
    }

    fun updateReminderSettings(enabled: Boolean, leadMinutes: Int, includePrepTip: Boolean) {
        viewModelScope.launch {
            reminderPreferences.updateSettings(enabled, leadMinutes, includePrepTip)
            _bannerMessage.value = "تم تحديث إعدادات التذكير الذكي"
        }
    }

    fun addCaseFileAdmin(
        student: StudentCase,
        title: String,
        category: String,
        notes: String,
        fileName: String,
        mimeType: String,
        fileSizeBytes: Long,
        contentBase64: String,
        externalUrl: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addCaseFileAdmin(
                student = student,
                title = title,
                category = category,
                notes = notes,
                fileName = fileName,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                contentBase64 = contentBase64,
                externalUrl = externalUrl,
                uploadedByName = userDisplayName
            )
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم إرفاق الملف لـ ${student.childName} وإرساله لولي الأمر" },
                onFailure = { it.localizedMessage ?: "تعذر إرفاق الملف" }
            )
        }
    }

    fun deleteCaseFileAdmin(fileId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteCaseFileAdmin(fileId)
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم حذف الملف المرفق" },
                onFailure = { it.localizedMessage ?: "تعذر حذف الملف" }
            )
        }
    }

    fun saveCenterConfigAdmin(config: CenterConfig, successMsg: String = "تم حفظ التعديلات وتحديث التطبيق للجميع") {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCenterConfigAdmin(config)
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { successMsg },
                onFailure = { it.localizedMessage ?: "تعذر حفظ إعدادات المركز" }
            )
        }
    }

    fun resetCenterConfigAdmin() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.resetCenterConfigAdmin()
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تمت استعادة الإعدادات الافتراضية للمركز" },
                onFailure = { it.localizedMessage ?: "تعذر استعادة الإعدادات الافتراضية" }
            )
        }
    }

    fun sendFeeNoticeAdmin(
        student: StudentCase,
        existingAppointment: Appointment?,
        description: String,
        amountEgp: Int,
        notes: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.sendFeeNoticeAdmin(
                student = student,
                existingAppointment = existingAppointment,
                description = description,
                amountEgp = amountEgp,
                notes = notes
            )
            _isBusy.value = false
            _bannerMessage.value = res.fold(
                onSuccess = { "تم إرسال السعر ($amountEgp ج.م) وإشعار ولي الأمر لـ ${student.childName}" },
                onFailure = { it.localizedMessage ?: "تعذر إرسال السعر" }
            )
        }
    }

    private companion object {
        const val TAG = "FekrVM"
        const val STOP_TIMEOUT = 5000L
    }
}
