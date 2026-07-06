package com.example.to_do_list

data class Task(
    var id: String = "",
    val title: String = "",
    val date: String = "",
    val time: String = "",
    var completed: Boolean = false,
    var important: Boolean = false,
    val userId: String = "",
    val locationName: String = "",
    val locationLatLng: String = ""
)