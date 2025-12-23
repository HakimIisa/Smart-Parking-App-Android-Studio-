package com.smartparking

data class ParkingSpot(
    val id: Int? = null, // ID can be null when adding a new parking spot
    val name: String,
    val location: String,
    val capacity: Int
)
