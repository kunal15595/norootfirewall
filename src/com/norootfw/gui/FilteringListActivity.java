package com.norootfw.gui;

import android.app.Activity;
import android.app.Fragment;
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
import android.view.ViewGroup;
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

    public static class FilteringListFragment extends Fragment implements
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
        private ListView mFilteringListView;
        private TextView mFilteringListEmpty;
        private TextView mFilteringListHeader;
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_filtering_list, container, false);
            mFilteringListView = (ListView) view.findViewById(R.id.filtering_list);
            mFilteringListEmpty = (TextView) view.findViewById(R.id.empty_filtering_list);
            mFilteringListHeader = (TextView) view.findViewById(R.id.filtering_list_header);
            return view;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mFilteringMode = PrefUtils.getFilteringMode(getActivity());
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
            mFilteringListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            mFilteringListView.setAdapter(mAdapter);
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
            if (data.getCount() == 0) {
                mFilteringListEmpty.setVisibility(View.VISIBLE);
                mFilteringListView.setVisibility(View.GONE);
                mFilteringListHeader.setVisibility(View.GONE);
            } else {
                mFilteringListEmpty.setVisibility(View.GONE);
                mFilteringListView.setVisibility(View.VISIBLE);
                mFilteringListHeader.setVisibility(View.VISIBLE);
            }
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
