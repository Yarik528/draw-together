package com.example.drawtogether

import java.util.UUID

data class Point(
    val x: Float,
    val y: Float
)

data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Point> = emptyList(),
    val color: Long = 0xFF000000,
    val width: Float = 5f
)
