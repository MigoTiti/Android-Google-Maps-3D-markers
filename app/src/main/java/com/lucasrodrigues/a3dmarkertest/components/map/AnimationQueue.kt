package com.lucasrodrigues.a3dmarkertest.components.map

import com.google.android.gms.maps.model.LatLng
import com.lucasrodrigues.a3dmarkertest.components.map.marker.MarkerView
import com.lucasrodrigues.a3dmarkertest.models.LocationUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AnimationQueue(private val markerView: MarkerView) {

    private var timeSinceLastUpdate = System.currentTimeMillis()

    private val items = mutableListOf<LocationUpdate>()

    private val mutex = Mutex()

    fun addToQueue(latLng: LatLng) {
        items.add(
            LocationUpdate(
                latLng = latLng,
                timeSinceLastUpdate = System.currentTimeMillis() - timeSinceLastUpdate
            )
        )

        timeSinceLastUpdate = System.currentTimeMillis()

        markerView.coroutineScope.launch {
            runNextItem()
        }
    }

    private suspend fun runNextItem() {
        mutex.withLock {
            while (items.isNotEmpty()) {
                items.removeAt(0).let {
                    markerView.animateToPosition(
                        endPosition = it.latLng,
                        duration = it.timeSinceLastUpdate,
                    )
                }
            }
        }
    }

    fun clearQueue() {
        items.clear()
        timeSinceLastUpdate = System.currentTimeMillis()
    }
}