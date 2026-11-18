package com.homecontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.homecontrol.app.ui.HomeControlRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as HomeControlApp).container
        setContent { HomeControlRoot(container) }
    }
}
