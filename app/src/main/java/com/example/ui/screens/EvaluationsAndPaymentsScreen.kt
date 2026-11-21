package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Appointment
import com.example.data.model.MonthlyEvaluation
import com.example.data.model.PaymentRecord
import com.example.data.model.UiState
import com.example.ui.components.DevelopmentalRadarChart
import com.example.ui.components.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationsAndPaymentsScreen(
    initialSubTab: Int,
    appointmentsState: UiState<List<Appointment>>,
    evaluationsState: UiState<List<MonthlyEvaluation>>,
    paymentsState: UiState<List<PaymentRecord>>,
    pendingPaymentAppointment: Appointment?,
    onClearPendingPayment: () -> Unit,
    onAcknowledgeEvaluation: (String) -> Unit,
    onPay: (Appointment?, String, String, Int, String, (String) -> Unit) -> Unit
) {
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    var checkoutTarget by remember(pendingPaymentAppointment) {
        mutableStateOf<CheckoutRequest?>(
            pendingPaymentAppointment?.let {
                CheckoutRequest(
                    appointment = it,
                    childName = it.childName,
                    description = "جلسة ${it.serviceName} (${it.dateLabel})",
                    amountEgp = it.feeAmount.toInt()
                )
            }
        )
    }
    var receiptRefCode by remember { mutableStateOf<String?>(null) }

    val appointments = (appointmentsState as? UiState.Success)?.data.orEmpty()
    val evaluations = (evaluationsState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            listOf("التقييم الشهري", "الدفع الإلكتروني").forEachIndexed { index, label ->
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
            when (evaluationsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(evaluationsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (evaluations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "لا توجد تقارير تقييم شهري منشورة بعد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "يصدر الأخصائي تقريراً شهرياً مفصلاً لقياس تقدم الطفل في النطق والتركيز والسلوك والمهارات لتعزيز الشفافية مع ولي الأمر.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(evaluations.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { eval ->
                                EvaluationReportCard(
                                    evaluation = eval,
                                    onAcknowledge = { onAcknowledgeEvaluation(eval.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val unpaidAppointments = appointments.filter {
                it.paymentStatus == "unpaid" && it.status != "cancelled" && it.feeAmount > 0L
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "المستحقات والرسوم المرسلة من الإدارة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (unpaidAppointments.isEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "لا توجد رسوم أو مطالبات مرسلة من الإدارة حالياً.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(unpaidAppointments, key = { it.id }) { appt ->
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${appt.serviceName} - ${appt.childName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${appt.dateLabel} • ${appt.timeSlot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (appt.notes.isNotBlank()) {
                                        Text(
                                            appt.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        checkoutTarget = CheckoutRequest(
                                            appointment = appt,
                                            childName = appt.childName,
                                            description = "${appt.serviceName} (${appt.dateLabel})",
                                            amountEgp = appt.feeAmount.toInt()
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("سداد ${appt.feeAmount} ج.م")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "سجل المدفوعات والإيصالات الإلكترونية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (payments.isEmpty()) {
                    item {
                        Text(
                            "لم يتم تسجيل عمليات دفع إلكتروني بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(payments.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }, key = { it.id }) { pay ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(pay.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${pay.childName} • مرجع ${pay.referenceCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                StatusPill(
                                    label = "${pay.amountEgp} ج.م",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (checkoutTarget != null) {
        val req = checkoutTarget!!
        PaymentCheckoutSheet(
            request = req,
            onDismiss = {
                checkoutTarget = null
                onClearPendingPayment()
            },
            onConfirmPay = { method ->
                onPay(req.appointment, req.childName, req.description, req.amountEgp, method) { refCode ->
                    checkoutTarget = null
                    onClearPendingPayment()
                    receiptRefCode = refCode
                }
            }
        )
    }

    if (receiptRefCode != null) {
        AlertDialog(
            onDismissRequest = { receiptRefCode = null },
            icon = { Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("تم الدفع الإلكتروني بنجاح") },
            text = { Text("تم إصدار الإيصال الإلكتروني رقم $receiptRefCode وإضافته لسجل مدفوعاتك بنجاح.") },
            confirmButton = {
                TextButton(onClick = { receiptRefCode = null }) { Text("حسناً") }
            }
        )
    }
}

private data class CheckoutRequest(
    val appointment: Appointment?,
    val childName: String,
    val description: String,
    val amountEgp: Int
)

@Composable
private fun EvaluationReportCard(
    evaluation: MonthlyEvaluation,
    onAcknowledge: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                        text = "تقييم ${evaluation.childName} — ${evaluation.monthLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بإشراف: ${evaluation.specialistName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    label = "المتوسط ${evaluation.overallScore}%",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DevelopmentalRadarChart(
                scores = listOf(
                    "النطق والتخاطب" to evaluation.speechScore.toInt(),
                    "الانتباه والتركيز" to evaluation.attentionScore.toInt(),
                    "السلوك الاجتماعي" to evaluation.behaviorScore.toInt(),
                    "المهارات الإدراكية" to evaluation.cognitiveScore.toInt(),
                    "الاستقلالية والحركية" to evaluation.motorScore.toInt()
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ملخص الأخصائي:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(evaluation.summary, style = MaterialTheme.typography.bodyMedium)
                    if (evaluation.homeRecommendations.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("توصيات التمارين المنزلية:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(evaluation.homeRecommendations, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!evaluation.parentAcknowledged) {
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد الاطلاع على التقييم الشهري")
                }
            } else {
                StatusPill(
                    label = "تم الاطلاع والتأكيد من ولي الأمر ✓",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentCheckoutSheet(
    request: CheckoutRequest,
    onDismiss: () -> Unit,
    onConfirmPay: (String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("card") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var walletPhone by remember { mutableStateOf("") }
    var instapayHandle by remember { mutableStateOf("") }

    val isInputValid = when (selectedMethod) {
        "card" -> cardNumber.filter { it.isDigit() }.length >= 12 && cardExpiry.length >= 4 && cardCvv.length >= 3
        "vodafone_cash" -> walletPhone.filter { it.isDigit() }.length >= 10
        "instapay" -> instapayHandle.trim().length >= 4
        else -> true
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("الدفع الإلكتروني الآمن", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${request.description} • ${request.amountEgp} ج.م", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val methods = listOf(
                    Triple("card", "بطاقة بنكية", Icons.Filled.CreditCard),
                    Triple("vodafone_cash", "محفظة إلكترونية", Icons.Filled.PhoneAndroid),
                    Triple("instapay", "إنستا باي InstaPay", Icons.Filled.AccountBalanceWallet)
                )
                items(methods) { (key, label, icon) ->
                    FilterChip(
                        selected = selectedMethod == key,
                        onClick = { selectedMethod = key },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(label) }
                    )
                }
            }

            when (selectedMethod) {
                "card" -> {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it.filter { c -> c.isDigit() }.take(16) },
                        label = { Text("رقم البطاقة (Visa / Mastercard)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { cardExpiry = it.take(5) },
                            label = { Text("تاريخ الانتهاء (MM/YY)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cardCvv,
                            onValueChange = { cardCvv = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("CVV") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                "vodafone_cash" -> {
                    OutlinedTextField(
                        value = walletPhone,
                        onValueChange = { walletPhone = it.filter { c -> c.isDigit() }.take(11) },
                        label = { Text("رقم المحفظة (فودافون / اتصالات / أورانج كاش)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "instapay" -> {
                    OutlinedTextField(
                        value = instapayHandle,
                        onValueChange = { instapayHandle = it.take(40) },
                        label = { Text("عنوان الدفع IPA (مثال: name@instapay)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = { onConfirmPay(selectedMethod) },
                enabled = isInputValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("تأكيد وسداد ${request.amountEgp} ج.م")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
