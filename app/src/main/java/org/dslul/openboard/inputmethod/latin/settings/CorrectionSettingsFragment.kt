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
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.Preference
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionaryList
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionarySettings

/**
 * "Text correction" settings sub screen.
 *
 * This settings sub screen handles the following text correction preferences.
 * - Personal dictionary
 * - Add-on dictionaries
 * - Block offensive words
 * - Auto-correction
 * - Auto-correction confidence
 * - Show correction suggestions
 * - Personalized suggestions
 * - Suggest Contact names
 * - Next-word suggestions
 */
class CorrectionSettingsFragment : SubScreenFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_correction)

        val context = activity
        val pm = context.packageManager

        val editPersonalDictionary = findPreference(Settings.PREF_EDIT_PERSONAL_DICTIONARY)
        val editPersonalDictionaryIntent = editPersonalDictionary.intent
        val ri = if (USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS) null else pm.resolveActivity(
            editPersonalDictionaryIntent, PackageManager.MATCH_DEFAULT_ONLY
        )
        if (ri == null) {
            overwriteUserDictionaryPreference(editPersonalDictionary)
        }

        refreshEnabledSettings()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        refreshEnabledSettings()
    }

    private fun refreshEnabledSettings() {
        setPreferenceEnabled(
            Settings.PREF_AUTO_CORRECTION_CONFIDENCE,
            Settings.readAutoCorrectEnabled(sharedPreferences, resources)
        )
    }

    private fun overwriteUserDictionaryPreference(userDictionaryPreference: Preference) {
        val activity = activity
        val localeList = UserDictionaryList.getUserDictionaryLocalesSet(activity)
        if (null == localeList) {
            // The locale list is null if and only if the user dictionary service is
            // not present or disabled. In this case we need to remove the preference.
            preferenceScreen.removePreference(userDictionaryPreference)
        } else if (localeList.size <= 1) {
            userDictionaryPreference.fragment = UserDictionarySettings::class.java.name
            // If the size of localeList is 0, we don't set the locale parameter in the
            // extras. This will be interpreted by the UserDictionarySettings class as
            // meaning "the current locale".
            // Note that with the current code for UserDictionaryList#getUserDictionaryLocalesSet()
            // the locale list always has at least one element, since it always includes the current
            // locale explicitly. @see UserDictionaryList.getUserDictionaryLocalesSet().
            if (localeList.size == 1) {
                val locale = localeList.first()
                userDictionaryPreference.extras.putString("locale", locale)
            }
        } else {
            userDictionaryPreference.fragment = UserDictionaryList::class.java.name
        }
    }

    companion object {
        private const val DBG_USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS = false
        private const val USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS = DBG_USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS
    }
}
