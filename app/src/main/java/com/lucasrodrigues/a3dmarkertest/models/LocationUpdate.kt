package com.lucasrodrigues.a3dmarkertest.models

import com.google.android.gms.maps.model.LatLng

data class LocationUpdate(
    val latLng: LatLng,
    val timeSinceLastUpdate: Long,
)