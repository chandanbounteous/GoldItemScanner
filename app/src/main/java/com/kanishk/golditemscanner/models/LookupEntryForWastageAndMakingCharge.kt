package com.kanishk.golditemscanner.models

data class LookupEntryForWastageAndMakingCharge(
    val minNetWeight: Double,
    val maxNetWeight: Double,
    val karat: Int,
    val wastage: (Double) -> Double,
    val makingCharge: (Double) -> Double
)
