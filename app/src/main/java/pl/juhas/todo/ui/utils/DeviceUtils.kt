package pl.juhas.todo.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Klasa pomocnicza do wykrywania typu urządzenia na podstawie rozmiaru ekranu.
 * Umożliwia dostosowanie interfejsu użytkownika do różnych typów urządzeń.
 */
object DeviceUtils {

    /**
     * Sprawdza, czy urządzenie jest tabletem na podstawie rozmiaru ekranu i gęstości pikseli.
     * Przyjmujemy, że urządzenie jest tabletem, jeśli ma co najmniej 600dp szerokości w trybie portretowym
     * lub co najmniej 720dp szerokości w trybie krajobrazowym.
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                configuration.screenWidthDp >= 720
            else ->
                configuration.screenWidthDp >= 600
        }
    }
}
