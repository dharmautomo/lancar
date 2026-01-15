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

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import org.dslul.openboard.inputmethod.latin.AudioAndHapticFeedbackManager
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.RichInputMethodManager

class PreferencesSettingsFragment : SubScreenFragment() {

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_preferences)

        val res = resources
        val context = activity

        // When we are called from the Settings application but we are not already running, some
        // singleton and utility classes may not have been initialized.  We have to call
        // initialization method of these classes here. See {@link LatinIME#onCreate()}.
        RichInputMethodManager.init(context)

        val showVoiceKeyOption = res.getBoolean(R.bool.config_enable_show_voice_key_option)
        if (!showVoiceKeyOption) {
            removePreference(Settings.PREF_VOICE_INPUT_KEY)
        }
        if (!AudioAndHapticFeedbackManager.getInstance().hasVibrator()) {
            removePreference(Settings.PREF_VIBRATE_ON)
            removePreference(Settings.PREF_VIBRATION_DURATION_SETTINGS)
        }
        if (!Settings.readFromBuildConfigIfToShowKeyPreviewPopupOption(res)) {
            removePreference(Settings.PREF_POPUP_ON)
        }

        setupKeypressVibrationDurationSettings()
        setupKeypressSoundVolumeSettings()
        setupHistoryRetentionTimeSettings()
        refreshEnablingsOfKeypressSoundAndVibrationAndHistRetentionSettings()
    }

    override fun onResume() {
        super.onResume()
        val voiceInputKeyOption = findPreference(Settings.PREF_VOICE_INPUT_KEY)
        if (voiceInputKeyOption != null) {
            RichInputMethodManager.getInstance().refreshSubtypeCaches()
            val voiceKeyEnabled = VOICE_IME_ENABLED && RichInputMethodManager.getInstance().hasShortcutIme()
            voiceInputKeyOption.isEnabled = voiceKeyEnabled
            voiceInputKeyOption.summary = if (voiceKeyEnabled) null else getText(R.string.voice_input_disabled_summary)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        refreshEnablingsOfKeypressSoundAndVibrationAndHistRetentionSettings()
    }

    private fun refreshEnablingsOfKeypressSoundAndVibrationAndHistRetentionSettings() {
        val prefs = sharedPreferences
        val res = resources
        setPreferenceEnabled(
            Settings.PREF_VIBRATION_DURATION_SETTINGS,
            Settings.readVibrationEnabled(prefs, res)
        )
        setPreferenceEnabled(
            Settings.PREF_KEYPRESS_SOUND_VOLUME,
            Settings.readKeypressSoundEnabled(prefs, res)
        )
        setPreferenceEnabled(
            Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME,
            Settings.readClipboardHistoryEnabled(prefs)
        )
    }

    private fun setupKeypressVibrationDurationSettings() {
        val pref = findPreference(Settings.PREF_VIBRATION_DURATION_SETTINGS) as? SeekBarDialogPreference ?: return
        val prefs = sharedPreferences
        val res = resources
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String) {
                prefs.edit().putInt(key, value).apply()
            }

            override fun writeDefaultValue(key: String) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String): Int {
                return Settings.readKeypressVibrationDuration(prefs, res)
            }

            override fun readDefaultValue(key: String): Int {
                return Settings.readDefaultKeypressVibrationDuration(res)
            }

            override fun feedbackValue(value: Int) {
                AudioAndHapticFeedbackManager.getInstance().vibrate(value.toLong())
            }

            override fun getValueText(value: Int): String {
                return if (value < 0) {
                    res.getString(R.string.settings_system_default)
                } else res.getString(R.string.abbreviation_unit_milliseconds, value)
            }
        })
    }

    private fun setupKeypressSoundVolumeSettings() {
        val pref = findPreference(Settings.PREF_KEYPRESS_SOUND_VOLUME) as? SeekBarDialogPreference ?: return
        val prefs = sharedPreferences
        val res = resources
        val am = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            private val PERCENTAGE_FLOAT = 100.0f

            private fun getValueFromPercentage(percentage: Int): Float {
                return percentage / PERCENTAGE_FLOAT
            }

            private fun getPercentageFromValue(floatValue: Float): Int {
                return (floatValue * PERCENTAGE_FLOAT).toInt()
            }

            override fun writeValue(value: Int, key: String) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply()
            }

            override fun writeDefaultValue(key: String) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String): Int {
                return getPercentageFromValue(Settings.readKeypressSoundVolume(prefs, res))
            }

            override fun readDefaultValue(key: String): Int {
                return getPercentageFromValue(Settings.readDefaultKeypressSoundVolume(res))
            }

            override fun getValueText(value: Int): String {
                return if (value < 0) {
                    res.getString(R.string.settings_system_default)
                } else Integer.toString(value)
            }

            override fun feedbackValue(value: Int) {
                am.playSoundEffect(
                    AudioManager.FX_KEYPRESS_STANDARD, getValueFromPercentage(value)
                )
            }
        })
    }

    private fun setupHistoryRetentionTimeSettings() {
        val prefs = sharedPreferences
        val res = resources
        val pref = findPreference(Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME) as? SeekBarDialogPreference ?: return
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String) {
                prefs.edit().putInt(key, value).apply()
            }

            override fun writeDefaultValue(key: String) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String): Int {
                return Settings.readClipboardHistoryRetentionTime(prefs, res)
            }

            override fun readDefaultValue(key: String): Int {
                return Settings.readDefaultClipboardHistoryRetentionTime(res)
            }

            override fun getValueText(value: Int): String {
                return if (value <= 0) {
                    res.getString(R.string.settings_no_limit)
                } else res.getString(R.string.abbreviation_unit_minutes, value)
            }

            override fun feedbackValue(value: Int) {}
        })
    }

    companion object {
        private const val VOICE_IME_ENABLED = true
    }
}
