package com.kanishk.golditemscanner.models


import com.google.gson.annotations.SerializedName

data class RateDate(
    @SerializedName("rate-date-day")
    val rateDateDay: Int,
    @SerializedName("rate-date-month")
    val rateDateMonth: Int,
    @SerializedName("rate-date-year")
    val rateDateYear: Int
)