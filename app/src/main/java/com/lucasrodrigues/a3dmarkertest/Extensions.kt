package com.lucasrodrigues.a3dmarkertest

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.roundToInt

fun LatLng.angleInDegrees(to: LatLng): Int {
    return ((SphericalUtil.computeHeading(this, to) + 360) % 360).roundToInt()
}