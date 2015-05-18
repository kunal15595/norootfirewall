package com.norootfw.gui;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.norootfw.R;
import com.norootfw.db.PolicyDataProvider;
import com.norootfw.db.PolicyDataProvider.ConnectionType;
import com.norootfw.utils.PrefUtils;

public class FilteringListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(android.R.id.content, new FilteringListFragment()).commit();
        }
    }

    public static class FilteringListFragment extends ListFragment implements
            LoaderManager.LoaderCallbacks<Cursor> {

        private static final int LOADER_ID = 1;
        private static final String[] COLUMNS = new String[] {
                PolicyDataProvider.Columns.IP_ADDRESS,
                PolicyDataProvider.Columns.PORT,
                PolicyDataProvider.Columns.CONNECTION_TYPE,
                PolicyDataProvider.Columns._ID
        };
        private ListAdapter mAdapter;
        private String mFilteringMode;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mFilteringMode = PrefUtils.getFilteringMode(getActivity());
            setEmptyText(getString(R.string.empty_list));
            if (mFilteringMode.equals(getString(R.string.filtering_mode_black_list_value))) {
                getActivity().setTitle(R.string.black_list_title);
            } else if (mFilteringMode.equals(getString(R.string.filtering_mode_white_list_value))) {
                getActivity().setTitle(R.string.white_list_title);
            } else {
                throw new IllegalArgumentException("Invalid filtering mode == " + mFilteringMode);
            }
            mAdapter = new ListAdapter(getActivity(),
                    R.layout.filter_list_item,
                    null,
                    COLUMNS,
                    new int[] { R.id.ip_address, R.id.port },
                    0);
            View header = LayoutInflater.from(getActivity()).inflate(R.layout.filter_list_header, getListView(), false);
            // TODO: Should always be visible, but it's part of scrolled content
            getListView().addHeaderView(header);
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            setListAdapter(mAdapter);
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
            case LOADER_ID:
                return new CursorLoader(getActivity(),
                        PolicyDataProvider.Uris.IP_PORT_TABLE, COLUMNS,
                        PolicyDataProvider.Columns.FILTERING_MODE + "=?",
                        new String[] { mFilteringMode },
                        null);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    }

    private static class ListAdapter extends SimpleCursorAdapter {

        OnLongClickListener mOnLongClickListener = null;
        private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

            // Called when the action mode is created; startActionMode() was called
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate a menu resource providing context menu items
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.filtering_list_menu, menu);
                return true;
            }

            // Called each time the action mode is shown. Always called after onCreateActionMode,
            // but
            // may be called multiple times if the mode is invalidated.
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false; // Return false if nothing is done
            }

            // Called when the user selects a contextual menu item
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            // Called when the user exits the action mode
            @Override
            public void onDestroyActionMode(ActionMode mode) {}
        };
        Activity mActivity;

        public ListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mActivity = (Activity) context;
            mOnLongClickListener = new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    mActivity.startActionMode(mActionModeCallback);
                    v.setSelected(true);
                    return true;
                }
            };
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            view.setOnLongClickListener(mOnLongClickListener);
            TextView ipAddress = (TextView) view.findViewById(R.id.ip_address);
            ipAddress.setText(cursor.getString(cursor.getColumnIndex(PolicyDataProvider.Columns.IP_ADDRESS)));

            TextView port = (TextView) view.findViewById(R.id.port);
            port.setText(cursor.getString(cursor.getColumnIndex(PolicyDataProvider.Columns.PORT)));

            ConnectionType connectionType = ConnectionType.valueOf(cursor.getString(cursor.getColumnIndex(PolicyDataProvider.Columns.CONNECTION_TYPE)));
            ImageView icon = (ImageView) view.findViewById(R.id.connection_type);
            switch (connectionType) {
            case INCOMING:
                icon.setImageResource(R.drawable.ico_download);
                break;
            case OUTGOING:
                icon.setImageResource(R.drawable.ico_upload);
                break;
            default:
                throw new IllegalArgumentException("Illegal connection type: " + connectionType);
            }
        }
    }
}
