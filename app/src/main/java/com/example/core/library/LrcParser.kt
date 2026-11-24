package com.example.core.library

import com.example.data.LyricsData
import com.example.data.LyricsLine

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(rawContent: String?, sourceNote: String = "Letras locales (.lrc)"): LyricsData? {
        if (rawContent.isNullOrBlank()) return null
        val rawLines = rawContent.lines()
        val synced = mutableListOf<LyricsLine>()
        val plain = mutableListOf<String>()

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isEmpty()) {
                // Ignore ID3/LRC metadata headers like [ar:...] or [ti:...]
                if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                    plain.add(trimmed)
                }
                continue
            }
            val lyricText = trimmed.replace(TIMESTAMP_REGEX, "").trim()
            if (lyricText.isEmpty()) continue
            matches.forEach { m ->
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = m.groupValues[3]
                val msExtra = when (fracRaw.length) {
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> (fracRaw.toLongOrNull() ?: 0L)
                    else -> 0L
                }
                val totalMs = min * 60_000L + sec * 1_000L + msExtra
                synced.add(LyricsLine(timestampMs = totalMs, text = lyricText))
            }
        }

        if (synced.isNotEmpty()) {
            val ordered = synced.sortedBy { it.timestampMs }
            return LyricsData(
                lines = ordered,
                isSynced = true,
                sourceNote = sourceNote,
            )
        }
        if (plain.isNotEmpty()) {
            return LyricsData(
                lines = plain.map { LyricsLine(timestampMs = 0L, text = it) },
                isSynced = false,
                sourceNote = sourceNote,
            )
        }
        return null
    }
}
