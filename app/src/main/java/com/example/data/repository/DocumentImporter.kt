package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipInputStream

sealed class DocumentImportResult {
    data class Success(
        val suggestedTitle: String,
        val extractedText: String,
        val formatLabel: String,
        val characterCount: Int,
        val wordCount: Int
    ) : DocumentImportResult()

    data class ScannedPdfError(val message: String) : DocumentImportResult()
    data class UnsupportedFormatError(val message: String) : DocumentImportResult()
    data class EmptyDocumentError(val message: String) : DocumentImportResult()
    data class ExtractionError(val message: String) : DocumentImportResult()
}

class DocumentImporter(private val context: Context) {

    @Volatile
    private var pdfBoxInitialized = false

    suspend fun importDocument(uri: Uri): DocumentImportResult = withContext(Dispatchers.IO) {
        try {
            val fileName = queryDisplayName(uri) ?: "Imported Script"
            val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.US)
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
            val suggestedTitle = fileName.substringBeforeLast('.').ifBlank { "Imported Script" }

            when {
                ext == "txt" || mimeType == "text/plain" -> {
                    extractTxt(uri, suggestedTitle)
                }

                ext == "docx" ||
                    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                    extractDocx(uri, suggestedTitle)
                }

                ext == "pdf" || mimeType == "application/pdf" -> {
                    extractPdf(uri, suggestedTitle)
                }

                else -> {
                    DocumentImportResult.UnsupportedFormatError(
                        "Unsupported file format (${ext.ifBlank { mimeType.ifBlank { "unknown" } }}). Please select a TXT, PDF, or DOCX file."
                    )
                }
            }
        } catch (se: SecurityException) {
            DocumentImportResult.ExtractionError(
                "Permission denied while reading this document. Please re-select the file using the system picker."
            )
        } catch (e: Exception) {
            DocumentImportResult.ExtractionError(
                "Unable to import document: ${e.localizedMessage ?: "Unexpected file error."}"
            )
        }
    }

    private fun extractTxt(uri: Uri, suggestedTitle: String): DocumentImportResult {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: return DocumentImportResult.ExtractionError("Could not open the selected TXT file.")

        if (rawBytes.isEmpty()) {
            return DocumentImportResult.EmptyDocumentError("The selected TXT file is empty.")
        }

        val text = decodeUnicodeBytes(rawBytes)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()

        if (text.isBlank()) {
            return DocumentImportResult.EmptyDocumentError("The selected TXT file contains only whitespace.")
        }

        return buildSuccess(suggestedTitle, text, "TXT")
    }

    private fun extractDocx(uri: Uri, suggestedTitle: String): DocumentImportResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return DocumentImportResult.ExtractionError("Could not open the selected DOCX file.")

        return inputStream.use { stream ->
            var documentXmlBytes: ByteArray? = null
            ZipInputStream(BufferedInputStream(stream)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.equals("word/document.xml", ignoreCase = true)) {
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var len = zip.read(buffer)
                        while (len > 0) {
                            out.write(buffer, 0, len)
                            len = zip.read(buffer)
                        }
                        documentXmlBytes = out.toByteArray()
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val xmlBytes = documentXmlBytes
                ?: return@use DocumentImportResult.ExtractionError(
                    "Invalid DOCX archive: word/document.xml was not found."
                )

            val extracted = parseWordDocumentXml(xmlBytes.inputStream()).trim()
            if (extracted.isBlank()) {
                DocumentImportResult.EmptyDocumentError("This DOCX document does not contain readable text.")
            } else {
                buildSuccess(suggestedTitle, extracted, "DOCX")
            }
        }
    }

    internal fun parseWordDocumentXml(xmlStream: InputStream): String {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(xmlStream, "UTF-8")

        val paragraphs = mutableListOf<String>()
        val currentParagraph = StringBuilder()
        var inParagraph = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "p" -> {
                            inParagraph = true
                            currentParagraph.clear()
                        }

                        "t" -> {
                            if (inParagraph) {
                                currentParagraph.append(parser.nextText())
                            }
                        }

                        "tab" -> {
                            if (inParagraph) currentParagraph.append("    ")
                        }

                        "br", "cr" -> {
                            if (inParagraph) currentParagraph.append("\n")
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "p" && inParagraph) {
                        paragraphs.add(currentParagraph.toString())
                        inParagraph = false
                    }
                }
            }
            eventType = parser.next()
        }

        return paragraphs.joinToString("\n\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun extractPdf(uri: Uri, suggestedTitle: String): DocumentImportResult {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxInitialized = true
        }

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return DocumentImportResult.ExtractionError("Could not open the selected PDF file.")

        return inputStream.use { stream ->
            try {
                PDDocument.load(stream).use { document ->
                    if (document.isEncrypted) {
                        return@use DocumentImportResult.ExtractionError(
                            "This PDF is password-protected or encrypted and cannot be extracted."
                        )
                    }
                    val pageCount = document.numberOfPages
                    if (pageCount == 0) {
                        return@use DocumentImportResult.EmptyDocumentError("This PDF has no pages.")
                    }

                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                    }
                    val rawText = stripper.getText(document)
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .replace(Regex("\n{3,}"), "\n\n")
                        .trim()

                    if (rawText.isBlank()) {
                        return@use DocumentImportResult.ScannedPdfError(
                            "This PDF appears to be scanned or image-only ($pageCount page${if (pageCount > 1) "s" else ""}) with no embedded text layer. Smart Teleprompter Pro works 100% offline without cloud OCR. Please import a text-based PDF, DOCX, or TXT file."
                        )
                    }

                    buildSuccess(suggestedTitle, rawText, "PDF ($pageCount pg)")
                }
            } catch (e: Exception) {
                DocumentImportResult.ExtractionError(
                    "PDF extraction failed: ${e.localizedMessage ?: "The file may be damaged or unsupported."}"
                )
            }
        }
    }

    private fun decodeUnicodeBytes(bytes: ByteArray): String {
        return when {
            bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte() -> String(bytes, 3, bytes.size - 3, Charsets.UTF_8)

            bytes.size >= 2 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xFE.toByte() -> String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16LE"))

            bytes.size >= 2 &&
                bytes[0] == 0xFE.toByte() &&
                bytes[1] == 0xFF.toByte() -> String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE"))

            else -> String(bytes, Charsets.UTF_8)
        }
    }

    private fun buildSuccess(title: String, text: String, formatLabel: String): DocumentImportResult.Success {
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        return DocumentImportResult.Success(
            suggestedTitle = title,
            extractedText = text,
            formatLabel = formatLabel,
            characterCount = text.length,
            wordCount = words
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }
}
