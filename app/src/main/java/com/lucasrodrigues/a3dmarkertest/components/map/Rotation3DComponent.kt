package com.lucasrodrigues.a3dmarkertest.components.map

import android.graphics.Matrix
import android.graphics.Point
import android.widget.ImageView
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.math.absoluteValue

class Rotation3DComponent(
    imageViewsToRotate: List<ImageView>,
    initialAngle: Int = 0,
    private val frameSize: Point,
) {

    companion object {
        const val MIN_ANGLE = 0
        const val MAX_ANGLE = 359
        const val FULL_ANGLE = 360

        const val ROTATION_DEFAULT_DURATION = 2000L
    }

    private var currentAngle = 0

    private val imageViewsToRotate = imageViewsToRotate.map { WeakReference(it) }

    init {
        jumpToAngle(initialAngle)
    }

    fun jumpToAngle(degree: Int) {
        val point = findAnglePositionOnImage(degree)

        imageViewsToRotate.forEach { weakReference ->
            weakReference.get()?.let { imageView ->
                currentAngle = degree

                imageView.imageMatrix = Matrix().apply {
                    setTranslate(-point.x.toFloat(), -point.y.toFloat())
                }
            }
        }
    }

    suspend fun animate(
        from: Int = currentAngle,
        to: Int
    ) {
        check(!(from !in 0..FULL_ANGLE || to !in 0..FULL_ANGLE)) {
            "Angle must be between 0 and 360 degrees"
        }

        if (from != to) {
            if (from != currentAngle)
                jumpToAngle(from)

            val degrees = shortestAngleMovement(from, to)
            val degreeCount = degrees.absoluteValue
            val isClockwiseTurn = degrees > 0

            val turnDuration = (degreeCount * ROTATION_DEFAULT_DURATION) / FULL_ANGLE

            for (i in 0..degreeCount) {
                delay(turnDuration / degreeCount)

                withContext(Dispatchers.Main) {
                    if (isClockwiseTurn) {
                        jumpToAngle(
                            if (currentAngle == MAX_ANGLE)
                                MIN_ANGLE
                            else
                                currentAngle + 1
                        )
                    } else {
                        jumpToAngle(
                            if (currentAngle == MIN_ANGLE)
                                MAX_ANGLE
                            else
                                currentAngle - 1
                        )
                    }
                }
            }
        }
    }

    private fun shortestAngleMovement(
        from: Int,
        to: Int
    ): Int {
        val difference = to - from

        val conjugate = if (difference >= 0) {
            difference - FULL_ANGLE
        } else {
            difference + FULL_ANGLE
        }

        return if (conjugate.absoluteValue < difference.absoluteValue)
            conjugate
        else
            difference
    }

    private fun findAnglePositionOnImage(degree: Int): Point {
        val (row, column) = when {
            degree == MAX_ANGLE -> Pair(0, 0)
            degree <= 18 -> Pair(0, degree)
            else -> Pair(degree / 19, degree % 19)
        }

        return Point(
            column * frameSize.x,
            row * frameSize.y,
        )
    }
}