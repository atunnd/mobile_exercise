package com.example.gallery_detection

data class ProcessedImage(
    val id: Long,
    val imageUrl: String,
    val tags: String,
    val user: String,
    val detectedLabels: List<String>
)

data class ImageItem(
    val id: Long,
    val pageURL: String,
    val type: String,
    val tags: String,
    val previewURL: String,
    val previewWidth: Int,
    val previewHeight: Int,
    val webformatURL: String,
    val webformatWidth: Int,
    val webformatHeight: Int,
    val largeImageURL: String,
    val fullHDURL: String?,
    val imageURL: String?,
    val imageWidth: Int,
    val imageHeight: Int,
    val imageSize: Int,
    val views: Int,
    val downloads: Int,
    val likes: Int,
    val comments: Int,
    val user_id: Int,
    val user: String,
    val userImageURL: String
)

data class PixabayResponse(
    val total: Int,
    val totalHits: Int,
    val hits: List<ImageItem>
)
