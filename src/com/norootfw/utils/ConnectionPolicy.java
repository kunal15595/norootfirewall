package com.norootfw.utils;

import com.norootfw.R;

public enum ConnectionPolicy {
    ALLOWED(R.string.allowed),
    FORBIDDEN(R.string.forbidden);

    private final int mTitle;

    private ConnectionPolicy(int title) {
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