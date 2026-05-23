package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.AppUI
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AutomationViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val viewModel = ViewModelProvider(this)[AutomationViewModel::class.java]
    
    setContent {
      MyApplicationTheme {
        AppUI(viewModel = viewModel)
      }
    }
  }
}
