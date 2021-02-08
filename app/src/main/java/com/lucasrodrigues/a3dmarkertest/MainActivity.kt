package com.lucasrodrigues.a3dmarkertest

import android.graphics.Color.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.directions.route.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import com.lucasrodrigues.a3dmarkertest.components.map.marker.CarRotation3DMarkerView
import com.lucasrodrigues.a3dmarkertest.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var googleMap: GoogleMap? = null

    private var originMarker: Marker? = null

    private var markerCounter = 0

    private val landmarks = mutableListOf(
        LatLng(-1.445825, -48.4913065),
        LatLng(-1.448614, -48.4976797),
        LatLng(-1.459211, -48.494890),
        LatLng(-1.459683, -48.482101),
        LatLng(-1.465689, -48.481415),
        LatLng(-1.448528, -48.471287),
        LatLng(-1.462943, -48.470600),
        LatLng(-1.473111, -48.479012),
        LatLng(-1.472596, -48.502400),
        LatLng(-1.457881, -48.505877),
        LatLng(-1.450158, -48.501199),
    )

    private val executor = Executors.newCachedThreadPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            changeColor.setOnClickListener {
                MaterialDialog(this@MainActivity).show {
                    title(text = "Choose car color")
                    colorChooser(
                        allowCustomArgb = true,
                        colors = intArrayOf(
                            RED,
                            GREEN,
                            BLUE,
                            YELLOW,
                            WHITE,
                            LTGRAY,
                            GRAY,
                            DKGRAY,
                            BLACK,
                            CYAN,
                            MAGENTA
                        )
                    ) { _, color ->
                        binding.mapOverlay.markers.forEach {
                            it.value.let { markerView ->
                                if (markerView is CarRotation3DMarkerView)
                                    markerView.changeCarColor(color)
                            }
                        }
                    }
                    positiveButton(text = "Ok")
                }
            }

            beginDemo.setOnClickListener {
                GlobalScope.launch(Dispatchers.Default) {
                    landmarks.forEachIndexed { index, origin ->
                        var destinationIndex = index

                        while (destinationIndex == index) {
                            destinationIndex = Random.nextInt(landmarks.size)
                        }

                        createMarkerAndGenerateRouteTo(
                            from = origin,
                            to = landmarks[destinationIndex],
                        )
                    }
                }
            }
        }

        (supportFragmentManager.findFragmentById(R.id.myMapView) as SupportMapFragment).getMapAsync { googleMap ->
            with(googleMap.uiSettings) {
                isMyLocationButtonEnabled = false
                isMapToolbarEnabled = false
                isCompassEnabled = false
            }

            googleMap.clear()

            googleMap.setOnMapClickListener { coordinates ->
                if (originMarker == null) {
                    originMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(coordinates)
                            .draggable(true)
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_GREEN
                                )
                            )
                    )
                } else {
                    originMarker?.position?.let { latLng ->
                        createMarkerAndGenerateRouteTo(latLng, coordinates)
                    }
                }
            }

            googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(p0: Marker?) {

                }

                override fun onMarkerDrag(p0: Marker?) {

                }

                override fun onMarkerDragEnd(p0: Marker?) {
                    p0?.let {
                        originMarker?.position = p0.position
                    }
                }
            })

            this.googleMap = googleMap

            binding.mapOverlay.bindTo(googleMap)
            binding.mapOverlay.postDelayed(100L) {
                centerOnMyLocation()
            }
        }
    }

    private fun createMarkerAndGenerateRouteTo(from: LatLng, to: LatLng) {
        Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.DRIVING)
            .withListener(object : RoutingListener {
                override fun onRoutingCancelled() {}

                override fun onRoutingStart() {}

                override fun onRoutingFailure(p0: RouteException?) {
                    p0?.printStackTrace()
                }

                override fun onRoutingSuccess(route: ArrayList<Route>?, p1: Int) {
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            val steps = route?.get(0)?.points

                            if (!steps.isNullOrEmpty()) {
                                val newSteps = mutableListOf<LatLng>()

                                steps.forEachIndexed { index, step ->
                                    if (index == 0 || step != steps[index - 1])
                                        newSteps.add(step)
                                }

                                val newId = markerCounter++

                                newSteps[0].let { first ->
                                    withContext(Dispatchers.Main) {
                                        binding.mapOverlay.createOrUpdateMarker(
                                            id = newId,
                                            position = first
                                        )
                                    }
                                }

                                if (newSteps.size > 1) {
                                    var index = 1

                                    var previousCoordinate = newSteps[index - 1]
                                    var coordinate = newSteps[index]

                                    var distanceKm =
                                        SphericalUtil.computeDistanceBetween(
                                            previousCoordinate,
                                            coordinate
                                        )
                                    var time =
                                        TimeUnit.HOURS.toMillis((distanceKm).roundToLong()) / 1000 / 200

                                    binding.mapOverlay.handler.postDelayed(object : Runnable {
                                        override fun run() {
                                            binding.mapOverlay.createOrUpdateMarker(
                                                id = newId,
                                                position = coordinate
                                            )

                                            if (++index <= newSteps.lastIndex) {
                                                previousCoordinate = newSteps[index - 1]
                                                coordinate = newSteps[index]

                                                distanceKm =
                                                    SphericalUtil.computeDistanceBetween(
                                                        previousCoordinate,
                                                        coordinate
                                                    )
                                                time =
                                                    TimeUnit.HOURS.toMillis((distanceKm).roundToLong()) / 1000 / 200

                                                binding.mapOverlay.handler.postDelayed(this, time)
                                            }
                                        }
                                    }, time)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            })
            .waypoints(from, to)
            .key(resources.getString(R.string.google_maps_key))
            .build()
            .executeOnExecutor(executor)
    }

    private fun centerOnMyLocation() {
        runOnUiThread {
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds
                        .builder()
                        .apply {
                            landmarks.forEach {
                                include(it)
                            }
                        }
                        .build(),
                    200,
                ),
                200,
                object : GoogleMap.CancelableCallback {
                    override fun onFinish() {}
                    override fun onCancel() {}
                })
        }
    }
}
