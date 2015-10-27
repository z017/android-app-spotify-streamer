package com.jeremiaslongo.apps.spotifystreamer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Gravity;
import android.widget.Toast;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

public class AppUtils {
    // TAG
    public static final String TAG = makeLogTag(AppUtils.class);

    // Time
    public static final int SECOND = 1000;
    public static final int MINUTE = 60 * SECOND;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;

    //Based on a stackoverflow snippet
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void alert(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
    }

    public static String timeText(int milliseconds){
        int totalSeconds = (int) milliseconds / SECOND;

        int hours = (int) totalSeconds / HOUR;
        int remainder = (int) totalSeconds - hours * HOUR;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs= remainder;

        String text = String.format("%02d", mins)+":"+String.format("%02d", secs);
        if (hours > 0){
            text = hours + ":" + text;
        }
        return text;
    }
}
