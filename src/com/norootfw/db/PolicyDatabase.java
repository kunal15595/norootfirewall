package com.norootfw.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract.Columns;
import android.util.Log;

import com.norootfw.R;
import com.norootfw.utils.NoRootFwApplication;

public class PolicyDatabase extends SQLiteOpenHelper {

    private static final String LOG_TAG = PolicyDatabase.class.getSimpleName();
    private static PolicyDatabase sInstance;

    private static final String DATABASE_NAME = "policy_db";
    private static final String WHILE_LIST_TABLE = "while_list";
    private static final String BLACK_LIST_TABLE = "black_list";

    // Names of columns
    private static final String IP_ADDRESS = "ip_address";
    private static final String PORT = "port";
    private static final String CONNECTION_TYPE = "conn_type";
    private static final int DATABASE_VERSION = 1;

    public static synchronized PolicyDatabase getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new PolicyDatabase(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static method "getInstance()" instead.
     */
    private PolicyDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createPolicyTable(db, BLACK_LIST_TABLE);
        createPolicyTable(db, WHILE_LIST_TABLE);
    }

    private void createPolicyTable(SQLiteDatabase db, String name) {
        db.execSQL("CREATE TABLE " + name
                + " (" + Columns._ID + " INTEGER PRIMARY KEY, "
                + IP_ADDRESS + " TEXT, "
                + PORT + " INTEGER, "
                + CONNECTION_TYPE + " INTEGER NOT NULL, "
                + "UNIQUE(" + IP_ADDRESS + ", " + PORT + ", " + CONNECTION_TYPE + "));");
    }

    public void insert(String ipAddress, int port, ConnectionType connectionType) {
        ContentValues values = new ContentValues();
        values.put(IP_ADDRESS, ipAddress);
        values.put(PORT, port);
        values.put(CONNECTION_TYPE, connectionType.ordinal());

        String table = getTable();
        long res = sInstance.getWritableDatabase().insert(table, null, values);
        if (res == -1) {
            Log.e(LOG_TAG, "Failed to insert a value to " + table);
        }
    }

    public void delete(int id) {
        String table = getTable();
        int res = sInstance.getWritableDatabase().delete(table, Columns._ID + "=?",
                new String[] { Integer.toString(id) });
        if (res == 0) {
            Log.w(LOG_TAG, "Nothing was deleted. id == " + id);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    private String getTable() {
        Context context = NoRootFwApplication.getAppContext();
        String table = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.filtering_mode_key), null);
        return table;
    }

    enum ConnectionType {
        INCOMING,
        OUTGOING
    }
}
