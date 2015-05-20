package com.norootfw.utils;

import android.app.Application;
import android.content.Context;

public class NoRootFwApplication extends Application {

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return sContext;
    }

}
