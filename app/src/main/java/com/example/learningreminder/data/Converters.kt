package com.example.learningreminder.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun toStringList(value: List<String>): String {
        return value.joinToString(",")
    }
}
