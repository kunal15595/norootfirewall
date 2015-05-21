package com.norootfw.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.LongSparseArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.norootfw.R;
import com.norootfw.db.PolicyDataProvider;
import com.norootfw.db.PolicyDataProvider.Columns;
import com.norootfw.db.PolicyDataProvider.Uris;
import com.norootfw.utils.ConnectionDirection;
import com.norootfw.utils.PrefUtils;
import com.norootfw.utils.Utils;

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
                PolicyDataProvider.Columns.CONNECTION_DIRECTION,
                PolicyDataProvider.Columns._ID
        };
        private static final int MAX_PORT = 65535;
        private ListAdapter mAdapter;
        private String mFilteringMode;
        private ListView mFilteringListView;
        private TextView mFilteringListEmpty;
        private TextView mFilteringListHeader;
        private AlertDialog mAddDialog;
        private EditText mAddPortEditText;
        private EditText mAddIpAddressEditText;

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
            setHasOptionsMenu(true);
            mFilteringMode = PrefUtils.getFilteringMode(getActivity());
            if (mFilteringMode.equals(getString(R.string.filtering_mode_black_list_value))) {
                getActivity().setTitle(R.string.black_list_title);
            } else if (mFilteringMode.equals(getString(R.string.filtering_mode_white_list_value))) {
                getActivity().setTitle(R.string.white_list_title);
            } else {
                throw new IllegalArgumentException("Invalid filtering mode == " + mFilteringMode);
            }

            View addDialogView = LayoutInflater.from(getActivity()).inflate(R.layout.add_new_dialog, null);
            mAddIpAddressEditText = (EditText) addDialogView.findViewById(R.id.add_ip_address);
            mAddIpAddressEditText.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void afterTextChanged(Editable s) {
                    mAddDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isIpAddressValid(s.toString()) && isPortValid(mAddPortEditText.getText().toString()));
                }
            });
            mAddIpAddressEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        EditText et = (EditText) v;
                        if (!isIpAddressValid(et.getText().toString())) {
                            et.setError(getString(R.string.invalid_ip_address));
                        }
                    }
                }
            });
            mAddPortEditText = (EditText) addDialogView.findViewById(R.id.add_port);
            mAddPortEditText.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void afterTextChanged(Editable s) {
                    mAddDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isIpAddressValid(s.toString()) && isPortValid(mAddIpAddressEditText.getText().toString()));
                }
            });
            mAddPortEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        EditText et = (EditText) v;
                        if (!isPortValid(et.getText().toString())) {
                            et.setError(getString(R.string.invalid_port_number));
                        }
                    }
                }
            });
            final Spinner connectionDirectionSpinner = (Spinner) addDialogView.findViewById(R.id.add_connection_direction);
            connectionDirectionSpinner.setAdapter(new ConnectionDirectionAdapter(getActivity(), android.R.layout.simple_list_item_1, ConnectionDirection.values()));
            mAddDialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add_new_policy)
                    .setView(addDialogView)
                    .setPositiveButton(R.string.add, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            insertPolicy(mAddIpAddressEditText.getText().toString(),
                                    Integer.parseInt(mAddPortEditText.getText().toString()),
                                    (ConnectionDirection) connectionDirectionSpinner.getSelectedItem());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();

            mAdapter = new ListAdapter(getActivity(),
                    R.layout.filter_list_item,
                    null,
                    COLUMNS,
                    new int[] { R.id.ip_address, R.id.port },
                    0);
            mFilteringListView.setAdapter(mAdapter);
            mFilteringListView.setOnItemLongClickListener(new OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mAdapter.mSelectedIds.get(id) == null || !mAdapter.mSelectedIds.get(id)) {
                        mAdapter.mSelectedIds.put(id, true);
                        view.setBackgroundColor(getActivity().getResources().getColor(R.color.highlighted_item));
                    } else {
                        /*
                         * I don't put a false because there is no reason to store unselected items.
                         * If the user selects and unselects items much, the hash table will grow
                         * rapidly
                         * 
                         * Maksim Dmitriev
                         * May 21, 2015
                         */
                        mAdapter.mSelectedIds.remove(id);
                        view.setBackgroundColor(getActivity().getResources().getColor(android.R.color.white));
                    }
                    return true;
                }
            });
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.filtering_list_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
            case R.id.item_add:
                mAddDialog.show();
                mAddDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                return true;

            default:
                return super.onOptionsItemSelected(item);
            }
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

        private boolean isIpAddressValid(String ipAddress) {
            return ipAddress.matches(Utils.IP_ADDRESS_PATTERN);
        }

        private boolean isPortValid(String port) {
            boolean valid = false;
            try {
                valid = Integer.parseInt(port) <= MAX_PORT;
            } catch (NumberFormatException e) {}
            return valid;
        }

        private void insertPolicy(String ipAddress, int port, ConnectionDirection direction) {
            ContentValues values = new ContentValues();
            values.put(Columns.IP_ADDRESS, ipAddress);
            values.put(Columns.PORT, port);
            values.put(Columns.CONNECTION_DIRECTION, direction.name());
            Uri itemUri = getActivity().getContentResolver().insert(Uris.IP_PORT_TABLE, values);
            if (itemUri == null) {
                throw new RuntimeException("Failed to insert a new policy");
            }
        }
    }

    private static class ConnectionDirectionAdapter extends ArrayAdapter<ConnectionDirection> {

        LayoutInflater mInflater;
        final int mRes;

        public ConnectionDirectionAdapter(Context context, int resource, ConnectionDirection[] objects) {
            super(context, resource, objects);
            mInflater = LayoutInflater.from(context);
            mRes = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(mRes, parent, false);
            }
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(ConnectionDirection.values()[position].getTitle());
            return convertView;
        }
    }

    private static class ListAdapter extends SimpleCursorAdapter {

        final Context mContext;
        LongSparseArray<Boolean> mSelectedIds = new LongSparseArray<Boolean>();

        public ListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mContext = context;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
            if (mSelectedIds.get(id) == null || !mSelectedIds.get(id)) {
                view.setBackgroundColor(mContext.getResources().getColor(android.R.color.white));
            } else {
                view.setBackgroundColor(mContext.getResources().getColor(R.color.highlighted_item));
            }
            TextView ipAddress = (TextView) view.findViewById(R.id.ip_address);
            ipAddress.setText(cursor.getString(cursor.getColumnIndex(PolicyDataProvider.Columns.IP_ADDRESS)));

            TextView port = (TextView) view.findViewById(R.id.port);
            port.setText(cursor.getString(cursor.getColumnIndex(PolicyDataProvider.Columns.PORT)));

            ConnectionDirection connectionType = ConnectionDirection.valueOf(cursor.getString(cursor.getColumnIndex(PolicyDataProvider.Columns.CONNECTION_DIRECTION)));
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
