package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SmartNotification
import com.example.data.model.UiState
import com.example.data.preferences.ReminderSettings
import com.example.notifications.SmartNotificationHelper
import com.example.ui.components.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsModalSheet(
    notificationsState: UiState<List<SmartNotification>>,
    reminderSettings: ReminderSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: (Boolean, Int, Boolean) -> Unit,
    onMarkRead: (String) -> Unit,
    onLogTestNotification: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(SmartNotificationHelper.hasNotificationPermission(context)) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("نظام الإشعارات الذكي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(
                    onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("تفعيل إذن تنبيهات الجهاز")
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("التذكير التلقائي للجلسات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = reminderSettings.enabled,
                            onCheckedChange = {
                                onUpdateSettings(it, reminderSettings.leadMinutes, reminderSettings.includeHomePrepTip)
                            }
                        )
                    }
                    Text("توقيت التنبيه قبل الجلسة:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30 to "30 دقيقة", 120 to "ساعتين", 1440 to "24 ساعة").forEach { (mins, lbl) ->
                            FilterChip(
                                selected = reminderSettings.leadMinutes == mins,
                                onClick = { onUpdateSettings(reminderSettings.enabled, mins, reminderSettings.includeHomePrepTip) },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            val title = "تذكير ذكي من مركز فكر"
                            val body = "تذكير تجريبي: جلسة التخاطب القادمة جاهزة! ينصح بإحضار كراسة المتابعة قبل الموعد بـ 10 دقائق."
                            SmartNotificationHelper.showNotificationNow(context, title, body, 9991)
                            onLogTestNotification(title, body, "تنبيه فوري")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("إرسال تنبيه ذكي تجريبي الآن")
                    }
                }
            }

            Text("سجل الإشعارات والتذكيرات (${notifications.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            if (notifications.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لا توجد إشعارات مسجلة بعد.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.read) MaterialTheme.colorScheme.surfaceContainerLow
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!item.read) onMarkRead(item.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (!item.read) {
                                        StatusPill("جديد", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.body, style = MaterialTheme.typography.bodySmall)
                                if (item.scheduledForLabel.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.scheduledForLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
