package com.lucasrodrigues.a3dmarkertest.components.map.marker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Point
import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.lucasrodrigues.a3dmarkertest.CarType
import com.lucasrodrigues.a3dmarkertest.R
import com.lucasrodrigues.a3dmarkertest.components.map.Rotation3DComponent
import com.lucasrodrigues.a3dmarkertest.angleInDegrees
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class CarRotation3DMarkerView(
    context: Context,
    googleMap: GoogleMap,
    coordinateOnMap: LatLng,
    initialBearing: Int
) : MarkerView(
    context = context,
    coordinateOnMap = coordinateOnMap,
    googleMap = googleMap,
    content = inflate(context, R.layout.map_overlay_car_marker, null),
) {

    private val carBodyView: ImageView by lazy {
        findViewById(R.id.carBody)
    }

    private val carMaskView: ImageView by lazy {
        findViewById(R.id.carMask)
    }

    private val carFrameSize by lazy {
        Point(
            context.resources.getDimension(R.dimen.frame_size).toInt(),
            context.resources.getDimension(R.dimen.frame_size).toInt(),
        )
    }

    private val rotationComponent by lazy {
        Rotation3DComponent(
            imageViewsToRotate = listOf(carBodyView, carMaskView),
            frameSize = carFrameSize,
            initialAngle = initialBearing,
        )
    }

    init {
        changeCarType(CarType.NORMAL)
    }

    override fun onNewPosition(position: LatLng) {
        super.onNewPosition(position)

        if (visibility != View.VISIBLE) {
            rotationComponent.jumpToAngle(coordinateOnMap.angleInDegrees(position))
        }
    }

    override suspend fun animateToPosition(
        endPosition: LatLng,
        duration: Long,
    ) {
        if (endPosition == coordinateOnMap) {
            return
        }

        coroutineScope.launch {
            rotationComponent.animate(
                to = coordinateOnMap.angleInDegrees(
                    to = endPosition,
                ),
            )
        }

        super.animateToPosition(endPosition, duration)
    }

    private fun hideUsingScale(duration: Long) {
        if (visibility != View.VISIBLE)
            return

        coroutineScope.launch {
            val animatorScaleX = scaleXAnimator(
                duration = duration,
                show = false
            )
            val animatorScaleY = scaleYAnimator(
                duration = duration,
                show = false
            )

            AnimatorSet().apply {
                doOnEnd {
                    visibility = View.GONE
                }

                playTogether(animatorScaleX, animatorScaleY)

                post {
                    start()
                }
            }
        }
    }

    private fun showUsingScale(duration: Long) {
        if (visibility == View.VISIBLE)
            return

        coroutineScope.launch {
            val animatorScaleX = scaleXAnimator(
                duration = duration,
                show = true
            )
            val animatorScaleY = scaleYAnimator(
                duration = duration,
                show = true
            )

            AnimatorSet().apply {
                doOnStart {
                    visibility = View.VISIBLE
                }

                playTogether(animatorScaleX, animatorScaleY)

                post {
                    start()
                }
            }
        }
    }

    private fun scaleXAnimator(
        duration: Long,
        show: Boolean
    ): ObjectAnimator {
        return ObjectAnimator
            .ofFloat(
                this,
                View.SCALE_X,
                if (show) 0f else zoomOnScreen,
                if (show) zoomOnScreen else 0f
            )
            .setDuration(duration)
    }

    private fun scaleYAnimator(
        duration: Long,
        show: Boolean
    ): ObjectAnimator {
        return ObjectAnimator
            .ofFloat(
                this,
                View.SCALE_Y,
                if (show) 0f else zoomOnScreen,
                if (show) zoomOnScreen else 0f
            )
            .setDuration(duration)
    }

    override fun show() {
        showUsingScale(200L)
        coroutineScope.coroutineContext.cancelChildren()
        animationQueue.clearQueue()
    }

    override fun hide() {
        hideUsingScale(200L)
        coroutineScope.coroutineContext.cancelChildren()
        animationQueue.clearQueue()
    }

    fun changeCarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            carBodyView
                .drawable
                .colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                color,
                BlendModeCompat.MODULATE,
            )
        } else {
            carBodyView
                .drawable
                .setColorFilter(
                    color,
                    PorterDuff.Mode.MULTIPLY
                )
        }
    }

    fun changeCarType(toModel: CarType) {
        val width = carFrameSize.x * 19
        val height = carFrameSize.y * 19

        Glide.with(this)
            .load(toModel.body)
            .override(width, height)
            .into(carBodyView)

        Glide.with(this)
            .load(toModel.mask)
            .override(width, height)
            .into(carMaskView)
    }
}