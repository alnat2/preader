package com.example.preader.domain

import kotlinx.serialization.Serializable

@Serializable
data class ReadingPage(
    val id: String,
    val displayName: String,
    val sourcePath: String,
    val sourceType: SourceType,
    val firstOpenedAt: Long,
    val positionRatio: Double,
    val progressPercent: Int
)

@Serializable
enum class SourceType {
    HtmlFile,
    Folder
}
