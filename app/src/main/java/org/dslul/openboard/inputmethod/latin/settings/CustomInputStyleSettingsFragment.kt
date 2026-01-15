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

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodSubtype
import android.widget.Toast
import androidx.core.view.ViewCompat
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.RichInputMethodManager
import org.dslul.openboard.inputmethod.latin.utils.AdditionalSubtypeUtils
import org.dslul.openboard.inputmethod.latin.utils.DialogUtils
import org.dslul.openboard.inputmethod.latin.utils.IntentUtils
import org.dslul.openboard.inputmethod.latin.utils.SubtypeLocaleUtils
import java.util.ArrayList

class CustomInputStyleSettingsFragment : PreferenceFragment(), CustomInputStylePreference.Listener {

    private lateinit var mRichImm: RichInputMethodManager
    private lateinit var mPrefs: SharedPreferences
    private var mSubtypeLocaleAdapter: CustomInputStylePreference.SubtypeLocaleAdapter? = null
    private var mKeyboardLayoutSetAdapter: CustomInputStylePreference.KeyboardLayoutSetAdapter? = null

    private var mIsAddingNewSubtype: Boolean = false
    private var mSubtypeEnablerNotificationDialog: AlertDialog? = null
    private var mSubtypePreferenceKeyForSubtypeEnabler: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            preferenceManager.setStorageDeviceProtected()
        }
        mPrefs = preferenceManager.sharedPreferences
        RichInputMethodManager.init(activity)
        mRichImm = RichInputMethodManager.getInstance()
        addPreferencesFromResource(R.xml.additional_subtype_settings)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        val actionBar = activity.actionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.custom_input_styles_title)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        // For correct display in RTL locales, we need to set the layout direction of the
        // fragment's top view.
        if (view != null) {
            ViewCompat.setLayoutDirection(view, ViewCompat.LAYOUT_DIRECTION_LOCALE)
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val context = activity
        mSubtypeLocaleAdapter = CustomInputStylePreference.SubtypeLocaleAdapter(context)
        mKeyboardLayoutSetAdapter = CustomInputStylePreference.KeyboardLayoutSetAdapter(context)

        val prefSubtypes = Settings.readPrefAdditionalSubtypes(mPrefs, resources)
        if (DEBUG_CUSTOM_INPUT_STYLES) {
            Log.i(TAG, "Load custom input styles: $prefSubtypes")
        }
        setPrefSubtypes(prefSubtypes, context)

        mIsAddingNewSubtype = savedInstanceState != null && savedInstanceState.containsKey(
            KEY_IS_ADDING_NEW_SUBTYPE
        )
        if (mIsAddingNewSubtype) {
            preferenceScreen.addPreference(
                CustomInputStylePreference.newIncompleteSubtypePreference(context, this)
            )
        }

        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.containsKey(
                KEY_IS_SUBTYPE_ENABLER_NOTIFICATION_DIALOG_OPEN
            )
        ) {
            mSubtypePreferenceKeyForSubtypeEnabler = savedInstanceState.getString(
                KEY_SUBTYPE_FOR_SUBTYPE_ENABLER
            )
            mSubtypeEnablerNotificationDialog = createDialog()
            mSubtypeEnablerNotificationDialog?.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mIsAddingNewSubtype) {
            outState.putBoolean(KEY_IS_ADDING_NEW_SUBTYPE, true)
        }
        if (mSubtypeEnablerNotificationDialog != null && mSubtypeEnablerNotificationDialog!!.isShowing) {
            outState.putBoolean(KEY_IS_SUBTYPE_ENABLER_NOTIFICATION_DIALOG_OPEN, true)
            outState.putString(
                KEY_SUBTYPE_FOR_SUBTYPE_ENABLER, mSubtypePreferenceKeyForSubtypeEnabler
            )
        }
    }

    override fun onRemoveCustomInputStyle(stylePref: CustomInputStylePreference) {
        mIsAddingNewSubtype = false
        val group = preferenceScreen
        group.removePreference(stylePref)
        mRichImm.setAdditionalInputMethodSubtypes(subtypes)
    }

    override fun onSaveCustomInputStyle(stylePref: CustomInputStylePreference) {
        val subtype = stylePref.subtype
        if (!stylePref.hasBeenModified()) {
            return
        }
        if (findDuplicatedSubtype(subtype!!) == null) {
            mRichImm.setAdditionalInputMethodSubtypes(subtypes)
            return
        }

        // Saved subtype is duplicated.
        val group = preferenceScreen
        group.removePreference(stylePref)
        stylePref.revert()
        group.addPreference(stylePref)
        showSubtypeAlreadyExistsToast(subtype)
    }

    override fun onAddCustomInputStyle(stylePref: CustomInputStylePreference) {
        mIsAddingNewSubtype = false
        val subtype = stylePref.subtype ?: return
        if (findDuplicatedSubtype(subtype) == null) {
            mRichImm.setAdditionalInputMethodSubtypes(subtypes)
            mSubtypePreferenceKeyForSubtypeEnabler = stylePref.key
            mSubtypeEnablerNotificationDialog = createDialog()
            mSubtypeEnablerNotificationDialog?.show()
            return
        }

        // Newly added subtype is duplicated.
        val group = preferenceScreen
        group.removePreference(stylePref)
        showSubtypeAlreadyExistsToast(subtype)
    }

    override fun getSubtypeLocaleAdapter(): CustomInputStylePreference.SubtypeLocaleAdapter {
        return mSubtypeLocaleAdapter!!
    }

    override fun getKeyboardLayoutSetAdapter(): CustomInputStylePreference.KeyboardLayoutSetAdapter {
        return mKeyboardLayoutSetAdapter!!
    }

    private fun showSubtypeAlreadyExistsToast(subtype: InputMethodSubtype) {
        val context = activity
        val res = context.resources
        val message = res.getString(
            R.string.custom_input_style_already_exists,
            SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)
        )
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun findDuplicatedSubtype(subtype: InputMethodSubtype): InputMethodSubtype? {
        val localeString = subtype.locale
        val keyboardLayoutSetName = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype)
        return mRichImm.findSubtypeByLocaleAndKeyboardLayoutSet(
            localeString, keyboardLayoutSetName
        )
    }

    private fun createDialog(): AlertDialog {
        val imeId = mRichImm.inputMethodIdOfThisIme
        val builder = AlertDialog.Builder(
            DialogUtils.getPlatformDialogThemeContext(activity)
        )
        builder.setTitle(R.string.custom_input_styles_title)
            .setMessage(R.string.custom_input_style_note_message)
            .setNegativeButton(R.string.not_now, null)
            .setPositiveButton(R.string.enable) { _, _ ->
                val intent = IntentUtils.getInputLanguageSelectionIntent(
                    imeId,
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                // TODO: Add newly adding subtype to extra value of the intent as a hint
                // for the input language selection activity.
                // intent.putExtra("newlyAddedSubtype", subtypePref.getSubtype());
                startActivity(intent)
            }

        return builder.create()
    }

    private fun setPrefSubtypes(prefSubtypes: String?, context: Context) {
        val group = preferenceScreen
        group.removeAll()
        val subtypesArray =
            AdditionalSubtypeUtils.createAdditionalSubtypesArray(prefSubtypes)
        for (subtype in subtypesArray) {
            val pref = CustomInputStylePreference(context, subtype, this)
            group.addPreference(pref)
        }
    }

    private val subtypes: Array<InputMethodSubtype>
        get() {
            val group = preferenceScreen
            val subtypes = ArrayList<InputMethodSubtype>()
            val count = group.preferenceCount
            for (i in 0 until count) {
                val pref = group.getPreference(i)
                if (pref is CustomInputStylePreference) {
                    val subtypePref = pref as CustomInputStylePreference
                    // We should not save newly adding subtype to preference because it is incomplete.
                    if (subtypePref.isIncomplete) continue
                    subtypes.add(subtypePref.subtype!!)
                }
            }
            return subtypes.toTypedArray()
        }

    override fun onPause() {
        super.onPause()
        val oldSubtypes = Settings.readPrefAdditionalSubtypes(mPrefs, resources)
        val subtypes = subtypes
        val prefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(subtypes)
        if (DEBUG_CUSTOM_INPUT_STYLES) {
            Log.i(TAG, "Save custom input styles: $prefSubtypes")
        }
        if (prefSubtypes == oldSubtypes) {
            return
        }
        Settings.writePrefAdditionalSubtypes(mPrefs, prefSubtypes)
        mRichImm.setAdditionalInputMethodSubtypes(subtypes)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_style, menu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val value = TypedValue()
            activity.theme.resolveAttribute(android.R.attr.colorForeground, value, true)
            menu.findItem(R.id.action_add_style).icon?.setTint(value.data)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_add_style) {
            val newSubtype = CustomInputStylePreference.newIncompleteSubtypePreference(activity, this)
            preferenceScreen.addPreference(newSubtype)
            newSubtype.show()
            mIsAddingNewSubtype = true
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = CustomInputStyleSettingsFragment::class.java.simpleName
        private const val DEBUG_CUSTOM_INPUT_STYLES = true

        private const val KEY_IS_ADDING_NEW_SUBTYPE = "is_adding_new_subtype"
        private const val KEY_IS_SUBTYPE_ENABLER_NOTIFICATION_DIALOG_OPEN =
            "is_subtype_enabler_notification_dialog_open"
        private const val KEY_SUBTYPE_FOR_SUBTYPE_ENABLER = "subtype_for_subtype_enabler"

        @JvmStatic
        fun updateCustomInputStylesSummary(pref: Preference) {
            // When we are called from the Settings application but we are not already running, some
            // singleton and utility classes may not have been initialized.  We have to call
            // initialization method of these classes here. See {@link LatinIME#onCreate()}.
            SubtypeLocaleUtils.init(pref.context)

            val res = pref.context.resources
            val prefs = pref.sharedPreferences
            val prefSubtype = Settings.readPrefAdditionalSubtypes(prefs, res)
            val subtypes = AdditionalSubtypeUtils.createAdditionalSubtypesArray(prefSubtype)
            val subtypeNames = ArrayList<String>()
            for (subtype in subtypes) {
                subtypeNames.add(SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype))
            }
            // TODO: A delimiter of custom input styles should be localized.
            pref.summary = TextUtils.join(", ", subtypeNames)
        }
    }
}
