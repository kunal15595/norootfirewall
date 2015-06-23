package com.norootfw.utils;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;

import com.norootfw.db.PolicyDataProvider.Columns;
import com.norootfw.db.PolicyDataProvider.Uris;

import java.util.Random;

public class NoRootFwApplication extends Application {

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        if (true) {
            return;
        }
        Random random = new Random();
        ContentValues contentValues = new ContentValues();
        for(int i = 0; i < 70; i++) {
            contentValues.put(Columns.CONNECTION_DIRECTION, random.nextBoolean() ? ConnectionDirection.INCOMING.name() : ConnectionDirection.OUTGOING.name());
            contentValues.put(Columns.CONNECTION_POLICY, random.nextBoolean() ? ConnectionPolicy.ALLOWED.name() : ConnectionPolicy.FORBIDDEN.name());
            contentValues.put(Columns.IP_ADDRESS, (random.nextInt(254) + 1) + ".12.34." + random.nextInt(45));
            contentValues.put(Columns.PORT, random.nextInt(40000));
            getContentResolver().insert(Uris.IP_PORT_TABLE, contentValues);
            contentValues.clear();
        }
    }

    public static Context getAppContext() {
        return sContext;
    }

}
