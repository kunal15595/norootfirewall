package com.norootfw.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import com.norootfw.R;

public class PrefUtils {

    private PrefUtils() {
        throw new AssertionError();
    }

    public static String getFilteringMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.filtering_mode_key), context.getString(R.string.filtering_mode_black_list_value));
    }

}
