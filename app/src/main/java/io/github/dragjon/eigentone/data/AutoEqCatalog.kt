package io.github.dragjon.eigentone.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class CatalogEntry(val name: String, val source: String, val path: String) {
    val title: String get() = "$name ($source)"

    /** Over-ear measurements say so in their path. Everything else in the index sits in or at the ear. */
    val form: HeadphoneForm get() = if ("over-ear" in path) HeadphoneForm.OVER_EAR else HeadphoneForm.IN_EAR
}

/** Searches AutoEq's published headphone profiles, downloaded from its GitHub repository. */
object AutoEqCatalog {
    private const val BASE = "https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master/results/"
    private const val MAX_INDEX_BYTES = 8 * 1024 * 1024
    private const val MAX_PROFILE_BYTES = 256 * 1024
    private const val MAX_RESULT_BYTES = 1024 * 1024
    private const val CACHE_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    private const val TIMEOUT_MS = 20_000
    private const val MAX_MEASUREMENTS = 5

    private val line = Regex("""^- \[(.+?)]\((\./.+)\) by (.+)$""")

    /** Parses INDEX.md. Lines that aren't profile links, or that try to leave the results, are skipped. */
    fun parseIndex(text: String): List<CatalogEntry> = text.lineSequence().mapNotNull { raw ->
        val m = line.find(raw.trim()) ?: return@mapNotNull null
        val path = m.groupValues[2]
        if (".." in path) return@mapNotNull null
        CatalogEntry(m.groupValues[1], m.groupValues[3], path.removePrefix("./"))
    }.toList()

    /** Every word must match, in any order, so "liberty 4 nc" finds "Anker Soundcore Liberty 4 NC". */
    fun search(entries: List<CatalogEntry>, query: String, limit: Int = 50): List<CatalogEntry> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return entries.asSequence()
            .filter { entry -> words.all { it in entry.title.lowercase() } }
            .take(limit)
            .toList()
    }

    /**
     * The profile for a connected device, judged by its name. The device's words must be the end of the
     * profile's name, so "Liberty 4" won't pick "Liberty 4 NC", while a leading brand such as "Sony" is
     * allowed. Vague names like "Buds" find nothing rather than the wrong headphones.
     */
    fun bestMatch(entries: List<CatalogEntry>, deviceName: String): CatalogEntry? {
        val device = words(deviceName.replace(Regex("""^.*?['’]s\s+"""), ""))
        val specific = device.size >= 2 || device.any { word -> word.any(Char::isDigit) && word.length >= 3 }
        if (!specific) return null
        return entries
            .filter { entry ->
                val name = words(entry.name.replace(Regex("""\(.*?\)"""), ""))
                name.size >= device.size && name.takeLast(device.size) == device
            }
            .minWithOrNull(
                compareBy<CatalogEntry> { entry ->
                    SOURCE_PREFERENCE.indexOfFirst { it in entry.source.lowercase() }.let { if (it < 0) SOURCE_PREFERENCE.size else it }
                }.thenBy { it.name.length },
            )
    }

    /**
     * Every published measurement of the same headphones, best-regarded sources first. Limited, because
     * each has to be downloaded and few people can tell more than a handful apart.
     */
    fun measurementsOf(entries: List<CatalogEntry>, entry: CatalogEntry, limit: Int = MAX_MEASUREMENTS): List<CatalogEntry> {
        val name = words(entry.name.replace(Regex("""\(.*?\)"""), ""))
        val same = entries.filter { other -> words(other.name.replace(Regex("""\(.*?\)"""), "")) == name }
        val ranked = same.sortedBy { other ->
            SOURCE_PREFERENCE.indexOfFirst { it in other.source.lowercase() }.let { if (it < 0) SOURCE_PREFERENCE.size else it }
        }
        // The one that was picked always stays in, even if there are many others.
        return (listOf(entry) + ranked.filterNot { it == entry }).take(limit)
    }

    /**
     * What tells each measurement apart: who measured it, and which version of the headphones, such as
     * "crinacle on GRAS 43AG-7 (2020, worn earpads)". Any still alike are numbered.
     */
    fun measurementLabels(measured: List<CatalogEntry>): List<String> {
        val base = measured.map { entry ->
            val version = Regex("""\((.*?)\)""").findAll(entry.name).joinToString(", ") { it.groupValues[1].trim() }
            if (version.isEmpty()) entry.source else "${entry.source} ($version)"
        }
        val seen = HashMap<String, Int>()
        return base.map { label ->
            if (base.count { it == label } < 2) label else "$label ${seen.merge(label, 1, Int::plus)}"
        }
    }

    private fun words(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

    /** Measurement rigs known for consistency come first. Any match is better than none. */
    private val SOURCE_PREFERENCE = listOf("oratory1990", "rtings", "crinacle", "dhrme")

    fun profileUrl(entry: CatalogEntry): String =
        BASE + entry.path + "/" + entry.path.substringAfterLast('/') + "%20ParametricEQ.txt"

    /** Blocking. What this result aims for, or null if it can't be found, which only hides the target choices. */
    fun loadCorrectionInfo(entry: CatalogEntry): CorrectionInfo? = runCatching {
        val name = entry.path.substringAfterLast('/')
        val text = download(BASE + entry.path + "/" + name + ".csv", MAX_RESULT_BYTES).decodeToString()
        HeadphoneTargets.aimFromResult(text)?.let { CorrectionInfo(entry.form, it) }
    }.getOrNull()

    /** Blocking. Uses a local copy of the index for a month, and falls back to it when offline. */
    @Throws(IOException::class)
    fun loadIndex(cacheDir: File): List<CatalogEntry> {
        val cache = File(cacheDir, "autoeq-index.md")
        val fresh = cache.exists() && System.currentTimeMillis() - cache.lastModified() < CACHE_MAX_AGE_MS
        if (!fresh) {
            try {
                val bytes = download(BASE + "INDEX.md", MAX_INDEX_BYTES)
                // Written beside it and then swapped in, so an interrupted write never leaves a short list that looks fresh.
                val partial = java.io.File(cache.parentFile, cache.name + ".part")
                partial.writeBytes(bytes)
                if (!partial.renameTo(cache)) {
                    cache.delete()
                    partial.renameTo(cache)
                }
            } catch (e: IOException) {
                if (!cache.exists()) throw e
            }
        }
        return parseIndex(cache.readText())
    }

    /** Blocking. */
    @Throws(IOException::class)
    fun loadProfile(entry: CatalogEntry): String = download(profileUrl(entry), MAX_PROFILE_BYTES).decodeToString()

    private fun download(url: String, maxBytes: Int): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server replied ${connection.responseCode}")
            }
            val out = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > maxBytes) throw IOException("Download was larger than expected")
                }
            }
            return out.toByteArray()
        } finally {
            connection.disconnect()
        }
    }
}
