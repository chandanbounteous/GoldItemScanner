package com.kanishk.golditemscanner.utils

import com.kanishk.golditemscanner.models.RateDate
import dev.shivathapaa.nepalidatepickerkmp.calendar_model.NepaliDateConverter
import dev.shivathapaa.nepalidatepickerkmp.data.SimpleDate




class Utils {
    companion object {
        private val nepaliMonths = arrayOf(
            "Baisakh", "Jestha", "Asar", "Shrawan", "Bhadra", "Ashwin",
            "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
        )

        fun getRateDate(day: Int, month: String, year: Int): SimpleDate {
            val monthIndex = nepaliMonths.indexOfFirst { it.equals(month.trim(), ignoreCase = true) }
            if (monthIndex == -1) {
                throw IllegalArgumentException("Invalid month name: $month")
            }
            return SimpleDate(year = year, month = monthIndex + 1, dayOfMonth = day) //RateDate(rateDateDay = day, rateDateMonth = monthIndex + 1, rateDateYear = year)
        }

        fun getTodayNepaliDate(): SimpleDate {
            return NepaliDateConverter.todayNepaliSimpleDate
        }
    }
}