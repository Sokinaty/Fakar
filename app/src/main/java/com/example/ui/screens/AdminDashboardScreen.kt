package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Appointment
import com.example.data.model.CaseFile
import com.example.data.model.CenterCatalog
import com.example.data.model.CenterConfig
import com.example.data.model.CenterServiceItem
import com.example.data.model.CreatedClientCredentials
import com.example.data.model.PaymentRecord
import com.example.data.model.SpecialistProfile
import com.example.data.model.StudentCase
import com.example.data.model.UiState
import com.example.ui.components.CaseFileViewerDialog
import com.example.ui.components.ServiceBreakdownBarChart
import com.example.ui.components.StatusPill
import com.example.util.CaseFileHelper
import com.example.util.PreparedPickedFile
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    centerConfig: CenterConfig,
    studentsState: UiState<List<StudentCase>>,
    appointmentsState: UiState<List<Appointment>>,
    paymentsState: UiState<List<PaymentRecord>>,
    caseFilesState: UiState<List<CaseFile>>,
    lastCreatedCredentials: CreatedClientCredentials?,
    onDismissCreatedCredentials: () -> Unit,
    onCreateStudentAdmin: (String, Int, String, String, String, String, String, String, String) -> Unit,
    onCreateAdminAccount: (String, String, String) -> Unit,
    onUpdateAttendance: (Appointment, String) -> Unit,
    onUpdateStudent: (String, String, Int, String, String, String) -> Unit,
    onPublishEvaluation: (StudentCase, String, String, Int, Int, Int, Int, Int, String, String) -> Unit,
    onAddCaseFile: (StudentCase, String, String, String, String, String, Long, String, String) -> Unit,
    onDeleteCaseFile: (String) -> Unit,
    onSendFeeNotice: (StudentCase, Appointment?, String, Int, String) -> Unit,
    onSaveCenterConfig: (CenterConfig, String) -> Unit,
    onResetCenterConfig: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var subTab by remember { mutableIntStateOf(0) } // 0 = Stats, 1 = Attendance, 2 = Students & Files & Fees, 3 = Center Editor
    var showAddStudentAdminSheet by remember { mutableStateOf(false) }
    var showAddAdminSheet by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentCase?>(null) }
    var evaluatingStudent by remember { mutableStateOf<StudentCase?>(null) }
    var attachingStudent by remember { mutableStateOf<StudentCase?>(null) }
    var billingStudent by remember { mutableStateOf<StudentCase?>(null) }
    var billingAppointment by remember { mutableStateOf<Appointment?>(null) }
    var viewingFile by remember { mutableStateOf<CaseFile?>(null) }

    val students = (studentsState as? UiState.Success)?.data.orEmpty()
    val appointments = (appointmentsState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val caseFiles = (caseFilesState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            listOf("التقارير", "الحضور", "العملاء", "إدارة المركز").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when (subTab) {
            0 -> AdminStatsReportsView(
                services = centerConfig.services,
                students = students,
                appointments = appointments,
                payments = payments,
                onOpenAddAdmin = { showAddAdminSheet = true }
            )
            1 -> AdminAttendanceView(
                appointments = appointments,
                onUpdateAttendance = onUpdateAttendance,
                onSendApptFee = { appt ->
                    val matchedStu = students.firstOrNull { it.id == appt.studentId }
                    if (matchedStu != null) {
                        billingStudent = matchedStu
                        billingAppointment = appt
                    } else {
                        onShowToast("لم يتم العثور على ملف الطالب المرتبط بهذا الموعد")
                    }
                }
            )
            2 -> AdminStudentsManagementView(
                students = students,
                caseFiles = caseFiles,
                onAddStudent = { showAddStudentAdminSheet = true },
                onCopyCredentials = { stu ->
                    val summary = "بيانات الدخول لتطبيق ${centerConfig.centerName}:\nالطفل: ${stu.childName}\nاسم المستخدم: ${stu.clientUsername}\nكلمة المرور: ${stu.clientPassword}"
                    clipboard.setText(AnnotatedString(summary))
                    onShowToast("تم نسخ بيانات دخول ${stu.childName}")
                },
                onEditStudent = { editingStudent = it },
                onEvaluateStudent = { evaluatingStudent = it },
                onAttachFile = { attachingStudent = it },
                onSendFeeNotice = { stu ->
                    billingStudent = stu
                    billingAppointment = null
                },
                onViewFile = { viewingFile = it },
                onDeleteFile = onDeleteCaseFile
            )
            3 -> AdminCenterConfigSection(
                centerConfig = centerConfig,
                onSaveConfig = onSaveCenterConfig,
                onResetConfig = onResetCenterConfig,
                onShowToast = onShowToast
            )
        }
    }

    if (lastCreatedCredentials != null) {
        AlertDialog(
            onDismissRequest = onDismissCreatedCredentials,
            icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("تم إنشاء حساب العميل بنجاح") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الطفل: ${lastCreatedCredentials.childName} • ولي الأمر: ${lastCreatedCredentials.parentName}")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "اسم المستخدم: ${lastCreatedCredentials.username}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "كلمة المرور: ${lastCreatedCredentials.password}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        "أرسل اسم المستخدم وكلمة المرور لولي الأمر للدخول ومتابعة التقييمات والمواعيد مباشرة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val copyText = "بيانات الدخول لتطبيق مركز فكر:\nالطفل: ${lastCreatedCredentials.childName}\nاسم المستخدم: ${lastCreatedCredentials.username}\nكلمة المرور: ${lastCreatedCredentials.password}"
                        clipboard.setText(AnnotatedString(copyText))
                        onShowToast("تم نسخ بيانات الدخول للمشاركة")
                        onDismissCreatedCredentials()
                    }
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ البيانات")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCreatedCredentials) { Text("إغلاق") }
            }
        )
    }

    if (showAddStudentAdminSheet) {
        AddStudentAdminModalSheet(
            services = centerConfig.services,
            specialists = centerConfig.specialists,
            onDismiss = { showAddStudentAdminSheet = false },
            onSave = { child, age, parentName, phone, srv, spec, goals, username, password ->
                onCreateStudentAdmin(child, age, parentName, phone, srv, spec, goals, username, password)
                showAddStudentAdminSheet = false
            }
        )
    }

    if (showAddAdminSheet) {
        AddAdminAccountModalSheet(
            onDismiss = { showAddAdminSheet = false },
            onSave = { user, pass, name ->
                onCreateAdminAccount(user, pass, name)
                showAddAdminSheet = false
            }
        )
    }

    if (editingStudent != null) {
        EditStudentAdminSheet(
            student = editingStudent!!,
            specialists = centerConfig.specialists,
            onDismiss = { editingStudent = null },
            onSave = { status, progress, specialist, notes, goals ->
                onUpdateStudent(editingStudent!!.id, status, progress, specialist, notes, goals)
                editingStudent = null
            }
        )
    }

    if (evaluatingStudent != null) {
        PublishEvaluationSheet(
            student = evaluatingStudent!!,
            onDismiss = { evaluatingStudent = null },
            onPublish = { month, spec, s1, s2, s3, s4, s5, summary, homeRec ->
                onPublishEvaluation(evaluatingStudent!!, month, spec, s1, s2, s3, s4, s5, summary, homeRec)
                evaluatingStudent = null
            }
        )
    }

    if (attachingStudent != null) {
        AddCaseFileAdminSheet(
            student = attachingStudent!!,
            onDismiss = { attachingStudent = null },
            onSave = { title, category, notes, fileName, mimeType, fileSizeBytes, base64, externalUrl ->
                onAddCaseFile(
                    attachingStudent!!,
                    title,
                    category,
                    notes,
                    fileName,
                    mimeType,
                    fileSizeBytes,
                    base64,
                    externalUrl
                )
                attachingStudent = null
            },
            onShowToast = onShowToast
        )
    }

    if (billingStudent != null) {
        SendFeeNoticeAdminSheet(
            student = billingStudent!!,
            existingAppointment = billingAppointment,
            services = centerConfig.services,
            onDismiss = {
                billingStudent = null
                billingAppointment = null
            },
            onSend = { desc, amount, notes ->
                onSendFeeNotice(billingStudent!!, billingAppointment, desc, amount, notes)
                billingStudent = null
                billingAppointment = null
            }
        )
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
private fun AdminStatsReportsView(
    services: List<CenterServiceItem>,
    students: List<StudentCase>,
    appointments: List<Appointment>,
    payments: List<PaymentRecord>,
    onOpenAddAdmin: () -> Unit
) {
    val activeCases = students.count { it.caseStatus == "active" }
    val completedSessions = appointments.count { it.status == "completed" }
    val absentSessions = appointments.count { it.status == "absent" }
    val totalAttendanceTracked = completedSessions + absentSessions
    val attendanceRate = if (totalAttendanceTracked == 0) 100 else (completedSessions * 100) / totalAttendanceTracked
    val totalRevenue = payments.filter { it.status == "completed" }.sumOf { it.amountEgp }

    val serviceDistribution = remember(services, students, appointments) {
        services.map { srv ->
            val count = appointments.count { it.serviceName == srv.title } +
                students.count { it.primaryService == srv.title }
            srv.title to count
        }.sortedByDescending { it.second }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "التقارير الإحصائية للأداء العام للمركز",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminKpiCard("الحالات النشطة", "$activeCases / ${students.size}", Icons.Filled.Groups, Modifier.weight(1f))
                AdminKpiCard("نسبة الحضور", "$attendanceRate%", Icons.Filled.FactCheck, Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminKpiCard("إجمالي الإيرادات", "$totalRevenue ج.م", Icons.Filled.Payments, Modifier.weight(1f))
                AdminKpiCard("جلسات مكتملة", "$completedSessions", Icons.Filled.Analytics, Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "توزيع الإقبال حسب التخصص",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ServiceBreakdownBarChart(items = serviceDistribution)
                }
            }
        }

        item {
            val avgProgress = if (students.isEmpty()) 0 else students.map { it.progressPercent }.average().roundToInt()
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "مؤشر جودة التحسن العام",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "متوسط تقدم الحالات المسجلة حالياً: $avgProgress% • إجمالي الحجوزات: ${appointments.size} • العمليات المالية: ${payments.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = onOpenAddAdmin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("open_add_admin_account_button")
            ) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إنشاء حساب دخول جديد للإدارة أو الأخصائيين")
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminAttendanceView(
    appointments: List<Appointment>,
    onUpdateAttendance: (Appointment, String) -> Unit,
    onSendApptFee: (Appointment) -> Unit
) {
    if (appointments.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
            Text("لا توجد مواعيد مسجلة للمتابعة حالياً.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(appointments.sortedByDescending { it.scheduledEpochMs }, key = { it.id }) { appt ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${appt.childName} — ${appt.serviceName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${appt.dateLabel} • ${appt.timeSlot} • ${appt.specialistName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(
                            label = if (appt.feeAmount > 0L) "${appt.feeAmount} ج.م" else appt.status,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = { onUpdateAttendance(appt, "confirmed") },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("تأكيد")
                        }
                        Button(
                            onClick = { onUpdateAttendance(appt, "completed") },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حضر")
                        }
                        OutlinedButton(
                            onClick = { onUpdateAttendance(appt, "absent") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("غاب")
                        }
                    }
                    if (appt.paymentStatus == "unpaid" && appt.status != "cancelled") {
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { onSendApptFee(appt) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (appt.feeAmount > 0L) "تعديل وإعادة إرسال السعر (${appt.feeAmount} ج.م)" else "تحديد وإبلاغ ولي الأمر بالسعر")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStudentsManagementView(
    students: List<StudentCase>,
    caseFiles: List<CaseFile>,
    onAddStudent: () -> Unit,
    onCopyCredentials: (StudentCase) -> Unit,
    onEditStudent: (StudentCase) -> Unit,
    onEvaluateStudent: (StudentCase) -> Unit,
    onAttachFile: (StudentCase) -> Unit,
    onSendFeeNotice: (StudentCase) -> Unit,
    onViewFile: (CaseFile) -> Unit,
    onDeleteFile: (String) -> Unit
) {
    if (students.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("لا يوجد عملاء أو طلاب مسجلون بعد.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "أضف العميل وسيتم توليد اسم مستخدم وكلمة مرور تلقائياً ليسجل بهما ولي الأمر.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddStudent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_add_student_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة عميل وتوليد اسم مستخدم وكلمة مرور")
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onAddStudent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_add_student_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة عميل وتوليد اسم مستخدم وكلمة مرور")
            }
        }
        items(students, key = { it.id }) { stu ->
            val studentFiles = caseFiles.filter { it.studentId == stu.id }
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${stu.childName} (${stu.ageYears} سنوات)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("ولي الأمر: ${stu.parentName} • ${stu.primaryService}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(
                            label = "${stu.progressPercent}%",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (stu.clientUsername.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        "اسم المستخدم: ${stu.clientUsername}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "كلمة المرور: ${stu.clientPassword}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { onCopyCredentials(stu) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ بيانات الدخول", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (studentFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "الملفات المرفقة لولي الأمر (${studentFiles.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                studentFiles.forEachIndexed { idx, file ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    file.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${CenterCatalog.caseFileCategoryLabel(file.category)} • ${CaseFileHelper.formatFileSize(file.fileSizeBytes)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { onViewFile(file) }) {
                                                Icon(Icons.Filled.Visibility, contentDescription = "عرض الملف", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { onDeleteFile(file.id) }) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الملف", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onEditStudent(stu) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديث")
                        }
                        Button(
                            onClick = { onEvaluateStudent(stu) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تقييم")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = { onAttachFile(stu) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_attach_file_${stu.id}")
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة ملف", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = { onSendFeeNotice(stu) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_send_fee_${stu.id}")
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إبلاغ بسعر", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentAdminModalSheet(
    services: List<CenterServiceItem>,
    specialists: List<SpecialistProfile>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String, String, String, String, String, String) -> Unit
) {
    var childName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("5") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var clientUsername by remember { mutableStateOf(CenterCatalog.generateClientUsername()) }
    var clientPassword by remember { mutableStateOf(CenterCatalog.generateClientPassword()) }
    var selectedService by remember { mutableStateOf(services.firstOrNull()?.title ?: "تخاطب") }
    var selectedSpecialist by remember { mutableStateOf(specialists.firstOrNull()?.name ?: "أ / أحمد عوف") }
    var goals by remember { mutableStateOf("") }

    val isFormValid = childName.isNotBlank() &&
        parentName.isNotBlank() &&
        clientUsername.trim().length >= 3 &&
        clientPassword.trim().length >= 4

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "إضافة عميل جديد وتوليد بيانات الدخول",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = childName,
                onValueChange = { childName = it },
                label = { Text("اسم الطفل") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_child_name_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { input -> ageText = input.filter { it.isDigit() }.take(2) },
                    label = { Text("العمر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it.take(15) },
                    label = { Text("رقم هاتف ولي الأمر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1.7f)
                )
            }
            OutlinedTextField(
                value = parentName,
                onValueChange = { parentName = it },
                label = { Text("اسم ولي الأمر") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_parent_name_input")
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "بيانات دخول العميل المولّدة تلقائياً",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = {
                                clientUsername = CenterCatalog.generateClientUsername()
                                clientPassword = CenterCatalog.generateClientPassword()
                            }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "توليد اسم مستخدم وكلمة مرور جديدة")
                        }
                    }
                    OutlinedTextField(
                        value = clientUsername,
                        onValueChange = { clientUsername = it },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_generated_username_input")
                    )
                    OutlinedTextField(
                        value = clientPassword,
                        onValueChange = { clientPassword = it },
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_generated_password_input")
                    )
                }
            }

            Text("البرنامج الأساسي", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(services) { srv ->
                    FilterChip(
                        selected = selectedService == srv.title,
                        onClick = { selectedService = srv.title },
                        label = { Text(srv.title) }
                    )
                }
            }

            Text("الأخصائي المتابع", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(specialists) { sp ->
                    FilterChip(
                        selected = selectedSpecialist == sp.name,
                        onClick = { selectedSpecialist = sp.name },
                        label = { Text(sp.name) }
                    )
                }
            }

            OutlinedTextField(
                value = goals,
                onValueChange = { goals = it },
                label = { Text("الأهداف العلاجية المبدئية (اختياري)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        childName.trim(),
                        ageText.toIntOrNull()?.coerceIn(1, 25) ?: 5,
                        parentName.trim(),
                        parentPhone.trim(),
                        selectedService,
                        selectedSpecialist,
                        goals.ifBlank { "بناء خطة تأهيل فردية شاملة" },
                        clientUsername.trim(),
                        clientPassword.trim()
                    )
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_save_student_button")
            ) {
                Text("حفظ العميل وتفعيل بيانات الدخول")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAdminAccountModalSheet(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("staff_${(100..999).random()}") }
    var password by remember { mutableStateOf(CenterCatalog.generateClientPassword()) }

    val valid = displayName.isNotBlank() && username.trim().length >= 3 && password.trim().length >= 4

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("إنشاء حساب دخول جديد للإدارة / الأخصائي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("اسم الأخصائي أو المسؤول") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    username = "staff_${(100..999).random()}"
                    password = CenterCatalog.generateClientPassword()
                }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "توليد تلقائي")
                }
            }
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onSave(username, password, displayName) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("حفظ حساب الإدارة")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStudentAdminSheet(
    student: StudentCase,
    specialists: List<SpecialistProfile>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String, String) -> Unit
) {
    var status by remember { mutableStateOf(student.caseStatus) }
    var progress by remember { mutableFloatStateOf(student.progressPercent.toFloat()) }
    var specialist by remember { mutableStateOf(student.specialistName) }
    var notes by remember { mutableStateOf(student.clinicalNotes) }
    var goals by remember { mutableStateOf(student.currentGoals) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("تحديث ملف ${student.childName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statuses = listOf("active" to "نشط", "assessment" to "تقييم", "completed" to "مكتمل", "paused" to "مؤقت")
                items(statuses) { (k, lbl) ->
                    FilterChip(selected = status == k, onClick = { status = k }, label = { Text(lbl) })
                }
            }

            Text("نسبة التقدم: ${progress.roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(specialists) { sp ->
                    FilterChip(selected = specialist == sp.name, onClick = { specialist = sp.name }, label = { Text(sp.name) })
                }
            }

            OutlinedTextField(value = goals, onValueChange = { goals = it }, label = { Text("الأهداف العلاجية") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات التقييم الدوري") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = { onSave(status, progress.roundToInt(), specialist, notes, goals) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("حفظ التحديثات")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishEvaluationSheet(
    student: StudentCase,
    onDismiss: () -> Unit,
    onPublish: (String, String, Int, Int, Int, Int, Int, String, String) -> Unit
) {
    var monthLabel by remember { mutableStateOf("نوفمبر 2026") }
    var speech by remember { mutableFloatStateOf(78f) }
    var attention by remember { mutableFloatStateOf(74f) }
    var behavior by remember { mutableFloatStateOf(82f) }
    var cognitive by remember { mutableFloatStateOf(76f) }
    var motor by remember { mutableFloatStateOf(80f) }
    var summary by remember { mutableStateOf("") }
    var homeRec by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("إصدار تقييم شهري — ${student.childName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = monthLabel, onValueChange = { monthLabel = it }, label = { Text("الشهر") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            ScoreSliderRow("النطق والتخاطب", speech) { speech = it }
            ScoreSliderRow("الانتباه والتركيز", attention) { attention = it }
            ScoreSliderRow("السلوك والتفاعل", behavior) { behavior = it }
            ScoreSliderRow("المهارات الإدراكية", cognitive) { cognitive = it }
            ScoreSliderRow("الاستقلالية والحركية", motor) { motor = it }

            OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("ملخص الأخصائي الشهري") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = homeRec, onValueChange = { homeRec = it }, label = { Text("التمارين المنزلية الموصى بها") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    onPublish(
                        monthLabel,
                        student.specialistName,
                        speech.roundToInt(),
                        attention.roundToInt(),
                        behavior.roundToInt(),
                        cognitive.roundToInt(),
                        motor.roundToInt(),
                        summary,
                        homeRec
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("نشر التقييم لولي الأمر")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ScoreSliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${value.roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 0f..100f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCaseFileAdminSheet(
    student: StudentCase,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Long, String, String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("report") }
    var notes by remember { mutableStateOf("") }
    var externalUrl by remember { mutableStateOf("") }
    var pickedFile by remember { mutableStateOf<PreparedPickedFile?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            CaseFileHelper.readPickedDocument(context, uri).fold(
                onSuccess = { prepared ->
                    pickedFile = prepared
                    if (title.isBlank()) {
                        title = prepared.fileName.substringBeforeLast('.').ifBlank { prepared.fileName }
                    }
                },
                onFailure = { err ->
                    onShowToast(err.localizedMessage ?: "تعذر قراءة الملف المختار")
                }
            )
        }
    }

    val hasContent = pickedFile != null || externalUrl.trim().isNotEmpty()
    val canSave = title.trim().isNotEmpty() && hasContent

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "إرفاق ملف لحالة ${student.childName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "سيظهر الملف فوراً في حساب ولي الأمر (${student.parentName}) مع إشعار تنبيهي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = {
                    filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain", "*/*"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("admin_pick_case_file_button")
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (pickedFile == null) "اختيار ملف من الجهاز (PDF / صورة / تقرير)" else "تغيير الملف المختار")
            }

            if (pickedFile != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                pickedFile!!.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${pickedFile!!.mimeType} • ${CaseFileHelper.formatFileSize(pickedFile!!.fileSizeBytes.toLong())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { pickedFile = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "إزالة الملف", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            Text("تصنيف الملف", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CenterCatalog.caseFileCategories) { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الملف (يظهر لولي الأمر)") },
                placeholder = { Text("مثال: تقرير مقياس ستانفورد بينيه") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("case_file_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات الأخصائي على الملف (اختياري)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = externalUrl,
                onValueChange = { externalUrl = it },
                label = { Text("رابط ملف خارجي (اختياري للملفات الكبيرة مثل Drive)") },
                placeholder = { Text("https://drive.google.com/...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val file = pickedFile
                    val trimmedUrl = externalUrl.trim()
                    val fName = file?.fileName ?: "${title.trim().take(40)}.url"
                    val fMime = file?.mimeType ?: "text/uri-list"
                    val fSize = file?.fileSizeBytes?.toLong() ?: 0L
                    val fBase64 = file?.contentBase64 ?: ""
                    onSave(
                        title.trim(),
                        category,
                        notes.trim(),
                        fName,
                        fMime,
                        fSize,
                        fBase64,
                        trimmedUrl
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_save_case_file_button")
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ وإرسال الملف لولي الأمر")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendFeeNoticeAdminSheet(
    student: StudentCase,
    existingAppointment: Appointment?,
    services: List<CenterServiceItem>,
    onDismiss: () -> Unit,
    onSend: (String, Int, String) -> Unit
) {
    var description by remember(student, existingAppointment) {
        mutableStateOf(
            existingAppointment?.serviceName
                ?: "رسوم جلسة ${student.primaryService.ifBlank { "تأهيل" }}"
        )
    }
    var amountText by remember(existingAppointment) {
        mutableStateOf(
            if (existingAppointment != null && existingAppointment.feeAmount > 0L) {
                existingAppointment.feeAmount.toString()
            } else {
                ""
            }
        )
    }
    var notes by remember { mutableStateOf("") }

    val parsedAmount = amountText.toIntOrNull() ?: 0
    val valid = parsedAmount in 1..10000 && description.trim().isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "إبلاغ بالسعر — ${student.childName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "لن تظهر أي أسعار في التطبيق إلا المبلغ الذي ترسله هنا لولي الأمر (${student.parentName}) مع إشعار فوري وخيار السداد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text("السعر / الرسوم المطلوبة (ج.م)") },
                placeholder = { Text("مثال: 300") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_fee_amount_input")
            )

            Text("اختر بيان سريع أو اكتب تفاصيل الرسوم:", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(services) { srv ->
                    val label = "رسوم جلسة ${srv.title}"
                    FilterChip(
                        selected = description == label,
                        onClick = { description = label },
                        label = { Text(srv.title) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("بيان الرسوم (يظهر لولي الأمر)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_fee_desc_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("تفاصيل أو ملاحظات إضافية (اختياري)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onSend(description.trim(), parsedAmount, notes.trim()) },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_send_fee_submit_button")
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال السعر وإشعار ولي الأمر")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
