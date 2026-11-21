package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CaseFile
import com.example.data.model.CenterCatalog
import com.example.util.CaseFileHelper

@Composable
fun CaseFileViewerDialog(
    caseFile: CaseFile,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = caseFile.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(
                                label = CenterCatalog.caseFileCategoryLabel(caseFile.category),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${caseFile.childName} • ${CaseFileHelper.formatFileSize(caseFile.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق المعاينة")
                    }
                }

                if (caseFile.notes.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ملاحظات الأخصائي: ${caseFile.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // In-app content preview for Image, PDF, Text, or Link
                val mime = caseFile.mimeType.lowercase()
                when {
                    caseFile.contentBase64.isNotBlank() && mime.startsWith("image/") -> {
                        val imageBitmap = remember(caseFile.id, caseFile.contentBase64) {
                            runCatching {
                                val bytes = Base64.decode(caseFile.contentBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            }.getOrNull()
                        }
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = caseFile.title,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            )
                        } else {
                            FallbackAttachmentCard(caseFile)
                        }
                    }

                    caseFile.contentBase64.isNotBlank() && mime == "application/pdf" -> {
                        InAppPdfPreview(caseFile = caseFile)
                    }

                    caseFile.contentBase64.isNotBlank() && mime.startsWith("text/") -> {
                        val textContent = remember(caseFile.id, caseFile.contentBase64) {
                            runCatching {
                                String(Base64.decode(caseFile.contentBase64, Base64.DEFAULT), Charsets.UTF_8)
                            }.getOrNull()
                        }
                        if (!textContent.isNullOrBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 340.dp)
                            ) {
                                Text(
                                    text = textContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(14.dp)
                                )
                            }
                        } else {
                            FallbackAttachmentCard(caseFile)
                        }
                    }

                    else -> {
                        FallbackAttachmentCard(caseFile)
                    }
                }

                if (caseFile.externalUrl.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = caseFile.externalUrl,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            CaseFileHelper.openExternalOrUrl(context, caseFile).onFailure {
                                onShowMessage(it.localizedMessage ?: "تعذر فتح الملف خارجياً")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("case_file_open_external_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (caseFile.contentBase64.isNotBlank()) "فتح بتطبيق خارجي" else "فتح الرابط")
                    }
                    OutlinedButton(
                        onClick = {
                            CaseFileHelper.shareCaseFile(context, caseFile).onFailure {
                                onShowMessage(it.localizedMessage ?: "تعذر مشاركة الملف")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("case_file_share_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة / حفظ")
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppPdfPreview(caseFile: CaseFile) {
    val context = LocalContext.current
    var pageIndex by remember(caseFile.id) { mutableIntStateOf(0) }
    var pageCount by remember(caseFile.id) { mutableIntStateOf(0) }
    var renderedPage by remember(caseFile.id) { mutableStateOf<Bitmap?>(null) }

    val tempFile = remember(caseFile.id, caseFile.contentBase64) {
        CaseFileHelper.writeTempCacheFile(context, caseFile).getOrNull()
    }

    DisposableEffect(tempFile, pageIndex) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            if (tempFile != null && tempFile.exists()) {
                pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount
                if (renderer.pageCount > 0) {
                    val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    if (safeIndex != pageIndex) pageIndex = safeIndex
                    renderer.openPage(safeIndex).use { page ->
                        val scale = 2
                        val bmp = Bitmap.createBitmap(
                            (page.width * scale).coerceAtLeast(1),
                            (page.height * scale).coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        renderedPage = bmp
                    }
                }
            }
        } catch (_: Exception) {
            renderedPage = null
        } finally {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
        onDispose { }
    }

    if (renderedPage != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                bitmap = renderedPage!!.asImageBitmap(),
                contentDescription = "صفحة ${pageIndex + 1} من ${caseFile.title}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(androidx.compose.ui.graphics.Color.White)
            )
            if (pageCount > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = { if (pageIndex > 0) pageIndex-- },
                        enabled = pageIndex > 0,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        Text("السابق")
                    }
                    Text(
                        text = "صفحة ${pageIndex + 1} من $pageCount",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                        enabled = pageIndex < pageCount - 1,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("التالي")
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                    }
                }
            }
        }
    } else {
        FallbackAttachmentCard(caseFile)
    }
}

@Composable
private fun FallbackAttachmentCard(caseFile: CaseFile) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (caseFile.mimeType == "application/pdf") Icons.Filled.PictureAsPdf else Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = caseFile.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اضغط على «فتح بتطبيق خارجي» أو «مشاركة / حفظ» لعرض الملف.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
