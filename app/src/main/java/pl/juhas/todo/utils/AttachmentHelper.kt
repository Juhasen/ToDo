package pl.juhas.todo.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Klasa pomocnicza do zarządzania załącznikami w aplikacji
 */
class AttachmentHelper(private val context: Context) {

    private val TAG = "AttachmentHelper"

    /**
     * Otwiera załącznik w zewnętrznej aplikacji
     * @return true jeśli udało się otworzyć załącznik, false w przeciwnym razie
     */
    fun openAttachment(uri: Uri, mimeType: String): Boolean {
        Log.d(TAG, "Próba otwarcia załącznika: $uri, mimetype: $mimeType")

        try {
            // Najpierw spróbuj uzyskać trwałe uprawnienia do URI
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                Log.d(TAG, "Uzyskano trwałe uprawnienia do URI: $uri")
            } catch (e: SecurityException) {
                // Ignoruj błąd, jeśli nie można uzyskać trwałych uprawnień
                Log.w(TAG, "Nie można uzyskać trwałych uprawnień do URI: ${e.message}")
            }

            // Standardowe otwarcie pliku
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))

            // Sprawdź, czy istnieje aplikacja, która może obsłużyć ten intent
            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "Znaleziono aplikację do obsługi załącznika, uruchamiam intent")
                context.startActivity(intent)
                return true
            } else {
                Log.e(TAG, "Nie znaleziono aplikacji do obsługi załącznika")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas otwierania załącznika: ${e.message}", e)
            e.printStackTrace()
            return false
        }
    }

}
