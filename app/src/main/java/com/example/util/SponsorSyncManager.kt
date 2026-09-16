package com.example.util

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.data.CachedSponsor
import com.example.data.ClassNotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object SponsorSyncManager {

    private const val DEFAULT_SUPABASE_URL = "https://mgfvivzwakdbigtuuzai.supabase.co"
    private const val DEFAULT_ANON_KEY = "sb_publishable_TTqMNEtjm0MlvtdWfq3q2g_53UCvoCC"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Throttle impression recording per sponsor ID within session to avoid spamming
    private val recordedImpressionTimestamps = mutableMapOf<String, Long>()

    suspend fun syncActiveSponsor(
        context: Context,
        repository: ClassNotesRepository,
        customEndpointUrl: String? = null,
        customAnonKey: String? = null
    ) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("sponsor_config", Context.MODE_PRIVATE)
        val supabaseUrl = customEndpointUrl ?: prefs.getString("supabase_url", DEFAULT_SUPABASE_URL) ?: DEFAULT_SUPABASE_URL
        val anonKey = customAnonKey ?: prefs.getString("supabase_anon_key", DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY

        if (supabaseUrl.isBlank() || supabaseUrl.contains("your-project")) {
            return@withContext
        }

        // 1. Sync pending offline impressions / clicks first
        flushPendingTelemetry(context, supabaseUrl, anonKey)

        // 2. Fetch fresh active campaigns
        val requestUrl = "${supabaseUrl.trimEnd('/')}/rest/v1/sponsors?is_active=eq.true&select=id,name,image_url,website_url,start_date,end_date,is_active"

        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext
                }

                val bodyString = response.body?.string() ?: return@withContext
                val jsonArray = JSONArray(bodyString)
                val now = System.currentTimeMillis()
                val activeList = mutableListOf<CachedSponsor>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val name = obj.optString("name", "")
                    val imageUrl = obj.optString("image_url", "")
                    val websiteUrl = obj.optString("website_url", "")
                    val isActive = obj.optBoolean("is_active", false)

                    val startMillis = parseDateToMillis(obj.optString("start_date", ""), isEndOfDay = false)
                    val endMillis = parseDateToMillis(obj.optString("end_date", ""), isEndOfDay = true)

                    if (isActive && now in startMillis..endMillis) {
                        activeList.add(
                            CachedSponsor(
                                id = id,
                                name = name,
                                imageUrl = imageUrl,
                                websiteUrl = websiteUrl,
                                startDate = startMillis,
                                endDate = endMillis,
                                isActive = true,
                                cachedAt = now
                            )
                        )
                    }
                }

                if (activeList.isNotEmpty()) {
                    repository.saveCachedSponsors(activeList)

                    // Pre-cache remote images to local disk cache immediately so offline is 100% instant
                    val imageLoader = ImageLoader(context)
                    for (sponsor in activeList) {
                        if (sponsor.imageUrl.startsWith("http")) {
                            try {
                                val imgReq = ImageRequest.Builder(context)
                                    .data(sponsor.imageUrl)
                                    .build()
                                imageLoader.enqueue(imgReq)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Offline or network timeout: Keep local Room cache intact so users see ads offline!
        }
    }

    /**
     * Records a view impression for the specified sponsor.
     * Queues locally if device is offline, and syncs to Supabase.
     */
    suspend fun recordImpression(context: Context, sponsorId: String) = withContext(Dispatchers.IO) {
        if (sponsorId.isBlank()) return@withContext

        // Don't count duplicate impressions for the same ad within 45 seconds on same screen
        val now = System.currentTimeMillis()
        val lastTime = recordedImpressionTimestamps[sponsorId] ?: 0L
        if (now - lastTime < 45_000L) {
            return@withContext
        }
        recordedImpressionTimestamps[sponsorId] = now

        incrementTelemetry(context, sponsorId, isClick = false)
    }

    /**
     * Records a click for the specified sponsor.
     */
    suspend fun recordClick(context: Context, sponsorId: String) = withContext(Dispatchers.IO) {
        if (sponsorId.isBlank()) return@withContext
        incrementTelemetry(context, sponsorId, isClick = true)
    }

    private suspend fun incrementTelemetry(context: Context, sponsorId: String, isClick: Boolean) {
        val prefs = context.getSharedPreferences("sponsor_config", Context.MODE_PRIVATE)
        val supabaseUrl = prefs.getString("supabase_url", DEFAULT_SUPABASE_URL) ?: DEFAULT_SUPABASE_URL
        val anonKey = prefs.getString("supabase_anon_key", DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY

        val columnToIncrement = if (isClick) "clicks_count" else "views_count"
        val rpcUrl = "${supabaseUrl.trimEnd('/')}/rest/v1/rpc/increment_sponsor_stat"

        // First attempt direct RPC call
        try {
            val jsonBody = JSONObject().apply {
                put("sponsor_id", sponsorId)
                put("stat_field", columnToIncrement)
            }
            val request = Request.Builder()
                .url(rpcUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.close()
                return
            }
            response.close()
        } catch (_: Exception) {}

        // Attempt direct REST PATCH
        try {
            val patchUrl = "${supabaseUrl.trimEnd('/')}/rest/v1/sponsors?id=eq.$sponsorId"
            // Query row to see which column exists
            val getUrl = "${supabaseUrl.trimEnd('/')}/rest/v1/sponsors?id=eq.$sponsorId&select=*"
            val getReq = Request.Builder()
                .url(getUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()

            httpClient.newCall(getReq).execute().use { getRes ->
                if (getRes.isSuccessful) {
                    val arr = JSONArray(getRes.body?.string() ?: "[]")
                    if (arr.length() > 0) {
                        val row = arr.getJSONObject(0)
                        val targetCol = when {
                            isClick && row.has("clicks") -> "clicks"
                            isClick -> "clicks_count"
                            !isClick && row.has("impressions") -> "impressions"
                            else -> "views_count"
                        }
                        val currentVal = row.optLong(targetCol, 0L)
                        val patchBody = JSONObject().apply {
                            put(targetCol, currentVal + 1)
                        }
                        val patchReq = Request.Builder()
                            .url(patchUrl)
                            .addHeader("apikey", anonKey)
                            .addHeader("Authorization", "Bearer $anonKey")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=minimal")
                            .patch(patchBody.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        httpClient.newCall(patchReq).execute().close()
                        return
                    }
                }
            }
        } catch (_: Exception) {
            // Network is unavailable: Queue for later sync
            queueOfflineStat(context, sponsorId, isClick)
        }
    }

    private fun queueOfflineStat(context: Context, sponsorId: String, isClick: Boolean) {
        try {
            val prefs = context.getSharedPreferences("sponsor_offline_queue", Context.MODE_PRIVATE)
            val key = if (isClick) "click_$sponsorId" else "view_$sponsorId"
            val current = prefs.getInt(key, 0)
            prefs.edit().putInt(key, current + 1).apply()
        } catch (_: Exception) {}
    }

    private suspend fun flushPendingTelemetry(context: Context, supabaseUrl: String, anonKey: String) {
        try {
            val prefs = context.getSharedPreferences("sponsor_offline_queue", Context.MODE_PRIVATE)
            val allEntries = prefs.all
            if (allEntries.isEmpty()) return

            for ((key, value) in allEntries) {
                val count = value as? Int ?: continue
                if (count <= 0) continue

                val isClick = key.startsWith("click_")
                val sponsorId = key.substringAfter("_")

                val getUrl = "${supabaseUrl.trimEnd('/')}/rest/v1/sponsors?id=eq.$sponsorId&select=*"
                val getReq = Request.Builder()
                    .url(getUrl)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .get()
                    .build()

                httpClient.newCall(getReq).execute().use { getRes ->
                    if (getRes.isSuccessful) {
                        val arr = JSONArray(getRes.body?.string() ?: "[]")
                        if (arr.length() > 0) {
                            val row = arr.getJSONObject(0)
                            val col = when {
                                isClick && row.has("clicks") -> "clicks"
                                isClick -> "clicks_count"
                                !isClick && row.has("impressions") -> "impressions"
                                else -> "views_count"
                            }
                            val currentVal = row.optLong(col, 0L)
                            val patchBody = JSONObject().apply {
                                put(col, currentVal + count)
                            }
                            val patchReq = Request.Builder()
                                .url("${supabaseUrl.trimEnd('/')}/rest/v1/sponsors?id=eq.$sponsorId")
                                .addHeader("apikey", anonKey)
                                .addHeader("Authorization", "Bearer $anonKey")
                                .addHeader("Content-Type", "application/json")
                                .patch(patchBody.toString().toRequestBody("application/json".toMediaType()))
                                .build()

                            httpClient.newCall(patchReq).execute().close()
                            prefs.edit().remove(key).apply()
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseDateToMillis(dateStr: String, isEndOfDay: Boolean = false): Long {
        if (dateStr.isBlank()) return if (isEndOfDay) Long.MAX_VALUE else 0L
        return try {
            dateStr.toLong()
        } catch (e: Exception) {
            try {
                isoDateFormat.parse(dateStr)?.time ?: if (isEndOfDay) Long.MAX_VALUE else 0L
            } catch (e2: Exception) {
                try {
                    val parsed = simpleDateFormat.parse(dateStr)?.time ?: 0L
                    if (isEndOfDay && parsed > 0L) {
                        parsed + 86399999L
                    } else {
                        parsed
                    }
                } catch (e3: Exception) {
                    if (isEndOfDay) Long.MAX_VALUE else 0L
                }
            }
        }
    }
}
