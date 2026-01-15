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

import android.content.res.Resources
import org.dslul.openboard.inputmethod.annotations.UsedForTesting
import org.dslul.openboard.inputmethod.keyboard.internal.MoreKeySpec
import org.dslul.openboard.inputmethod.latin.PunctuationSuggestions
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.common.Constants
import org.dslul.openboard.inputmethod.latin.common.StringUtils
import java.util.Arrays
import java.util.Locale

class SpacingAndPunctuations {
    @JvmField val mSortedSymbolsPrecededBySpace: IntArray
    @JvmField val mSortedSymbolsFollowedBySpace: IntArray
    @JvmField val mSortedSymbolsClusteringTogether: IntArray
    @JvmField val mSortedWordConnectors: IntArray
    @JvmField val mSortedWordSeparators: IntArray
    @JvmField val mSuggestPuncList: PunctuationSuggestions
    @JvmField val mSentenceSeparator: Int
    @JvmField val mAbbreviationMarker: Int
    @JvmField val mSortedSentenceTerminators: IntArray
    @JvmField val mSentenceSeparatorAndSpace: String
    @JvmField val mCurrentLanguageHasSpaces: Boolean
    @JvmField val mUsesAmericanTypography: Boolean
    @JvmField val mUsesGermanRules: Boolean

    constructor(res: Resources) {
        // To be able to binary search the code point. See {@link #isUsuallyPrecededBySpace(int)}.
        mSortedSymbolsPrecededBySpace = StringUtils.toSortedCodePointArray(
            res.getString(R.string.symbols_preceded_by_space)
        )
        // To be able to binary search the code point. See {@link #isUsuallyFollowedBySpace(int)}.
        mSortedSymbolsFollowedBySpace = StringUtils.toSortedCodePointArray(
            res.getString(R.string.symbols_followed_by_space)
        )
        mSortedSymbolsClusteringTogether = StringUtils.toSortedCodePointArray(
            res.getString(R.string.symbols_clustering_together)
        )
        // To be able to binary search the code point. See {@link #isWordConnector(int)}.
        mSortedWordConnectors = StringUtils.toSortedCodePointArray(
            res.getString(R.string.symbols_word_connectors)
        )
        mSortedWordSeparators = StringUtils.toSortedCodePointArray(
            res.getString(R.string.symbols_word_separators)
        )
        mSortedSentenceTerminators = StringUtils.toSortedCodePointArray(
            res.getString(R.string.symbols_sentence_terminators)
        )
        mSentenceSeparator = res.getInteger(R.integer.sentence_separator)
        mAbbreviationMarker = res.getInteger(R.integer.abbreviation_marker)
        mSentenceSeparatorAndSpace = String(
            intArrayOf(mSentenceSeparator, Constants.CODE_SPACE), 0, 2
        )
        mCurrentLanguageHasSpaces = res.getBoolean(R.bool.current_language_has_spaces)
        val locale = res.configuration.locale
        // Heuristic: we use American Typography rules because it's the most common rules for all
        // English variants. German rules (not "German typography") also have small gotchas.
        mUsesAmericanTypography = Locale.ENGLISH.language == locale.language
        mUsesGermanRules = Locale.GERMAN.language == locale.language
        val suggestPuncsSpec = MoreKeySpec.splitKeySpecs(
            res.getString(R.string.suggested_punctuations)
        )
        mSuggestPuncList = PunctuationSuggestions.newPunctuationSuggestions(suggestPuncsSpec)
    }

    @UsedForTesting
    constructor(
        model: SpacingAndPunctuations,
        overrideSortedWordSeparators: IntArray
    ) {
        mSortedSymbolsPrecededBySpace = model.mSortedSymbolsPrecededBySpace
        mSortedSymbolsFollowedBySpace = model.mSortedSymbolsFollowedBySpace
        mSortedSymbolsClusteringTogether = model.mSortedSymbolsClusteringTogether
        mSortedWordConnectors = model.mSortedWordConnectors
        mSortedWordSeparators = overrideSortedWordSeparators
        mSortedSentenceTerminators = model.mSortedSentenceTerminators
        mSuggestPuncList = model.mSuggestPuncList
        mSentenceSeparator = model.mSentenceSeparator
        mAbbreviationMarker = model.mAbbreviationMarker
        mSentenceSeparatorAndSpace = model.mSentenceSeparatorAndSpace
        mCurrentLanguageHasSpaces = model.mCurrentLanguageHasSpaces
        mUsesAmericanTypography = model.mUsesAmericanTypography
        mUsesGermanRules = model.mUsesGermanRules
    }

    fun isWordSeparator(code: Int): Boolean {
        return Arrays.binarySearch(mSortedWordSeparators, code) >= 0
    }

    fun isWordConnector(code: Int): Boolean {
        return Arrays.binarySearch(mSortedWordConnectors, code) >= 0
    }

    fun isWordCodePoint(code: Int): Boolean {
        return Character.isLetter(code) || isWordConnector(code)
    }

    fun isUsuallyPrecededBySpace(code: Int): Boolean {
        return Arrays.binarySearch(mSortedSymbolsPrecededBySpace, code) >= 0
    }

    fun isUsuallyFollowedBySpace(code: Int): Boolean {
        return Arrays.binarySearch(mSortedSymbolsFollowedBySpace, code) >= 0
    }

    fun isClusteringSymbol(code: Int): Boolean {
        return Arrays.binarySearch(mSortedSymbolsClusteringTogether, code) >= 0
    }

    fun isSentenceTerminator(code: Int): Boolean {
        return Arrays.binarySearch(mSortedSentenceTerminators, code) >= 0
    }

    fun isAbbreviationMarker(code: Int): Boolean {
        return code == mAbbreviationMarker
    }

    fun isSentenceSeparator(code: Int): Boolean {
        return code == mSentenceSeparator
    }

    fun dump(): String {
        val sb = StringBuilder()
        sb.append("mSortedSymbolsPrecededBySpace = ")
        sb.append("" + Arrays.toString(mSortedSymbolsPrecededBySpace))
        sb.append("\n   mSortedSymbolsFollowedBySpace = ")
        sb.append("" + Arrays.toString(mSortedSymbolsFollowedBySpace))
        sb.append("\n   mSortedWordConnectors = ")
        sb.append("" + Arrays.toString(mSortedWordConnectors))
        sb.append("\n   mSortedWordSeparators = ")
        sb.append("" + Arrays.toString(mSortedWordSeparators))
        sb.append("\n   mSuggestPuncList = ")
        sb.append("" + mSuggestPuncList)
        sb.append("\n   mSentenceSeparator = ")
        sb.append("" + mSentenceSeparator)
        sb.append("\n   mSentenceSeparatorAndSpace = ")
        sb.append("" + mSentenceSeparatorAndSpace)
        sb.append("\n   mCurrentLanguageHasSpaces = ")
        sb.append("" + mCurrentLanguageHasSpaces)
        sb.append("\n   mUsesAmericanTypography = ")
        sb.append("" + mUsesAmericanTypography)
        sb.append("\n   mUsesGermanRules = ")
        sb.append("" + mUsesGermanRules)
        return sb.toString()
    }
}
