package com.lucasrodrigues.a3dmarkertest.components.map

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.lucasrodrigues.a3dmarkertest.R
import com.lucasrodrigues.a3dmarkertest.components.map.marker.CarRotation3DMarkerView
import com.lucasrodrigues.a3dmarkertest.components.map.marker.MarkerView

class MapOverlayLayout(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var googleMap: GoogleMap? = null

    fun bindTo(googleMap: GoogleMap) {
        this.googleMap = googleMap

        googleMap.setOnCameraMoveListener {
            refreshMarkers()
        }
    }

    val markers = hashMapOf<Any, MarkerView>()

    private fun refreshMarkers() {
        markers.forEach { markerView ->
            post {
                markerView.value.updatePositionOnScreen()
                markerView.value.updateZoomOnScreen()
            }
        }
    }

    private fun addMarkerAndShow(id: Any, view: MarkerView) {
        post {
            markers[id] = view
            addView(view)
            view.show()
        }
    }

    private fun getCarMarker(id: Any): MarkerView? {
        return markers[id]
    }

    @Synchronized
    fun createOrUpdateMarker(
        id: Any,
        position: LatLng,
        rotation: Int = 0
    ): MarkerView? {
        return getCarMarker(id)?.apply {
            onNewPosition(position)
        } ?: createCarMarker(
            latLng = position,
            id = id,
            rotation = rotation,
        )
    }

    private fun createMarker(
        id: Any,
        latLng: LatLng,
    ): MarkerView? {
        return googleMap?.let { googleMap ->
            MarkerView(
                context = context,
                googleMap = googleMap,
                coordinateOnMap = latLng,
                content = inflate(context, R.layout.map_overlay_marker, null)
            ).apply {
                addMarkerAndShow(id, this)
            }
        }
    }

    private fun createCarMarker(
        id: Any,
        latLng: LatLng,
        rotation: Int
    ): MarkerView? {
        return googleMap?.let { googleMap ->
            CarRotation3DMarkerView(
                context = context,
                googleMap = googleMap,
                coordinateOnMap = latLng,
                initialBearing = rotation
            ).apply {
                addMarkerAndShow(id, this)
            }
        }
    }
}