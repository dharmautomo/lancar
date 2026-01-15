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

package org.dslul.openboard.inputmethod.latin.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import android.view.Gravity
import org.dslul.openboard.inputmethod.latin.AudioAndHapticFeedbackManager
import org.dslul.openboard.inputmethod.latin.InputAttributes
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.common.StringUtils
import org.dslul.openboard.inputmethod.latin.utils.AdditionalSubtypeUtils
import org.dslul.openboard.inputmethod.latin.utils.DeviceProtectedUtils
import org.dslul.openboard.inputmethod.latin.utils.JniUtils
import org.dslul.openboard.inputmethod.latin.utils.ResourceUtils
import org.dslul.openboard.inputmethod.latin.utils.RunInLocale
import org.dslul.openboard.inputmethod.latin.utils.StatsUtils
import java.util.Collections
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock

class Settings private constructor() : SharedPreferences.OnSharedPreferenceChangeListener {

    private var mContext: Context? = null
    private var mRes: Resources? = null
    private lateinit var mPrefs: SharedPreferences
    private var mSettingsValues: SettingsValues? = null
    private val mSettingsValuesLock = ReentrantLock()

    fun onCreate(context: Context) {
        mContext = context
        mRes = context.resources
        mPrefs = DeviceProtectedUtils.getSharedPreferences(context)
        mPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    fun onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        mSettingsValuesLock.lock()
        try {
            if (mSettingsValues == null) {
                // TODO: Introduce a static function to register this class and ensure that
                // loadSettings must be called before "onSharedPreferenceChanged" is called.
                Log.w(TAG, "onSharedPreferenceChanged called before loadSettings.")
                return
            }
            loadSettings(mContext!!, mSettingsValues!!.mLocale, mSettingsValues!!.mInputAttributes)
            StatsUtils.onLoadSettings(mSettingsValues)
        } finally {
            mSettingsValuesLock.unlock()
        }
    }

    fun loadSettings(
        context: Context, locale: Locale,
        inputAttributes: InputAttributes
    ) {
        mSettingsValuesLock.lock()
        mContext = context
        try {
            val prefs = mPrefs
            val job = object : RunInLocale<SettingsValues>() {
                override fun job(res: Resources): SettingsValues {
                    return SettingsValues(context, mPrefs, res, inputAttributes)
                }
            }
            mSettingsValues = job.runInLocale(mRes, locale)
        } finally {
            mSettingsValuesLock.unlock()
        }
    }

    // TODO: Remove this method and add proxy method to SettingsValues.
    val current: SettingsValues?
        get() = mSettingsValues

    val isInternal: Boolean
        get() = mSettingsValues!!.mIsInternal

    fun writeOneHandedModeEnabled(enabled: Boolean) {
        mPrefs.edit().putBoolean(PREF_ONE_HANDED_MODE, enabled).apply()
    }

    fun writeOneHandedModeGravity(gravity: Int) {
        mPrefs.edit().putInt(PREF_ONE_HANDED_GRAVITY, gravity).apply()
    }

    fun writeLastUsedPersonalizationToken(token: ByteArray?) {
        if (token == null) {
            mPrefs.edit().remove(PREF_LAST_USED_PERSONALIZATION_TOKEN).apply()
        } else {
            val tokenStr = StringUtils.byteArrayToHexString(token)
            mPrefs.edit().putString(PREF_LAST_USED_PERSONALIZATION_TOKEN, tokenStr).apply()
        }
    }

    fun readLastUsedPersonalizationToken(): ByteArray? {
        val tokenStr = mPrefs.getString(PREF_LAST_USED_PERSONALIZATION_TOKEN, null)
        return StringUtils.hexStringToByteArray(tokenStr)
    }

    fun writeLastPersonalizationDictWipedTime(timestamp: Long) {
        mPrefs.edit().putLong(PREF_LAST_PERSONALIZATION_DICT_WIPED_TIME, timestamp).apply()
    }

    fun readLastPersonalizationDictGeneratedTime(): Long {
        return mPrefs.getLong(PREF_LAST_PERSONALIZATION_DICT_WIPED_TIME, 0)
    }

    fun writeCorpusHandlesForPersonalization(corpusHandles: Set<String>) {
        mPrefs.edit().putStringSet(PREF_CORPUS_HANDLES_FOR_PERSONALIZATION, corpusHandles).apply()
    }

    fun readCorpusHandlesForPersonalization(): Set<String> {
        val emptySet = Collections.emptySet<String>()
        return mPrefs.getStringSet(PREF_CORPUS_HANDLES_FOR_PERSONALIZATION, emptySet)!!
    }

    companion object {
        private val TAG = Settings::class.java.simpleName

        // Settings screens
        const val SCREEN_THEME = "screen_theme"
        const val SCREEN_DEBUG = "screen_debug"
        const val SCREEN_GESTURE = "screen_gesture"

        // In the same order as xml/prefs.xml
        const val PREF_AUTO_CAP = "auto_cap"
        const val PREF_VIBRATE_ON = "vibrate_on"
        const val PREF_SOUND_ON = "sound_on"
        const val PREF_POPUP_ON = "popup_on"
        const val PREF_THEME_FAMILY = "theme_family"
        const val PREF_THEME_VARIANT = "theme_variant"
        const val PREF_THEME_KEY_BORDERS = "theme_key_borders"
        const val PREF_THEME_DAY_NIGHT = "theme_auto_day_night"
        const val PREF_THEME_AMOLED_MODE = "theme_amoled_mode"

        // PREF_VOICE_MODE_OBSOLETE is obsolete. Use PREF_VOICE_INPUT_KEY instead.
        const val PREF_VOICE_MODE_OBSOLETE = "voice_mode"
        const val PREF_VOICE_INPUT_KEY = "pref_voice_input_key"
        const val PREF_CLIPBOARD_CLIPBOARD_KEY = "pref_clipboard_clipboard_key"
        const val PREF_EDIT_PERSONAL_DICTIONARY = "edit_personal_dictionary"
        const val PREF_AUTO_CORRECTION = "pref_key_auto_correction"
        const val PREF_AUTO_CORRECTION_CONFIDENCE = "pref_key_auto_correction_confidence"

        // PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE is obsolete. Use PREF_SHOW_SUGGESTIONS instead.
        const val PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE = "show_suggestions_setting"
        const val PREF_SHOW_SUGGESTIONS = "show_suggestions"
        const val PREF_KEY_USE_PERSONALIZED_DICTS = "pref_key_use_personalized_dicts"
        const val PREF_KEY_USE_DOUBLE_SPACE_PERIOD = "pref_key_use_double_space_period"
        const val PREF_BLOCK_POTENTIALLY_OFFENSIVE = "pref_key_block_potentially_offensive"
        @JvmField
        val ENABLE_SHOW_LANGUAGE_SWITCH_KEY_SETTINGS = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT
        @JvmField
        val SHOULD_SHOW_LXX_SUGGESTION_UI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        const val PREF_SHOW_LANGUAGE_SWITCH_KEY = "pref_show_language_switch_key"
        const val PREF_SHOW_EMOJI_KEY = "pref_show_emoji_key"
        const val PREF_SHOW_CLIPBOARD_KEY = "pref_show_clipboard_key"
        const val PREF_INCLUDE_OTHER_IMES_IN_LANGUAGE_SWITCH_LIST = "pref_include_other_imes_in_language_switch_list"
        const val PREF_CUSTOM_INPUT_STYLES = "custom_input_styles"
        const val PREF_ENABLE_SPLIT_KEYBOARD = "pref_split_keyboard"
        const val PREF_KEYBOARD_HEIGHT_SCALE = "pref_keyboard_height_scale"
        const val PREF_SPACE_TRACKPAD = "pref_space_trackpad"
        const val PREF_DELETE_SWIPE = "pref_delete_swipe"
        const val PREF_AUTOSPACE_AFTER_PUNCTUATION = "pref_autospace_after_punctuation"
        const val PREF_ALWAYS_INCOGNITO_MODE = "pref_always_incognito_mode"
        const val PREF_BIGRAM_PREDICTIONS = "next_word_prediction"
        const val PREF_GESTURE_INPUT = "gesture_input"
        const val PREF_VIBRATION_DURATION_SETTINGS = "pref_vibration_duration_settings"
        const val PREF_KEYPRESS_SOUND_VOLUME = "pref_keypress_sound_volume"
        const val PREF_KEY_LONGPRESS_TIMEOUT = "pref_key_longpress_timeout"
        const val PREF_ENABLE_EMOJI_ALT_PHYSICAL_KEY = "pref_enable_emoji_alt_physical_key"
        const val PREF_GESTURE_PREVIEW_TRAIL = "pref_gesture_preview_trail"
        const val PREF_GESTURE_FLOATING_PREVIEW_TEXT = "pref_gesture_floating_preview_text"
        const val PREF_SHOW_SETUP_WIZARD_ICON = "pref_show_setup_wizard_icon"

        const val PREF_ONE_HANDED_MODE = "pref_one_handed_mode_enabled"
        const val PREF_ONE_HANDED_GRAVITY = "pref_one_handed_mode_gravity"

        const val PREF_KEY_IS_INTERNAL = "pref_key_is_internal"

        const val PREF_ENABLE_METRICS_LOGGING = "pref_enable_metrics_logging"

        const val PREF_SHOW_NUMBER_ROW = "pref_show_number_row"

        const val PREF_SHOW_HINTS = "pref_show_hints"

        const val PREF_SPACE_TO_CHANGE_LANG = "prefs_long_press_keyboard_to_change_lang"

        const val PREF_ENABLE_CLIPBOARD_HISTORY = "pref_enable_clipboard_history"
        const val PREF_CLIPBOARD_HISTORY_RETENTION_TIME = "pref_clipboard_history_retention_time"

        // This preference key is deprecated. Use {@link #PREF_SHOW_LANGUAGE_SWITCH_KEY} instead.
        // This is being used only for the backward compatibility.
        private const val PREF_SUPPRESS_LANGUAGE_SWITCH_KEY = "pref_suppress_language_switch_key"

        private const val PREF_LAST_USED_PERSONALIZATION_TOKEN = "pref_last_used_personalization_token"
        private const val PREF_LAST_PERSONALIZATION_DICT_WIPED_TIME = "pref_last_used_personalization_dict_wiped_time"
        private const val PREF_CORPUS_HANDLES_FOR_PERSONALIZATION = "pref_corpus_handles_for_personalization"

        // Emoji
        const val PREF_EMOJI_RECENT_KEYS = "emoji_recent_keys"
        const val PREF_EMOJI_CATEGORY_LAST_TYPED_ID = "emoji_category_last_typed_id"
        const val PREF_LAST_SHOWN_EMOJI_CATEGORY_ID = "last_shown_emoji_category_id"
        const val PREF_LAST_SHOWN_EMOJI_CATEGORY_PAGE_ID = "last_shown_emoji_category_page_id"

        private const val UNDEFINED_PREFERENCE_VALUE_FLOAT = -1.0f
        private const val UNDEFINED_PREFERENCE_VALUE_INT = -1

        private val sInstance = Settings()

        @JvmStatic
        fun getInstance(): Settings {
            return sInstance
        }

        @JvmStatic
        fun init(context: Context) {
            sInstance.onCreate(context)
        }

        @JvmStatic
        fun readScreenMetrics(res: Resources): Int {
            return res.getInteger(R.integer.config_screen_metrics)
        }

        // Accessed from the settings interface, hence public
        @JvmStatic
        fun readKeypressSoundEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return prefs.getBoolean(
                PREF_SOUND_ON,
                res.getBoolean(R.bool.config_default_sound_enabled)
            )
        }

        @JvmStatic
        fun readVibrationEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            val hasVibrator = AudioAndHapticFeedbackManager.getInstance().hasVibrator()
            return hasVibrator && prefs.getBoolean(
                PREF_VIBRATE_ON,
                res.getBoolean(R.bool.config_default_vibration_enabled)
            )
        }

        @JvmStatic
        fun readAutoCorrectEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return prefs.getBoolean(PREF_AUTO_CORRECTION, true)
        }

        @JvmStatic
        fun readAutoCorrectConfidence(
            prefs: SharedPreferences,
            res: Resources
        ): String {
            return prefs.getString(
                PREF_AUTO_CORRECTION_CONFIDENCE,
                res.getString(R.string.auto_correction_threshold_mode_index_modest)
            )!!
        }

        @JvmStatic
        fun readPlausibilityThreshold(res: Resources): Float {
            return res.getString(R.string.plausibility_threshold).toFloat()
        }

        @JvmStatic
        fun readBlockPotentiallyOffensive(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return prefs.getBoolean(
                PREF_BLOCK_POTENTIALLY_OFFENSIVE,
                res.getBoolean(R.bool.config_block_potentially_offensive)
            )
        }

        @JvmStatic
        fun readFromBuildConfigIfGestureInputEnabled(res: Resources): Boolean {
            if (!JniUtils.sHaveGestureLib) {
                return false
            }
            return res.getBoolean(R.bool.config_gesture_input_enabled_by_build_config)
        }

        @JvmStatic
        fun readGestureInputEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return readFromBuildConfigIfGestureInputEnabled(res) &&
                    prefs.getBoolean(PREF_GESTURE_INPUT, true)
        }

        @JvmStatic
        fun readFromBuildConfigIfToShowKeyPreviewPopupOption(res: Resources): Boolean {
            return res.getBoolean(R.bool.config_enable_show_key_preview_popup_option)
        }

        @JvmStatic
        fun readKeyPreviewPopupEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            val defaultKeyPreviewPopup = res.getBoolean(
                R.bool.config_default_key_preview_popup
            )
            if (!readFromBuildConfigIfToShowKeyPreviewPopupOption(res)) {
                return defaultKeyPreviewPopup
            }
            return prefs.getBoolean(PREF_POPUP_ON, defaultKeyPreviewPopup)
        }

        @JvmStatic
        fun readAlwaysIncognitoMode(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_ALWAYS_INCOGNITO_MODE, false)
        }

        @JvmStatic
        fun readPrefAdditionalSubtypes(
            prefs: SharedPreferences,
            res: Resources
        ): String {
            val predefinedPrefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(
                res.getStringArray(R.array.predefined_subtypes)
            )
            return prefs.getString(PREF_CUSTOM_INPUT_STYLES, predefinedPrefSubtypes)!!
        }

        @JvmStatic
        fun writePrefAdditionalSubtypes(
            prefs: SharedPreferences,
            prefSubtypes: String
        ) {
            prefs.edit().putString(PREF_CUSTOM_INPUT_STYLES, prefSubtypes).apply()
        }

        @JvmStatic
        fun readKeypressSoundVolume(
            prefs: SharedPreferences,
            res: Resources
        ): Float {
            val volume = prefs.getFloat(
                PREF_KEYPRESS_SOUND_VOLUME, UNDEFINED_PREFERENCE_VALUE_FLOAT
            )
            return if (volume != UNDEFINED_PREFERENCE_VALUE_FLOAT) volume else readDefaultKeypressSoundVolume(
                res
            )
        }

        // Default keypress sound volume for unknown devices.
        // The negative value means system default.
        private val DEFAULT_KEYPRESS_SOUND_VOLUME = (-1.0f).toString()

        @JvmStatic
        fun readDefaultKeypressSoundVolume(res: Resources): Float {
            return ResourceUtils.getDeviceOverrideValue(
                res,
                R.array.keypress_volumes, DEFAULT_KEYPRESS_SOUND_VOLUME
            ).toFloat()
        }

        @JvmStatic
        fun readKeyLongpressTimeout(
            prefs: SharedPreferences,
            res: Resources
        ): Int {
            val milliseconds = prefs.getInt(
                PREF_KEY_LONGPRESS_TIMEOUT, UNDEFINED_PREFERENCE_VALUE_INT
            )
            return if (milliseconds != UNDEFINED_PREFERENCE_VALUE_INT) milliseconds else readDefaultKeyLongpressTimeout(
                res
            )
        }

        @JvmStatic
        fun readDefaultKeyLongpressTimeout(res: Resources): Int {
            return res.getInteger(R.integer.config_default_longpress_key_timeout)
        }

        @JvmStatic
        fun readKeypressVibrationDuration(
            prefs: SharedPreferences,
            res: Resources
        ): Int {
            val milliseconds = prefs.getInt(
                PREF_VIBRATION_DURATION_SETTINGS, UNDEFINED_PREFERENCE_VALUE_INT
            )
            return if (milliseconds != UNDEFINED_PREFERENCE_VALUE_INT) milliseconds else readDefaultKeypressVibrationDuration(
                res
            )
        }

        // Default keypress vibration duration for unknown devices.
        // The negative value means system default.
        private val DEFAULT_KEYPRESS_VIBRATION_DURATION = (-1).toString()

        @JvmStatic
        fun readDefaultKeypressVibrationDuration(res: Resources): Int {
            return ResourceUtils.getDeviceOverrideValue(
                res,
                R.array.keypress_vibration_durations, DEFAULT_KEYPRESS_VIBRATION_DURATION
            ).toInt()
        }

        @JvmStatic
        fun readKeyPreviewAnimationScale(
            prefs: SharedPreferences,
            prefKey: String, defaultValue: Float
        ): Float {
            val fraction = prefs.getFloat(prefKey, UNDEFINED_PREFERENCE_VALUE_FLOAT)
            return if (fraction != UNDEFINED_PREFERENCE_VALUE_FLOAT) fraction else defaultValue
        }

        @JvmStatic
        fun readKeyPreviewAnimationDuration(
            prefs: SharedPreferences,
            prefKey: String, defaultValue: Int
        ): Int {
            val milliseconds = prefs.getInt(prefKey, UNDEFINED_PREFERENCE_VALUE_INT)
            return if (milliseconds != UNDEFINED_PREFERENCE_VALUE_INT) milliseconds else defaultValue
        }

        @JvmStatic
        fun readClipboardHistoryEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_ENABLE_CLIPBOARD_HISTORY, true)
        }

        @JvmStatic
        fun readClipboardHistoryRetentionTime(
            prefs: SharedPreferences,
            res: Resources
        ): Int {
            val minutes = prefs.getInt(
                PREF_CLIPBOARD_HISTORY_RETENTION_TIME, UNDEFINED_PREFERENCE_VALUE_INT
            )
            return if (minutes != UNDEFINED_PREFERENCE_VALUE_INT) minutes else readDefaultClipboardHistoryRetentionTime(
                res
            )
        }

        @JvmStatic
        fun readDefaultClipboardHistoryRetentionTime(res: Resources): Int {
            return res.getInteger(R.integer.config_clipboard_history_retention_time)
        }

        @JvmStatic
        fun readShowsNumberRow(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_SHOW_NUMBER_ROW, false)
        }

        @JvmStatic
        fun readKeyboardHeight(
            prefs: SharedPreferences,
            defaultValue: Float
        ): Float {
            val percentage = prefs.getFloat(
                PREF_KEYBOARD_HEIGHT_SCALE, UNDEFINED_PREFERENCE_VALUE_FLOAT
            )
            return if (percentage != UNDEFINED_PREFERENCE_VALUE_FLOAT) percentage else defaultValue
        }

        @JvmStatic
        fun readSpaceTrackpadEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_SPACE_TRACKPAD, true)
        }

        @JvmStatic
        fun readDeleteSwipeEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_DELETE_SWIPE, true)
        }

        @JvmStatic
        fun readAutospaceAfterPunctuationEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_AUTOSPACE_AFTER_PUNCTUATION, false)
        }

        @JvmStatic
        fun readUseFullscreenMode(res: Resources): Boolean {
            return res.getBoolean(R.bool.config_use_fullscreen_mode)
        }

        @JvmStatic
        fun readShowSetupWizardIcon(
            prefs: SharedPreferences,
            context: Context
        ): Boolean {
            if (!prefs.contains(PREF_SHOW_SETUP_WIZARD_ICON)) {
                val appInfo = context.applicationInfo
                val isApplicationInSystemImage = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                // Default value
                return !isApplicationInSystemImage
            }
            return prefs.getBoolean(PREF_SHOW_SETUP_WIZARD_ICON, false)
        }

        @JvmStatic
        fun readOneHandedModeEnabled(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_ONE_HANDED_MODE, false)
        }

        @SuppressLint("RtlHardcoded")
        @JvmStatic
        fun readOneHandedModeGravity(prefs: SharedPreferences): Int {
            return prefs.getInt(PREF_ONE_HANDED_GRAVITY, Gravity.LEFT)
        }

        @JvmStatic
        fun readHasHardwareKeyboard(conf: Configuration): Boolean {
            // The standard way of finding out whether we have a hardware keyboard. This code is taken
            // from InputMethodService#onEvaluateInputShown, which canonically determines this.
            // In a nutshell, we have a keyboard if the configuration says the type of hardware keyboard
            // is NOKEYS and if it's not hidden (e.g. folded inside the device).
            return conf.keyboard != Configuration.KEYBOARD_NOKEYS && conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
        }

        @JvmStatic
        fun isInternal(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_KEY_IS_INTERNAL, false)
        }

        @JvmStatic
        fun writeEmojiRecentKeys(prefs: SharedPreferences, str: String?) {
            prefs.edit().putString(PREF_EMOJI_RECENT_KEYS, str).apply()
        }

        @JvmStatic
        fun readEmojiRecentKeys(prefs: SharedPreferences): String {
            return prefs.getString(PREF_EMOJI_RECENT_KEYS, "")!!
        }

        @JvmStatic
        fun writeLastTypedEmojiCategoryPageId(
            prefs: SharedPreferences, categoryId: Int, categoryPageId: Int
        ) {
            val key = PREF_EMOJI_CATEGORY_LAST_TYPED_ID + categoryId
            prefs.edit().putInt(key, categoryPageId).apply()
        }

        @JvmStatic
        fun readLastTypedEmojiCategoryPageId(
            prefs: SharedPreferences, categoryId: Int
        ): Int {
            val key = PREF_EMOJI_CATEGORY_LAST_TYPED_ID + categoryId
            return prefs.getInt(key, 0)
        }

        @JvmStatic
        fun writeLastShownEmojiCategoryId(
            prefs: SharedPreferences, categoryId: Int
        ) {
            prefs.edit().putInt(PREF_LAST_SHOWN_EMOJI_CATEGORY_ID, categoryId).apply()
        }

        @JvmStatic
        fun readLastShownEmojiCategoryId(
            prefs: SharedPreferences, defValue: Int
        ): Int {
            return prefs.getInt(PREF_LAST_SHOWN_EMOJI_CATEGORY_ID, defValue)
        }

        @JvmStatic
        fun writeLastShownEmojiCategoryPageId(
            prefs: SharedPreferences, categoryId: Int
        ) {
            prefs.edit().putInt(PREF_LAST_SHOWN_EMOJI_CATEGORY_PAGE_ID, categoryId).apply()
        }

        @JvmStatic
        fun readLastShownEmojiCategoryPageId(
            prefs: SharedPreferences, defValue: Int
        ): Int {
            return prefs.getInt(PREF_LAST_SHOWN_EMOJI_CATEGORY_PAGE_ID, defValue)
        }
    }
}
