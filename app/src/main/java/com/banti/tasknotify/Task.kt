package com.banti.tasknotify

data class Task(
    val id: String,
    var name: String,
    var category: String,
    var priority: String,
    var time: String,
    var recurring: String,
    var completed: Boolean,
    val createdAt: Long
)
