package com.jeremiaslongo.apps.spotifystreamer.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class ArtistModel implements Parcelable {

    // Keys
    private static final String KEY_SPOTIFY_ID = "spotify_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_IMAGE = "image";

    // Data
    private String spotify_id;
    private String name;
    private String image;

    // Constructor
    public ArtistModel(String sp_id, String name,String image){
        this.spotify_id = sp_id;
        this.name = name;
        this.image = image;
    }

    public String getName() {
        return name;
    }
    public String getSpotifyId(){
        return spotify_id;
    }
    public String getImage(){
        return image;
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags){
        // Create a bundle for the key/value pairs
        Bundle bundle = new Bundle();

        // insert the key value pairs to the bundle
        bundle.putString(KEY_SPOTIFY_ID, spotify_id);
        bundle.putString(KEY_NAME, name);
        bundle.putString(KEY_IMAGE, image);

        // write the key/value pairs to the parcel
        dest.writeBundle(bundle);
    }

    // Parcelable Creator
    public static final Parcelable.Creator<ArtistModel> CREATOR = new Creator<ArtistModel>() {
        @Override
        public ArtistModel createFromParcel(Parcel source) {
            // read the bundle containing key/value pairs from the parcel
            Bundle bundle = source.readBundle();

            // instantiate an ArtistModel using values from the bundle
            return new ArtistModel(
                    bundle.getString(KEY_SPOTIFY_ID),
                    bundle.getString(KEY_NAME),
                    bundle.getString(KEY_IMAGE)
            );
        }

        @Override
        public ArtistModel[] newArray(int size) {
            return new ArtistModel[size];
        }
    };

}
