package pl.juhas.todo.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Klasa pomocnicza do zarządzania załącznikami w aplikacji
 */
class AttachmentHelper(private val context: Context) {

    /**
     * Otwarcie pliku o podanym URI i typie MIME przy użyciu zainstalowanych aplikacji.
     * @param uri ścieżka do pliku (np. z FileProvider)
     * @param mimeType typ MIME pliku (np. "image/jpeg", "application/pdf", "application/msword")
     * @return true jeśli znalazła się aplikacja do otwarcia pliku, false w przeciwnym razie
     */
    fun openAttachment(uri: Uri, mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Brak aplikacji do otwarcia pliku",
                Toast.LENGTH_SHORT
            ).show()
            false
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Błąd podczas otwierania pliku: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
            false
        }
    }
}