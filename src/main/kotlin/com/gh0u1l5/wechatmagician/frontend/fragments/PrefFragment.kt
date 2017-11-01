package com.gh0u1l5.wechatmagician.frontend.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.*
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.gh0u1l5.wechatmagician.Global.ACTION_UPDATE_PREF
import com.gh0u1l5.wechatmagician.Global.INTENT_PREF_KEYS
import com.gh0u1l5.wechatmagician.Global.LOG_TAG
import com.gh0u1l5.wechatmagician.Global.MAGICIAN_PACKAGE_NAME
import com.gh0u1l5.wechatmagician.Global.PREFERENCE_STRING_LIST_KEYS
import com.gh0u1l5.wechatmagician.R
import com.gh0u1l5.wechatmagician.util.ViewUtil.getColor

class PrefFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = preferenceManager
        if (arguments != null) {
            val preferencesResId = arguments.getInt(ARG_PREF_RES)
            manager.sharedPreferencesName = arguments.getString(ARG_PREF_NAME)
            addPreferencesFromResource(preferencesResId)
        }
        manager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            setBackgroundColor(getColor(activity, resources, R.color.card_background))
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        if (key == "settings_interface_hide_icon") {
            // Hide/Show the icon as required.
            try {
                val hide = preferences.getBoolean(key, false)
                val newState = if (hide) COMPONENT_ENABLED_STATE_DISABLED else COMPONENT_ENABLED_STATE_ENABLED
                val className = "$MAGICIAN_PACKAGE_NAME.frontend.MainActivityAlias"
                val componentName = ComponentName(MAGICIAN_PACKAGE_NAME, className)
                activity.packageManager.setComponentEnabledSetting(componentName, newState, DONT_KILL_APP)
            } catch (e: Throwable) {
                Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
            }
            return // this setting is useless for backend part.
        }

        val value = preferences.all[key]
        notifyPreferenceChange(activity, mapOf(key to value))
    }

    override fun onDestroy() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    companion object {
        private val ARG_PREF_RES = "preferencesResId"
        private val ARG_PREF_NAME = "preferencesFileName"

        fun newInstance(preferencesResId: Int, preferencesName: String): PrefFragment {
            val fragment = PrefFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_PREF_RES, preferencesResId)
                putString(ARG_PREF_NAME, preferencesName)
            }
            return fragment
        }

        // notifyPreferenceChange notifies the backend that the shared preference has been changed.
        fun notifyPreferenceChange(context: Context?, data: Map<String, Any?>) {
            context?.sendBroadcast(Intent(ACTION_UPDATE_PREF).apply {
                putExtra(INTENT_PREF_KEYS, data.keys.toTypedArray())
                for (entry in data) {
                    val key = entry.key; var value = entry.value
                    if (key in PREFERENCE_STRING_LIST_KEYS && value is String) {
                        // Split value into an Array<String> if it's a list.
                        value = value.split(' ').toTypedArray()
                    }
                    // Note: Here's a trick called "smart cast"
                    when(value) {
                        is String ->   putExtra(key, value)
                        is Boolean ->  putExtra(key, value)
                        is Array<*> -> putExtra(key, value)
                        else ->
                            Log.e(LOG_TAG, "Unknown Preference Type: ${value?.javaClass}")
                    }
                }
            })
        }
    }
}