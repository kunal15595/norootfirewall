package com.norootfw.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.norootfw.utils.PrefUtils;

public class PolicyDataProvider extends ContentProvider {

    private static final String AUTHORITY = "com.norootfw.db.PolicyDataProvider";
    private static final String LOG_TAG = PolicyDataProvider.class.getSimpleName();
    private static final UriMatcher sUriMatcher;

    private PolicyDatabase mPolicyDatabase;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, Tables.IP_PORT_TABLE, UriCodes.IP_PORT_TABLE);
    }

    @Override
    public boolean onCreate() {
        mPolicyDatabase = new PolicyDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (sUriMatcher.match(uri)) {
        case UriCodes.IP_PORT_TABLE:
            Cursor cursor = mPolicyDatabase.getReadableDatabase().query(Tables.IP_PORT_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        default:
            throw new RuntimeException("Unsupported URI: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = -1;
        switch (sUriMatcher.match(uri)) {
        case UriCodes.IP_PORT_TABLE:
            id = mPolicyDatabase.getWritableDatabase().insertWithOnConflict(Tables.IP_PORT_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            break;
        default:
            throw new RuntimeException("Unsupported URI: " + uri);
        }
        if (id == -1) {
            Log.w(LOG_TAG, "Failed to insert a new item");
            return null;
        } else {
            Uri itemUri = Uri.withAppendedPath(Uris.IP_PORT_TABLE, Long.toString(id));
            getContext().getContentResolver().notifyChange(itemUri, null);
            return itemUri;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
        case UriCodes.IP_PORT_TABLE:
            int deleted = mPolicyDatabase.getWritableDatabase().delete(Tables.IP_PORT_TABLE, selection, selectionArgs);
            if (deleted == 0) {
                Log.w(LOG_TAG, "No items deleted");
            } else {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return deleted;
        default:
            throw new RuntimeException("Unsupported URI: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /* Inner classes */
    private static class PolicyDatabase extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "policy_db";
        private static final int DATABASE_VERSION = 1;

        /**
         * Constructor should be private to prevent direct instantiation.
         * make call to static method "getInstance()" instead.
         */
        private PolicyDatabase(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Tables.IP_PORT_TABLE
                    + " (" + Columns._ID + " INTEGER PRIMARY KEY, "
                    + Columns.CONNECTION_POLICY + " TEXT NOT NULL, "
                    + Columns.IP_ADDRESS + " TEXT, "
                    + Columns.PORT + " INTEGER, "
                    + Columns.CONNECTION_DIRECTION + " TEXT NOT NULL, "

                    + "UNIQUE(" + Columns.IP_ADDRESS + ", " + Columns.PORT + ", " + Columns.CONNECTION_DIRECTION + "));");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    }

    private static class Tables {

        private Tables() {
            throw new AssertionError();
        }

        static final String IP_PORT_TABLE = "ip_port_table";
    }

    // Names of columns
    public static class Columns implements BaseColumns {

        private Columns() {
            throw new AssertionError();
        }

        public static final String IP_ADDRESS = "ip_address";
        /** Allowed or prohibited
         */
        public static final String CONNECTION_POLICY = "connection_policy";
        public static final String PORT = "port";
        public static final String CONNECTION_DIRECTION = "connection_direction";
    }

    private static class UriCodes {

        private UriCodes() {
            throw new AssertionError();
        }

        static final int IP_PORT_TABLE = 0x01;
    }

    public static class Uris {

        private Uris() {
            throw new AssertionError();
        }

        public static final Uri IP_PORT_TABLE = Uri.parse("content://" + AUTHORITY + "/" + Tables.IP_PORT_TABLE);
    }
}
