package com.zdragon.videoio;

/**
 * @author created by luokaixuan
 * @date 2019/5/22
 * 这个类是用来干嘛的
 */

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MyApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        Log.d("MyApplication", "is started ... ");
    }

    public static Context getContext() {
        return context;
    }
}
