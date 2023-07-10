package com.indest.hah.other

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.indest.hah.R
import java.util.Locale


fun NavController.safeNavigate(action: Int, levelSelectType: LevelSelectType = LevelSelectType.NONE, language:String? = null) {
    val bundle = bundleOf(
        "levelSelectType" to levelSelectType.name,
        "selectedLanguage" to language
    )

    currentDestination?.getAction(action)?.run {
        navigate(action, bundle)
    }
}


fun String.isPalindrome():Boolean {
    val testString = this.lowercase()
        .replace("""[\W+]""".toRegex(), "")
    return testString.length >= 3 && testString == testString.reversed()
}


fun AppCompatActivity.hideSystemBars(viewGroup: ViewGroup) {
    val insetsController = WindowInsetsControllerCompat(window, viewGroup)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

fun String.mapCountryCodeToLanguageAndFlag():Pair<String, Int>
{
    when(this)
    {
        "cn" -> return Pair("Chinese", R.drawable.cn)
        "ee" -> return Pair("Estonian", R.drawable.ee)
        "fi" -> return Pair("Finnish", R.drawable.fi)
        "fr" -> return Pair("French", R.drawable.fr)
        "de" -> return Pair("German", R.drawable.de)
        "in" -> return Pair("Hindi", R.drawable.`in`)
        "id" -> return Pair("Indonesian", R.drawable.id)
        "it" -> return Pair("Italian", R.drawable.it)
        "jp" -> return Pair("Japanese", R.drawable.jp)
        "kr" -> return Pair("Korean", R.drawable.kr)
        "no" -> return Pair("Norwegian", R.drawable.no)
        "br" -> return Pair("Portuguese", R.drawable.br)
        "ru" -> return Pair("Russian", R.drawable.ru)
        "es" -> return Pair("Spanish", R.drawable.es)
        "se" -> return Pair("Swedish", R.drawable.se)
        "th" -> return Pair("Thai", R.drawable.th)
        "tr" -> return Pair("Turkish", R.drawable.tr)
        "vn" -> return Pair("Vietnamese", R.drawable.vn)
    }
    return Pair("English", R.drawable.gb)
}

