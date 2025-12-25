package com.kanishk.golditemscanner.utils

import android.content.Context
import com.kanishk.golditemscanner.models.RateAtDate
import com.kanishk.golditemscanner.utils.LocalStorage.Companion.getObject
import com.kanishk.golditemscanner.utils.LocalStorage.Companion.saveObject
import dev.shivathapaa.nepalidatepickerkmp.data.SimpleDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object GoldRateFetcher {
    suspend fun getCurrentGoldRate(context: Context): RateAtDate? {
        val currentNepaliDate = Utils.getTodayNepaliDate()

        // Retrieve the saved RateAtDate object from LocalStorage
        val savedRateAtDate: RateAtDate? = LocalStorage.getObject(context, LocalStorage.StorageKey.CURRENT_RATE)

        // If savedRateAtDate is null or the date doesn't match the current Nepali date, fetch and save the new rate
        if (savedRateAtDate == null || savedRateAtDate.date != currentNepaliDate) {
            return fetchAndSaveGoldRate(context)
        }

        // If the saved rate's date matches the current Nepali date, return the saved rate
        return savedRateAtDate
    }

    private suspend fun fetchAndSaveGoldRate(context: Context): RateAtDate? {
        return withContext(Dispatchers.IO) {
            val url = "https://fenegosida.org/"
            val html = fetchHtml(url)

            if (html != null) {
                val rateAtDate = extractFineGoldPerTola(html)
                if (rateAtDate != null) {
                    // Save the fetched RateAtDate object to LocalStorage
                    LocalStorage.saveObject(context, LocalStorage.StorageKey.CURRENT_RATE, rateAtDate)
                }
                return@withContext rateAtDate
            }

            return@withContext null
        }
    }

    private suspend fun fetchHtml(url: String): String? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    return@withContext response.body?.string()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private fun extractFineGoldPerTola(html: String): RateAtDate? {
        val doc: Document = Jsoup.parse(html)

        val candidates = doc.select("p").filter { p ->
            val text = p.text().uppercase()
            text.contains("FINE GOLD (9999)") && text.contains("PER 1 TOLA")
        }

        candidates.forEach { p ->
            val rate = parseBoldNumber(p)
            if (rate != null) {
                val rateDate = extractRateDate(doc)
                if (rateDate != null) {
                    return RateAtDate(rate = rate.toDouble(), date = rateDate)
                }
            }
        }

        val labelElems = doc.allElements.filter { el ->
            el.text().uppercase().contains("FINE GOLD (9999)")
        }

        labelElems.forEach { el ->
            val nearby = sequence {
                generateSequence(el.nextElementSibling()) { it.nextElementSibling() }
                    .take(5)
                    .forEach { yield(it) }

                el.parent()?.children()?.forEach { yield(it) }
            }.toSet()

            nearby.forEach { candidate ->
                val text = candidate.text().uppercase()
                if (text.contains("PER 1 TOLA")) {
                    val rate = parseBoldNumber(candidate)
                    if (rate != null) {
                        val rateDate = extractRateDate(doc)
                        if (rateDate != null) {
                            return RateAtDate(rate = rate.toDouble(), date = rateDate)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun parseBoldNumber(root: Element): Int? {
        val bolds = root.select("b")
        for (b in bolds) {
            val raw = b.text().trim()
            val normalized = raw
                .replace(",", "")
                .replace("[^0-9.]".toRegex(), "")

            if (normalized.isNotEmpty()) {
                return normalized.toDoubleOrNull()?.toInt()
            }
        }
        return null
    }

    private fun extractRateDate(doc: Document): SimpleDate? {
        val dayElement = doc.selectFirst("div.rate-date-day")
        val monthElement = doc.selectFirst("div.rate-date-month")
        val yearElement = doc.selectFirst("div.rate-date-year")

        if (dayElement != null && monthElement != null && yearElement != null) {
            val day = dayElement.text().toIntOrNull()
            val month = monthElement.text()
            val year = yearElement.text().toIntOrNull()

            if (day != null && year != null) {
                return Utils.getRateDate(day, month, year)
            }
        }

        return null
    }
}