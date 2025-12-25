package com.kanishk.golditemscanner.models


import com.google.gson.annotations.SerializedName

data class ArticleScannedInfo(
    @SerializedName("article_name")
    var articleName: String,
    @SerializedName("gross_weight")
    var grossWeight: Double,
    @SerializedName("karat")
    var karat: Int,
    @SerializedName("net_weight")
    var netWeight: Double
)