/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import android.view.inputmethod.EditorInfo
import org.dslul.openboard.inputmethod.compat.AppWorkaroundsUtils
import org.dslul.openboard.inputmethod.latin.InputAttributes
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.RichInputMethodManager
import org.dslul.openboard.inputmethod.latin.utils.AsyncResultHolder
import org.dslul.openboard.inputmethod.latin.utils.ResourceUtils
import org.dslul.openboard.inputmethod.latin.utils.ScriptUtils
import org.dslul.openboard.inputmethod.latin.utils.TargetPackageInfoGetterTask
import java.util.Arrays
import java.util.Locale

/**
 * When you call the constructor of this class, you may want to change the current system locale by
 * using {@link org.dslul.openboard.inputmethod.latin.utils.RunInLocale}.
 */
// Non-final for testing via mock library.
class SettingsValues(
    context: Context,
    prefs: SharedPreferences,
    res: Resources,
    @JvmField val mInputAttributes: InputAttributes
) {

    // From resources:
    @JvmField val mSpacingAndPunctuations: SpacingAndPunctuations
    @JvmField val mDelayInMillisecondsToUpdateOldSuggestions: Int
    @JvmField val mDoubleSpacePeriodTimeout: Long
    // From configuration:
    @JvmField val mLocale: Locale
    @JvmField val mHasHardwareKeyboard: Boolean
    @JvmField val mDisplayOrientation: Int
    // From preferences, in the same order as xml/prefs.xml:
    @JvmField val mAutoCap: Boolean
    @JvmField val mVibrateOn: Boolean
    @JvmField val mSoundOn: Boolean
    @JvmField val mKeyPreviewPopupOn: Boolean
    @JvmField val mShowsVoiceInputKey: Boolean
    @JvmField val mIncludesOtherImesInLanguageSwitchList: Boolean
    @JvmField val mShowsNumberRow: Boolean
    @JvmField val mShowsHints: Boolean
    @JvmField val mSpaceForLangChange: Boolean
    @JvmField val mShowsLanguageSwitchKey: Boolean
    @JvmField val mShowsEmojiKey: Boolean
    @JvmField val mShowsClipboardKey: Boolean
    @JvmField val mUsePersonalizedDicts: Boolean
    @JvmField val mUseDoubleSpacePeriod: Boolean
    @JvmField val mBlockPotentiallyOffensive: Boolean
    @JvmField val mSpaceTrackpadEnabled: Boolean
    @JvmField val mDeleteSwipeEnabled: Boolean
    @JvmField val mAutospaceAfterPunctuationEnabled: Boolean
    @JvmField val mClipboardHistoryEnabled: Boolean
    @JvmField val mClipboardHistoryRetentionTime: Long
    @JvmField val mOneHandedModeEnabled: Boolean
    @JvmField val mOneHandedModeGravity: Int
    // Use bigrams to predict the next word when there is no input for it yet
    @JvmField val mBigramPredictionEnabled: Boolean
    @JvmField val mGestureInputEnabled: Boolean
    @JvmField val mGestureTrailEnabled: Boolean
    @JvmField val mGestureFloatingPreviewTextEnabled: Boolean
    @JvmField val mSlidingKeyInputPreviewEnabled: Boolean
    @JvmField val mKeyLongpressTimeout: Int
    @JvmField val mEnableEmojiAltPhysicalKey: Boolean
    @JvmField val mShowAppIcon: Boolean
    @JvmField val mIsShowAppIconSettingInPreferences: Boolean
    @JvmField val mCloudSyncEnabled: Boolean
    @JvmField val mEnableMetricsLogging: Boolean
    @JvmField val mShouldShowLxxSuggestionUi: Boolean
    // Use split layout for keyboard.
    @JvmField val mIsSplitKeyboardEnabled: Boolean
    @JvmField val mScreenMetrics: Int

    // Deduced settings
    @JvmField val mKeypressVibrationDuration: Int
    @JvmField val mKeypressSoundVolume: Float
    @JvmField val mAutoCorrectEnabled: Boolean
    @JvmField val mAutoCorrectionThreshold: Float
    @JvmField val mPlausibilityThreshold: Float
    @JvmField val mAutoCorrectionEnabledPerUserSettings: Boolean
    @JvmField val mSuggestionsEnabledPerUserSettings: Boolean
    @JvmField val mIncognitoModeEnabled: Boolean
    @JvmField val mAppWorkarounds: AsyncResultHolder<AppWorkaroundsUtils>

    // Debug settings
    @JvmField val mIsInternal: Boolean
    @JvmField val mHasCustomKeyPreviewAnimationParams: Boolean
    @JvmField val mHasKeyboardResize: Boolean
    @JvmField val mKeyboardHeightScale: Float
    @JvmField val mKeyPreviewShowUpDuration: Int
    @JvmField val mKeyPreviewDismissDuration: Int
    @JvmField val mKeyPreviewShowUpStartXScale: Float
    @JvmField val mKeyPreviewShowUpStartYScale: Float
    @JvmField val mKeyPreviewDismissEndXScale: Float
    @JvmField val mKeyPreviewDismissEndYScale: Float

    @JvmField val mAccount: String?

    init {
        mLocale = res.configuration.locale
        // Get the resources
        mDelayInMillisecondsToUpdateOldSuggestions =
            res.getInteger(R.integer.config_delay_in_milliseconds_to_update_old_suggestions)
        mSpacingAndPunctuations = SpacingAndPunctuations(res)

        // Get the settings preferences
        mAutoCap = prefs.getBoolean(Settings.PREF_AUTO_CAP, true) && ScriptUtils.scriptSupportsUppercase(mLocale.language)
        mVibrateOn = Settings.readVibrationEnabled(prefs, res)
        mSoundOn = Settings.readKeypressSoundEnabled(prefs, res)
        mKeyPreviewPopupOn = Settings.readKeyPreviewPopupEnabled(prefs, res)
        mSlidingKeyInputPreviewEnabled = prefs.getBoolean(
            DebugSettings.PREF_SLIDING_KEY_INPUT_PREVIEW, true
        )
        mShowsVoiceInputKey = needsToShowVoiceInputKey(prefs, res) && mInputAttributes.mShouldShowVoiceInputKey
        mIncludesOtherImesInLanguageSwitchList =
            !Settings.ENABLE_SHOW_LANGUAGE_SWITCH_KEY_SETTINGS || prefs.getBoolean(
                Settings.PREF_INCLUDE_OTHER_IMES_IN_LANGUAGE_SWITCH_LIST,
                false
            ) /* forcibly */
        mShowsNumberRow = prefs.getBoolean(Settings.PREF_SHOW_NUMBER_ROW, false)
        mShowsHints = prefs.getBoolean(Settings.PREF_SHOW_HINTS, true)
        mSpaceForLangChange = prefs.getBoolean(Settings.PREF_SPACE_TO_CHANGE_LANG, true)
        mShowsLanguageSwitchKey = prefs.getBoolean(Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY, false)
        mShowsEmojiKey = prefs.getBoolean(Settings.PREF_SHOW_EMOJI_KEY, false)
        mShowsClipboardKey = prefs.getBoolean(Settings.PREF_SHOW_CLIPBOARD_KEY, false)
        mUsePersonalizedDicts = prefs.getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true)
        mUseDoubleSpacePeriod = prefs.getBoolean(Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, true) &&
                mInputAttributes.mIsGeneralTextInput
        mBlockPotentiallyOffensive = Settings.readBlockPotentiallyOffensive(prefs, res)
        mAutoCorrectEnabled = Settings.readAutoCorrectEnabled(prefs, res)
        mAutoCorrectionThreshold = if (mAutoCorrectEnabled) {
            readAutoCorrectionThreshold(res, prefs)
        } else {
            AUTO_CORRECTION_DISABLED_THRESHOLD
        }
        mBigramPredictionEnabled = readBigramPredictionEnabled(prefs, res)
        mDoubleSpacePeriodTimeout = res.getInteger(R.integer.config_double_space_period_timeout).toLong()
        mHasHardwareKeyboard = Settings.readHasHardwareKeyboard(res.configuration)
        mEnableMetricsLogging = prefs.getBoolean(Settings.PREF_ENABLE_METRICS_LOGGING, true)
        mIsSplitKeyboardEnabled = prefs.getBoolean(Settings.PREF_ENABLE_SPLIT_KEYBOARD, false)
        mScreenMetrics = Settings.readScreenMetrics(res)

        mShouldShowLxxSuggestionUi = Settings.SHOULD_SHOW_LXX_SUGGESTION_UI &&
                prefs.getBoolean(DebugSettings.PREF_SHOULD_SHOW_LXX_SUGGESTION_UI, true)
        // Compute other readable settings
        mKeyLongpressTimeout = Settings.readKeyLongpressTimeout(prefs, res)
        mKeypressVibrationDuration = Settings.readKeypressVibrationDuration(prefs, res)
        mKeypressSoundVolume = Settings.readKeypressSoundVolume(prefs, res)
        mEnableEmojiAltPhysicalKey = prefs.getBoolean(
            Settings.PREF_ENABLE_EMOJI_ALT_PHYSICAL_KEY, true
        )
        mShowAppIcon = Settings.readShowSetupWizardIcon(prefs, context)
        mIsShowAppIconSettingInPreferences = prefs.contains(Settings.PREF_SHOW_SETUP_WIZARD_ICON)
        mPlausibilityThreshold = Settings.readPlausibilityThreshold(res)
        mGestureInputEnabled = Settings.readGestureInputEnabled(prefs, res)
        mGestureTrailEnabled = prefs.getBoolean(Settings.PREF_GESTURE_PREVIEW_TRAIL, true)
        mCloudSyncEnabled = prefs.getBoolean(LocalSettingsConstants.PREF_ENABLE_CLOUD_SYNC, false)
        mAccount = prefs.getString(
            LocalSettingsConstants.PREF_ACCOUNT_NAME,
            null /* default */
        )
        mGestureFloatingPreviewTextEnabled = !mInputAttributes.mDisableGestureFloatingPreviewText &&
                prefs.getBoolean(Settings.PREF_GESTURE_FLOATING_PREVIEW_TEXT, true)
        mAutoCorrectionEnabledPerUserSettings = mAutoCorrectEnabled
        //&& !mInputAttributes.mInputTypeNoAutoCorrect;
        mSuggestionsEnabledPerUserSettings = !mInputAttributes.mIsPasswordField &&
                readSuggestionsEnabled(prefs)
        mIncognitoModeEnabled = Settings.readAlwaysIncognitoMode(prefs) || mInputAttributes.mNoLearning ||
                mInputAttributes.mIsPasswordField
        mIsInternal = Settings.isInternal(prefs)
        mHasCustomKeyPreviewAnimationParams = prefs.getBoolean(
            DebugSettings.PREF_HAS_CUSTOM_KEY_PREVIEW_ANIMATION_PARAMS, false
        )
        mHasKeyboardResize = prefs.getBoolean(DebugSettings.PREF_RESIZE_KEYBOARD, false)
        mKeyboardHeightScale = Settings.readKeyboardHeight(prefs, DEFAULT_SIZE_SCALE)
        mKeyPreviewShowUpDuration = Settings.readKeyPreviewAnimationDuration(
            prefs, DebugSettings.PREF_KEY_PREVIEW_SHOW_UP_DURATION,
            res.getInteger(R.integer.config_key_preview_show_up_duration)
        )
        mKeyPreviewDismissDuration = Settings.readKeyPreviewAnimationDuration(
            prefs, DebugSettings.PREF_KEY_PREVIEW_DISMISS_DURATION,
            res.getInteger(R.integer.config_key_preview_dismiss_duration)
        )
        val defaultKeyPreviewShowUpStartScale = ResourceUtils.getFloatFromFraction(
            res, R.fraction.config_key_preview_show_up_start_scale
        )
        val defaultKeyPreviewDismissEndScale = ResourceUtils.getFloatFromFraction(
            res, R.fraction.config_key_preview_dismiss_end_scale
        )
        mKeyPreviewShowUpStartXScale = Settings.readKeyPreviewAnimationScale(
            prefs, DebugSettings.PREF_KEY_PREVIEW_SHOW_UP_START_X_SCALE,
            defaultKeyPreviewShowUpStartScale
        )
        mKeyPreviewShowUpStartYScale = Settings.readKeyPreviewAnimationScale(
            prefs, DebugSettings.PREF_KEY_PREVIEW_SHOW_UP_START_Y_SCALE,
            defaultKeyPreviewShowUpStartScale
        )
        mKeyPreviewDismissEndXScale = Settings.readKeyPreviewAnimationScale(
            prefs, DebugSettings.PREF_KEY_PREVIEW_DISMISS_END_X_SCALE,
            defaultKeyPreviewDismissEndScale
        )
        mKeyPreviewDismissEndYScale = Settings.readKeyPreviewAnimationScale(
            prefs, DebugSettings.PREF_KEY_PREVIEW_DISMISS_END_Y_SCALE,
            defaultKeyPreviewDismissEndScale
        )
        mDisplayOrientation = res.configuration.orientation
        mAppWorkarounds = AsyncResultHolder("AppWorkarounds")
        val packageInfo = TargetPackageInfoGetterTask.getCachedPackageInfo(
            mInputAttributes.mTargetApplicationPackageName
        )
        if (null != packageInfo) {
            mAppWorkarounds.set(AppWorkaroundsUtils(packageInfo))
        } else {
            TargetPackageInfoGetterTask(context, mAppWorkarounds)
                .execute(mInputAttributes.mTargetApplicationPackageName)
        }
        mSpaceTrackpadEnabled = Settings.readSpaceTrackpadEnabled(prefs)
        mDeleteSwipeEnabled = Settings.readDeleteSwipeEnabled(prefs)
        mAutospaceAfterPunctuationEnabled = Settings.readAutospaceAfterPunctuationEnabled(prefs)
        mClipboardHistoryEnabled = Settings.readClipboardHistoryEnabled(prefs)
        mClipboardHistoryRetentionTime = Settings.readClipboardHistoryRetentionTime(prefs, res).toLong()
        mOneHandedModeEnabled = Settings.readOneHandedModeEnabled(prefs)
        mOneHandedModeGravity = Settings.readOneHandedModeGravity(prefs)
    }

    fun isMetricsLoggingEnabled(): Boolean {
        return mEnableMetricsLogging
    }

    fun isApplicationSpecifiedCompletionsOn(): Boolean {
        return mInputAttributes.mApplicationSpecifiedCompletionOn
    }

    fun needsToLookupSuggestions(): Boolean {
        return mInputAttributes.mShouldShowSuggestions &&
                (mAutoCorrectionEnabledPerUserSettings || isSuggestionsEnabledPerUserSettings)
    }

    val isSuggestionsEnabledPerUserSettings: Boolean
        get() = mSuggestionsEnabledPerUserSettings

    fun isPersonalizationEnabled(): Boolean {
        return mUsePersonalizedDicts
    }

    fun isWordSeparator(code: Int): Boolean {
        return mSpacingAndPunctuations.isWordSeparator(code)
    }

    fun isWordConnector(code: Int): Boolean {
        return mSpacingAndPunctuations.isWordConnector(code)
    }

    fun isWordCodePoint(code: Int): Boolean {
        return Character.isLetter(code) || isWordConnector(code) ||
                Character.COMBINING_SPACING_MARK.toInt() == Character.getType(code)
    }

    fun isUsuallyPrecededBySpace(code: Int): Boolean {
        return mSpacingAndPunctuations.isUsuallyPrecededBySpace(code)
    }

    fun isUsuallyFollowedBySpace(code: Int): Boolean {
        return mSpacingAndPunctuations.isUsuallyFollowedBySpace(code)
    }

    fun shouldInsertSpacesAutomatically(): Boolean {
        return mInputAttributes.mShouldInsertSpacesAutomatically
    }

    fun isLanguageSwitchKeyEnabled(): Boolean {
        if (!mShowsLanguageSwitchKey) {
            return false
        }
        val imm = RichInputMethodManager.getInstance()
        if (mIncludesOtherImesInLanguageSwitchList) {
            return imm.hasMultipleEnabledIMEsOrSubtypes(false /* include aux subtypes */)
        }
        return imm.hasMultipleEnabledSubtypesInThisIme(false /* include aux subtypes */)
    }

    fun isSameInputType(editorInfo: EditorInfo): Boolean {
        return mInputAttributes.isSameInputType(editorInfo)
    }

    fun hasSameOrientation(configuration: Configuration): Boolean {
        return mDisplayOrientation == configuration.orientation
    }

    fun dump(): String {
        val sb = StringBuilder("Current settings :")
        sb.append("\n   mSpacingAndPunctuations = ")
        sb.append("" + mSpacingAndPunctuations.dump())
        sb.append("\n   mDelayInMillisecondsToUpdateOldSuggestions = ")
        sb.append("" + mDelayInMillisecondsToUpdateOldSuggestions)
        sb.append("\n   mAutoCap = ")
        sb.append("" + mAutoCap)
        sb.append("\n   mVibrateOn = ")
        sb.append("" + mVibrateOn)
        sb.append("\n   mSoundOn = ")
        sb.append("" + mSoundOn)
        sb.append("\n   mKeyPreviewPopupOn = ")
        sb.append("" + mKeyPreviewPopupOn)
        sb.append("\n   mShowsVoiceInputKey = ")
        sb.append("" + mShowsVoiceInputKey)
        sb.append("\n   mIncludesOtherImesInLanguageSwitchList = ")
        sb.append("" + mIncludesOtherImesInLanguageSwitchList)
        sb.append("\n   mShowsLanguageSwitchKey = ")
        sb.append("" + mShowsLanguageSwitchKey)
        sb.append("\n   mUsePersonalizedDicts = ")
        sb.append("" + mUsePersonalizedDicts)
        sb.append("\n   mUseDoubleSpacePeriod = ")
        sb.append("" + mUseDoubleSpacePeriod)
        sb.append("\n   mBlockPotentiallyOffensive = ")
        sb.append("" + mBlockPotentiallyOffensive)
        sb.append("\n   mBigramPredictionEnabled = ")
        sb.append("" + mBigramPredictionEnabled)
        sb.append("\n   mGestureInputEnabled = ")
        sb.append("" + mGestureInputEnabled)
        sb.append("\n   mGestureTrailEnabled = ")
        sb.append("" + mGestureTrailEnabled)
        sb.append("\n   mGestureFloatingPreviewTextEnabled = ")
        sb.append("" + mGestureFloatingPreviewTextEnabled)
        sb.append("\n   mSlidingKeyInputPreviewEnabled = ")
        sb.append("" + mSlidingKeyInputPreviewEnabled)
        sb.append("\n   mKeyLongpressTimeout = ")
        sb.append("" + mKeyLongpressTimeout)
        sb.append("\n   mLocale = ")
        sb.append("" + mLocale)
        sb.append("\n   mInputAttributes = ")
        sb.append("" + mInputAttributes)
        sb.append("\n   mKeypressVibrationDuration = ")
        sb.append("" + mKeypressVibrationDuration)
        sb.append("\n   mKeypressSoundVolume = ")
        sb.append("" + mKeypressSoundVolume)
        sb.append("\n   mAutoCorrectEnabled = ")
        sb.append("" + mAutoCorrectEnabled)
        sb.append("\n   mAutoCorrectionThreshold = ")
        sb.append("" + mAutoCorrectionThreshold)
        sb.append("\n   mAutoCorrectionEnabledPerUserSettings = ")
        sb.append("" + mAutoCorrectionEnabledPerUserSettings)
        sb.append("\n   mSuggestionsEnabledPerUserSettings = ")
        sb.append("" + mSuggestionsEnabledPerUserSettings)
        sb.append("\n   mDisplayOrientation = ")
        sb.append("" + mDisplayOrientation)
        sb.append("\n   mAppWorkarounds = ")
        val awu = mAppWorkarounds.get(null, 0)
        sb.append("" + (if (null == awu) "null" else awu.toString()))
        sb.append("\n   mIsInternal = ")
        sb.append("" + mIsInternal)
        sb.append("\n   mKeyPreviewShowUpDuration = ")
        sb.append("" + mKeyPreviewShowUpDuration)
        sb.append("\n   mKeyPreviewDismissDuration = ")
        sb.append("" + mKeyPreviewDismissDuration)
        sb.append("\n   mKeyPreviewShowUpStartScaleX = ")
        sb.append("" + mKeyPreviewShowUpStartXScale)
        sb.append("\n   mKeyPreviewShowUpStartScaleY = ")
        sb.append("" + mKeyPreviewShowUpStartYScale)
        sb.append("\n   mKeyPreviewDismissEndScaleX = ")
        sb.append("" + mKeyPreviewDismissEndXScale)
        sb.append("\n   mKeyPreviewDismissEndScaleY = ")
        sb.append("" + mKeyPreviewDismissEndYScale)
        return sb.toString()
    }

    companion object {
        private val TAG = SettingsValues::class.java.simpleName
        // "floatMaxValue" and "floatNegativeInfinity" are special marker strings for
        // Float.NEGATIVE_INFINITE and Float.MAX_VALUE. Currently used for auto-correction settings.
        private const val FLOAT_MAX_VALUE_MARKER_STRING = "floatMaxValue"
        private const val FLOAT_NEGATIVE_INFINITY_MARKER_STRING = "floatNegativeInfinity"
        private const val TIMEOUT_TO_GET_TARGET_PACKAGE = 5 // seconds
        const val DEFAULT_SIZE_SCALE = 1.0f // 100%
        const val AUTO_CORRECTION_DISABLED_THRESHOLD = Float.MAX_VALUE

        private const val SUGGESTIONS_VISIBILITY_HIDE_VALUE_OBSOLETE = "2"

        private fun readSuggestionsEnabled(prefs: SharedPreferences): Boolean {
            if (prefs.contains(Settings.PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE)) {
                val alwaysHide = SUGGESTIONS_VISIBILITY_HIDE_VALUE_OBSOLETE == prefs.getString(
                    Settings.PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE, null
                )
                prefs.edit()
                    .remove(Settings.PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE)
                    .putBoolean(Settings.PREF_SHOW_SUGGESTIONS, !alwaysHide)
                    .apply()
            }
            return prefs.getBoolean(Settings.PREF_SHOW_SUGGESTIONS, true)
        }

        private fun readBigramPredictionEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return prefs.getBoolean(
                Settings.PREF_BIGRAM_PREDICTIONS, res.getBoolean(
                    R.bool.config_default_next_word_prediction
                )
            )
        }

        private fun readAutoCorrectionThreshold(
            res: Resources,
            prefs: SharedPreferences
        ): Float {
            val currentAutoCorrectionSetting = Settings.readAutoCorrectConfidence(prefs, res)
            val autoCorrectionThresholdValues = res.getStringArray(
                R.array.auto_correction_threshold_values
            )
            // When autoCorrectionThreshold is greater than 1.0, it's like auto correction is off.
            var autoCorrectionThreshold: Float
            try {
                val arrayIndex = currentAutoCorrectionSetting.toInt()
                if (arrayIndex >= 0 && arrayIndex < autoCorrectionThresholdValues.size) {
                    val `val` = autoCorrectionThresholdValues[arrayIndex]
                    if (FLOAT_MAX_VALUE_MARKER_STRING == `val`) {
                        autoCorrectionThreshold = Float.MAX_VALUE
                    } else if (FLOAT_NEGATIVE_INFINITY_MARKER_STRING == `val`) {
                        autoCorrectionThreshold = Float.NEGATIVE_INFINITY
                    } else {
                        autoCorrectionThreshold = `val`.toFloat()
                    }
                } else {
                    autoCorrectionThreshold = Float.MAX_VALUE
                }
            } catch (e: NumberFormatException) {
                // Whenever the threshold settings are correct, never come here.
                Log.w(
                    TAG, "Cannot load auto correction threshold setting."
                            + " currentAutoCorrectionSetting: " + currentAutoCorrectionSetting
                            + ", autoCorrectionThresholdValues: "
                            + Arrays.toString(autoCorrectionThresholdValues), e
                )
                return Float.MAX_VALUE
            }
            return autoCorrectionThreshold
        }

        private fun needsToShowVoiceInputKey(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            // Migrate preference from {@link Settings#PREF_VOICE_MODE_OBSOLETE} to
            // {@link Settings#PREF_VOICE_INPUT_KEY}.
            if (prefs.contains(Settings.PREF_VOICE_MODE_OBSOLETE)) {
                val voiceModeMain = res.getString(R.string.voice_mode_main)
                val voiceMode = prefs.getString(
                    Settings.PREF_VOICE_MODE_OBSOLETE, voiceModeMain
                )
                val shouldShowVoiceInputKey = voiceModeMain == voiceMode
                prefs.edit()
                    .putBoolean(Settings.PREF_VOICE_INPUT_KEY, shouldShowVoiceInputKey)
                    // Remove the obsolete preference if exists.
                    .remove(Settings.PREF_VOICE_MODE_OBSOLETE)
                    .apply()
            }
            return prefs.getBoolean(Settings.PREF_VOICE_INPUT_KEY, true)
        }
    }
}
