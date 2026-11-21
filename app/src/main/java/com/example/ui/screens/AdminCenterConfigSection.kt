package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CenterCatalog
import com.example.data.model.CenterConfig
import com.example.data.model.CenterServiceItem
import com.example.data.model.CustomActionButton
import com.example.data.model.SpecialistProfile
import com.example.ui.components.ServiceArtwork
import com.example.ui.components.StatusPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCenterConfigSection(
    centerConfig: CenterConfig,
    onSaveConfig: (CenterConfig, String) -> Unit,
    onResetConfig: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var centerName by remember { mutableStateOf(centerConfig.centerName) }
    var centerSlogan by remember { mutableStateOf(centerConfig.centerSlogan) }
    var centerPhone by remember { mutableStateOf(centerConfig.centerPhone) }
    var centerAddress by remember { mutableStateOf(centerConfig.centerAddress) }
    var centerHours by remember { mutableStateOf(centerConfig.centerHours) }
    var centerLatText by remember { mutableStateOf(centerConfig.centerLat.toString()) }
    var centerLngText by remember { mutableStateOf(centerConfig.centerLng.toString()) }
    var welcomeNote by remember { mutableStateOf(centerConfig.welcomeNote) }
    var bookButtonLabel by remember { mutableStateOf(centerConfig.bookButtonLabel) }
    var posterBannerTitle by remember { mutableStateOf(centerConfig.posterBannerTitle) }
    var showPosterBanner by remember { mutableStateOf(centerConfig.showPosterBanner) }

    LaunchedEffect(centerConfig) {
        centerName = centerConfig.centerName
        centerSlogan = centerConfig.centerSlogan
        centerPhone = centerConfig.centerPhone
        centerAddress = centerConfig.centerAddress
        centerHours = centerConfig.centerHours
        centerLatText = centerConfig.centerLat.toString()
        centerLngText = centerConfig.centerLng.toString()
        welcomeNote = centerConfig.welcomeNote
        bookButtonLabel = centerConfig.bookButtonLabel
        posterBannerTitle = centerConfig.posterBannerTitle
        showPosterBanner = centerConfig.showPosterBanner
    }

    var newTimeSlotText by remember { mutableStateOf("") }
    var editingButton by remember { mutableStateOf<CustomActionButton?>(null) }
    var isAddingButton by remember { mutableStateOf(false) }

    var editingService by remember { mutableStateOf<CenterServiceItem?>(null) }
    var isAddingService by remember { mutableStateOf(false) }

    var editingSpecialist by remember { mutableStateOf<SpecialistProfile?>(null) }
    var isAddingSpecialist by remember { mutableStateOf(false) }

    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. General Center Info, Contact, Address, Labels
        item {
            AdminSectionCard(
                title = "معلومات المركز والعنوان والأزرار الرئيسية",
                subtitle = "تعديل الاسم، رقم الهاتف، العنوان، الإحداثيات، ونصوص الشاشة الرئيسية",
                icon = Icons.Filled.Business
            ) {
                OutlinedTextField(
                    value = centerName,
                    onValueChange = { centerName = it },
                    label = { Text("اسم المركز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_cfg_center_name")
                )
                OutlinedTextField(
                    value = centerSlogan,
                    onValueChange = { centerSlogan = it },
                    label = { Text("الشعار النصي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = centerPhone,
                    onValueChange = { centerPhone = it },
                    label = { Text("رقم الهاتف الأساسي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_cfg_center_phone")
                )
                OutlinedTextField(
                    value = centerAddress,
                    onValueChange = { centerAddress = it },
                    label = { Text("العنوان التفصيلي للمركز") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("admin_cfg_center_address")
                )
                OutlinedTextField(
                    value = centerHours,
                    onValueChange = { centerHours = it },
                    label = { Text("مواعيد العمل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = centerLatText,
                        onValueChange = { centerLatText = it },
                        label = { Text("خط العرض (Lat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = centerLngText,
                        onValueChange = { centerLngText = it },
                        label = { Text("خط الطول (Lng)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = bookButtonLabel,
                    onValueChange = { bookButtonLabel = it },
                    label = { Text("نص زر الحجز الرئيسي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = welcomeNote,
                    onValueChange = { welcomeNote = it },
                    label = { Text("إعلان أو رسالة ترحيبية أعلى الرئيسية (اختياري)") },
                    placeholder = { Text("مثال: خصم 15% على جلسات التقييم خلال هذا الأسبوع") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = posterBannerTitle,
                    onValueChange = { posterBannerTitle = it },
                    label = { Text("عنوان البانر التعريفي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إظهار صورة البوستر في الرئيسية", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = showPosterBanner,
                        onCheckedChange = { showPosterBanner = it }
                    )
                }

                Button(
                    onClick = {
                        if (centerName.isBlank() || centerPhone.isBlank() || centerAddress.isBlank()) {
                            onShowToast("يرجى التأكد من عدم ترك الاسم أو الهاتف أو العنوان فارغاً")
                            return@Button
                        }
                        val lat = centerLatText.trim().toDoubleOrNull() ?: centerConfig.centerLat
                        val lng = centerLngText.trim().toDoubleOrNull() ?: centerConfig.centerLng
                        val updated = centerConfig.copy(
                            centerName = centerName.trim(),
                            centerSlogan = centerSlogan.trim(),
                            centerPhone = centerPhone.trim(),
                            centerAddress = centerAddress.trim(),
                            centerHours = centerHours.trim(),
                            centerLat = lat,
                            centerLng = lng,
                            welcomeNote = welcomeNote.trim(),
                            bookButtonLabel = bookButtonLabel.trim().ifBlank { "احجز جلسة" },
                            posterBannerTitle = posterBannerTitle.trim(),
                            showPosterBanner = showPosterBanner
                        )
                        onSaveConfig(updated, "تم حفظ بيانات المركز والعنوان بنجاح")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_save_center_info_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ بيانات المركز")
                }
            }
        }

        // 2. Custom Action Buttons (Add / Edit / Delete)
        item {
            AdminSectionCard(
                title = "الأزرار المخصصة في الصفحة الرئيسية (${centerConfig.customButtons.size})",
                subtitle = "إضافة وتعديل وحذف أي زر (واتساب، اتصال، رابط، حجز، محادثة، دفع، خريطة)",
                icon = Icons.Filled.SmartButton
            ) {
                FilledTonalButton(
                    onClick = {
                        isAddingButton = true
                        editingButton = CustomActionButton(
                            id = "btn_${System.currentTimeMillis()}",
                            label = "",
                            actionType = "whatsapp",
                            targetValue = centerConfig.centerPhone,
                            style = "secondary"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_add_custom_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة زر جديد للرئيسية")
                }

                if (centerConfig.customButtons.isEmpty()) {
                    Text(
                        "لا توجد أزرار إضافية حالياً. اضغط «إضافة زر جديد للرئيسية» لإنشاء زر فوري.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    centerConfig.customButtons.forEachIndexed { idx, btn ->
                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(btn.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    StatusPill(
                                        label = CenterCatalog.customButtonActionLabel(btn.actionType),
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                if (btn.targetValue.isNotBlank()) {
                                    Text(
                                        btn.targetValue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        isAddingButton = false
                                        editingButton = btn
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "تعديل الزر", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = {
                                        val updated = centerConfig.copy(
                                            customButtons = centerConfig.customButtons.filterNot { it.id == btn.id }
                                        )
                                        onSaveConfig(updated, "تم حذف الزر «${btn.label}»")
                                    }
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الزر", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Services & Images (no public prices)
        item {
            AdminSectionCard(
                title = "خدمات وبرامج المركز (${centerConfig.services.size})",
                subtitle = "تعديل الأسماء، الصور التوضيحية، مدة الجلسة، أو إضافة وحذف برامج المركز",
                icon = Icons.Filled.MedicalServices
            ) {
                FilledTonalButton(
                    onClick = {
                        isAddingService = true
                        editingService = CenterServiceItem(
                            id = "srv_${System.currentTimeMillis()}",
                            title = "",
                            subtitle = "",
                            description = "",
                            sessionDurationMin = 45,
                            feeEgp = 0,
                            categoryTag = "أساسي",
                            imageKey = "speech"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_add_service_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة خدمة أو برنامج جديد")
                }

                centerConfig.services.forEachIndexed { idx, srv ->
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
                            Image(
                                painter = painterResource(id = ServiceArtwork.drawableForService(srv)),
                                contentDescription = srv.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(srv.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    StatusPill(
                                        label = srv.categoryTag,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    "${srv.subtitle} • ${srv.sessionDurationMin} دقيقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    isAddingService = false
                                    editingService = srv
                                }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "تعديل الخدمة", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    if (centerConfig.services.size <= 1) {
                                        onShowToast("يجب الإبقاء على خدمة واحدة على الأقل")
                                    } else {
                                        val updated = centerConfig.services.filterNot { it.id == srv.id }
                                        onSaveConfig(centerConfig.copy(services = updated), "تم حذف خدمة «${srv.title}»")
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الخدمة", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 5. Specialists Team
        item {
            AdminSectionCard(
                title = "فريق الأخصائيين (${centerConfig.specialists.size})",
                subtitle = "إضافة وتعديل أسماء وتخصصات الأخصائيين الظاهرين في الحجز والمحادثة",
                icon = Icons.Filled.People
            ) {
                FilledTonalButton(
                    onClick = {
                        isAddingSpecialist = true
                        editingSpecialist = SpecialistProfile(
                            id = "spec_${System.currentTimeMillis()}",
                            name = "",
                            roleTitle = "",
                            specialties = listOf("تخاطب"),
                            experienceYears = 5
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة أخصائي جديد")
                }

                centerConfig.specialists.forEachIndexed { idx, spec ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(spec.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${spec.roleTitle} • خبرة ${spec.experienceYears} سنة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    isAddingSpecialist = false
                                    editingSpecialist = spec
                                }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "تعديل الأخصائي", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    if (centerConfig.specialists.size <= 1) {
                                        onShowToast("يجب الإبقاء على أخصائي واحد على الأقل")
                                    } else {
                                        val updated = centerConfig.copy(
                                            specialists = centerConfig.specialists.filterNot { it.id == spec.id }
                                        )
                                        onSaveConfig(updated, "تم حذف الأخصائي «${spec.name}»")
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الأخصائي", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 6. Time Slots
        item {
            AdminSectionCard(
                title = "أوقات الحجز المتاحة (${centerConfig.timeSlots.size})",
                subtitle = "إضافة أو إزالة مواعيد الجلسات المتاحة للحجز اليومي",
                icon = Icons.Filled.AccessTime
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    centerConfig.timeSlots.forEach { slot ->
                        InputChip(
                            selected = true,
                            onClick = {
                                if (centerConfig.timeSlots.size <= 1) {
                                    onShowToast("يجب الإبقاء على ميعاد واحد على الأقل")
                                } else {
                                    val updated = centerConfig.copy(
                                        timeSlots = centerConfig.timeSlots.filterNot { it == slot }
                                    )
                                    onSaveConfig(updated, "تم إزالة الموعد $slot")
                                }
                            },
                            label = { Text(slot) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "إزالة $slot", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTimeSlotText,
                        onValueChange = { newTimeSlotText = it },
                        label = { Text("ميعاد جديد (مثال: 09:00 م)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val cleanSlot = newTimeSlotText.trim()
                            if (cleanSlot.isNotEmpty() && cleanSlot !in centerConfig.timeSlots) {
                                val updated = centerConfig.copy(timeSlots = centerConfig.timeSlots + cleanSlot)
                                onSaveConfig(updated, "تم إضافة الموعد $cleanSlot")
                                newTimeSlotText = ""
                            }
                        },
                        enabled = newTimeSlotText.trim().isNotEmpty(),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("إضافة")
                    }
                }
            }
        }

        // 7. Reset to Defaults
        item {
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("استعادة كافة الإعدادات الافتراضية للمركز")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (editingButton != null) {
        EditCustomButtonSheet(
            initial = editingButton!!,
            isNew = isAddingButton,
            defaultPhone = centerConfig.centerPhone,
            onDismiss = { editingButton = null },
            onSave = { saved ->
                val list = if (isAddingButton) {
                    centerConfig.customButtons + saved
                } else {
                    centerConfig.customButtons.map { if (it.id == saved.id) saved else it }
                }
                onSaveConfig(centerConfig.copy(customButtons = list), "تم حفظ الزر «${saved.label}»")
                editingButton = null
            }
        )
    }

    if (editingService != null) {
        EditServiceSheet(
            initial = editingService!!,
            isNew = isAddingService,
            onDismiss = { editingService = null },
            onSave = { saved ->
                val list = if (isAddingService) {
                    centerConfig.services + saved
                } else {
                    centerConfig.services.map { if (it.id == saved.id) saved else it }
                }
                onSaveConfig(centerConfig.copy(services = list), "تم حفظ بيانات وصورة خدمة «${saved.title}»")
                editingService = null
            }
        )
    }

    if (editingSpecialist != null) {
        EditSpecialistSheet(
            initial = editingSpecialist!!,
            isNew = isAddingSpecialist,
            onDismiss = { editingSpecialist = null },
            onSave = { saved ->
                val list = if (isAddingSpecialist) {
                    centerConfig.specialists + saved
                } else {
                    centerConfig.specialists.map { if (it.id == saved.id) saved else it }
                }
                onSaveConfig(centerConfig.copy(specialists = list), "تم حفظ بيانات الأخصائي «${saved.name}»")
                editingSpecialist = null
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
            title = { Text("استعادة الإعدادات الافتراضية؟") },
            text = { Text("سيتم إعادة اسم المركز والخدمات والعنوان والأزرار للقيم الأساسية الافتراضية.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetConfig()
                    }
                ) {
                    Text("استعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun AdminSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditCustomButtonSheet(
    initial: CustomActionButton,
    isNew: Boolean,
    defaultPhone: String,
    onDismiss: () -> Unit,
    onSave: (CustomActionButton) -> Unit
) {
    var label by remember(initial) { mutableStateOf(initial.label) }
    var actionType by remember(initial) { mutableStateOf(initial.actionType) }
    var targetValue by remember(initial) { mutableStateOf(initial.targetValue) }
    var style by remember(initial) { mutableStateOf(initial.style) }

    val targetLabel = when (actionType) {
        "whatsapp" -> "رقم الواتساب (مثال: $defaultPhone)"
        "phone" -> "رقم الهاتف للاتصال"
        "url" -> "الرابط الكامل (https://...)"
        "chat" -> "اسم أو معرف الأخصائي (اختياري)"
        else -> "ملاحظة أو قيمة إضافية (اختياري)"
    }
    val requiresTarget = actionType in listOf("whatsapp", "phone", "url")
    val canSave = label.trim().isNotEmpty() && (!requiresTarget || targetValue.trim().isNotEmpty())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (isNew) "إضافة زر جديد في الرئيسية" else "تعديل الزر",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("النص الظاهر على الزر") },
                placeholder = { Text("مثال: تواصل واتساب أو حسابنا على فيسبوك") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("custom_btn_label_input")
            )

            Text("وظيفة الزر عند الضغط:", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CenterCatalog.customButtonActionTypes.forEach { (key, title) ->
                    FilterChip(
                        selected = actionType == key,
                        onClick = {
                            actionType = key
                            if ((key == "phone" || key == "whatsapp") && targetValue.isBlank()) {
                                targetValue = defaultPhone
                            }
                        },
                        label = { Text(title) }
                    )
                }
            }

            OutlinedTextField(
                value = targetValue,
                onValueChange = { targetValue = it },
                label = { Text(targetLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("custom_btn_target_input")
            )

            Text("شكل ولون الزر:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("primary" to "أساسي بارز", "secondary" to "ثانوي مميز", "outlined" to "بإطار").forEach { (key, title) ->
                    FilterChip(
                        selected = style == key,
                        onClick = { style = key },
                        label = { Text(title) }
                    )
                }
            }

            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            label = label.trim(),
                            actionType = actionType,
                            targetValue = targetValue.trim(),
                            style = style
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_custom_btn_button")
            ) {
                Text("حفظ الزر وتطبيقه فوراً")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditServiceSheet(
    initial: CenterServiceItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (CenterServiceItem) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial.title) }
    var subtitle by remember(initial) { mutableStateOf(initial.subtitle) }
    var description by remember(initial) { mutableStateOf(initial.description) }
    var durationText by remember(initial) { mutableStateOf(initial.sessionDurationMin.toString()) }
    var categoryTag by remember(initial) { mutableStateOf(initial.categoryTag) }
    var selectedImageKey by remember(initial) { mutableStateOf(ServiceArtwork.inferKeyForService(initial)) }

    val valid = title.trim().isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (isNew) "إضافة خدمة وبرنامج جديد" else "تعديل ${initial.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text("الصورة التوضيحية للخدمة:", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ServiceArtwork.imageChoices) { (key, label) ->
                    val selected = selectedImageKey == key
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedImageKey = key }
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = ServiceArtwork.drawableForKey(key)),
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("اسم الخدمة / التخصص") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = subtitle,
                onValueChange = { subtitle = it },
                label = { Text("الوصف المختصر") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("المدة (دقيقة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = categoryTag,
                    onValueChange = { categoryTag = it },
                    label = { Text("التصنيف") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("التفاصيل الكاملة للجلسة") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            subtitle = subtitle.trim().ifBlank { "جلسات متخصصة" },
                            description = description.trim().ifBlank { subtitle.trim() },
                            feeEgp = 0,
                            sessionDurationMin = (durationText.toIntOrNull() ?: 45).coerceIn(10, 240),
                            categoryTag = categoryTag.trim().ifBlank { "أساسي" },
                            imageKey = selectedImageKey
                        )
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("حفظ الخدمة والصورة")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSpecialistSheet(
    initial: SpecialistProfile,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (SpecialistProfile) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var roleTitle by remember(initial) { mutableStateOf(initial.roleTitle) }
    var yearsText by remember(initial) { mutableStateOf(initial.experienceYears.toString()) }
    var specialtiesText by remember(initial) { mutableStateOf(initial.specialties.joinToString("، ")) }

    val valid = name.trim().isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (isNew) "إضافة أخصائي جديد" else "تعديل بيانات الأخصائي",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم الأخصائي") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = roleTitle,
                onValueChange = { roleTitle = it },
                label = { Text("المسمى والتخصص") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = yearsText,
                onValueChange = { yearsText = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("سنوات الخبرة") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = specialtiesText,
                onValueChange = { specialtiesText = it },
                label = { Text("المجالات (افصل بفاصلة)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val specs = specialtiesText.split("،", ",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            roleTitle = roleTitle.trim().ifBlank { "أخصائي تأهيل وتخاطب" },
                            experienceYears = (yearsText.toIntOrNull() ?: 5).coerceIn(0, 60),
                            specialties = specs.ifEmpty { listOf("تخاطب") }
                        )
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("حفظ بيانات الأخصائي")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
