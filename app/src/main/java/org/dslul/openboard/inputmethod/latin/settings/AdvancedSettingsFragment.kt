/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dslul.openboard.inputmethod.latin.settings

import android.content.SharedPreferences
import android.os.Bundle
import org.dslul.openboard.inputmethod.latin.AudioAndHapticFeedbackManager
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.SystemBroadcastReceiver

/**
 * "Advanced" settings sub screen.
 *
 * This settings sub screen handles the following advanced preferences.
 * - Key popup dismiss delay
 * - Keypress vibration duration
 * - Keypress sound volume
 * - Show app icon
 * - Improve keyboard
 * - Debug settings
 */
class AdvancedSettingsFragment : SubScreenFragment() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_advanced)

        val context = activity

        // When we are called from the Settings application but we are not already running, some
        // singleton and utility classes may not have been initialized.  We have to call
        // initialization method of these classes here. See {@link LatinIME#onCreate()}.
        AudioAndHapticFeedbackManager.init(context)

        val prefs = sharedPreferences

        if (!Settings.isInternal(prefs)) {
            removePreference(Settings.SCREEN_DEBUG)
        }

        setupKeyLongpressTimeoutSettings()
    }


    private fun setupKeyLongpressTimeoutSettings() {
        val prefs = sharedPreferences
        val res = resources
        val pref = findPreference(Settings.PREF_KEY_LONGPRESS_TIMEOUT) as? SeekBarDialogPreference ?: return
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String) {
                prefs.edit().putInt(key, value).apply()
            }

            override fun writeDefaultValue(key: String) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String): Int {
                return Settings.readKeyLongpressTimeout(prefs, res)
            }

            override fun readDefaultValue(key: String): Int {
                return Settings.readDefaultKeyLongpressTimeout(res)
            }

            override fun getValueText(value: Int): String {
                return res.getString(R.string.abbreviation_unit_milliseconds, value)
            }

            override fun feedbackValue(value: Int) {}
        })
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        if (key == Settings.PREF_SHOW_SETUP_WIZARD_ICON) {
            SystemBroadcastReceiver.toggleAppIcon(activity)
        }
    }
}
