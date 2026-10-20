package dev.ideonq.aetheros.agent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import dev.ideonq.aetheros.DeviceRepository

sealed interface ContactMatch {
    data class Found(val name: String, val number: String) : ContactMatch
    data object NeedsPermission : ContactMatch
    data object Missing : ContactMatch
    data object Ambiguous : ContactMatch
}

class PhoneActions(private val context: Context) {
    private val repo = DeviceRepository(context)

    fun resolveContact(name: String): ContactMatch {
        if (name.matches(Regex("""\+?[0-9() -]{7,20}"""))) {
            return ContactMatch.Found(name, name.filter { it.isDigit() || it == '+' })
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED) return ContactMatch.NeedsPermission

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val matches = mutableListOf<Pair<String, String>>()
        try {
            context.contentResolver.query(
                uri, projection,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?",
                arrayOf(name),
                null
            )?.use { cursor ->
                while (cursor.moveToNext() && matches.size < 10) {
                    val label = cursor.getString(0) ?: continue
                    val number = cursor.getString(1) ?: continue
                    matches.add(label to number)
                }
            }
        } catch (_: SecurityException) {
            return ContactMatch.NeedsPermission
        }
        val distinct = matches.distinctBy { it.second.filter(Char::isDigit) }
        return when (distinct.size) {
            0 -> ContactMatch.Missing
            1 -> ContactMatch.Found(distinct[0].first, distinct[0].second)
            else -> ContactMatch.Ambiguous
        }
    }

    fun smsDraft(recipient: String, message: String): Boolean {
        return try {
            val sms = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.fromParts("smsto", recipient, null)
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sms)
            true
        } catch (_: Exception) { false }
    }

    /** Native Sharesheet hands the content to KakaoTalk. Recipient selection
     * and final sending happen INSIDE KakaoTalk; not an automatic message send. */
    fun kakaoShare(message: String): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.kakao.talk")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) { false }
    }

    fun openApp(name: String): Boolean {
        val matches = repo.launchableApps().filter { it.title.equals(name, ignoreCase = true) }
        if (matches.size != 1) return false
        return try {
            repo.launchApp(matches[0].packageName)
            true
        } catch (_: Exception) { false }
    }

    fun openSetting(name: String) {
        val action = when {
            name.contains("배터리") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            name.contains("화면") -> Settings.ACTION_DISPLAY_SETTINGS
            name.contains("와이파이") -> Settings.ACTION_WIFI_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        repo.openSettings(action)
    }
}
