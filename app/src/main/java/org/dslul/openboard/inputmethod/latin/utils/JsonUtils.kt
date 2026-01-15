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

import android.text.TextUtils
import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import org.dslul.openboard.inputmethod.latin.ClipboardHistoryEntry
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.io.StringWriter
import java.util.ArrayList
import java.util.Collections

object JsonUtils {
    private val TAG = JsonUtils::class.java.simpleName

    private val INTEGER_CLASS_NAME = Int::class.javaObjectType.simpleName // "Integer"
    private val STRING_CLASS_NAME = String::class.java.simpleName
    private const val CLIPBOARD_HISTORY_ENTRY_ID_KEY = "id"
    private const val CLIPBOARD_HISTORY_ENTRY_CONTENT_KEY = "content"

    private const val EMPTY_STRING = ""

    @JvmStatic
    fun jsonStrToList(s: String): List<Any> {
        val list = ArrayList<Any>()
        val reader = JsonReader(StringReader(s))
        try {
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (name == INTEGER_CLASS_NAME) {
                        list.add(reader.nextInt())
                    } else if (name == STRING_CLASS_NAME) {
                        list.add(reader.nextString())
                    } else {
                        Log.w(TAG, "Invalid name: $name")
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
            reader.endArray()
            return list
        } catch (e: IOException) {
            // Ignore
        } finally {
            close(reader)
        }
        return Collections.emptyList()
    }

    @JvmStatic
    fun listToJsonStr(list: List<Any>?): String {
        if (list == null || list.isEmpty()) {
            return EMPTY_STRING
        }
        val sw = StringWriter()
        val writer = JsonWriter(sw)
        try {
            writer.beginArray()
            for (o in list) {
                writer.beginObject()
                if (o is Int) {
                    writer.name(INTEGER_CLASS_NAME).value(o)
                } else if (o is String) {
                    writer.name(STRING_CLASS_NAME).value(o)
                }
                writer.endObject()
            }
            writer.endArray()
            return sw.toString()
        } catch (e: IOException) {
            // Ignore
        } finally {
            close(writer)
        }
        return EMPTY_STRING
    }

    @JvmStatic
    fun jsonBytesToHistoryEntryList(bytes: ByteArray): List<ClipboardHistoryEntry> {
        val list = ArrayList<ClipboardHistoryEntry>()
        val reader = JsonReader(InputStreamReader(ByteArrayInputStream(bytes)))
        try {
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                var id: Long = 0
                var content = EMPTY_STRING
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (name == CLIPBOARD_HISTORY_ENTRY_ID_KEY) {
                        id = reader.nextLong()
                    } else if (name == CLIPBOARD_HISTORY_ENTRY_CONTENT_KEY) {
                        content = reader.nextString()
                    } else {
                        Log.w(TAG, "Invalid name: $name")
                        reader.skipValue()
                    }
                }
                if (id > 0 && !TextUtils.isEmpty(content)) {
                    list.add(ClipboardHistoryEntry(id, content, true))
                }
                reader.endObject()
            }
            reader.endArray()
            return list
        } catch (e: IOException) {
            // Ignore
        } finally {
            close(reader)
        }
        return Collections.emptyList()
    }

    @JvmStatic
    fun historyEntryListToJsonStr(entries: Collection<ClipboardHistoryEntry>?): String {
        if (entries == null || entries.isEmpty()) {
            return EMPTY_STRING
        }
        val sw = StringWriter()
        val writer = JsonWriter(sw)
        try {
            writer.beginArray()
            for (e in entries) {
                writer.beginObject()
                writer.name(CLIPBOARD_HISTORY_ENTRY_ID_KEY).value(e.timeStamp)
                writer.name(CLIPBOARD_HISTORY_ENTRY_CONTENT_KEY).value(e.content.toString())
                writer.endObject()
            }
            writer.endArray()
            return sw.toString()
        } catch (e: IOException) {
            // Ignore
        } finally {
            close(writer)
        }
        return EMPTY_STRING
    }

    private fun close(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: IOException) {
            // Ignore
        }
    }
}
