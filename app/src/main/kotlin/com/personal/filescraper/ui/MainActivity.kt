package com.personal.filescraper.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.personal.filescraper.FileScraperApp
import com.personal.filescraper.di.ViewModelFactory
import com.personal.filescraper.ui.navigation.AppNavHost
import com.personal.filescraper.ui.theme.FileScraperTheme

class MainActivity : ComponentActivity() {

    private val factory by lazy { ViewModelFactory((application as FileScraperApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileScraperTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost(factory = factory)
                }
            }
        }
    }
}
