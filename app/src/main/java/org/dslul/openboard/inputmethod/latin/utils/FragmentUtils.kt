/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.dslul.openboard.inputmethod.latin.utils

import org.dslul.openboard.inputmethod.latin.settings.AdvancedSettingsFragment
import org.dslul.openboard.inputmethod.latin.settings.AppearanceSettingsFragment
import org.dslul.openboard.inputmethod.latin.settings.CorrectionSettingsFragment
import org.dslul.openboard.inputmethod.latin.settings.CustomInputStyleSettingsFragment
import org.dslul.openboard.inputmethod.latin.settings.DebugSettingsFragment
import org.dslul.openboard.inputmethod.latin.settings.GestureSettingsFragment
import org.dslul.openboard.inputmethod.latin.settings.PreferencesSettingsFragment
import org.dslul.openboard.inputmethod.latin.settings.SettingsFragment
import org.dslul.openboard.inputmethod.latin.spellcheck.SpellCheckerSettingsFragment
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionaryAddWordFragment
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionaryList
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionaryLocalePicker
import org.dslul.openboard.inputmethod.latin.userdictionary.UserDictionarySettings
import java.util.HashSet

object FragmentUtils {
    private val sLatinImeFragments = HashSet<String>()

    init {
        sLatinImeFragments.add(PreferencesSettingsFragment::class.java.name)
        sLatinImeFragments.add(AppearanceSettingsFragment::class.java.name)
        sLatinImeFragments.add(CustomInputStyleSettingsFragment::class.java.name)
        sLatinImeFragments.add(GestureSettingsFragment::class.java.name)
        sLatinImeFragments.add(CorrectionSettingsFragment::class.java.name)
        sLatinImeFragments.add(AdvancedSettingsFragment::class.java.name)
        sLatinImeFragments.add(DebugSettingsFragment::class.java.name)
        sLatinImeFragments.add(SettingsFragment::class.java.name)
        sLatinImeFragments.add(SpellCheckerSettingsFragment::class.java.name)
        sLatinImeFragments.add(UserDictionaryAddWordFragment::class.java.name)
        sLatinImeFragments.add(UserDictionaryList::class.java.name)
        sLatinImeFragments.add(UserDictionaryLocalePicker::class.java.name)
        sLatinImeFragments.add(UserDictionarySettings::class.java.name)
    }

    @JvmStatic
    fun isValidFragment(fragmentName: String): Boolean {
        return sLatinImeFragments.contains(fragmentName)
    }
}
