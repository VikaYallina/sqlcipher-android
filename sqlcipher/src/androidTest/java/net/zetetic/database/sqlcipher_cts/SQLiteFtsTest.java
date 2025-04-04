/*
 * Copyright (C) 2009 The Android Open Source Project
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

package net.zetetic.database.sqlcipher_cts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.test.AndroidTestCase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Tests to verify FTS3/4 SQLite support.
 */
@RunWith(AndroidJUnit4.class)
public class SQLiteFtsTest {

    private static final String TEST_TABLE = "cts_fts";

    private static final String[] TEST_CONTENT = {
            "Any sufficiently advanced TECHnology is indistinguishable from magic.",
            "Those who would give up Essential Liberty to purchase a little Temporary Safety, deserve neither Liberty nor Safety.",
            "It is poor civic hygiene to install technologies that could someday facilitate a police state.",
    };

    private SQLiteDatabase mDatabase;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        System.loadLibrary("sqlcipher");
        File f = mContext.getDatabasePath("CTS_FTS");
        f.mkdirs();
        if (f.exists()) { f.delete(); }
        mDatabase = SQLiteDatabase.openOrCreateDatabase(f,null);
    }

    @After
    public void tearDown() throws Exception {
        final String path = mDatabase.getPath();
        mDatabase.close();
        SQLiteDatabase.deleteDatabase(new File(path));
    }

    @Test
    public void testFts3Porter() throws Exception {
        prepareFtsTable(TEST_TABLE, "fts3", "tokenize=porter");

        // Porter should include stemmed words
        final Cursor cursor = queryFtsTable(TEST_TABLE, "technology");
        try {
            assertEquals(2, cursor.getCount());
            cursor.moveToPosition(0);
            assertTrue(cursor.getString(0).contains(">TECHnology<"));
            cursor.moveToPosition(1);
            assertTrue(cursor.getString(0).contains(">technologies<"));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testFts3Simple() throws Exception {
        prepareFtsTable(TEST_TABLE, "fts3", "tokenize=simple");

        // Simple shouldn't include stemmed words
        final Cursor cursor = queryFtsTable(TEST_TABLE, "technology");
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToPosition(0);
            assertTrue(cursor.getString(0).contains(">TECHnology<"));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testFts4Simple() throws Exception {
        prepareFtsTable(TEST_TABLE, "fts4", "tokenize=simple");

        // Simple shouldn't include stemmed words
        final Cursor cursor = queryFtsTable(TEST_TABLE, "technology");
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToPosition(0);
            assertTrue(cursor.getString(0).contains(">TECHnology<"));
        } finally {
            cursor.close();
        }
    }

    private void prepareFtsTable(String table, String ftsType, String options)
            throws Exception {
        mDatabase.execSQL(
                "CREATE VIRTUAL TABLE " + table + " USING " + ftsType
                + "(content TEXT, " + options + ");");

        final Resources res = mContext.getResources();
        final ContentValues values = new ContentValues();
        for (String content : TEST_CONTENT) {
            values.clear();
            values.put("content", content);
            mDatabase.insert(table, null, values);
        }
    }

    private Cursor queryFtsTable(String table, String match) {
        return mDatabase.query(table, new String[] { "snippet(" + table + ")" },
                "content MATCH ?", new String[] { match },
                null, null, "rowid ASC");
    }
}
