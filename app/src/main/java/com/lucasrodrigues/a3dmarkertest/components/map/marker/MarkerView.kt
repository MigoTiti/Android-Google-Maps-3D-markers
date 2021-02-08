package com.lucasrodrigues.a3dmarkertest.components.map.marker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.lucasrodrigues.a3dmarkertest.LatLngInterpolator
import com.lucasrodrigues.a3dmarkertest.components.map.AnimationQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class MarkerView(
    context: Context,
    coordinateOnMap: LatLng,
    val googleMap: GoogleMap,
    protected val content: View,
) : FrameLayout(context) {

    companion object {
        const val DEFAULT_REFERENCE_ZOOM = 17f
    }

    private var calculatedFrameSize: Point? = null

    protected var zoomOnScreen =
        (googleMap.cameraPosition?.zoom ?: 0f) / DEFAULT_REFERENCE_ZOOM
        set(value) {
            if (field != value) {
                field = value

                post {
                    scaleX = value
                    scaleY = value
                }
            }
        }

    private var coordinateOnScreen: Point = googleMap.projection.toScreenLocation(coordinateOnMap)
        set(value) {
            if (field != value) {
                field = value

                refresh()
            }
        }

    var coordinateOnMap: LatLng = coordinateOnMap
        set(value) {
            if (field != value) {
                field = value

                updatePositionOnScreen()
            }
        }

    protected val animationQueue by lazy {
        AnimationQueue(markerView = this)
    }

    val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        visibility = View.GONE
        addView(content)
    }

    fun updatePositionOnScreen() {
        coordinateOnScreen = googleMap.projection.toScreenLocation(coordinateOnMap)
    }

    fun updateZoomOnScreen() {
        zoomOnScreen = googleMap.cameraPosition.zoom / DEFAULT_REFERENCE_ZOOM.toInt()
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        refresh()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w != oldw || h != oldh) {
            calculatedFrameSize = Point(w, h)
            refresh()
        }
    }

    private fun refresh() {
        val params = ((layoutParams as? LayoutParams) ?: LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        )).apply {
            leftMargin = coordinateOnScreen.x - ((calculatedFrameSize?.x ?: 0) / 2)
            topMargin = coordinateOnScreen.y - ((calculatedFrameSize?.y ?: 0) / 2)
        }

        super.setLayoutParams(params)
    }

    open fun onNewPosition(position: LatLng) {
        if (visibility == View.VISIBLE)
            animationQueue.addToQueue(position)
        else {
            coroutineScope.coroutineContext.cancelChildren()
            coordinateOnMap = position
        }
    }

    open suspend fun animateToPosition(
        endPosition: LatLng,
        duration: Long,
    ) {
        if (endPosition == coordinateOnMap) {
            return
        }

        val startPosition = this.coordinateOnMap

        suspendCoroutine<Unit> { continuation ->
            ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration
                interpolator = null

                addUpdateListener { animation ->
                    coordinateOnMap = LatLngInterpolator.Linear.interpolate(
                        animation.animatedFraction,
                        startPosition,
                        endPosition
                    )
                }

                doOnEnd {
                    continuation.resume(Unit)
                }

                doOnCancel {
                    continuation.resume(Unit)
                }

                post {
                    start()
                }
            }
        }
    }

    open fun show() {
        visibility = View.VISIBLE
        animationQueue.clearQueue()
    }

    open fun hide() {
        visibility = View.GONE
        animationQueue.clearQueue()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.coroutineContext.cancelChildren()
        animationQueue.clearQueue()
    }
}