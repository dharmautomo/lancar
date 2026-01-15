/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceActivity
import androidx.core.app.ActivityCompat
import org.dslul.openboard.inputmethod.latin.permissions.PermissionsManager
import org.dslul.openboard.inputmethod.latin.utils.FragmentUtils

class SettingsActivity : PreferenceActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private var mShowHomeAsUp: Boolean = false

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        val actionBar = actionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }
    }

    override fun getIntent(): Intent {
        val intent = super.getIntent()
        val fragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
        if (fragment == null) {
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT)
        }
        intent.putExtra(EXTRA_NO_HEADERS, true)
        return intent
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return FragmentUtils.isValidFragment(fragmentName)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        PermissionsManager.get(this).onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val DEFAULT_FRAGMENT = SettingsFragment::class.java.name

        const val EXTRA_ENTRY_KEY = "entry"
        const val EXTRA_ENTRY_VALUE_LONG_PRESS_COMMA = "long_press_comma"
        const val EXTRA_ENTRY_VALUE_APP_ICON = "app_icon"
        const val EXTRA_ENTRY_VALUE_NOTICE_DIALOG = "important_notice"
        const val EXTRA_ENTRY_VALUE_SYSTEM_SETTINGS = "system_settings"
    }
}
