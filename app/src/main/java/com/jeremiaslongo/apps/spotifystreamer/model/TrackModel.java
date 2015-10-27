package com.jeremiaslongo.apps.spotifystreamer.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

public class TrackModel implements Parcelable{
    // TAG
    public static final String TAG = makeLogTag(TrackModel.class);

    // Keys
    private static final String KEY_NAME = "name";
    private static final String KEY_ALBUM_NAME = "album_name";
    private static final String KEY_ALBUM_ART_LARGE = "album_art_large";
    private static final String KEY_ALBUM_ART_ICON = "album_art_icon";
    private static final String KEY_PREVIEW_URL = "preview_url";

    // Data
    private String name;
    private String album_name;
    private String album_art_large;
    private String album_art_icon;
    private String preview_url;

    // Constructor
    public TrackModel (String n, String an, String aal, String aai, String pu) {
        this.name = n;
        this.album_name = an;
        this.album_art_large = aal;
        this.album_art_icon = aai;
        this.preview_url = pu;
    }

    public String getName() { return this.name; }
    public String getAlbumName() { return this.album_name; }
    public String getAlbumArtLarge() { return this.album_art_large; }
    public String getAlbumArtIcon() { return this.album_art_icon; }
    public String getPreviewUrl() { return this.preview_url; }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags){
        // Create a bundle for the key/value pairs
        Bundle bundle = new Bundle();

        // insert the key value pairs to the bundle
        bundle.putString(KEY_NAME, name);
        bundle.putString(KEY_ALBUM_NAME, album_name);
        bundle.putString(KEY_ALBUM_ART_LARGE, album_art_large);
        bundle.putString(KEY_ALBUM_ART_ICON, album_art_icon);
        bundle.putString(KEY_PREVIEW_URL, preview_url);

        // write the key/value pairs to the parcel
        dest.writeBundle(bundle);
    }

    // Parcelable Creator
    public static final Parcelable.Creator<TrackModel> CREATOR = new Creator<TrackModel>() {
        @Override
        public TrackModel createFromParcel(Parcel source) {
            // read the bundle containing key/value pairs from the parcel
            Bundle bundle = source.readBundle();

            // instantiate a TrackModel using values from the bundle
            return new TrackModel(
                    bundle.getString(KEY_NAME),
                    bundle.getString(KEY_ALBUM_NAME),
                    bundle.getString(KEY_ALBUM_ART_LARGE),
                    bundle.getString(KEY_ALBUM_ART_ICON),
                    bundle.getString(KEY_PREVIEW_URL)
            );
        }

        @Override
        public TrackModel[] newArray(int size) {
            return new TrackModel[size];
        }
    };
}
