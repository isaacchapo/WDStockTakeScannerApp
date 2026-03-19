package com.example.stockapp.data

import android.util.Log
import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Parses QR code data and extracts dynamic identifiers and raw data.
 */
object QrDataParser {
    private const val REQUIRED_IDENTIFIER_COUNT = 3

    private val identifierPriority = listOf(
        "itemid",
        "productid",
        "sku",
        "barcode",
        "serialno",
        "serialnumber",
        "batchno",
        "lotno",
        "partno",
        "modelno",
        "orderno",
        "code",
        "id",
        "uid"
    )

    private val deEmphasizedTokens = listOf(
        "qty",
        "quantity",
        "count",
        "price",
        "amount",
        "date",
        "time",
        "timestamp",
        "owner",
        "location",
        "description"
    )

    /**
     * Parsed QR data model.
     */
    data class ParsedQrData(
        val fields: LinkedHashMap<String, String>,
        val variableData: String = "{}"
    )

    /**
     * Parses raw QR barcode data and extracts all fields as JSON.
     *
     * @param rawValue The raw barcode value to parse
     * @return ParsedQrData with extracted fields and variableData, or null if parsing fails
     */
    fun parseQrData(rawValue: String): ParsedQrData? {
        val trimmed = sanitizePayload(rawValue.trim().removePrefix("\uFEFF"))
        if (trimmed.isBlank()) return null

        val candidates = linkedSetOf<String>()
        candidates.add(trimmed)

        val noParens = unwrapSurroundingParentheses(trimmed)
        if (noParens.isNotBlank()) candidates.add(noParens)

        extractJsonObject(trimmed)?.let(candidates::add)
        extractJsonObject(noParens)?.let(candidates::add)

        decodeQuotedJson(trimmed)?.let { decoded ->
            candidates.add(decoded)
            extractJsonObject(decoded)?.let(candidates::add)
        }

        decodeEscapedCandidate(trimmed)?.let { decoded ->
            candidates.add(decoded)
            val decodedNoParens = unwrapSurroundingParentheses(decoded)
            if (decodedNoParens.isNotBlank()) candidates.add(decodedNoParens)
            extractJsonObject(decoded)?.let(candidates::add)
            extractJsonObject(decodedNoParens)?.let(candidates::add)
            decodeQuotedJson(decoded)?.let { decodedQuoted ->
                candidates.add(decodedQuoted)
                extractJsonObject(decodedQuoted)?.let(candidates::add)
            }
        }

        decodePercentEncoded(trimmed)?.let { decoded ->
            candidates.add(decoded)
            val decodedNoParens = unwrapSurroundingParentheses(decoded)
            if (decodedNoParens.isNotBlank()) candidates.add(decodedNoParens)
            extractJsonObject(decoded)?.let(candidates::add)
            extractJsonObject(decodedNoParens)?.let(candidates::add)
            decodeQuotedJson(decoded)?.let { decodedQuoted ->
                candidates.add(decodedQuoted)
                extractJsonObject(decodedQuoted)?.let(candidates::add)
            }
        }

        decodeBase64Candidate(trimmed)?.let { decoded ->
            candidates.add(decoded)
            val decodedNoParens = unwrapSurroundingParentheses(decoded)
            if (decodedNoParens.isNotBlank()) candidates.add(decodedNoParens)
            extractJsonObject(decoded)?.let(candidates::add)
            extractJsonObject(decodedNoParens)?.let(candidates::add)
            decodeEscapedCandidate(decoded)?.let { escapedDecoded ->
                candidates.add(escapedDecoded)
                extractJsonObject(escapedDecoded)?.let(candidates::add)
            }
            decodeQuotedJson(decoded)?.let { decodedQuoted ->
                candidates.add(decodedQuoted)
                extractJsonObject(decodedQuoted)?.let(candidates::add)
            }
        }

        for (candidate in candidates) {
            parseJsonCandidate(candidate)?.let { return it }
        }

        for (candidate in candidates) {
            parseKeyValueCandidate(candidate)?.let { return it }
        }

        for (candidate in candidates) {
            parseGs1Candidate(candidate)?.let { return it }
        }

        Log.e("QrDataParser", "Could not parse QR data: $trimmed")
        return null
    }

    fun selectMajorIdentifierKeys(
        fields: Map<String, String>,
        requiredCount: Int = REQUIRED_IDENTIFIER_COUNT
    ): List<String> {
        if (requiredCount <= 0) return emptyList()

        val nonBlankEntries = fields
            .filter { (_, value) -> value.trim().isNotBlank() }
            .map { (key, _) ->
                ScoredField(
                    key = key,
                    normalizedKey = normalizeKey(key),
                    score = scoreKey(key)
                )
            }

        if (nonBlankEntries.size < requiredCount) return emptyList()

        val ordered = nonBlankEntries
            .sortedWith(
                compareByDescending<ScoredField> { it.score }
                    .thenBy { it.normalizedKey }
            )

        return ordered.take(requiredCount).map { it.key }
    }

    fun getFieldValue(fields: Map<String, String>, fieldName: String): String {
        val direct = fields[fieldName]
        if (!direct.isNullOrBlank()) return direct

        val normalizedTarget = normalizeKey(fieldName)
        return fields.entries.firstOrNull { normalizeKey(it.key) == normalizedTarget }?.value.orEmpty()
    }

    private fun parseJsonCandidate(candidate: String): ParsedQrData? {
        return try {
            val jsonObj = JSONObject(candidate)
            val fields = linkedMapOf<String, String>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rawValue = jsonObj.opt(key)
                val normalizedValue = when (rawValue) {
                    null, JSONObject.NULL -> ""
                    else -> rawValue.toString().trim()
                }
                fields[key] = normalizedValue
            }
            if (fields.isEmpty()) return null
            val variableData = jsonObj.toString()
            ParsedQrData(fields = LinkedHashMap(fields), variableData = variableData)
        } catch (e: Exception) {
            null
        }
    }

    private fun unwrapSurroundingParentheses(s: String): String {
        if (s.startsWith("(") && s.endsWith(")")) {
            return s.substring(1, s.length - 1)
        }
        return ""
    }

    private fun extractJsonObject(s: String): String? {
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        return if (start >= 0 && end > start) s.substring(start, end + 1) else null
    }

    private fun decodeQuotedJson(s: String): String? {
        if (!s.startsWith("\"") || !s.endsWith("\"")) return null
        return try {
            val unquoted = s.substring(1, s.length - 1)
            val unescaped = unquoted.replace("\\\"", "\"")
            if (unescaped.startsWith("{") && unescaped.endsWith("}")) unescaped else null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodePercentEncoded(s: String): String? {
        if (!s.contains('%')) return null
        return try {
            val decoded = URLDecoder.decode(s, StandardCharsets.UTF_8.name()).trim()
            val sanitized = sanitizePayload(decoded)
            if (sanitized.isNotBlank() && sanitized != s) sanitized else null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeEscapedCandidate(s: String): String? {
        if (!s.contains('\\')) return null
        val decoded = sanitizePayload(unescapeJsonEscapes(s).trim())
        return if (decoded.isNotBlank() && decoded != s) decoded else null
    }

    private fun decodeBase64Candidate(s: String): String? {
        val compact = s.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
        if (compact.length < 8) return null

        val normalized = compact
            .replace('-', '+')
            .replace('_', '/')
        if (!normalized.matches(Regex("^[A-Za-z0-9+/=]+$"))) return null

        val padded = when (normalized.length % 4) {
            0 -> normalized
            2 -> "$normalized=="
            3 -> "$normalized="
            else -> return null
        }

        return runCatching {
            val decodedBytes = Base64.decode(padded, Base64.DEFAULT)
            val decoded = String(decodedBytes, StandardCharsets.UTF_8).trim()
            val sanitized = sanitizePayload(decoded)
            if (
                sanitized.isNotBlank() &&
                (sanitized.contains('{') ||
                    sanitized.contains('=') ||
                    sanitized.contains(':') ||
                    sanitized.contains('&'))
            ) {
                sanitized
            } else {
                null
            }
        }.getOrNull()
    }

    private fun parseKeyValueCandidate(candidate: String): ParsedQrData? {
        val trimmed = sanitizePayload(candidate.trim())
        if (trimmed.isBlank()) return null

        val querySegment = trimmed.substringAfter('?', "")
        val parseTarget = when {
            querySegment.contains('=') -> querySegment
            trimmed.contains('=') || trimmed.contains(':') -> trimmed
            else -> return null
        }

        val cleaned = parseTarget
            .removePrefix("{")
            .removeSuffix("}")
            .trim()
        if (cleaned.isBlank()) return null

        val tokens = splitKeyValueTokens(cleaned)
        if (tokens.isEmpty()) return null

        val fields = linkedMapOf<String, String>()
        for (token in tokens) {
            val entry = token.trim()
            if (entry.isBlank()) continue

            val separatorIndex = firstSeparatorIndex(entry) ?: continue
            val key = entry.substring(0, separatorIndex)
                .trim()
                .trim('"', '\'')
            val value = entry.substring(separatorIndex + 1)
                .trim()
                .trim('"', '\'')
                .let(::sanitizePayload)

            if (key.isNotBlank()) {
                fields[key] = value
            }
        }

        if (fields.isEmpty()) return null

        val jsonObj = JSONObject()
        fields.forEach { (key, value) ->
            jsonObj.put(key, value)
        }
        return ParsedQrData(fields = LinkedHashMap(fields), variableData = jsonObj.toString())
    }

    private fun parseGs1Candidate(candidate: String): ParsedQrData? {
        val trimmed = sanitizePayload(candidate.trim())
        if (trimmed.isBlank()) return null

        val fields = linkedMapOf<String, String>()

        // Common readable GS1 representation, e.g. (01)123...(21)ABC...
        Regex("\\((\\d{2,4})\\)([^()\\u001D]+)")
            .findAll(trimmed)
            .forEach { match ->
                val ai = match.groupValues[1]
                val value = match.groupValues[2].trim()
                if (value.isNotBlank()) {
                    fields["AI$ai"] = value
                }
            }

        // Raw GS1 payloads may use ASCII 29 (Group Separator / FNC1)
        if (fields.isEmpty() && trimmed.contains('\u001D')) {
            trimmed
                .split('\u001D')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEachIndexed { index, segment ->
                    val aiMatch = Regex("^(\\d{2,4})(.+)$").find(segment)
                    if (aiMatch != null) {
                        val ai = aiMatch.groupValues[1]
                        val value = aiMatch.groupValues[2].trim()
                        if (value.isNotBlank()) {
                            fields["AI$ai"] = value
                        }
                    } else {
                        fields["FIELD${index + 1}"] = segment
                    }
                }
        }

        if (fields.isEmpty()) return null

        val jsonObj = JSONObject()
        fields.forEach { (key, value) ->
            jsonObj.put(key, value)
        }
        return ParsedQrData(fields = LinkedHashMap(fields), variableData = jsonObj.toString())
    }

    private fun splitKeyValueTokens(input: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var inDoubleQuote = false
        var inSingleQuote = false

        for (ch in input) {
            when (ch) {
                '"' -> {
                    if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                    current.append(ch)
                }
                '\'' -> {
                    if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                    current.append(ch)
                }
                ',', ';', '|', '&', '\n', '\r' -> {
                    if (!inDoubleQuote && !inSingleQuote) {
                        val token = current.toString().trim()
                        if (token.isNotEmpty()) parts.add(token)
                        current.clear()
                    } else {
                        current.append(ch)
                    }
                }
                else -> current.append(ch)
            }
        }

        val tail = current.toString().trim()
        if (tail.isNotEmpty()) parts.add(tail)
        return parts
    }

    private fun firstSeparatorIndex(token: String): Int? {
        val equalsIndex = token.indexOf('=')
        if (equalsIndex > 0) return equalsIndex

        if (token.contains("://")) return null
        val colonIndex = token.indexOf(':')
        return if (colonIndex > 0) colonIndex else null
    }

    private fun unescapeJsonEscapes(input: String): String {
        val out = StringBuilder(input.length)
        var index = 0
        while (index < input.length) {
            val ch = input[index]
            if (ch == '\\' && index + 1 < input.length) {
                val next = input[index + 1]
                when (next) {
                    '"', '\\', '/' -> {
                        out.append(next)
                        index += 2
                        continue
                    }
                    'b' -> {
                        out.append('\b')
                        index += 2
                        continue
                    }
                    'f' -> {
                        out.append('\u000C')
                        index += 2
                        continue
                    }
                    'n' -> {
                        out.append('\n')
                        index += 2
                        continue
                    }
                    'r' -> {
                        out.append('\r')
                        index += 2
                        continue
                    }
                    't' -> {
                        out.append('\t')
                        index += 2
                        continue
                    }
                    'u' -> {
                        if (index + 5 < input.length) {
                            val hex = input.substring(index + 2, index + 6)
                            val codePoint = hex.toIntOrNull(16)
                            if (codePoint != null) {
                                out.append(codePoint.toChar())
                                index += 6
                                continue
                            }
                        }
                    }
                }
            }
            out.append(ch)
            index += 1
        }
        return out.toString()
    }

    private fun sanitizePayload(raw: String): String {
        if (raw.isBlank()) return raw
        return raw.filterNot { ch ->
            ch.code in 0..31 && ch != '\n' && ch != '\r' && ch != '\t' && ch != '\u001D'
        }
    }

    private fun normalizeKey(rawKey: String): String {
        return rawKey
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun scoreKey(rawKey: String): Int {
        val normalized = normalizeKey(rawKey)
        if (normalized.isBlank()) return Int.MIN_VALUE

        var score = 0
        val priorityIndex = identifierPriority.indexOf(normalized)
        if (priorityIndex >= 0) {
            score += 200 - priorityIndex
        }

        if (normalized.contains("id")) score += 80
        if (normalized.contains("code")) score += 70
        if (normalized.contains("sku")) score += 60
        if (normalized.contains("serial")) score += 50
        if (normalized.contains("batch")) score += 40
        if (normalized.contains("lot")) score += 35
        if (normalized.contains("order")) score += 30
        if (normalized.contains("part")) score += 25
        if (normalized.contains("model")) score += 20

        if (deEmphasizedTokens.any { token -> normalized.contains(token) }) {
            score -= 50
        }

        return score
    }

    private data class ScoredField(
        val key: String,
        val normalizedKey: String,
        val score: Int
    )
}
