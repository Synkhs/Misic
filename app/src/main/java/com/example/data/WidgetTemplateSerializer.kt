package com.example.data

import com.example.designsystem.ColorRoleOverrides
import com.example.designsystem.ContrastPreset
import com.example.designsystem.DEFAULT_VIOLET_SEED_ARGB
import com.example.designsystem.McuSchemeVariant
import com.example.designsystem.SavedColorProfile
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode

/**
 * Pure Kotlin JSON serializer/deserializer for widget templates, per-instance configs,
 * saved color profiles, custom EQ presets, local history/stats, and full backups
 * without relying on Android stubbed org.json in JVM tests.
 */
object WidgetTemplateSerializer {

    fun encodeTemplate(template: WidgetTemplate): String {
        val c = template.config
        return buildString {
            append("{")
            append("\"id\":\"").append(escape(template.id)).append("\",")
            append("\"name\":\"").append(escape(template.name)).append("\",")
            append("\"format\":\"").append(template.format.name).append("\",")
            append("\"style\":\"").append(c.style.name).append("\",")
            append("\"cornerRadiusDp\":").append(c.cornerRadiusDp).append(",")
            append("\"glassOpacity\":").append(c.glassOpacity).append(",")
            append("\"showProgressBar\":").append(c.showProgressBar).append(",")
            append("\"showServiceBadge\":").append(c.showServiceBadge).append(",")
            append("\"showCompactHeader\":").append(c.showCompactHeader).append(",")
            append("\"leftCustomAction\":\"").append(c.leftCustomAction.name).append("\",")
            append("\"rightCustomAction\":\"").append(c.rightCustomAction.name).append("\"")
            append("}")
        }
    }

    fun encodeTemplates(templates: List<WidgetTemplate>): String {
        return templates.joinToString(prefix = "[", postfix = "]", separator = ",") { encodeTemplate(it) }
    }

    fun decodeTemplate(json: String): WidgetTemplate? {
        val map = parseFlatJsonObject(json) ?: return null
        val id = map["id"]?.takeIf { it.isNotBlank() } ?: "tpl_${System.currentTimeMillis()}"
        val name = map["name"]?.takeIf { it.isNotBlank() } ?: "Plantilla"
        val format = parseEnum(map["format"], WidgetFormat.CARD_4X2)
        val style = migrateWidgetStyleRaw(map["style"])
        val cornerRadius = map["cornerRadiusDp"]?.toIntOrNull()?.coerceIn(12, 32) ?: 24
        val opacity = map["glassOpacity"]?.toFloatOrNull()?.coerceIn(0.45f, 1.0f) ?: 0.94f
        val showProgress = map["showProgressBar"]?.toBooleanStrictOrNull() ?: true
        val showBadge = map["showServiceBadge"]?.toBooleanStrictOrNull() ?: true
        val showCompact = map["showCompactHeader"]?.toBooleanStrictOrNull() ?: true
        val left = parseEnum(map["leftCustomAction"], CustomControlAction.REWIND_15)
        val right = parseEnum(map["rightCustomAction"], CustomControlAction.FORWARD_15)

        return WidgetTemplate(
            id = id,
            name = name,
            format = format,
            config = WidgetConfig(
                style = style,
                cornerRadiusDp = cornerRadius,
                glassOpacity = opacity,
                showProgressBar = showProgress,
                showServiceBadge = showBadge,
                showCompactHeader = showCompact,
                leftCustomAction = left,
                rightCustomAction = right,
            ),
        )
    }

    fun decodeTemplates(json: String): List<WidgetTemplate> {
        val trimmed = json.trim()
        if (trimmed.isBlank()) return emptyList()
        if (trimmed.startsWith("{")) {
            return listOfNotNull(decodeTemplate(trimmed))
        }
        if (!trimmed.startsWith("[")) return emptyList()
        return splitTopLevelObjects(trimmed).mapNotNull { decodeTemplate(it) }
    }

    fun encodeHistory(entries: List<TrackHistoryEntry>): String {
        return entries.joinToString(prefix = "[", postfix = "]", separator = ",") { item ->
            buildString {
                append("{")
                append("\"id\":\"").append(escape(item.id)).append("\",")
                append("\"title\":\"").append(escape(item.title)).append("\",")
                append("\"artist\":\"").append(escape(item.artist)).append("\",")
                append("\"album\":\"").append(escape(item.album)).append("\",")
                append("\"packageName\":\"").append(escape(item.packageName)).append("\",")
                append("\"serviceId\":\"").append(escape(item.serviceId)).append("\",")
                append("\"lastPlayedMs\":").append(item.lastPlayedMs).append(",")
                append("\"playCount\":").append(item.playCount).append(",")
                append("\"durationMs\":").append(item.durationMs)
                append("}")
            }
        }
    }

    fun decodeHistory(json: String): List<TrackHistoryEntry> {
        if (json.isBlank()) return emptyList()
        return splitTopLevelObjects(json).mapNotNull { obj ->
            val map = parseFlatJsonObject(obj) ?: return@mapNotNull null
            val id = map["id"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val title = map["title"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            TrackHistoryEntry(
                id = id,
                title = title,
                artist = map["artist"].orEmpty(),
                album = map["album"].orEmpty(),
                packageName = map["packageName"].orEmpty(),
                serviceId = map["serviceId"] ?: MusicServiceApp.OTHER.id,
                lastPlayedMs = map["lastPlayedMs"]?.toLongOrNull() ?: 0L,
                playCount = map["playCount"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                durationMs = map["durationMs"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
            )
        }
    }

    fun encodeColorProfiles(profiles: List<SavedColorProfile>): String {
        return profiles.joinToString(prefix = "[", postfix = "]", separator = ",") { p ->
            buildString {
                append("{")
                append("\"id\":\"").append(escape(p.id)).append("\",")
                append("\"name\":\"").append(escape(p.name)).append("\",")
                append("\"seedColorArgb\":").append(p.seedColorArgb).append(",")
                append("\"variant\":\"").append(p.variant.name).append("\",")
                append("\"themeMode\":\"").append(p.themeMode.name).append("\",")
                append("\"surfaceTone\":\"").append(p.surfaceToneStyle.name).append("\",")
                append("\"contrast\":\"").append(p.contrastPreset.name).append("\",")
                append("\"pureAmoled\":").append(p.pureAmoled).append(",")
                append("\"primaryOv\":").append(p.overrides.primaryArgb ?: 0L).append(",")
                append("\"secondaryOv\":").append(p.overrides.secondaryArgb ?: 0L).append(",")
                append("\"tertiaryOv\":").append(p.overrides.tertiaryArgb ?: 0L).append(",")
                append("\"surfaceOv\":").append(p.overrides.surfaceArgb ?: 0L)
                append("}")
            }
        }
    }

    fun decodeColorProfiles(json: String): List<SavedColorProfile> {
        val trimmed = json.trim()
        if (trimmed.isBlank()) return emptyList()
        return splitTopLevelObjects(trimmed).mapNotNull { obj ->
            val m = parseFlatJsonObject(obj) ?: return@mapNotNull null
            val id = m["id"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = m["name"]?.takeIf { it.isNotBlank() } ?: "Perfil"
            SavedColorProfile(
                id = id,
                name = name,
                seedColorArgb = m["seedColorArgb"]?.toLongOrNull() ?: DEFAULT_VIOLET_SEED_ARGB,
                variant = parseEnum(m["variant"], McuSchemeVariant.TONAL_SPOT),
                themeMode = parseEnum(m["themeMode"], ThemeMode.DARK),
                surfaceToneStyle = parseEnum(m["surfaceTone"], SurfaceToneStyle.TONAL),
                contrastPreset = parseEnum(m["contrast"], ContrastPreset.STANDARD),
                pureAmoled = m["pureAmoled"]?.toBooleanStrictOrNull() ?: false,
                overrides = ColorRoleOverrides(
                    primaryArgb = m["primaryOv"]?.toLongOrNull()?.takeIf { it != 0L },
                    secondaryArgb = m["secondaryOv"]?.toLongOrNull()?.takeIf { it != 0L },
                    tertiaryArgb = m["tertiaryOv"]?.toLongOrNull()?.takeIf { it != 0L },
                    surfaceArgb = m["surfaceOv"]?.toLongOrNull()?.takeIf { it != 0L },
                ),
            )
        }
    }

    fun encodeWidgetInstances(instances: Map<Int, WidgetInstanceConfig>): String {
        return instances.values.joinToString(prefix = "[", postfix = "]", separator = ",") { w ->
            buildString {
                append("{")
                append("\"appWidgetId\":").append(w.appWidgetId).append(",")
                append("\"format\":\"").append(w.format.name).append("\",")
                append("\"style\":\"").append(w.style.name).append("\",")
                append("\"colorSource\":\"").append(w.colorSource.name).append("\",")
                append("\"seedColorArgb\":").append(w.seedColorArgb).append(",")
                append("\"schemeVariant\":\"").append(w.schemeVariant.name).append("\",")
                append("\"themeMode\":\"").append(w.themeMode.name).append("\",")
                append("\"contrastLevel\":\"").append(w.contrastLevel.name).append("\",")
                append("\"primaryOv\":").append(w.roleOverrides.primaryArgb ?: 0L).append(",")
                append("\"secondaryOv\":").append(w.roleOverrides.secondaryArgb ?: 0L).append(",")
                append("\"tertiaryOv\":").append(w.roleOverrides.tertiaryArgb ?: 0L).append(",")
                append("\"surfaceOv\":").append(w.roleOverrides.surfaceArgb ?: 0L).append(",")
                append("\"cornerRadiusDp\":").append(w.cornerRadiusDp).append(",")
                append("\"containerOpacity\":").append(w.containerOpacity).append(",")
                append("\"showProgressBar\":").append(w.showProgressBar).append(",")
                append("\"showServiceBadge\":").append(w.showServiceBadge).append(",")
                append("\"showCompactHeader\":").append(w.showCompactHeader).append(",")
                append("\"leftCustomAction\":\"").append(w.leftCustomAction.name).append("\",")
                append("\"rightCustomAction\":\"").append(w.rightCustomAction.name).append("\"")
                append("}")
            }
        }
    }

    fun decodeWidgetInstances(json: String): Map<Int, WidgetInstanceConfig> {
        val trimmed = json.trim()
        if (trimmed.isBlank()) return emptyMap()
        return splitTopLevelObjects(trimmed).mapNotNull { obj ->
            val m = parseFlatJsonObject(obj) ?: return@mapNotNull null
            val id = m["appWidgetId"]?.toIntOrNull() ?: return@mapNotNull null
            id to WidgetInstanceConfig(
                appWidgetId = id,
                format = parseEnum(m["format"], WidgetFormat.CARD_4X2),
                style = migrateWidgetStyleRaw(m["style"]),
                colorSource = parseEnum(m["colorSource"], WidgetColorSource.FOLLOW_APP),
                seedColorArgb = m["seedColorArgb"]?.toLongOrNull() ?: DEFAULT_VIOLET_SEED_ARGB,
                schemeVariant = parseEnum(m["schemeVariant"], McuSchemeVariant.TONAL_SPOT),
                themeMode = parseEnum(m["themeMode"], ThemeMode.SYSTEM),
                contrastLevel = parseEnum(m["contrastLevel"], ContrastPreset.STANDARD),
                roleOverrides = ColorRoleOverrides(
                    primaryArgb = m["primaryOv"]?.toLongOrNull()?.takeIf { it != 0L },
                    secondaryArgb = m["secondaryOv"]?.toLongOrNull()?.takeIf { it != 0L },
                    tertiaryArgb = m["tertiaryOv"]?.toLongOrNull()?.takeIf { it != 0L },
                    surfaceArgb = m["surfaceOv"]?.toLongOrNull()?.takeIf { it != 0L },
                ),
                cornerRadiusDp = m["cornerRadiusDp"]?.toIntOrNull()?.coerceIn(12, 32) ?: 24,
                containerOpacity = m["containerOpacity"]?.toFloatOrNull()?.coerceIn(0.45f, 1.0f) ?: 0.94f,
                showProgressBar = m["showProgressBar"]?.toBooleanStrictOrNull() ?: true,
                showServiceBadge = m["showServiceBadge"]?.toBooleanStrictOrNull() ?: true,
                showCompactHeader = m["showCompactHeader"]?.toBooleanStrictOrNull() ?: true,
                leftCustomAction = parseEnum(m["leftCustomAction"], CustomControlAction.REWIND_15),
                rightCustomAction = parseEnum(m["rightCustomAction"], CustomControlAction.FORWARD_15),
            )
        }.toMap()
    }

    fun encodeCustomEqPresets(presets: List<CustomEqPreset>): String {
        return presets.joinToString(prefix = "[", postfix = "]", separator = ",") { p ->
            buildString {
                append("{")
                append("\"id\":\"").append(escape(p.id)).append("\",")
                append("\"name\":\"").append(escape(p.name)).append("\",")
                append("\"bandsCsv\":\"").append(p.bandGainsDb.joinToString(",")).append("\",")
                append("\"bassBoost\":").append(p.bassBoostStrength).append(",")
                append("\"virtualizer\":").append(p.virtualizerStrength).append(",")
                append("\"loudness\":").append(p.loudnessGainMb).append(",")
                append("\"preamp\":").append(p.preampGainDb)
                append("}")
            }
        }
    }

    fun decodeCustomEqPresets(json: String): List<CustomEqPreset> {
        val trimmed = json.trim()
        if (trimmed.isBlank()) return emptyList()
        return splitTopLevelObjects(trimmed).mapNotNull { obj ->
            val m = parseFlatJsonObject(obj) ?: return@mapNotNull null
            val id = m["id"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = m["name"]?.takeIf { it.isNotBlank() } ?: "Personalizado"
            val bands = m["bandsCsv"].orEmpty().split(",").mapNotNull { it.toFloatOrNull()?.coerceIn(-12f, 12f) }
            val fixed = if (bands.size == 5) bands else listOf(0f, 0f, 0f, 0f, 0f)
            CustomEqPreset(
                id = id,
                name = name,
                bandGainsDb = fixed,
                bassBoostStrength = (m["bassBoost"]?.toIntOrNull() ?: 0).coerceIn(0, 1000),
                virtualizerStrength = (m["virtualizer"]?.toIntOrNull() ?: 0).coerceIn(0, 1000),
                loudnessGainMb = (m["loudness"]?.toIntOrNull() ?: 0).coerceIn(0, 1000),
                preampGainDb = (m["preamp"]?.toFloatOrNull() ?: 0f).coerceIn(-6f, 6f),
            )
        }
    }

    fun migrateWidgetStyleRaw(raw: String?): WidgetStyle {
        if (raw.isNullOrBlank()) return WidgetStyle.TONAL
        return when (raw) {
            "CAPSULE_DARK" -> WidgetStyle.SURFACE
            "FROSTED_GLASS" -> WidgetStyle.TONAL
            "PORCELAIN_LIGHT" -> WidgetStyle.ARTWORK
            "DYNAMIC_NEON" -> WidgetStyle.TRANSPARENT
            else -> runCatching { WidgetStyle.valueOf(raw) }.getOrDefault(WidgetStyle.TONAL)
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    private fun unescape(value: String): String = buildString(value.length) {
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> append('\\')
                    '"' -> append('"')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    else -> append(value[i + 1])
                }
                i += 2
            } else {
                append(ch)
                i++
            }
        }
    }

    private fun splitTopLevelObjects(raw: String): List<String> {
        val result = mutableListOf<String>()
        var inString = false
        var escaped = false
        var depth = 0
        var startIdx = -1
        for (i in raw.indices) {
            val c = raw[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) startIdx = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && startIdx >= 0) {
                        result.add(raw.substring(startIdx, i + 1))
                        startIdx = -1
                    }
                }
            }
        }
        return result
    }

    private fun parseFlatJsonObject(raw: String): Map<String, String>? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val body = trimmed.substring(1, trimmed.length - 1).trim()
        if (body.isEmpty()) return emptyMap()

        val pairs = mutableListOf<String>()
        var inString = false
        var escaped = false
        var tokenStart = 0
        for (i in body.indices) {
            val c = body[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
            } else if (c == ',' && !inString) {
                pairs.add(body.substring(tokenStart, i))
                tokenStart = i + 1
            }
        }
        if (tokenStart < body.length) {
            pairs.add(body.substring(tokenStart))
        }

        val map = mutableMapOf<String, String>()
        pairs.forEach { pair ->
            val colonIdx = findColonOutsideQuotes(pair) ?: return@forEach
            val key = stripQuotes(pair.substring(0, colonIdx).trim())
            val valRaw = pair.substring(colonIdx + 1).trim()
            val value = if (valRaw.startsWith("\"") && valRaw.endsWith("\"")) {
                unescape(stripQuotes(valRaw))
            } else {
                valRaw
            }
            map[key] = value
        }
        return map
    }

    private fun findColonOutsideQuotes(str: String): Int? {
        var inString = false
        var escaped = false
        for (i in str.indices) {
            val c = str[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                continue
            }
            if (c == '"') inString = !inString
            else if (c == ':' && !inString) return i
        }
        return null
    }

    private fun stripQuotes(s: String): String {
        return if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s.substring(1, s.length - 1)
        } else {
            s
        }
    }
}
