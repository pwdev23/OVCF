package site.irhas.ovcf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import site.irhas.ovcf.ui.screens.MainScreen
import site.irhas.ovcf.ui.theme.OVCFTheme
import site.irhas.ovcf.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OVCFTheme {
                val viewModel: MainViewModel = viewModel()
                MainScreen(
                    viewModel = viewModel, initialUri = intent?.data
                )
            }
        }
    }
}
