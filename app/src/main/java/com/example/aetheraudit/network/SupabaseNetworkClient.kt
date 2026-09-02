package com.example.aetheraudit.network

import com.example.aetheraudit.data.LocalOUIEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseNetworkClient(
    private val supabaseUrl: String = "https://veextsfurygcyflznnvf.supabase.co",
    private val supabaseApiKey: String = "sb_publishable_zr3TwD8ORIpKwI7fTt8ssQ_TS4hWImZ"
) {
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Download vulnerable OUI Master List
    suspend fun fetchMasterBlacklist(): List<LocalOUIEntry> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/oui_vulnerability_blacklist?select=*")
            .addHeader("apikey", supabaseApiKey)
            .addHeader("Authorization", "Bearer $supabaseApiKey")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(responseBody)
            val list = mutableListOf<LocalOUIEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LocalOUIEntry(
                        oui = obj.getString("oui").uppercase(),
                        vendorName = obj.getString("vendor_name"),
                        chipsetManufacturer = obj.getString("chipset_manufacturer"),
                        vulnerabilityDetails = obj.optString("vulnerability_details", "Vulnerable OUI Found."),
                        isUserDefined = false
                    )
                )
            }
            list
        }
    }

    // Upload Infrastructure Resilience Report (Scan session log) to remote Supabase DB
    suspend fun uploadAuditLog(
        deviceName: String,
        macAddress: String,
        rssi: Int,
        threatLevel: String
    ): Boolean = withContext(Dispatchers.IO) {
        val jsonPayload = JSONObject().apply {
            put("device_name", deviceName)
            put("mac_address", macAddress)
            put("rssi", rssi)
            put("threat_level", threatLevel)
            put("operator_email", "admin@aetheraudit.local")
        }.toString()

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/infrastructure_resilience_reports")
            .addHeader("apikey", supabaseApiKey)
            .addHeader("Authorization", "Bearer $supabaseApiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(jsonPayload.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }
}