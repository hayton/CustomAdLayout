package com.hypebeast.adview

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.RelativeLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.doubleclick.PublisherAdRequest
import com.google.android.gms.ads.doubleclick.PublisherAdView
import java.lang.IllegalStateException
import kotlin.math.abs

/**
 * Created by hayton on 2020-01-07.
 */
class CustomAdLayout: RelativeLayout {
    var TAG = this::class.java.simpleName
    lateinit var adRequest: PublisherAdRequest
    lateinit var adView : PublisherAdView
    var adHolder: RelativeLayout
    var shouldShowAnimation = false
    var animationDuration = 0L
    var supportedAppSize = ""
    var adUnit = ""
    var layoutWidth = 0
    var isFailedPreviously = false

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomAdLayout,
            0,0
        ).apply {
            try {
                shouldShowAnimation = getBoolean(R.styleable.CustomAdLayout_enableAnimation, false)
                animationDuration = getInt(R.styleable.CustomAdLayout_animationDuration, 0).toLong()
                supportedAppSize = getString(R.styleable.CustomAdLayout_supportedAdSize) ?: ""
                adUnit = getString(R.styleable.CustomAdLayout_adUnit) ?: ""

                val isProperlyInitialized = supportedAppSize.isNotEmpty() && adUnit.isNotEmpty()
                if (!isProperlyInitialized) {
                    when {
                        (supportedAppSize.isEmpty()) -> throw IllegalStateException("SupportedAppSize is empty")
                        (adUnit.isEmpty()) -> throw IllegalStateException("AdUnit is empty")
                    }
                }
            } finally {
                recycle()
            }
        }
        initAdView()
    }

    init {
        View.inflate(context, R.layout.custom_ad_layout, this)
        adHolder = findViewById(R.id.adHolder)

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        (this@CustomAdLayout.parent as ViewGroup).addOnLayoutChangeListener(object :
            OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (right != oldRight) {
                    if (!::adView.isInitialized) {
                        return
                    }
                    resizeAd(0)
                    (this@CustomAdLayout.parent as ViewGroup).removeOnLayoutChangeListener(this)
                }

            }
        })
    }

    fun loadAd(publisherAdRequest: PublisherAdRequest) {
        adRequest = publisherAdRequest
        adView.loadAd(publisherAdRequest)
    }

    fun isAdLoaded(): Boolean{
        return ::adRequest.isInitialized
    }

    private fun initAdView() {
        PublisherAdView(context).apply {
            adView = this
            adView.isHorizontalScrollBarEnabled = false
            adView.isVerticalScrollBarEnabled = false
            adHolder.addView(this)
            this.adUnitId = adUnit
            val adSizesList = mutableListOf<AdSize>()
            supportedAppSize.split(",").map {
                adSizesList.add(AdSize(it.trim().split("x")[0].toInt(), it.trim().split("x")[1].toInt()))
            }
            setAdSizes(*adSizesList.toTypedArray())
            adListener = object: AdListener() {
                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    Log.d(TAG, "fail to load ad article= ${p0}")
                    isFailedPreviously = true
                    listOf(parent.parent as ViewGroup, parent as ViewGroup).map {
                        it.startAnimation(
                            CustomLayoutAnimation(0, it).apply {
                                fillAfter = true
                                duration = if (shouldShowAnimation) animationDuration else 0
                            }
                        )
                    }
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()

                    resizeAd(animationDuration)

                    startAnimation(
                        AlphaAnimation(0f, 1f).apply {
                            fillAfter = true
                            duration = if (shouldShowAnimation) animationDuration + 50 else 0
                        }
                    )

                }

            }
        }
    }

    private fun resizeAd(animTime: Long) {
        if (!::adView.isInitialized || isFailedPreviously) {
            return
        }
        adView.apply {
            val layoutparams = this@CustomAdLayout.layoutParams as MarginLayoutParams
            val initialWidth =  if
                                    (layoutWidth == 0) (this@CustomAdLayout.parent as ViewGroup).width
                                else
                                    layoutWidth
            val screenWidth = (initialWidth.toFloat() - layoutparams.marginStart - layoutparams.marginEnd).apply { Log.d(TAG, "screenwidth; onadloaded= $this, layoutWidth= ${layoutWidth}") }

            val adWidth = adSize.getWidthInPixels(context)
            this@CustomAdLayout.layoutParams.width = adWidth /** must include this line for adview to set its width properly */
            (parent as ViewGroup).layoutParams.width = adWidth

            val ratio = (screenWidth / adWidth).coerceAtMost(1f)
            scaleX = ratio
            scaleY = ratio
            val height = ((adSize.getHeightInPixels(context) * ratio)).toInt() + layoutparams.topMargin + layoutparams.bottomMargin + 1

            (layoutParams as LayoutParams).setMargins(
                0,
                -(adSize.getHeightInPixels(context) * abs(1 - scaleY) / 2).toInt(),
                0,
                -(adSize.getHeightInPixels(context) * abs(1 - scaleY) / 2).toInt()
            )

            listOf(parent.parent as ViewGroup, parent as ViewGroup).map {
                it.startAnimation(
                    CustomLayoutAnimation(height, it).apply {
                        fillAfter = true
                        duration = animTime
                    }
                )
            }
        }
    }

    inner class CustomLayoutAnimation(finalHeight: Int, val layout: ViewGroup): Animation() {
        val diff = finalHeight - layout.height
        var initialHeight = layout.height

        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)

            val newHeight = (initialHeight + diff * interpolatedTime).toInt()

            layout.layoutParams.height = newHeight
            if (layout.layoutParams.height != initialHeight+diff) {
                layout.requestLayout()
            }
        }

    }

}