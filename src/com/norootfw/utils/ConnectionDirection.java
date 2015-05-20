package com.norootfw.utils;

import com.norootfw.R;

public enum ConnectionDirection {
    INCOMING(R.string.incoming),
    OUTGOING(R.string.outgoing);

    private final int mTitle;

    private ConnectionDirection(int title) {
        mTitle = title;
    }

    public int getTitle() {
        return mTitle;
    }

    @Override
    public String toString() {
        return NoRootFwApplication.getAppContext().getResources().getString(mTitle);
    }
}