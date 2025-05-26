package com.example.collegeentranceguardclocke.entity

data class History(
    var hid: Int? = null,
    var uid: Int? = null,
    var state: Int? = null, // 1为进校 0为出校
    var createDateTime: String? = null,
    var method: String? = null // Added field for unlock method
)
