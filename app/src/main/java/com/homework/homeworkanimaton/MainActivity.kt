package com.homework.homeworkanimaton

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Size
import android.view.WindowInsets
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.CycleInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private lateinit var ballView: ShapeableImageView
    private lateinit var imageCardView: ShapeableImageView
    private lateinit var shrink: ObjectAnimator
    private lateinit var grow: ObjectAnimator
    private lateinit var rotateAnimator: ValueAnimator
    private lateinit var bmpCardFront: Bitmap
    private lateinit var bmpCardBack: Bitmap
    private lateinit var textInterpolatorView : TextView
    private var isFrontImage = true
    private var isRotate = true
    private var scaleFactor = 0f
    private val someInterpolator = listOf (
        AccelerateDecelerateInterpolator(),
        LinearInterpolator(),
        DecelerateInterpolator(),
        BounceInterpolator(),
        CycleInterpolator(2f),
        OvershootInterpolator(),
        AnticipateInterpolator(),
        AnticipateOvershootInterpolator(/* tension = */ 2f, /* extraTension = */ 1.5f))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textInterpolatorView = findViewById(R.id.textInterpolator)

        bmpCardFront = (ResourcesCompat.getDrawable(
            resources, R.drawable.card_front, null
        ) as BitmapDrawable).bitmap
        bmpCardFront

        bmpCardBack = (ResourcesCompat.getDrawable(
            resources, R.drawable.card_back, null
        ) as BitmapDrawable).bitmap

        imageCardView = findViewById(R.id.ImageViewCard)
        imageCardView.setBackgroundColor(resources.getColor(android.R.color.transparent,
            applicationContext.theme
        ))

        shrink = ObjectAnimator.ofFloat(imageCardView, "scaleX", 1f, 0f)
        shrink.duration = 500

        grow = ObjectAnimator.ofFloat(imageCardView, "scaleX", 0f, 1f)
        grow.duration = 500
        imageCardView.shapeAppearanceModel = imageCardView.shapeAppearanceModel.toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, 50f)
            .build()
        imageCardView.setImageBitmap(bmpCardFront)
        imageCardView.animate()
            .scaleXBy(0f)
            .scaleXBy(0f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setStartDelay(50)
            .withEndAction {
                val translationY = calcScaleCardView(imageCardView)
                imageCardView.animate()
                    .scaleX(scaleFactor)
                    .scaleY(scaleFactor)
                    .translationY(translationY)
                    .withEndAction {buildImageCardClickListener()}
        }

        buildBallAnimator()
        buildRotateCardAnimator()
    }

    private fun buildImageCardClickListener() {
        imageCardView.setOnClickListener {
            if (isRotate)
                startRotate()
            else {
                shrink.setFloatValues(scaleFactor, 0f)
                shrink.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isFrontImage = !isFrontImage
                        imageCardView.setImageBitmap(
                            when (isFrontImage) {
                                true -> bmpCardFront
                                else -> bmpCardBack
                            }
                        )
                        grow.setFloatValues(0f, scaleFactor)
                        grow.start()
                    }
                })
                shrink.start()
                isRotate = true
            }
        }
    }

    private fun buildRotateCardAnimator() {
        rotateAnimator = ValueAnimator.ofFloat(0f, 360f)
            .apply {
                duration = 1000
                addUpdateListener { animation ->
                    imageCardView.rotation = animation.animatedValue as Float
                }
            }
    }

    private fun buildBallAnimator() {
        ballView = findViewById(R.id.ImageViewBall)
        ballView.setOnClickListener {
            // It's don't need in this game
            ballView.animate().cancel()
            ballView.translationY = 0f

            ballView.animate().apply {
                val targetY = calculateWindowSize().height.toFloat() - ballView.measuredHeight
                val durationCalc = (targetY / ballView.context.resources.displayMetrics.density).toLong()
                duration = durationCalc*2
                interpolator = someInterpolator.random()
                setStartDelay(0)
                translationYBy(-targetY)
                interpolator!!::class.simpleName.also {
                    textInterpolatorView.text = it
                }
            }.withEndAction {
                ballView.animate().apply {
                    translationY(0f)
                    setStartDelay(1200)
                }
            }
        }
    }

    private fun calcScaleCardView(imageView : ShapeableImageView): Float {
        val sizeWindow = calculateWindowSize()
        val minVal = min(sizeWindow.height, sizeWindow.width)
        val minSqrt = sqrt(
            (imageView.height * imageView.height
                    + imageView.width * imageView.width).toDouble()
        )
        val minSizeImg = min(imageView.width, imageView.height)
        scaleFactor = (minVal / minSqrt).toFloat()
        return (((minSqrt * scaleFactor - minSizeImg) / 2f)).toFloat()
    }

    private fun calculateWindowSize() : Size {
        val  metrics = windowManager.currentWindowMetrics
        val windowInsets = metrics.getWindowInsets()
        val  insets = windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.navigationBars()
                    or WindowInsets.Type.displayCutout() or WindowInsets.Type.statusBars())
        val insetsHeight = insets.top + insets.bottom
        val insetsWidth = insets.right + insets.left
        val bounds = metrics.bounds
        val legacySize = Size(bounds.width() - insetsWidth, bounds.height() - insetsHeight)
        return legacySize
    }

    private fun startRotate(){
        rotateAnimator.start()
        isRotate = false
    }

    override fun onPause() {
        super.onPause()
        shrink.cancel()
        grow.cancel()
        rotateAnimator.cancel()
    }
}