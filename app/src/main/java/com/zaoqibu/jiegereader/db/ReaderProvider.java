package com.zaoqibu.jiegereader.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

/**
 * Created by vwarship on 2015/3/6.
 */
public class ReaderProvider {
    private static final String TAG = "ReaderProvider";

    private static final String DATABASE_NAME = "reader.db";
    private static final int DATABASE_VERSION = 1;

    private DatabaseHelper mOpenHelper;

    public ReaderProvider(Context context) {
        mOpenHelper = new DatabaseHelper(context);
    }

    /**
     *
     * This class helps open, create, and upgrade the database file. Set to package visibility
     * for testing purposes.
     */
    static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Reader.Newses.TABLE_NAME + " ("
                    + Reader.Newses._ID + " INTEGER PRIMARY KEY autoincrement,"
                    + Reader.Newses.COLUMN_NAME_TITLE + " TEXT,"
                    + Reader.Newses.COLUMN_NAME_LINK + " TEXT,"
                    + Reader.Newses.COLUMN_NAME_SOURCE + " TEXT,"
                    + Reader.Newses.COLUMN_NAME_DESCRIPTION + " TEXT,"
                    + Reader.Newses.COLUMN_NAME_PUB_DATE + " INTEGER,"
                    + Reader.Newses.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + Reader.Newses.COLUMN_NAME_STATE + " INTEGER,"
                    + Reader.Newses.COLUMN_NAME_RSS_ID + " INTEGER"
                    + ");");
            db.execSQL(String.format("CREATE INDEX i_link on %s (%s);", Reader.Newses.TABLE_NAME, Reader.Newses.COLUMN_NAME_LINK));
            db.execSQL(String.format("CREATE INDEX i_state_pubdate on %s (%s, %s);", Reader.Newses.TABLE_NAME, Reader.Newses.COLUMN_NAME_STATE, Reader.Newses.COLUMN_NAME_PUB_DATE));
            db.execSQL(String.format("CREATE INDEX i_state_rssid_pubdate on %s (%s, %s, %s);", Reader.Newses.TABLE_NAME, Reader.Newses.COLUMN_NAME_STATE, Reader.Newses.COLUMN_NAME_RSS_ID, Reader.Newses.COLUMN_NAME_PUB_DATE));

            db.execSQL("CREATE TABLE " + Reader.Rsses.TABLE_NAME + " ("
                    + Reader.Rsses._ID + " INTEGER PRIMARY KEY autoincrement,"
                    + Reader.Rsses.COLUMN_NAME_TITLE + " TEXT,"
                    + Reader.Rsses.COLUMN_NAME_LINK + " TEXT,"
                    + Reader.Rsses.COLUMN_NAME_IS_FEED + " INTEGER default 0,"
                    + Reader.Rsses.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + Reader.Rsses.COLUMN_NAME_UPDATE_DATE + " INTEGER"
                    + ");");
            db.execSQL(String.format("CREATE INDEX i_isfeed on %s (%s);", Reader.Rsses.TABLE_NAME, Reader.Rsses.COLUMN_NAME_IS_FEED));

            initRss(db);
        }

        private void initRss(SQLiteDatabase db) {
            class Rss {
                String title;
                String link;
                int isFeed;
                public Rss(String title, String link, int isFeed) {
                    this.title = title;
                    this.link = link;
                    this.isFeed = isFeed;
                }
            }

            Rss[] rsses = {
                    new Rss("互联网创业-36氪", "http://www.36kr.com/feed", 1),
                    new Rss("互联网-腾讯", "http://tech.qq.com/web/rss_web.xml", 1),
                    new Rss("新闻国内-腾讯", "http://news.qq.com/newsgn/rss_newsgn.xml", 1),
                    new Rss("国内新闻-人民网", "http://www.people.com.cn/rss/politics.xml", 1),
                    new Rss("国际新闻-人民网", "http://www.people.com.cn/rss/world.xml", 1),
                    new Rss("人人都是产品经理", "http://www.woshipm.com/feed", 1),
                    new Rss("知乎每日精选", "http://www.zhihu.com/rss", 1),
                    new Rss("InfoQ", "http://www.infoq.com/cn/feed", 0),
                    new Rss("开源中国", "http://www.oschina.net/news/rss", 0),
                    new Rss("虎嗅网", "http://www.huxiu.com/rss/0.xml", 0),
                    new Rss("雷锋网", "http://www.leiphone.com/feed", 0),
                    new Rss("译言-最新译文", "http://feed.yeeyan.org/latest", 0),
                    new Rss("煎蛋", "http://jandan.net/feed", 0),
                    new Rss("互联网新闻-新浪", "http://rss.sina.com.cn/tech/internet/home28.xml", 0)
            };

            for (Rss rss : rsses) {
                insertRss(db, rss.title, rss.link, rss.isFeed);
            }
        }

        private void insertRss(SQLiteDatabase db, String title, String link, int isFeed) {
            db.execSQL("INSERT INTO " + Reader.Rsses.TABLE_NAME
                    + String.format("(%s, %s, %s)", Reader.Rsses.COLUMN_NAME_TITLE, Reader.Rsses.COLUMN_NAME_LINK, Reader.Rsses.COLUMN_NAME_IS_FEED)
                    + " VALUES(" + String.format("'%s', '%s', %d", title, link, isFeed) + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + Reader.Newses.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Reader.Rsses.TABLE_NAME);

            // Recreates the database with a new version
            onCreate(db);
        }
    }

    public Cursor queryNews(String[] projection, String selection, String[] selectionArgs, String sortOrder, String limit) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(Reader.Newses.TABLE_NAME);

//        qb.setProjectionMap(sNotesProjectionMap);

        // Opens the database object in "read" mode, since no writes need to be done.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

       /*
        * Performs the query. If no problems occur trying to read the database, then a Cursor
        * object is returned; otherwise, the cursor variable contains null. If no records were
        * selected, then the Cursor object is empty, and Cursor.getCount() returns 0.
        */
        Cursor c = qb.query(
                db,            // The database to query
                projection,    // The columns to return from the query
                selection,     // The columns for the where clause
                selectionArgs, // The values for the where clause
                null,          // don't group the rows
                null,          // don't filter by row groups
                sortOrder,      // The sort order
                limit
        );

        return c;
    }

    public boolean insertNews(ContentValues values) {
        if (values == null)
            return false;

        // Gets the current system time in milliseconds
        Long now = Long.valueOf(System.currentTimeMillis());

        if (values.containsKey(Reader.Newses.COLUMN_NAME_PUB_DATE) == false) {
            values.put(Reader.Newses.COLUMN_NAME_PUB_DATE, now);
        }

        if (values.containsKey(Reader.Newses.COLUMN_NAME_CREATE_DATE) == false) {
            values.put(Reader.Newses.COLUMN_NAME_CREATE_DATE, now);
        }

        if (values.containsKey(Reader.Newses.COLUMN_NAME_STATE) == false) {
            values.put(Reader.Newses.COLUMN_NAME_STATE, Reader.Newses.StateValue.Unread.getValue());
        }

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(
                Reader.Newses.TABLE_NAME,        // The table to insert into.
                null,  // A hack, SQLite sets this column value to null
                // if values is empty.
                values                           // A map of column names, and the values to insert
                // into the columns.
        );

        // If the insert succeeded, the row ID exists.
        if (rowId > 0)
            return true;

        return false;
    }

    public int deleteNews(String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int count = db.delete(Reader.Newses.TABLE_NAME,
                where,
                whereArgs);

        return count;
    }

    public int updateNews(ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int count = db.update(
                Reader.Newses.TABLE_NAME,
                values,
                where,
                whereArgs);

        return count;
    }

//    @Override
//    public int delete(Uri uri, String where, String[] whereArgs) {
//
//        // Opens the database object in "write" mode.
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//        String finalWhere;
//
//        int count;
//
//        // Does the delete based on the incoming URI pattern.
//        switch (sUriMatcher.match(uri)) {
//
//            // If the incoming pattern matches the general pattern for notes, does a delete
//            // based on the incoming "where" columns and arguments.
//            case NOTES:
//                count = db.delete(
//                        NotePad.Notes.TABLE_NAME,  // The database table name
//                        where,                     // The incoming where clause column names
//                        whereArgs                  // The incoming where clause values
//                );
//                break;
//
//            // If the incoming URI matches a single note ID, does the delete based on the
//            // incoming data, but modifies the where clause to restrict it to the
//            // particular note ID.
//            case NOTE_ID:
//                /*
//                 * Starts a final WHERE clause by restricting it to the
//                 * desired note ID.
//                 */
//                finalWhere =
//                        NotePad.Notes._ID +                              // The ID column name
//                                " = " +                                          // test for equality
//                                uri.getPathSegments().                           // the incoming note ID
//                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
//                ;
//
//                // If there were additional selection criteria, append them to the final
//                // WHERE clause
//                if (where != null) {
//                    finalWhere = finalWhere + " AND " + where;
//                }
//
//                // Performs the delete.
//                count = db.delete(
//                        NotePad.Notes.TABLE_NAME,  // The database table name.
//                        finalWhere,                // The final WHERE clause
//                        whereArgs                  // The incoming where clause values.
//                );
//                break;
//
//            // If the incoming pattern is invalid, throws an exception.
//            default:
//                throw new IllegalArgumentException("Unknown URI " + uri);
//        }
//
//        /*Gets a handle to the content resolver object for the current context, and notifies it
//         * that the incoming URI changed. The object passes this along to the resolver framework,
//         * and observers that have registered themselves for the provider are notified.
//         */
//        getContext().getContentResolver().notifyChange(uri, null);
//
//        // Returns the number of rows deleted.
//        return count;
//    }

//    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
//        // Opens the database object in "write" mode.
//        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//        int count;
//        String finalWhere;
//
//        // Does the update based on the incoming URI pattern
//        switch (sUriMatcher.match(uri)) {
//
//            // If the incoming URI matches the general notes pattern, does the update based on
//            // the incoming data.
//            case NOTES:
//
//                // Does the update and returns the number of rows updated.
//                count = db.update(
//                        NotePad.Notes.TABLE_NAME, // The database table name.
//                        values,                   // A map of column names and new values to use.
//                        where,                    // The where clause column names.
//                        whereArgs                 // The where clause column values to select on.
//                );
//                break;
//
//            // If the incoming URI matches a single note ID, does the update based on the incoming
//            // data, but modifies the where clause to restrict it to the particular note ID.
//            case NOTE_ID:
//                // From the incoming URI, get the note ID
//                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);
//
//                /*
//                 * Starts creating the final WHERE clause by restricting it to the incoming
//                 * note ID.
//                 */
//                finalWhere =
//                        NotePad.Notes._ID +                              // The ID column name
//                                " = " +                                          // test for equality
//                                uri.getPathSegments().                           // the incoming note ID
//                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
//                ;
//
//                // If there were additional selection criteria, append them to the final WHERE
//                // clause
//                if (where !=null) {
//                    finalWhere = finalWhere + " AND " + where;
//                }
//
//
//                // Does the update and returns the number of rows updated.
//                count = db.update(
//                        NotePad.Notes.TABLE_NAME, // The database table name.
//                        values,                   // A map of column names and new values to use.
//                        finalWhere,               // The final WHERE clause to use
//                        // placeholders for whereArgs
//                        whereArgs                 // The where clause column values to select on, or
//                        // null if the values are in the where argument.
//                );
//                break;
//            // If the incoming pattern is invalid, throws an exception.
//            default:
//                throw new IllegalArgumentException("Unknown URI " + uri);
//        }
//
//        /*Gets a handle to the content resolver object for the current context, and notifies it
//         * that the incoming URI changed. The object passes this along to the resolver framework,
//         * and observers that have registered themselves for the provider are notified.
//         */
//        getContext().getContentResolver().notifyChange(uri, null);
//
//        // Returns the number of rows updated.
//        return count;
//    }

    /**
     * Table Rsses
     */

    public Cursor queryRsses(String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(Reader.Rsses.TABLE_NAME);

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = qb.query(
                db,            // The database to query
                projection,    // The columns to return from the query
                selection,     // The columns for the where clause
                selectionArgs, // The values for the where clause
                null,          // don't group the rows
                null,          // don't filter by row groups
                sortOrder      // The sort order
        );

        return c;
    }

    public boolean insertRsses(ContentValues values) {
        if (values == null)
            return false;

        Long now = Long.valueOf(System.currentTimeMillis());

        if (values.containsKey(Reader.Rsses.COLUMN_NAME_CREATE_DATE) == false) {
            values.put(Reader.Rsses.COLUMN_NAME_CREATE_DATE, now);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(
                Reader.Rsses.TABLE_NAME,
                null,
                values
        );

        if (rowId > 0)
            return true;

        return false;
    }

    public int updateRsses(ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int count = db.update(
                Reader.Rsses.TABLE_NAME,
                values,
                where,
                whereArgs);

        return count;
    }
}
