package com.kanishk.golditemscanner.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.kanishk.golditemscanner.models.ArticleScannedInfo
import com.kanishk.golditemscanner.utils.GoldRateFetcher
import com.kanishk.golditemscanner.utils.ScannedArticleProcessor

class ScanItemViewModel : ViewModel() {

    private val _goldRate24K1Tola = MutableLiveData<String>().apply {
        value = "0.0"
    }
    val goldRate24K1Tola: LiveData<String> = _goldRate24K1Tola

    private val _karat = MutableLiveData<Int>().apply { value = 24 }
    val karat: LiveData<Int> = _karat

    private val _netWeight = MutableLiveData<Double>().apply { value = 0.0 }
    val netWeight: LiveData<Double> = _netWeight

    private val _wastage = MutableLiveData<Double>().apply { value = 0.0 }
    val wastage: LiveData<Double> = _wastage

    private val _makingCharge = MutableLiveData<Double>().apply { value = 0.0 }
    val makingCharge: LiveData<Double> = _makingCharge

    private val _luxuryTax = MutableLiveData<Double>().apply { value = 0.0 }
    val luxuryTax: LiveData<Double> = _luxuryTax

    private val _approxTotalAmount = MutableLiveData<Double>().apply { value = 0.0 }
    val approxTotalAmount: LiveData<Double> = _approxTotalAmount

    fun initialLoad(context: Context) {
        viewModelScope.launch {
            val rateAtDate = GoldRateFetcher.getCurrentGoldRate(context)
            _goldRate24K1Tola.value = rateAtDate?.rate?.toString() ?: "0.0"
        }
    }

    fun processScannedImage(image: InputImage) {
        ScannedArticleProcessor.processImage(image) { articleScannedInfo ->
            // Step 3: Set _netWeight to ArticleScannedInfo.netWeight
            _netWeight.postValue(articleScannedInfo.netWeight)

            // Step 4: Set _karat to ArticleScannedInfo.karat
            _karat.postValue(articleScannedInfo.karat)

            // Step 5: Set _wastage to calculateWastage()
            val wastage = calculateWastage(articleScannedInfo)
            _wastage.postValue(wastage)

            // Step 6: Set _makingCharge to calculateMakingCharge()
            val makingCharge = calculateMakingCharge(articleScannedInfo)
            _makingCharge.postValue(makingCharge)

            // Step 7: Calculate taxableAmount
            val goldRatePerTola = _goldRate24K1Tola.value?.toDoubleOrNull() ?: 0.0
            val taxableAmount = calculateTaxableAmount(
                articleScannedInfo,
                goldRatePerTola,
                wastage,
                makingCharge
            )

            // Step 8: Set _luxuryTax to calculateLuxuryTax()
            val luxuryTax = calculateLuxuryTax(taxableAmount)
            _luxuryTax.postValue(luxuryTax)

            // Step 9: Set _approxTotalAmount to calculateApproxTotalAmount()
            val approxTotalAmount = calculateApproxTotalAmount(taxableAmount, luxuryTax)
            _approxTotalAmount.postValue(approxTotalAmount)
        }
    }

    fun calculateWastage(articleScannedInfo: ArticleScannedInfo): Double =
        (articleScannedInfo.netWeight * 0.07).let {
            kotlin.math.round(it * 100) / 100
        }

    fun calculateMakingCharge(articleScannedInfo: ArticleScannedInfo) =
        2000.0

    fun calculateTaxableAmount(
        articleScannedInfo: ArticleScannedInfo,
        goldRatePerTola: Double,
        wastage: Double,
        makingCharge: Double
    ): Double {
        // Step 1: Calculate total weight
        val totalWeight = articleScannedInfo.netWeight + wastage

        // Step 2: Convert total weight to weight in tolas
        val weightInTolas = totalWeight / 11.664

        // Step 3: Determine purity factor
        val purityFactor = if (articleScannedInfo.karat == 24) 1.0 else 0.92

        // Step 4: Calculate cash value for gold
        val cashValueForGold = weightInTolas * purityFactor * goldRatePerTola

        // Step 5: Calculate total taxable amount
        val totalTaxableAmount = cashValueForGold + makingCharge

        return totalTaxableAmount.let {
            kotlin.math.round(it * 100) / 100
        }
    }

    fun calculateLuxuryTax(totalTaxableAmount: Double) = (0.02 * totalTaxableAmount).let {
        kotlin.math.round(it * 100) / 100
    }

    fun calculateApproxTotalAmount(
        totalTaxableAmount: Double,
        luxuryTaxAmount: Double
    ) = (totalTaxableAmount + luxuryTaxAmount).let {
        kotlin.math.round(it * 100) / 100
    }


}