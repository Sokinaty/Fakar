package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Appointment
import com.example.data.model.CaseFile
import com.example.data.model.CenterCatalog
import com.example.data.model.CenterConfig
import com.example.data.model.CenterServiceItem
import com.example.data.model.CustomActionButton
import com.example.data.model.MonthlyEvaluation
import com.example.data.model.StudentCase
import com.example.data.model.UiState
import com.example.ui.components.CaseFileViewerDialog
import com.example.ui.components.ServiceArtwork
import com.example.ui.components.StatusPill
import com.example.util.CaseFileHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    userName: String,
    centerConfig: CenterConfig,
    studentsState: UiState<List<StudentCase>>,
    appointmentsState: UiState<List<Appointment>>,
    evaluationsState: UiState<List<MonthlyEvaluation>>,
    caseFilesState: UiState<List<CaseFile>>,
    onBookServiceClick: (CenterServiceItem?) -> Unit,
    onOpenScheduleTab: () -> Unit,
    onOpenCasesTab: () -> Unit,
    onOpenEvaluationsTab: () -> Unit,
    onOpenPaymentsTab: () -> Unit,
    onOpenChatWithSpecialist: (String, String) -> Unit,
    onOpenMapTab: () -> Unit,
    onOpenNotifications: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedService by remember { mutableStateOf<CenterServiceItem?>(null) }
    var viewingFile by remember { mutableStateOf<CaseFile?>(null) }

    val students = (studentsState as? UiState.Success)?.data.orEmpty()
    val appointments = (appointmentsState as? UiState.Success)?.data.orEmpty()
    val evaluations = (evaluationsState as? UiState.Success)?.data.orEmpty()
    val caseFiles = (caseFilesState as? UiState.Success)?.data.orEmpty()

    val nextAppointment = appointments
        .filter { it.status in listOf("pending", "confirmed") }
        .minByOrNull { it.scheduledEpochMs }

    val unpaidCount = appointments.count {
        it.paymentStatus == "unpaid" && it.status != "cancelled" && it.feeAmount > 0L
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Hero Banner
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.84f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_fekr),
                                    contentDescription = stringResource(id = R.string.cd_center_logo),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "أهلاً بك، $userName",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                )
                                Text(
                                    text = centerConfig.centerName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                if (centerConfig.centerSlogan.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusPill(
                                        label = "« ${centerConfig.centerSlogan} »",
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onBookServiceClick(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_book_button")
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    centerConfig.bookButtonLabel.ifBlank { "احجز جلسة" },
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${centerConfig.centerPhone}"))
                                        context.startActivity(dialIntent)
                                    }.onFailure {
                                        onShowToast("تعذر فتح تطبيق الاتصال")
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("home_call_button")
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(centerConfig.centerPhone, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        if (centerConfig.welcomeNote.isNotBlank()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = centerConfig.welcomeNote,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (centerConfig.customButtons.isNotEmpty()) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    centerConfig.customButtons.forEach { btn ->
                        CustomActionChipButton(
                            button = btn,
                            onClick = {
                                executeCustomButton(
                                    button = btn,
                                    defaultPhone = centerConfig.centerPhone,
                                    defaultSpecId = centerConfig.specialists.firstOrNull()?.id ?: "spec_ahmad",
                                    defaultSpecName = centerConfig.specialists.firstOrNull()?.name ?: "أ / أحمد عوف",
                                    onBook = { onBookServiceClick(null) },
                                    onOpenChat = onOpenChatWithSpecialist,
                                    onOpenPayments = onOpenPaymentsTab,
                                    onOpenMap = onOpenMapTab,
                                    onOpenUri = { uri ->
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }.onFailure {
                                            onShowToast("تعذر فتح الرابط المطلوب")
                                        }
                                    },
                                    onDial = { tel ->
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
                                        }.onFailure {
                                            onShowToast("تعذر فتح تطبيق الاتصال")
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Next Session & Smart Reminder Strip
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "متابعة الجلسة القادمة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (unpaidCount > 0) {
                            StatusPill(
                                label = "$unpaidCount مستحقات مرسلة",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (nextAppointment != null) {
                        Text(
                            text = "${nextAppointment.serviceName} - ${nextAppointment.childName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "التاريخ: ${nextAppointment.dateLabel} • الساعة ${nextAppointment.timeSlot} • مع ${nextAppointment.specialistName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onOpenScheduleTab,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("تفاصيل الموعد")
                            }
                            if (nextAppointment.paymentStatus == "unpaid" && nextAppointment.feeAmount > 0L) {
                                Button(
                                    onClick = onOpenPaymentsTab,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("سداد ${nextAppointment.feeAmount} ج.م")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (students.isEmpty()) {
                                "ابدأ بإضافة ملف طفلك لحجز أول جلسة تقييم أو تخاطب وتفعيل التذكير الذكي."
                            } else {
                                "لا توجد جلسات مجدولة حالياً. يمكنك حجز جلسة جديدة بضغطة زر."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { onBookServiceClick(null) },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("حجز موعد جديد")
                            }
                            OutlinedButton(
                                onClick = onOpenNotifications,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("إعدادات التذكير")
                            }
                        }
                    }
                }
            }
        }

        // Quick Overview Metrics
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickStatTile(
                    title = "ملفات الحالة",
                    value = if (caseFiles.isNotEmpty()) "${caseFiles.size} مرفق" else "${students.size}",
                    icon = Icons.Filled.School,
                    onClick = onOpenCasesTab,
                    modifier = Modifier.weight(1f)
                )
                QuickStatTile(
                    title = "التقييمات",
                    value = "${evaluations.size}",
                    icon = Icons.Filled.Assessment,
                    onClick = onOpenEvaluationsTab,
                    modifier = Modifier.weight(1f)
                )
                QuickStatTile(
                    title = "موقع المركز",
                    value = "الخريطة",
                    icon = Icons.Filled.LocationOn,
                    onClick = onOpenMapTab,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (caseFiles.isNotEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "ملفات وتقارير الحالة (${caseFiles.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            FilledTonalButton(onClick = onOpenCasesTab, modifier = Modifier.height(48.dp)) {
                                Text("عرض الكل")
                            }
                        }
                        caseFiles.take(3).forEach { file ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = when {
                                                file.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
                                                file.mimeType.startsWith("image/") -> Icons.Filled.Image
                                                file.contentBase64.isBlank() && file.externalUrl.isNotBlank() -> Icons.Filled.Link
                                                else -> Icons.Filled.Description
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                file.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${file.childName} • ${CenterCatalog.caseFileCategoryLabel(file.category)} • ${CaseFileHelper.formatFileSize(file.fileSizeBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = { viewingFile = file },
                                        modifier = Modifier.height(48.dp)
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

        // Center Services Showcase with Illustrations
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "خدمات وبرامج المركز",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${centerConfig.services.size} تخصصات",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        items(centerConfig.services.chunked(2)) { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                pair.forEach { service ->
                    ServiceTileCard(
                        service = service,
                        onClick = { selectedService = service },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Specialists Team Horizontal Strip
        item {
            Text(
                text = "فريق الأخصائيين — محادثة فورية",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(centerConfig.specialists) { specialist ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.width(260.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Psychology,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = specialist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "خبرة ${specialist.experienceYears} سنة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = specialist.roleTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { onOpenChatWithSpecialist(specialist.id, specialist.name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تواصل الآن")
                            }
                        }
                    }
                }
            }
        }

        // Center Official Poster Banner
        if (centerConfig.showPosterBanner) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = centerConfig.posterBannerTitle.ifBlank { "رؤيتنا: ${centerConfig.centerSlogan}" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Image(
                            painter = painterResource(id = R.drawable.poster_fekr),
                            contentDescription = stringResource(id = R.string.cd_center_poster),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    // Service Detail Bottom Sheet (Artwork + Description + Duration, no public prices)
    if (selectedService != null) {
        val srv = selectedService!!
        ModalBottomSheet(onDismissRequest = { selectedService = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = ServiceArtwork.drawableForService(srv)),
                    contentDescription = srv.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = srv.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatusPill(
                        label = srv.categoryTag,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = srv.subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = srv.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("مدة الجلسة", style = MaterialTheme.typography.labelMedium)
                            Text("${srv.sessionDurationMin} دقيقة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تحديد الرسوم", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "تُرسل لك من إدارة المركز حسب الخطة العلاجية",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        selectedService = null
                        onBookServiceClick(srv)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حجز موعد ${srv.title}")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
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
private fun QuickStatTile(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomActionChipButton(
    button: CustomActionButton,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (button.actionType) {
        "whatsapp" -> Icons.AutoMirrored.Filled.Chat
        "phone" -> Icons.Filled.Call
        "book" -> Icons.Filled.CalendarMonth
        "chat" -> Icons.AutoMirrored.Filled.Chat
        "payments" -> Icons.Filled.CreditCard
        "map" -> Icons.Filled.LocationOn
        else -> Icons.AutoMirrored.Filled.OpenInNew
    }
    when (button.style) {
        "primary" -> Button(
            onClick = onClick,
            modifier = Modifier.height(48.dp).testTag("custom_action_btn_${button.id}")
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(button.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        "outlined" -> OutlinedButton(
            onClick = onClick,
            modifier = Modifier.height(48.dp).testTag("custom_action_btn_${button.id}")
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(button.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        else -> FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.height(48.dp).testTag("custom_action_btn_${button.id}")
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(button.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun executeCustomButton(
    button: CustomActionButton,
    defaultPhone: String,
    defaultSpecId: String,
    defaultSpecName: String,
    onBook: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    onOpenPayments: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenUri: (Uri) -> Unit,
    onDial: (String) -> Unit
) {
    when (button.actionType) {
        "book" -> onBook()
        "chat" -> onOpenChat(defaultSpecId, button.targetValue.ifBlank { defaultSpecName })
        "payments" -> onOpenPayments()
        "map" -> onOpenMap()
        "phone" -> onDial(button.targetValue.ifBlank { defaultPhone })
        "whatsapp" -> {
            val digits = button.targetValue.ifBlank { defaultPhone }.filter { it.isDigit() }
            val intl = when {
                digits.startsWith("0") -> "2$digits"
                digits.isNotEmpty() -> digits
                else -> "201006478010"
            }
            onOpenUri(Uri.parse("https://wa.me/$intl"))
        }
        else -> {
            val raw = button.targetValue.trim()
            if (raw.isNotEmpty()) {
                val safeUrl = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                onOpenUri(Uri.parse(safeUrl))
            }
        }
    }
}

@Composable
private fun ServiceTileCard(
    service: CenterServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("service_card_${service.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                Image(
                    painter = painterResource(id = ServiceArtwork.drawableForService(service)),
                    contentDescription = service.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = service.categoryTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = service.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
