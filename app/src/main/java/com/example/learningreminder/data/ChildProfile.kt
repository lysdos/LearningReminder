package com.example.learningreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_profile")
data class ChildProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val birthDate: String? = null,
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
