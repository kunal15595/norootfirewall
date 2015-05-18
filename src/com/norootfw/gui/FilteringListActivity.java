package com.norootfw.gui;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

import com.norootfw.db.PolicyDataProvider;

public class FilteringListActivity extends ListActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 1;
    private static final String[] COLUMNS = new String[] {
            PolicyDataProvider.Columns.CONNECTION_TYPE,
            PolicyDataProvider.Columns.IP_ADDRESS,
            PolicyDataProvider.Columns.PORT,
            PolicyDataProvider.Columns._ID
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
        case LOADER_ID:
            // return new CursorLoader(this, PolicyDataProvider.Uris.IP_PORT_TABLE, COLUMNS, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // TODO Auto-generated method stub

    }
}
