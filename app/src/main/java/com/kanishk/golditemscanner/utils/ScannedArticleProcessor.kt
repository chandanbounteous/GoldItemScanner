package com.kanishk.golditemscanner.utils

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kanishk.golditemscanner.models.ArticleScannedInfo

object ScannedArticleProcessor {

    private val textRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.Builder().build()
    )

    fun processImage(image: InputImage, callback: (ArticleScannedInfo) -> Unit) {
        Log.d("ScannedArticleProcessor", "Starting image processing...")
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("ScannedArticleProcessor", "Image processing successful. Extracting data...")
                val articleInfo = ArticleScannedInfo(
                    articleName = "Unable to determine",
                    grossWeight = 0.0,
                    karat = 24,
                    netWeight = 0.0
                )

                val netWeightPattern = Regex("Nt\\.Wt\\s*:\\s*([0-9]*\\.?[0-9]+)")
                val grossWeightPattern = Regex("G\\.Wt\\s*:\\s*([0-9]*\\.?[0-9]+)")
                val karatPattern = Regex("Karat\\s*:\\s*([0-9]*\\.?[0-9]+)")
                val articleNamePattern = Regex("Kar\\s*:\\s*(.+)")

                for (block in visionText.textBlocks) {
                    Log.d("ScannedArticleProcessor", "Processing text block: ${block.text}")
                    for (line in block.lines) {
                        val text = line.text
                        Log.d("ScannedArticleProcessor", "Processing line: $text")

                        netWeightPattern.find(text)?.let {
                            articleInfo.netWeight = it.groupValues[1].toDouble()
                            Log.d("ScannedArticleProcessor", "Extracted net weight: ${articleInfo.netWeight}")
                        }

                        grossWeightPattern.find(text)?.let {
                            articleInfo.grossWeight = it.groupValues[1].toDouble()
                            Log.d("ScannedArticleProcessor", "Extracted gross weight: ${articleInfo.grossWeight}")
                        }

                        karatPattern.find(text)?.let {
                            val karatValue = it.groupValues[1].toDouble()
                            articleInfo.karat = if (karatValue > 98) 24 else 22
                            Log.d("ScannedArticleProcessor", "Extracted karat: ${articleInfo.karat}")
                        }

                        articleNamePattern.find(text)?.let {
                            articleInfo.articleName = it.groupValues[1].trim()
                            Log.d("ScannedArticleProcessor", "Extracted article name: ${articleInfo.articleName}")
                        }
                    }
                }

                Log.d("ScannedArticleProcessor", "Data extraction complete. Callback with article info.")
                callback(articleInfo)
            }
            .addOnFailureListener { e ->
                Log.e("ScannedArticleProcessor", "Image processing failed.", e)
                callback(
                    ArticleScannedInfo(
                        articleName = "Unable to determine",
                        grossWeight = 0.0,
                        karat = 24,
                        netWeight = 0.0
                    )
                )
            }
    }


}