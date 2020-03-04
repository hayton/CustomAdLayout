package com.hypebeast.adview

import android.content.Context
import android.content.res.Configuration
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


    interface OnResizeListener {
        fun onResizing(scale: Float)
    }


    var TAG = this::class.java.simpleName
    lateinit var adRequest: PublisherAdRequest
    lateinit var adView : PublisherAdView
    var adHolder: RelativeLayout
    private var shouldShowAnimation = false
    private var animationDuration = 0L
    private var supportedAdSize = ""
    private var adUnit = ""
    var hasResized = false
    lateinit var listener: OnResizeListener

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomAdLayout,
            0,0
        ).apply {
            try {
                shouldShowAnimation = getBoolean(R.styleable.CustomAdLayout_enableAnimation, false)
                animationDuration = if (shouldShowAnimation)
                                        getInt(R.styleable.CustomAdLayout_animationDuration, 0).toLong()
                                    else
                                        0L
                supportedAdSize = getString(R.styleable.CustomAdLayout_supportedAdSize) ?: ""
                adUnit = getString(R.styleable.CustomAdLayout_adUnit) ?: ""

                val isProperlyInitialized = supportedAdSize.isNotEmpty() && adUnit.isNotEmpty()
                if (!isProperlyInitialized) {
                    when {
                        (supportedAdSize.isEmpty()) -> throw IllegalStateException("SupportedAppSize is empty")
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

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        (this@CustomAdLayout.parent as? ViewGroup)?.addOnLayoutChangeListener(object :
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
                    resizeAd(animTime = 0)
                    (this@CustomAdLayout.parent as ViewGroup).removeOnLayoutChangeListener(this)
                }
            }
        })
    }

    fun loadAd(publisherAdRequest: PublisherAdRequest): CustomAdLayout {
        adRequest = publisherAdRequest
        (this@CustomAdLayout.parent as? ViewGroup)?.addOnLayoutChangeListener(object :
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
                    resizeAd(animTime = animationDuration)
                    (this@CustomAdLayout.parent as ViewGroup).removeOnLayoutChangeListener(this)
                }
            }
        })
        adView.loadAd(publisherAdRequest)
        return this
    }

    fun setOnResizeListener(resizeListener: OnResizeListener) {
        listener = resizeListener
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
            supportedAdSize.split(",").map {
                adSizesList.add(AdSize(it.trim().split("x")[0].toInt(), it.trim().split("x")[1].toInt()))
            }
            setAdSizes(*adSizesList.toTypedArray())
            adListener = object: AdListener() {
                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    Log.d(TAG, "fail to load ad article= ${p0}")
                    listOf(parent.parent as ViewGroup, parent as ViewGroup).map {
                        it.startAnimation(
                            CustomLayoutAnimation(0, it).apply {
                                fillAfter = true
                                duration = animationDuration
                            }
                        )
                    }
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()

                    resizeAd(animTime = animationDuration)

                    startAnimation(
                        AlphaAnimation(0f, 1f).apply {
                            fillAfter = true
                            duration = animationDuration + 50
                        }
                    )

                }

            }
        }
    }

    private fun resizeAd(parentWidth: Int = (this@CustomAdLayout.parent as ViewGroup).width, animTime: Long) {
        if (!::adView.isInitialized) {
            return
        }
        adView.apply {
            val layoutparams = this@CustomAdLayout.layoutParams as MarginLayoutParams
            val screenWidth = (parentWidth.toFloat() - layoutparams.marginStart - layoutparams.marginEnd).apply { Log.d(TAG, "screenwidth on resize= $this") }

            val adWidth = adSize.getWidthInPixels(context)
            this@CustomAdLayout.layoutParams.width = adWidth /** must include this line for adview to set its width properly */
            (parent as ViewGroup).layoutParams.width = adWidth

            val ratio = (screenWidth / adWidth).coerceAtMost(1f)
            scaleX = ratio
            scaleY = ratio
            if (::listener.isInitialized) {
                listener.onResizing(ratio)
            }
            hasResized = ratio < 1f
            val height =
                (((adSize.getHeightInPixels(context) * ratio)).toInt() + layoutparams.topMargin + layoutparams.bottomMargin + 1).apply {
                    Log.d(TAG, "adHeightInPixels= ${adSize.getHeightInPixels(context)}, scaled adHeight= $this, adview size= ${adView.adSize}")
                }

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