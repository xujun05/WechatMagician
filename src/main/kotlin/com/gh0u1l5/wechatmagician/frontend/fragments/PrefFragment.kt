package com.gh0u1l5.wechatmagician.frontend.fragments

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.*
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.gh0u1l5.wechatmagician.Global.ACTION_UPDATE_PREF
import com.gh0u1l5.wechatmagician.Global.FOLDER_SHARED_PREFS
import com.gh0u1l5.wechatmagician.Global.LOG_TAG
import com.gh0u1l5.wechatmagician.Global.MAGICIAN_PACKAGE_NAME
import com.gh0u1l5.wechatmagician.R
import com.gh0u1l5.wechatmagician.util.FileUtil
import com.gh0u1l5.wechatmagician.util.FileUtil.getApplicationDataDir
import com.gh0u1l5.wechatmagician.util.ViewUtil.getColor
import java.io.File

class PrefFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = preferenceManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Move old shared preferences to device protected storage if it exists
            val oldPrefDir = "${activity?.applicationInfo?.dataDir}/$FOLDER_SHARED_PREFS"
            val newPrefDir = "${activity?.applicationInfo?.deviceProtectedDataDir}/$FOLDER_SHARED_PREFS"
            try {
                File(oldPrefDir).renameTo(File(newPrefDir))
            } catch (e: Throwable) {
                // Ignore this one
            }
            manager.setStorageDeviceProtected()
        }
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
                Log.e(LOG_TAG, "Cannot hide icon: $e")
                Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Reference: https://github.com/rovo89/XposedBridge/issues/206
    override fun onPause() {
        // Set shared preferences as world readable.
        val dataDir = getApplicationDataDir(activity)
        val folder = File("$dataDir/$FOLDER_SHARED_PREFS")
        val filename = preferenceManager.sharedPreferencesName + ".xml"
        FileUtil.setWorldReadable(File(folder, filename))

        // Notify the backend to reload the preferences
        activity?.sendBroadcast(Intent(ACTION_UPDATE_PREF))

        super.onPause()
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
    }
}