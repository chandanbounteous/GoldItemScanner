package com.kanishk.golditemscanner.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.kanishk.golditemscanner.models.LookupEntryForWastageAndMakingCharge
import com.kanishk.golditemscanner.utils.GoldRateFetcher
import com.kanishk.golditemscanner.utils.ScannedArticleProcessor
import com.kanishk.golditemscanner.utils.Utils

private const val LUXURY_TAX_PERCENT = 0.02
private const val ONE_TOLA_IN_GMS = 11.664
private const val MAX_ARTICLE_WEIGHT = 999.0
private const val KARAT_24 = 24
private const val KARAT_22 = 22
private const val PURITY_FACTOR_24_Karat = 1.0
private const val PURITY_FACTOR_22_Karat = 0.92

class ScanItemViewModel : ViewModel() {

    private val lookupTable = listOf(
        LookupEntryForWastageAndMakingCharge(0.0, 1.0, 0, { 0.39 }, { 1200.0 }),
        LookupEntryForWastageAndMakingCharge(1.0, 2.0, 0, { 0.65 }, { 1500.0 }),
        LookupEntryForWastageAndMakingCharge(2.0, 3.0, 0, { 0.70 }, { 1700.0 }),
        LookupEntryForWastageAndMakingCharge(3.0, 4.0, 0, { 0.75 }, { 1800.0 }),
        LookupEntryForWastageAndMakingCharge(4.0, 6.0, 0, { 0.95 }, { 2200.0 }),
        LookupEntryForWastageAndMakingCharge(6.0, 7.0, 0, { 1.00 }, { 3500.0 }),
        LookupEntryForWastageAndMakingCharge(7.0, MAX_ARTICLE_WEIGHT, KARAT_24, { it * 0.07 }, { it * 0.01 }),
        LookupEntryForWastageAndMakingCharge(7.0, ONE_TOLA_IN_GMS, KARAT_22, { it * 0.07 }, { it * 0.01 }),
        LookupEntryForWastageAndMakingCharge(ONE_TOLA_IN_GMS,
            MAX_ARTICLE_WEIGHT, KARAT_22, { it * 0.09 }, { it * 0.01 })
    )

    private val _goldRate24K1Tola = MutableLiveData<String>().apply {
        value = "0.0"
    }
    val goldRate24K1Tola: LiveData<String> = _goldRate24K1Tola

    private val _karat = MutableLiveData<Int>().apply { value = KARAT_24 }
    val karat: LiveData<Int> = _karat

    private val _netWeight = MutableLiveData<Double>().apply { value = 0.0 }
    val netWeight: LiveData<Double> = _netWeight

    val wastage = MutableLiveData<Double>().apply { value = 0.0 }

    val makingCharge = MutableLiveData<Double>().apply { value = 0.0 }


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
            val goldRatePerTola = _goldRate24K1Tola.value?.toDoubleOrNull() ?: 0.0

            // Step 3: Set _netWeight to ArticleScannedInfo.netWeight
            _netWeight.postValue(articleScannedInfo.netWeight)

            // Step 4: Set _karat to ArticleScannedInfo.karat
            _karat.postValue(articleScannedInfo.karat)

            // Step 5: Set wastage to calculateWastage()
            val calcWastage = calculateWastage(
                articleScannedInfo.netWeight,
                articleScannedInfo.karat,
            )

//            val wastage = calculateWastage(articleScannedInfo)
            wastage.postValue(Utils.roundToTwoDecimalPlaces(calcWastage,2))

            // Step 6: Set makingCharge to calculateMakingCharge()
            val articleCostAsPerWeightRateAndKarat = calculateArticleCostAsPerWeightRateAndKarat(
                articleScannedInfo.netWeight + calcWastage,
                articleScannedInfo.karat,
                goldRatePerTola
            )
//            val makingCharge = calculateMakingCharge(articleScannedInfo)
            val calcMakingCharge = calculateMakingCharge(articleScannedInfo.netWeight,
                articleScannedInfo.karat, articleCostAsPerWeightRateAndKarat)
            makingCharge.postValue(Utils.roundToTwoDecimalPlaces(calcMakingCharge,2))

            // Step 7: Calculate taxableAmount

            val taxableAmount = calculateTaxableAmount(
                articleCostAsPerWeightRateAndKarat,
                calcMakingCharge
            )

            // Step 8: Set _luxuryTax to calculateLuxuryTax()
            val luxuryTax = calculateLuxuryTax(taxableAmount)
            _luxuryTax.postValue(luxuryTax)

            // Step 9: Set _approxTotalAmount to calculateApproxTotalAmount()
            val approxTotalAmount = calculateApproxTotalAmount(taxableAmount, luxuryTax)
            _approxTotalAmount.postValue(approxTotalAmount)
        }
    }


    private fun calculateWastage(netWeight: Double, karat: Int): Double {
        val matchedEntry = lookupTable.firstOrNull { entry ->
            netWeight >= entry.minNetWeight &&
                    netWeight < entry.maxNetWeight &&
                    (karat == entry.karat || entry.karat == 0)
        }
        return matchedEntry?.wastage?.invoke(netWeight) ?: 0.0
    }


    private fun calculateMakingCharge(netWeight: Double, karat: Int, totalTaxableAmount: Double): Double {
        val matchedEntry = lookupTable.firstOrNull { entry ->
            netWeight >= entry.minNetWeight &&
                    netWeight < entry.maxNetWeight &&
                    (karat == entry.karat || entry.karat == 0)
        }
        return matchedEntry?.makingCharge?.invoke(totalTaxableAmount) ?: 0.0
    }


    private fun calculateTaxableAmount(
        costAsPerGoldRateAndKarat: Double,
        makingCharge: Double
    ): Double {
        // Step 5: Calculate total taxable amount
        val totalTaxableAmount = costAsPerGoldRateAndKarat + makingCharge

        return Utils.roundToTwoDecimalPlaces(totalTaxableAmount, 2)
    }


    private fun calculateArticleCostAsPerWeightRateAndKarat(
        totalWeight: Double,
        karat: Int,
        goldRatePerTola: Double
    ): Double {
        val weightInTolas = totalWeight / ONE_TOLA_IN_GMS
        val purityFactor = if (karat == KARAT_24) PURITY_FACTOR_24_Karat else PURITY_FACTOR_22_Karat
        val cost = weightInTolas * purityFactor * goldRatePerTola
        return Utils.roundToTwoDecimalPlaces(cost, 2)
    }

    private fun calculateLuxuryTax(totalTaxableAmount: Double) =
        Utils.roundToTwoDecimalPlaces(LUXURY_TAX_PERCENT * totalTaxableAmount, 2)

    private fun calculateApproxTotalAmount(
        totalTaxableAmount: Double,
        luxuryTaxAmount: Double
    ) = Utils.roundToTwoDecimalPlaces(totalTaxableAmount + luxuryTaxAmount, 2)

    fun recalculateApproxTotalAmount(isMakingChargeChanged:Boolean = false) {
        val articleCostAsPerWeightRateAndKarat = calculateArticleCostAsPerWeightRateAndKarat(
            (_netWeight.value ?: 0.0) + (wastage.value ?: 0.0),
            _karat.value ?: KARAT_24,
            _goldRate24K1Tola.value?.toDoubleOrNull() ?: 0.0
        )

        var makingChargeValue: Double = makingCharge.value ?: 0.0
        if(!isMakingChargeChanged) {
            makingChargeValue = calculateMakingCharge(_netWeight.value ?: 0.0,
                _karat.value ?: KARAT_24, articleCostAsPerWeightRateAndKarat)
            // Update the LiveData with the recalculated value
            makingCharge.postValue(Utils.roundToTwoDecimalPlaces(makingChargeValue,2))
        }



        // Step 7: Calculate taxableAmount

        val taxableAmount = calculateTaxableAmount(
            articleCostAsPerWeightRateAndKarat,
            makingChargeValue
        )

        // Step 8: Set _luxuryTax to calculateLuxuryTax()
        val luxuryTax = calculateLuxuryTax(taxableAmount)
        _luxuryTax.postValue(luxuryTax)

        // Step 9: Set _approxTotalAmount to calculateApproxTotalAmount()
        val approxTotalAmount = calculateApproxTotalAmount(taxableAmount, luxuryTax)
        _approxTotalAmount.postValue(approxTotalAmount)
    }

}