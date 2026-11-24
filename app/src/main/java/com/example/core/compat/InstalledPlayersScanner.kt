package com.example.core.compat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.data.MusicServiceApp

data class DiscoveredPlayerApp(
    val packageName: String,
    val appName: String,
    val serviceCategory: MusicServiceApp,
    val isInstalled: Boolean,
    val isActiveSession: Boolean,
    val isVideoLikely: Boolean,
    val icon: Drawable? = null,
)

object InstalledPlayersScanner {

    private val knownVideoPackages = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "tv.twitch.android.app",
    )

    fun isLikelyVideoApp(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (knownVideoPackages.contains(packageName)) return true
        val lower = packageName.lowercase()
        return (lower.contains("youtube") && !lower.contains("youtube.music")) ||
            lower.contains("netflix") ||
            lower.contains("twitch") ||
            lower.contains("video")
    }

    fun scanPlayers(
        context: Context,
        activeSessionPackages: Set<String>,
    ): List<DiscoveredPlayerApp> {
        val pm = context.packageManager
        val discoveredByPkg = linkedMapOf<String, DiscoveredPlayerApp>()

        // 1. Query dynamic apps declaring CATEGORY_APP_MUSIC on any Android device
        val musicIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
        val resolvedMusicApps = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(musicIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(musicIntent, 0)
            }
        }.getOrDefault(emptyList())

        for (resolveInfo in resolvedMusicApps) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
            val label = runCatching { resolveInfo.loadLabel(pm).toString() }.getOrDefault(pkg)
            val icon = runCatching { resolveInfo.loadIcon(pm) }.getOrNull()
            discoveredByPkg[pkg] = DiscoveredPlayerApp(
                packageName = pkg,
                appName = label,
                serviceCategory = MusicServiceApp.fromPackageName(pkg),
                isInstalled = true,
                isActiveSession = activeSessionPackages.contains(pkg),
                isVideoLikely = isLikelyVideoApp(pkg),
                icon = icon,
            )
        }

        // 2. Ensure popular streaming services appear even if not installed (for quick install/link)
        for (svc in MusicServiceApp.supportedThirdParty) {
            if (svc.packageName.isBlank()) continue
            if (discoveredByPkg.containsKey(svc.packageName)) continue
            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(svc.packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(svc.packageName, 0)
                }
            }.getOrNull()

            val installed = appInfo != null
            val realLabel = appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
            val realIcon = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }

            discoveredByPkg[svc.packageName] = DiscoveredPlayerApp(
                packageName = svc.packageName,
                appName = realLabel ?: svc.displayName,
                serviceCategory = svc,
                isInstalled = installed,
                isActiveSession = activeSessionPackages.contains(svc.packageName),
                isVideoLikely = false,
                icon = realIcon,
            )
        }

        // 3. Include any other active MediaSession player discovered at runtime
        for (activePkg in activeSessionPackages) {
            if (activePkg == context.packageName || discoveredByPkg.containsKey(activePkg)) continue
            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(activePkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(activePkg, 0)
                }
            }.getOrNull()
            val realLabel = appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
            val realIcon = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }

            discoveredByPkg[activePkg] = DiscoveredPlayerApp(
                packageName = activePkg,
                appName = realLabel ?: MusicServiceApp.fromPackageName(activePkg).displayName,
                serviceCategory = MusicServiceApp.fromPackageName(activePkg),
                isInstalled = appInfo != null,
                isActiveSession = true,
                isVideoLikely = isLikelyVideoApp(activePkg),
                icon = realIcon,
            )
        }

        return discoveredByPkg.values.sortedWith(
            compareByDescending<DiscoveredPlayerApp> { it.isActiveSession }
                .thenByDescending { it.isInstalled }
                .thenBy { it.appName.lowercase() },
        )
    }
}
