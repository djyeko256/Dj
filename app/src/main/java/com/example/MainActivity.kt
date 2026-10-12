package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.DjConsoleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DjViewModel

class MainActivity : ComponentActivity() {

  private val djViewModel: DjViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
          DjConsoleScreen(viewModel = djViewModel)
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Virtual DJ: $name", modifier = modifier)
}
