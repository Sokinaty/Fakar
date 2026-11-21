package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CenterConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.UiState
import kotlin.math.hypot

@Composable
fun ConnectScreen(
    initialSubTab: Int,
    centerConfig: CenterConfig,
    currentUserId: String,
    userDisplayName: String,
    isAdmin: Boolean,
    chatsState: UiState<List<ChatThread>>,
    chatMessagesState: UiState<List<ChatMessage>>,
    selectedSpecialistId: String,
    selectedSpecialistName: String,
    onSelectChatThread: (String, String, String, String, String) -> Unit,
    onSendMessage: (String, String, String, String, String, String) -> Unit
) {
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            listOf("محادثة الأخصائيين", "خريطة المركز").forEachIndexed { index, label ->
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
            SpecialistLiveChatView(
                centerConfig = centerConfig,
                currentUserId = currentUserId,
                userDisplayName = userDisplayName,
                isAdmin = isAdmin,
                chatsState = chatsState,
                chatMessagesState = chatMessagesState,
                initialSpecialistId = selectedSpecialistId,
                initialSpecialistName = selectedSpecialistName,
                onSelectChatThread = onSelectChatThread,
                onSendMessage = onSendMessage
            )
        } else {
            InteractiveCenterMapView(centerConfig = centerConfig)
        }
    }
}

@Composable
private fun SpecialistLiveChatView(
    centerConfig: CenterConfig,
    currentUserId: String,
    userDisplayName: String,
    isAdmin: Boolean,
    chatsState: UiState<List<ChatThread>>,
    chatMessagesState: UiState<List<ChatMessage>>,
    initialSpecialistId: String,
    initialSpecialistName: String,
    onSelectChatThread: (String, String, String, String, String) -> Unit,
    onSendMessage: (String, String, String, String, String, String) -> Unit
) {
    val chats = (chatsState as? UiState.Success)?.data.orEmpty()
    var activeSpecId by remember(initialSpecialistId) { mutableStateOf(initialSpecialistId) }
    var activeSpecName by remember(initialSpecialistName) { mutableStateOf(initialSpecialistName) }
    var activeParentUid by remember { mutableStateOf(currentUserId) }
    var activeParentName by remember { mutableStateOf(userDisplayName) }
    var messageText by remember { mutableStateOf("") }

    val activeChatId = "${activeParentUid}_${activeSpecId}"

    LaunchedEffect(activeChatId) {
        onSelectChatThread(activeChatId, activeParentUid, activeParentName, activeSpecId, activeSpecName)
    }

    val messages = (chatMessagesState as? UiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Specialist or Admin Thread Selector
        if (isAdmin && chats.isNotEmpty()) {
            Text(
                "محادثات أولياء الأمور:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chats, key = { it.id }) { thread ->
                    FilterChip(
                        selected = activeChatId == thread.id,
                        onClick = {
                            activeParentUid = thread.parentUid
                            activeParentName = thread.parentName
                            activeSpecId = thread.specialistId
                            activeSpecName = thread.specialistName
                        },
                        label = { Text("${thread.parentName} ← ${thread.specialistName}") }
                    )
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(centerConfig.specialists, key = { it.id }) { spec ->
                    FilterChip(
                        selected = activeSpecId == spec.id,
                        onClick = {
                            activeParentUid = currentUserId
                            activeParentName = userDisplayName
                            activeSpecId = spec.id
                            activeSpecName = spec.name
                        },
                        label = { Text(spec.name) }
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp)) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "محادثة فورية مباشرة مع $activeSpecName",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ابدأ المحادثة مع $activeSpecName", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "يمكنك الاستفسار عن التمارين المنزلية، مواعيد الجلسات، أو تطورات حالة الطفل في أي وقت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderUid == currentUserId
                    Row(
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isMine) 18.dp else 4.dp,
                                bottomEnd = if (isMine) 4.dp else 18.dp
                            ),
                            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick prompts
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickPrompts = listOf(
                "استفسار عن الجلسة القادمة",
                "تمارين النطق المنزلية",
                "تأكيد موعد الحضور"
            )
            items(quickPrompts) { prompt ->
                AssistChip(
                    onClick = {
                        onSendMessage(activeChatId, activeParentUid, activeParentName, activeSpecId, activeSpecName, prompt)
                    },
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Input bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("اكتب رسالتك للأخصائي...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    val textToSend = messageText.trim()
                    if (textToSend.isNotEmpty()) {
                        onSendMessage(activeChatId, activeParentUid, activeParentName, activeSpecId, activeSpecName, textToSend)
                        messageText = ""
                    }
                },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال")
            }
        }
    }
}

private data class MapLandmark(
    val id: String,
    val title: String,
    val subtitle: String,
    val relX: Float,
    val relY: Float,
    val isCenter: Boolean = false
)

@Composable
private fun InteractiveCenterMapView(centerConfig: CenterConfig) {
    val context = LocalContext.current
    val landmarks = remember(centerConfig.centerName) {
        listOf(
            MapLandmark("center", centerConfig.centerName, "البوابة الرئيسية - الدور الثالث (مصعد مجهز)", 0.5f, 0.48f, true),
            MapLandmark("metro", "محطة المترو الرئيسية", "يبعد 4 دقائق سيراً عن المركز", 0.24f, 0.28f),
            MapLandmark("parking", "موقف سيارات العائلات", "متاح أمام برج الأطباء مباشرة", 0.74f, 0.34f),
            MapLandmark("pharmacy", "صيدلية الشفاء والمكتبة", "على ناصية الشارع الرئيسي", 0.32f, 0.74f),
            MapLandmark("park", "منطقة انتظار وألعاب أطفال", "بجوار مدخل الاستقبال", 0.72f, 0.68f)
        )
    }
    var selectedLandmark by remember(landmarks) { mutableStateOf(landmarks.first()) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceBg = MaterialTheme.colorScheme.surfaceContainerLow

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(surfaceBg)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panOffset = Offset(
                                (panOffset.x + dragAmount.x).coerceIn(-160f, 160f),
                                (panOffset.y + dragAmount.y).coerceIn(-160f, 160f)
                            )
                        }
                    }
                    .pointerInput(landmarks, panOffset, zoom) {
                        detectTapGestures { tap ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val hit = landmarks.minByOrNull { lm ->
                                val px = w / 2f + (lm.relX - 0.5f) * w * zoom + panOffset.x
                                val py = h / 2f + (lm.relY - 0.5f) * h * zoom + panOffset.y
                                hypot(tap.x - px, tap.y - py)
                            }
                            if (hit != null) {
                                val px = w / 2f + (hit.relX - 0.5f) * w * zoom + panOffset.x
                                val py = h / 2f + (hit.relY - 0.5f) * h * zoom + panOffset.y
                                if (hypot(tap.x - px, tap.y - py) < 85f) {
                                    selectedLandmark = hit
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                fun toScreen(rx: Float, ry: Float): Offset = Offset(
                    w / 2f + (rx - 0.5f) * w * zoom + panOffset.x,
                    h / 2f + (ry - 0.5f) * h * zoom + panOffset.y
                )

                // Map grid roads
                val roadColor = Color.White
                drawLine(roadColor, toScreen(0.05f, 0.48f), toScreen(0.95f, 0.48f), strokeWidth = 26f * zoom)
                drawLine(roadColor, toScreen(0.5f, 0.05f), toScreen(0.5f, 0.95f), strokeWidth = 24f * zoom)
                drawLine(roadColor.copy(alpha = 0.7f), toScreen(0.15f, 0.2f), toScreen(0.85f, 0.8f), strokeWidth = 14f * zoom)

                // Route path from metro to Fekr Center
                val routePath = Path().apply {
                    val p1 = toScreen(0.24f, 0.28f)
                    val p2 = toScreen(0.24f, 0.48f)
                    val p3 = toScreen(0.5f, 0.48f)
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                }
                drawPath(routePath, color = primaryColor, style = Stroke(width = 7f, cap = StrokeCap.Round))

                // Landmarks & Center Pin
                landmarks.forEach { lm ->
                    val pt = toScreen(lm.relX, lm.relY)
                    val isSel = selectedLandmark.id == lm.id
                    if (lm.isCenter) {
                        drawCircle(color = secondaryColor.copy(alpha = 0.22f), radius = 48f * zoom, center = pt)
                        drawCircle(color = secondaryColor, radius = if (isSel) 26f else 22f, center = pt)
                        drawCircle(color = Color.White, radius = 9f, center = pt)
                    } else {
                        drawCircle(
                            color = if (isSel) primaryColor else primaryColor.copy(alpha = 0.72f),
                            radius = if (isSel) 18f else 14f,
                            center = pt
                        )
                        drawCircle(color = Color.White, radius = 5f, center = pt)
                    }
                }
            }

            // Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = CircleShape, shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                    IconButton(onClick = { zoom = (zoom + 0.2f).coerceAtMost(1.8f) }) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "تكبير")
                    }
                }
                Surface(shape = CircleShape, shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                    IconButton(onClick = { zoom = (zoom - 0.2f).coerceAtLeast(0.8f) }) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "تصغير")
                    }
                }
            }

            // Helper Tip Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    "اسحب الخريطة واضغط على أي نقطة للتفاصيل",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Selected Landmark & Navigation Actions Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(selectedLandmark.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(selectedLandmark.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${centerConfig.centerAddress} • ${centerConfig.centerHours}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val uri = Uri.parse("geo:${centerConfig.centerLat},${centerConfig.centerLng}?q=${Uri.encode(centerConfig.centerName)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            runCatching { context.startActivity(mapIntent) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خرائط جوجل")
                    }
                    OutlinedButton(
                        onClick = {
                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${centerConfig.centerPhone}"))
                            runCatching { context.startActivity(dial) }
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اتصال")
                    }
                }
            }
        }
    }
}
