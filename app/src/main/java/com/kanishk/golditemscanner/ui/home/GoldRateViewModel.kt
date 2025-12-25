package com.kanishk.golditemscanner.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.content.Context
import com.kanishk.golditemscanner.utils.GoldRateFetcher
import dev.shivathapaa.nepalidatepickerkmp.calendar_model.NepaliDateConverter
import dev.shivathapaa.nepalidatepickerkmp.data.NepaliDateLocale

class GoldRateViewModel : ViewModel() {

    private val _goldRate = MutableLiveData<String>().apply {
        value = "Getting..."
    }
    val goldRate: LiveData<String> = _goldRate

    private val _goldRateDate = MutableLiveData<String>().apply {
        value = "Getting..."
    }
    val goldRateDate: LiveData<String> = _goldRateDate

    suspend fun loadGoldRate(context: Context) {
        val rateAtDate = GoldRateFetcher.getCurrentGoldRate(context)
        if (rateAtDate != null) {
            _goldRate.postValue("Gold Rate is Rs. ${rateAtDate.rate} per tola")
            val date = rateAtDate.date
            _goldRateDate.postValue("As on Date: ${NepaliDateConverter.formatNepaliDate(NepaliDateConverter.getNepaliCalendar(nepaliYYYY =  date.year, nepaliMM = date.month, nepaliDD = date.dayOfMonth),
                NepaliDateLocale() ) }")
        } else {
            _goldRate.postValue("Failed to fetch gold rate")
            _goldRateDate.postValue("")
        }
    }
}




