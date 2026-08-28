package com.indest.hah.push

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.messaging

/**
 * Stagenator integration: push notifications, per-uid token registration,
 * pseudonymous analytics identity, and claim-link taps. Mirrors the pattern
 * shipped in Subliminal Words 5.1.0 / AI Movie Quiz 1.3.0.
 */
object StagenatorPush {
    private const val TAG = "StagenatorPush"
    // Android shows the player-created level collection; iOS does not (and
    // loads no remote level content at all), so level announcements must be
    // Android-scoped. "new_levels" stays the platform-neutral channel.
    private const val TOPIC = "new_levels"
    private const val TOPIC_PLAYER_LEVELS = "player_levels"
    private const val PREFS = "stagenator_claim"

    /** Call whenever a Firebase user is available (sign-in or app start). */
    fun register(context: Context) {
        val user = Firebase.auth.currentUser ?: return
        Firebase.analytics.setUserId(user.uid)
        Firebase.messaging.subscribeToTopic(TOPIC)
        Firebase.messaging.subscribeToTopic(TOPIC_PLAYER_LEVELS)
        Firebase.messaging.token.addOnSuccessListener { token ->
            saveToken(context, token)
        }
    }

    internal fun saveToken(context: Context, token: String) {
        val user = Firebase.auth.currentUser ?: return
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) { null }
        Firebase.firestore.collection("fcmTokensAndroid").document(user.uid)
            .set(
                mapOf(
                    "token" to token,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "platform" to "android",
                    "appVersion" to version,
                ),
                SetOptions.merge(),
            )
            .addOnFailureListener { Log.w(TAG, "token registration failed", it) }
    }

    /**
     * Open a Stagenator claim link carried by a notification tap. FCM puts the
     * data payload into the launch intent's extras; Android re-delivers the
     * task-root intent on later plain opens, so each message id is consumed
     * exactly once (persisted).
     */
    /** True the first time a given key is seen. Android re-delivers the launch
     * intent on later plain opens, so every notification payload is acted on
     * at most once, persisted across process death. */
    private fun consumeOnce(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getStringSet("consumed_ids", emptySet())!!.toMutableSet()
        if (!seen.add(key)) return false
        while (seen.size > 40) seen.remove(seen.first())
        prefs.edit().putStringSet("consumed_ids", seen).apply()
        return true
    }

    private fun messageIdOf(intent: Intent?): String? =
        intent?.extras?.getString("google.message_id")
            ?: intent?.extras?.getString("google_message_id")

    fun handleClaimIntent(activity: Activity, intent: Intent?) {
        val extras = intent?.extras ?: return
        val url = extras.getString("claimUrl") ?: return
        if (!url.startsWith("https://")) return
        val messageId = messageIdOf(intent) ?: return
        if (!consumeOnce(activity, "$messageId:claim")) return
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Log.w(TAG, "claim link launch failed", e)
        }
    }

    /**
     * The level a Stagenator announcement points at, or null. The caller opens
     * it only once the level is present locally (it arrives with the Firestore
     * sync), so a tap can never navigate into a level that does not exist yet.
     */
    fun pendingLevelFromIntent(context: Context, intent: Intent?): Int? {
        val level = intent?.extras?.getString("levelNumber")?.toIntOrNull() ?: return null
        val messageId = messageIdOf(intent) ?: return null
        return if (consumeOnce(context, "$messageId:level")) level else null
    }
}

/** Keeps the registered token fresh when FCM rotates it. */
class StagenatorMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        StagenatorPush.saveToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Notification messages are displayed by the system; nothing to do in
        // the foreground (matches the other games' behavior).
    }
}
