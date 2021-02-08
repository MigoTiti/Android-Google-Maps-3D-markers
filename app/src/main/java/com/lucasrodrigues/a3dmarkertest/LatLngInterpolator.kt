package com.lucasrodrigues.a3dmarkertest

import com.google.android.gms.maps.model.LatLng

interface LatLngInterpolator {

    fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng

    class Linear {
        companion object : LatLngInterpolator {
            override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
                val lat = (b.latitude - a.latitude) * fraction + a.latitude
                val lng = (b.longitude - a.longitude) * fraction + a.longitude
                return LatLng(lat, lng)
            }
        }
    }
}