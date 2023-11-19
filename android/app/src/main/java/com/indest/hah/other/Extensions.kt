package com.indest.hah.other

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.indest.hah.R
import java.text.Normalizer
import java.util.Locale

fun NavController.safeNavigate(
    action: Int,
    levelSelectType: LevelSelectType = LevelSelectType.NONE,
    language: String? = null
) {
    val bundle = bundleOf(
        "levelSelectType" to levelSelectType.name,
        "selectedLanguage" to language
    )

    currentDestination?.getAction(action)?.run {
        navigate(action, bundle)
    }
}

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()
fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

fun getCountryTagOrDefault():String {
    // Fetching the current application locale using the AndroidX support Library
    var currentLocaleName = if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
        // Fetches the current Application Locale from the list
        AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
    } else {
        // Fetches the default System Locale
        Locale.getDefault().toLanguageTag()
    }?.substring(0, 2)

    if (currentLocaleName == "en") {
        currentLocaleName = "gb"
    }
    else if (currentLocaleName == "zh")
    {
        currentLocaleName = "cn"
    }
    else if (currentLocaleName == "in")
    {
        currentLocaleName = "id"
    }

    if (currentLocaleName == null || !C.languages.containsKey(currentLocaleName))
    {
        currentLocaleName = "gb"
    }

    return currentLocaleName
}



fun String.isPalindrome(language: String = "gb"): Boolean {
    val testString = this.lowercase()
        .replace("""[\W+]""".toRegex(), "").unaccent()
    // japanese kaibun might not be classic palindrome
    val isStringPalindrome = if (language == "ja") true else testString == testString.reversed()
    return testString.length >= 4 && isStringPalindrome
}


fun String.lettersOnlyPalindrome(): String {
    return this.replace("[!?¿’,.;:'\\-\"\\s]".toRegex(), "").unaccent()
}


fun String.appendSmallSuperscript(superScript:String):SpannableStringBuilder
{
    val superscriptLevelCount = SpannableString(superScript)
    superscriptLevelCount.setSpan(
        RelativeSizeSpan(0.6f),
        0, superScript.length,
        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    superscriptLevelCount.setSpan(
        SuperscriptSpan(),
        0, superScript.length,
        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    val spannableStringBuilder = SpannableStringBuilder()
    spannableStringBuilder.append(this)
    spannableStringBuilder.append(" ")
    spannableStringBuilder.append(superscriptLevelCount)
    return spannableStringBuilder
}

fun AppCompatActivity.hideSystemBars(viewGroup: ViewGroup) {
    /*
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val buttonNavigation =
            Settings.Secure.getInt(this.contentResolver, "navigation_mode", 0) == 2
        if (!buttonNavigation) {
        }
        }*/
            val insetsController = WindowInsetsControllerCompat(window, viewGroup)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
}


// Return old value
fun AppCompatActivity.saveBooleanPreference(preference: String, newValue: Boolean): Boolean {
    var oldValue = false
    getSharedPreferences(
        getString(R.string.app_preferences),
        Context.MODE_PRIVATE
    )?.let {
        oldValue = it.getBoolean(preference, false)
        with(it.edit()) {
            putBoolean(preference, newValue)
            apply()
        }
    }
    return oldValue
}

internal fun View?.findSuitableParent(): ViewGroup? {
    var view = this
    var fallback: ViewGroup? = null
    do {
        if (view is CoordinatorLayout) {
            // We've found a CoordinatorLayout, use it
            return view
        } else if (view is FrameLayout) {
            if (view.id == android.R.id.content) {
                // If we've hit the decor content view, then we didn't find a CoL in the
                // hierarchy, so use it.
                return view
            } else {
                // It's not the content view but we'll use it as our fallback
                fallback = view
            }
        }

        if (view != null) {
            // Else, we will loop and crawl up the view hierarchy and try to find a parent
            val parent = view.parent
            view = if (parent is View) parent else null
        }
    } while (view != null)

    // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
    return fallback
}

fun String.mapCountryCodeToLanguageAndFlag(context: Context): Pair<String, Int> {
    when (this) {
        "cn" -> return Pair(context.getString(R.string.chinese), R.drawable.cn)
        "et" -> return Pair(context.getString(R.string.estonian), R.drawable.et)
        "fi" -> return Pair(context.getString(R.string.finnish), R.drawable.fi)
        "fr" -> return Pair(context.getString(R.string.french), R.drawable.fr)
        "de" -> return Pair(context.getString(R.string.german), R.drawable.de)
        "hi" -> return Pair(context.getString(R.string.hindi), R.drawable.hi)
        "id" -> return Pair(context.getString(R.string.indonesian), R.drawable.id)
        "it" -> return Pair(context.getString(R.string.italian), R.drawable.it)
        "ja" -> return Pair(context.getString(R.string.japanese), R.drawable.ja)
        "ko" -> return Pair(context.getString(R.string.korean), R.drawable.ko)
        "pt" -> return Pair(context.getString(R.string.portuguese), R.drawable.pt)
        "ru" -> return Pair(context.getString(R.string.russian), R.drawable.ru)
        "es" -> return Pair(context.getString(R.string.spanish), R.drawable.es)
        "sv" -> return Pair(context.getString(R.string.swedish), R.drawable.sv)
        "tr" -> return Pair(context.getString(R.string.turkish), R.drawable.tr)
        "uk" -> return Pair(context.getString(R.string.ukrainian), R.drawable.uk)
        "pl" -> return Pair(context.getString(R.string.polish), R.drawable.pl)
        "ro" -> return Pair(context.getString(R.string.romanian), R.drawable.ro)
        "nl" -> return Pair(context.getString(R.string.dutch), R.drawable.nl)
    }
    return Pair(context.getString(R.string.english), R.drawable.gb)
}

fun String.countryCodeToEmoji(): String {
    var allowedCountryCode = "GB"
    when (this) {
        "cn" -> allowedCountryCode = "CN"
        "et" -> allowedCountryCode = "EE"
        "fi" -> allowedCountryCode = "FI"
        "fr" -> allowedCountryCode = "FR"
        "de" -> allowedCountryCode = "DE"
        "hi" -> allowedCountryCode = "IN"
        "id" -> allowedCountryCode = "ID"
        "it" -> allowedCountryCode = "IT"
        "ja" -> allowedCountryCode = "JP"
        "ko" -> allowedCountryCode = "KR"
        "pt" -> allowedCountryCode = "BR"
        "ru" -> allowedCountryCode = "RU"
        "es" -> allowedCountryCode = "ES"
        "sv" -> allowedCountryCode = "SE"
        "tr" -> allowedCountryCode = "TR"
        "uk" -> allowedCountryCode = "UA"
        "pl" -> allowedCountryCode = "PL"
        "ro" -> allowedCountryCode = "RO"
        "nl" -> allowedCountryCode = "NL"
    }
    val firstLetter = Character.codePointAt(allowedCountryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(allowedCountryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}


fun Int.getLevelScore(puzzle: String = ""): Int {
    val multiplier = 100
    if (this <= 100) {
        return this * multiplier
    }
    return puzzle.replace("-", "").replace("\\s".toRegex(), "").length * multiplier
}


fun String.getCountryCodeLeaderboardId(context: Context) : String {
    when(this)
    {
        "cn" -> return context.resources.getString(R.string.leaderboard_chinese)
        "de" -> return context.resources.getString(R.string.leaderboard_german)
        "es" -> return context.resources.getString(R.string.leaderboard_spanish)
        "et" -> return context.resources.getString(R.string.leaderboard_estonian)
        "fi" -> return context.resources.getString(R.string.leaderboard_finnish)
        "fr" -> return context.resources.getString(R.string.leaderboard_french)
        "id" -> return context.resources.getString(R.string.leaderboard_indonesian)
        "it" -> return context.resources.getString(R.string.leaderboard_italian)
        "ja" -> return context.resources.getString(R.string.leaderboard_japanese)
        "ko" -> return context.resources.getString(R.string.leaderboard_korean)
        "nl" -> return context.resources.getString(R.string.leaderboard_dutch)
        "pl" -> return context.resources.getString(R.string.leaderboard_polish)
        "pt" -> return context.resources.getString(R.string.leaderboard_portuguese)
        "ro" -> return context.resources.getString(R.string.leaderboard_romanian)
        "sv" -> return context.resources.getString(R.string.leaderboard_swedish)
        "tr" -> return context.resources.getString(R.string.leaderboard_turkish)
        "uk" -> return context.resources.getString(R.string.leaderboard_ukrainian)
    }
    return context.resources.getString(R.string.global_leaderboards)
}
