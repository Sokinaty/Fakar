package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.CaseFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlin.math.max

data class PreparedPickedFile(
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Int,
    val contentBase64: String
)

object CaseFileHelper {
    private const val MAX_RAW_DOCUMENT_BYTES = 620_000
    private const val MAX_IMAGE_DIMENSION = 1400

    fun readPickedDocument(context: Context, uri: Uri): Result<PreparedPickedFile> {
        return try {
            val resolver = context.contentResolver
            val detectedMime = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
            var displayName = "case_attachment"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    }
                }
            }
            val safeName = sanitizeFileName(displayName, detectedMime)
            val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalStateException("تعذر قراءة الملف المحدد من الجهاز."))

            if (rawBytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("الملف المختار فارغ."))
            }

            if (detectedMime.startsWith("image/")) {
                val compressed = compressImageIfNeeded(rawBytes, detectedMime)
                if (compressed.bytes.size > MAX_RAW_DOCUMENT_BYTES) {
                    return Result.failure(
                        IllegalArgumentException("حجم الصورة كبير جداً حتى بعد الضغط. يرجى اختيار صورة أصغر أو لصق رابط خارجي.")
                    )
                }
                val encoded = Base64.encodeToString(compressed.bytes, Base64.NO_WRAP)
                Result.success(
                    PreparedPickedFile(
                        fileName = safeName,
                        mimeType = compressed.mimeType,
                        fileSizeBytes = compressed.bytes.size,
                        contentBase64 = encoded
                    )
                )
            } else {
                if (rawBytes.size > MAX_RAW_DOCUMENT_BYTES) {
                    val sizeKb = rawBytes.size / 1024
                    return Result.failure(
                        IllegalArgumentException(
                            "حجم الملف ($sizeKb كيلوبايت) يتجاوز الحد المباشر (600 كيلوبايت). يمكنك ضغط الملف أو إضافة رابط Google Drive في الخانة بالأسفل."
                        )
                    )
                }
                val encoded = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                Result.success(
                    PreparedPickedFile(
                        fileName = safeName,
                        mimeType = detectedMime,
                        fileSizeBytes = rawBytes.size,
                        contentBase64 = encoded
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "تعذر معالجة الملف المختار."))
        }
    }

    fun writeTempCacheFile(context: Context, caseFile: CaseFile): Result<File> {
        return try {
            if (caseFile.contentBase64.isBlank()) {
                return Result.failure(IllegalStateException("لا توجد بيانات مرفقة محلية لهذا الملف."))
            }
            val bytes = Base64.decode(caseFile.contentBase64, Base64.DEFAULT)
            val dir = File(context.cacheDir, "case_files").apply { mkdirs() }
            val safeName = sanitizeFileName(
                raw = "${caseFile.id.take(8)}_${caseFile.fileName}",
                mimeType = caseFile.mimeType
            )
            val target = File(dir, safeName)
            target.writeBytes(bytes)
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openExternalOrUrl(context: Context, caseFile: CaseFile): Result<Unit> {
        return try {
            if (caseFile.contentBase64.isNotBlank()) {
                val file = writeTempCacheFile(context, caseFile).getOrThrow()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, caseFile.mimeType.ifBlank { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(viewIntent, "فتح الملف عبر").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success(Unit)
            } else if (caseFile.externalUrl.isNotBlank()) {
                val safeUrl = normalizeUrl(caseFile.externalUrl)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("لا يوجد محتوى أو رابط لفتحه."))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("لا يوجد تطبيق مناسب لفتح هذا النوع من الملفات على الجهاز."))
        }
    }

    fun shareCaseFile(context: Context, caseFile: CaseFile): Result<Unit> {
        return try {
            if (caseFile.contentBase64.isNotBlank()) {
                val file = writeTempCacheFile(context, caseFile).getOrThrow()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = caseFile.mimeType.ifBlank { "*/*" }
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, caseFile.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "مشاركة / حفظ الملف").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success(Unit)
            } else if (caseFile.externalUrl.isNotBlank()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, caseFile.title)
                    putExtra(Intent.EXTRA_TEXT, "${caseFile.title}\n${normalizeUrl(caseFile.externalUrl)}")
                }
                val chooser = Intent.createChooser(shareIntent, "مشاركة رابط الملف").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("لا يوجد ملف للمشاركة."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0L -> "رابط خارجي"
            bytes < 1024L -> "$bytes بايت"
            bytes < 1024L * 1024L -> "${(bytes / 1024L).coerceAtLeast(1)} كيلوبايت"
            else -> String.format(Locale.US, "%.1f ميجابايت", bytes / (1024.0 * 1024.0))
        }
    }

    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) trimmed else "https://$trimmed"
    }

    private data class CompressedResult(val bytes: ByteArray, val mimeType: String)

    private fun compressImageIfNeeded(rawBytes: ByteArray, originalMime: String): CompressedResult {
        if (rawBytes.size <= 350_000) {
            return CompressedResult(rawBytes, originalMime)
        }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: return CompressedResult(rawBytes, originalMime)
        val maxSide = max(decoded.width, decoded.height)
        val scaled = if (maxSide > MAX_IMAGE_DIMENSION) {
            val ratio = MAX_IMAGE_DIMENSION.toFloat() / maxSide.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }
        var quality = 80
        var outBytes: ByteArray
        do {
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            outBytes = baos.toByteArray()
            quality -= 12
        } while (outBytes.size > MAX_RAW_DOCUMENT_BYTES && quality >= 35)
        if (scaled !== decoded) decoded.recycle()
        return CompressedResult(outBytes, "image/jpeg")
    }

    private fun sanitizeFileName(raw: String, mimeType: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF ]"), "_")
            .trim()
            .ifBlank { "case_file" }
            .take(100)
        return if (cleaned.contains('.')) cleaned else when {
            mimeType == "application/pdf" -> "$cleaned.pdf"
            mimeType.startsWith("image/png") -> "$cleaned.png"
            mimeType.startsWith("image/") -> "$cleaned.jpg"
            mimeType.startsWith("text/") -> "$cleaned.txt"
            else -> cleaned
        }
    }
}
