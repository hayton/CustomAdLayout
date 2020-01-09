package com.hypebeast.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.ads.doubleclick.PublisherAdRequest

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * set AdUnit in the layout to start the demo
         * */

        customAdLayout.loadAd(PublisherAdRequest.Builder().build())

    }
}
