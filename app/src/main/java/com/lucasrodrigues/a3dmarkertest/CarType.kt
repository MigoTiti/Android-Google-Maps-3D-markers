package com.lucasrodrigues.a3dmarkertest

enum class CarType(
    val body: Int,
    val mask: Int
) {
    //TODO insert your own sprite sheets
    NORMAL(R.drawable.car_type_normal_body, R.drawable.car_type_normal_mask),
}