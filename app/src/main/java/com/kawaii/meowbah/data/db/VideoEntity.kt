package com.kawaii.meowbah.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val publishedAt: String?
)
