package com.jeremiaslongo.apps.spotifystreamer.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGE;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

public class ImageLoader {
    public static final String TAG = makeLogTag(ImageLoader.class);
    /**
     * REVIEW
     *
     * Picasso can crash your app if you the url you pass it in empty/malformed.
     * Consider doing the check before calling into it.
     *
     * https://github.com/square/picasso/issues/609
     */
    public static void drawWithDummy(Context context, String uriStr, ImageView view) {
        String thumb = null;
        try {
            thumb = Uri.parse(uriStr).toString();
        } catch (Exception e) {
            // Invalid URI
            // LOGD(TAG, e.toString());
        }
        Picasso.with(context)
                .load(thumb)
                .placeholder(R.drawable.dummy)
                .into(view);
    }
}
