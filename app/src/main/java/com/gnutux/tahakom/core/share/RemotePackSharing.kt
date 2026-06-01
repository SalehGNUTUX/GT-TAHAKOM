package com.gnutux.tahakom.core.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * تصدير/استيراد حزم الريموت المشتركة.
 *
 * - [exportToShareIntent]: يكتب الحزمة في ملف `.tahakom` بالكاش ويبني Intent مشاركة.
 * - [readFromIntent]: يستخرج الحزمة من Intent وارد (فتح ملف، رابط عميق، أو مشاركة).
 */
object RemotePackSharing {

    private const val SHARE_DIR = "shared"

    /** يبني Intent مشاركة لحزمة، عبر ملف مؤقت يُقدَّم بـ FileProvider. */
    fun exportToShareIntent(context: Context, pack: RemotePack): Intent {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val safeName = buildString {
            append(pack.brand.ifBlank { "remote" })
            pack.model?.let { append("-").append(it) }
        }.replace(Regex("[^A-Za-z0-9._-]"), "_")

        val file = File(dir, "$safeName.${RemotePack.FILE_EXTENSION}")
        file.writeText(RemotePackCodec.encode(pack))

        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = RemotePack.MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * يقرأ حزمة من Intent وارد. يدعم:
     * 1. VIEW على ملف (content:// أو file://) — النقر على ملف مشترَك.
     * 2. رابط عميق tahakom://import?data=<base64> أو ?url=<...>.
     * 3. SEND (EXTRA_STREAM ملفاً، أو EXTRA_TEXT نصاً).
     *
     * يُرجع null إذا تعذّرت القراءة أو كان المحتوى غير صالح.
     */
    fun readFromIntent(context: Context, intent: Intent): RemotePack? {
        val text = when (intent.action) {
            Intent.ACTION_VIEW -> readFromViewData(context, intent.data)
            Intent.ACTION_SEND -> readFromSend(context, intent)
            else -> null
        } ?: return null
        return RemotePackCodec.decode(text)
    }

    private fun readFromViewData(context: Context, uri: Uri?): String? {
        uri ?: return null
        return when (uri.scheme) {
            // رابط عميق: tahakom://import?data=... (base64) — حزمة مضمّنة في الرابط نفسه
            RemotePack.DEEP_LINK_SCHEME -> uri.getQueryParameter("data")
                ?.let { runCatching { decodeBase64(it) }.getOrNull() }
            // ملف فعلي
            "content", "file" -> readUri(context, uri)
            else -> null
        }
    }

    private fun readFromSend(context: Context, intent: Intent): String? {
        @Suppress("DEPRECATION")
        val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (stream != null) return readUri(context, stream)
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }

    private fun readUri(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

    private fun decodeBase64(value: String): String =
        String(android.util.Base64.decode(value, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
}
