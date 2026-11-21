package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Appointment
import com.example.data.model.CaseFile
import com.example.data.model.CenterCatalog
import com.example.data.model.CenterConfig
import com.example.data.model.CenterServiceItem
import com.example.data.model.StudentCase
import com.example.data.model.UiState
import com.example.data.preferences.ReminderSettings
import com.example.notifications.SmartNotificationHelper
import com.example.ui.components.CaseFileViewerDialog
import com.example.ui.components.StatusPill
import com.example.util.CaseFileHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAndCasesScreen(
    centerConfig: CenterConfig,
    studentsState: UiState<List<StudentCase>>,
    appointmentsState: UiState<List<Appointment>>,
    caseFilesState: UiState<List<CaseFile>>,
    reminderSettings: ReminderSettings,
    initialSubTab: Int = 0,
    preselectedService: CenterServiceItem?,
    openBookingInitially: Boolean,
    onClearBookingTrigger: () -> Unit,
    onCreateStudent: (String, Int, String, String, String, String) -> Unit,
    onBookAppointment: (StudentCase, String, Int, String, String, String, Long, String, Int, (String) -> Unit) -> Unit,
    onCancelAppointment: (String) -> Unit,
    onPayAppointment: (Appointment) -> Unit,
    onReminderScheduled: (String, String, String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var subTab by remember { mutableIntStateOf(initialSubTab) } // 0 = Appointments, 1 = Cases
    LaunchedEffect(initialSubTab) {
        subTab = initialSubTab
    }
    var showAddStudentSheet by remember { mutableStateOf(false) }
    var showBookSheet by remember(openBookingInitially) { mutableStateOf(openBookingInitially) }
    var apptFilter by remember { mutableStateOf("all") }
    var viewingFile by remember { mutableStateOf<CaseFile?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val students = (studentsState as? UiState.Success)?.data.orEmpty()
    val appointments = (appointmentsState as? UiState.Success)?.data.orEmpty()
    val caseFiles = (caseFilesState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (subTab == 1 || students.isEmpty()) showAddStudentSheet = true
                    else showBookSheet = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = {
                    Text(if (subTab == 1 || students.isEmpty()) "إضافة ملف طفل" else "حجز موعد جديد")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("schedule_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                listOf("جدول المواعيد", "ملفات الحالات").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = subTab == index,
                        onClick = { subTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (subTab == 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "all" to "الكل",
                        "upcoming" to "القادمة",
                        "unpaid" to "غير مدفوعة",
                        "completed" to "مكتملة"
                    )
                    items(filters) { (key, title) ->
                        FilterChip(
                            selected = apptFilter == key,
                            onClick = { apptFilter = key },
                            label = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                when (appointmentsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(appointmentsState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val filtered = appointments.filter { appt ->
                            when (apptFilter) {
                                "upcoming" -> appt.status in listOf("pending", "confirmed")
                                "unpaid" -> appt.paymentStatus == "unpaid" && appt.status != "cancelled" && appt.feeAmount > 0L
                                "completed" -> appt.status == "completed"
                                else -> true
                            }
                        }.sortedByDescending { it.scheduledEpochMs }

                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "لا توجد مواعيد مطابقة حالياً",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (students.isEmpty()) "أضف ملف طفلك أولاً ثم احجز أول جلسة تخاطب أو تقييم."
                                        else "اضغط على زر «حجز موعد جديد» لاختيار الخدمة واليوم والوقت المناسب.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filtered, key = { it.id }) { appt ->
                                    AppointmentItemCard(
                                        appointment = appt,
                                        onPayClick = { onPayAppointment(appt) },
                                        onRemindClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                !SmartNotificationHelper.hasNotificationPermission(context)
                                            ) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            SmartNotificationHelper.scheduleSessionReminder(
                                                context = context,
                                                appointmentId = appt.id,
                                                childName = appt.childName,
                                                serviceName = appt.serviceName,
                                                dateLabel = appt.dateLabel,
                                                timeSlot = appt.timeSlot,
                                                sessionEpochMs = appt.scheduledEpochMs,
                                                minutesBefore = reminderSettings.leadMinutes
                                            )
                                            SmartNotificationHelper.showNotificationNow(
                                                context = context,
                                                title = "تم تفعيل التذكير الذكي لـ ${appt.childName}",
                                                body = "موعد ${appt.serviceName} يوم ${appt.dateLabel} الساعة ${appt.timeSlot}. سننبهك قبلها بـ ${reminderSettings.leadMinutes} دقيقة.",
                                                notificationId = appt.id.hashCode()
                                            )
                                            onReminderScheduled(
                                                "تذكير ذكي نشط: ${appt.serviceName}",
                                                "تم جدولة تنبيه لجلسة ${appt.childName} يوم ${appt.dateLabel} الساعة ${appt.timeSlot}",
                                                "${appt.dateLabel} • ${appt.timeSlot}"
                                            )
                                        },
                                        onCancelClick = { onCancelAppointment(appt.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when (studentsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(studentsState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        if (students.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.ChildCare,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "لا توجد ملفات حالات مسجلة بعد",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "سجل بيانات طفلك لمتابعة تطور الحالة، نسبة الحضور، والتقارير الشهرية.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(students, key = { it.id }) { student ->
                                    val studentFiles = caseFiles.filter { it.studentId == student.id }
                                    StudentCaseCard(
                                        student = student,
                                        caseFiles = studentFiles,
                                        onViewFile = { viewingFile = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStudentSheet) {
        AddStudentModalSheet(
            centerConfig = centerConfig,
            onDismiss = { showAddStudentSheet = false },
            onSave = { name, age, phone, service, specialist, goals ->
                onCreateStudent(name, age, phone, service, specialist, goals)
                showAddStudentSheet = false
            }
        )
    }

    if (showBookSheet) {
        if (students.isEmpty()) {
            showBookSheet = false
            showAddStudentSheet = true
            onClearBookingTrigger()
        } else {
            BookAppointmentModalSheet(
                centerConfig = centerConfig,
                students = students,
                initialService = preselectedService,
                defaultLeadMinutes = reminderSettings.leadMinutes,
                onDismiss = {
                    showBookSheet = false
                    onClearBookingTrigger()
                },
                onConfirm = { student, serviceName, fee, specialist, dateLabel, timeSlot, epochMs, notes, leadMin ->
                    onBookAppointment(
                        student, serviceName, fee, specialist, dateLabel, timeSlot, epochMs, notes, leadMin
                    ) { apptId ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !SmartNotificationHelper.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        SmartNotificationHelper.scheduleSessionReminder(
                            context = context,
                            appointmentId = apptId,
                            childName = student.childName,
                            serviceName = serviceName,
                            dateLabel = dateLabel,
                            timeSlot = timeSlot,
                            sessionEpochMs = epochMs,
                            minutesBefore = leadMin
                        )
                    }
                    showBookSheet = false
                    onClearBookingTrigger()
                }
            )
        }
    }

    if (viewingFile != null) {
        CaseFileViewerDialog(
            caseFile = viewingFile!!,
            onDismiss = { viewingFile = null },
            onShowMessage = onShowToast
        )
    }
}

@Composable
private fun AppointmentItemCard(
    appointment: Appointment,
    onPayClick: () -> Unit,
    onRemindClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val (statusLabel, statusBg, statusFg) = when (appointment.status) {
        "confirmed" -> Triple("مؤكد", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "completed" -> Triple("تم الحضور", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        "absent" -> Triple("غياب", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        "cancelled" -> Triple("ملغي", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple("قيد التأكيد", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${appointment.serviceName} — ${appointment.childName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appointment.specialistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = statusLabel, containerColor = statusBg, contentColor = statusFg)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${appointment.dateLabel} • ${appointment.timeSlot}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                val (payLabel, payBg, payFg) = when {
                    appointment.feeAmount <= 0L -> Triple(
                        "تُحدد الرسوم من الإدارة",
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    appointment.paymentStatus == "paid" -> Triple(
                        "مدفوع (${appointment.feeAmount} ج.م)",
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    else -> Triple(
                        "رسوم مطلوبة: ${appointment.feeAmount} ج.م",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                StatusPill(
                    label = payLabel,
                    containerColor = payBg,
                    contentColor = payFg
                )
            }

            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ملاحظة: ${appointment.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val showPayButton = appointment.paymentStatus == "unpaid" && appointment.status != "cancelled" && appointment.feeAmount > 0L
            if (appointment.status in listOf("pending", "confirmed") || showPayButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showPayButton) {
                        Button(
                            onClick = onPayClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دفع ${appointment.feeAmount} ج.م")
                        }
                    }
                    if (appointment.status in listOf("pending", "confirmed")) {
                        OutlinedButton(
                            onClick = onRemindClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تذكير ذكي")
                        }
                        OutlinedButton(
                            onClick = onCancelClick,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentCaseCard(
    student: StudentCase,
    caseFiles: List<CaseFile>,
    onViewFile: (CaseFile) -> Unit
) {
    val statusText = when (student.caseStatus) {
        "assessment" -> "قيد التقييم"
        "completed" -> "خطة مكتملة"
        "paused" -> "متوقف مؤقتاً"
        else -> "برنامج نشط"
    }
    val progress = student.progressPercent.toInt().coerceIn(0, 100)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${student.childName} (${student.ageYears} سنوات)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${student.primaryService} • ${student.specialistName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusPill(
                    label = statusText,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("مؤشر التقدم العام", style = MaterialTheme.typography.labelMedium)
                Text(
                    "$progress%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "الحضور: ${student.attendedSessions} من ${student.totalSessions} جلسة",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (caseFiles.isNotEmpty()) {
                    StatusPill(
                        label = "${caseFiles.size} ملف مرفق",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (student.currentGoals.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الأهداف الحالية: ${student.currentGoals}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (student.clinicalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظات الأخصائي: ${student.clinicalNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "ملفات وتقارير الحالة (${caseFiles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (caseFiles.isEmpty()) {
                        Text(
                            "لا توجد ملفات أو تقارير مرفقة لهذه الحالة بعد. ستظهر هنا فور رفعها من المركز.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        caseFiles.forEachIndexed { index, file ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            file.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
                                            file.mimeType.startsWith("image/") -> Icons.Filled.Image
                                            file.contentBase64.isBlank() && file.externalUrl.isNotBlank() -> Icons.Filled.Link
                                            else -> Icons.Filled.Description
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = file.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${CenterCatalog.caseFileCategoryLabel(file.category)} • ${CaseFileHelper.formatFileSize(file.fileSizeBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (file.notes.isNotBlank()) {
                                            Text(
                                                text = file.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = { onViewFile(file) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("parent_view_case_file_${file.id}")
                                ) {
                                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("عرض")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentModalSheet(
    centerConfig: CenterConfig,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String, String, String) -> Unit
) {
    var childName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("6") }
    var phone by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf(centerConfig.services.firstOrNull()?.title ?: "تخاطب") }
    var selectedSpecialist by remember { mutableStateOf(centerConfig.specialists.firstOrNull()?.name ?: "أ / أحمد عوف") }
    var goals by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "إضافة ملف طفل جديد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = childName,
                onValueChange = { childName = it },
                label = { Text("اسم الطفل") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("العمر (سنوات)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(15) },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1.6f)
                )
            }

            Text("البرنامج الأساسي", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(centerConfig.services) { srv ->
                    FilterChip(
                        selected = selectedService == srv.title,
                        onClick = { selectedService = srv.title },
                        label = { Text(srv.title) }
                    )
                }
            }

            Text("الأخصائي المتابع", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(centerConfig.specialists) { spec ->
                    FilterChip(
                        selected = selectedSpecialist == spec.name,
                        onClick = { selectedSpecialist = spec.name },
                        label = { Text(spec.name) }
                    )
                }
            }

            OutlinedTextField(
                value = goals,
                onValueChange = { goals = it },
                label = { Text("الهدف العلاجي أو سبب التقييم") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val age = ageText.toIntOrNull()?.coerceIn(1, 25) ?: 6
                    onSave(childName, age, phone, selectedService, selectedSpecialist, goals)
                },
                enabled = childName.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("حفظ ملف الطفل")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookAppointmentModalSheet(
    centerConfig: CenterConfig,
    students: List<StudentCase>,
    initialService: CenterServiceItem?,
    defaultLeadMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (StudentCase, String, Int, String, String, String, Long, String, Int) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.first()) }
    var selectedService by remember { mutableStateOf(initialService ?: centerConfig.services.first()) }
    var selectedSpecialist by remember { mutableStateOf(centerConfig.specialists.firstOrNull()?.name ?: "أ / أحمد عوف") }
    var dayOffset by remember { mutableIntStateOf(1) }
    var selectedTimeSlot by remember { mutableStateOf(centerConfig.timeSlots.getOrNull(3) ?: centerConfig.timeSlots.firstOrNull() ?: "04:00 م") }
    var notes by remember { mutableStateOf("") }
    var leadMinutes by remember { mutableIntStateOf(defaultLeadMinutes) }

    val dateOptions = remember {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFmt = SimpleDateFormat("EEE d MMM", Locale("ar"))
        (1..6).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            Triple(offset, fmt.format(cal.time), displayFmt.format(cal.time))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("حجز جلسة جديدة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("اختر الطفل", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students) { stu ->
                    FilterChip(
                        selected = selectedStudent.id == stu.id,
                        onClick = { selectedStudent = stu },
                        label = { Text(stu.childName) }
                    )
                }
            }

            Text("اختر الخدمة", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(centerConfig.services) { srv ->
                    FilterChip(
                        selected = selectedService.id == srv.id,
                        onClick = { selectedService = srv },
                        label = { Text(srv.title) }
                    )
                }
            }

            Text("الأخصائي", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(centerConfig.specialists) { spec ->
                    FilterChip(
                        selected = selectedSpecialist == spec.name,
                        onClick = { selectedSpecialist = spec.name },
                        label = { Text(spec.name) }
                    )
                }
            }

            Text("اليوم", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dateOptions) { (offset, _, label) ->
                    FilterChip(
                        selected = dayOffset == offset,
                        onClick = { dayOffset = offset },
                        label = { Text(label) }
                    )
                }
            }

            Text("الموعد", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(centerConfig.timeSlots) { slot ->
                    FilterChip(
                        selected = selectedTimeSlot == slot,
                        onClick = { selectedTimeSlot = slot },
                        label = { Text(slot) }
                    )
                }
            }

            Text("تذكير ذكي قبل الجلسة", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30 to "30 د", 120 to "ساعتين", 1440 to "24 ساعة").forEach { (mins, lbl) ->
                    FilterChip(
                        selected = leadMinutes == mins,
                        onClick = { leadMinutes = mins },
                        label = { Text(lbl) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات للأخصائي (اختياري)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val chosenDate = dateOptions.first { it.first == dayOffset }.second
            val epochMs = System.currentTimeMillis() + dayOffset * 86_400_000L

            Button(
                onClick = {
                    onConfirm(
                        selectedStudent,
                        selectedService.title,
                        0,
                        selectedSpecialist,
                        chosenDate,
                        selectedTimeSlot,
                        epochMs,
                        notes,
                        leadMinutes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("تأكيد طلب الحجز")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
