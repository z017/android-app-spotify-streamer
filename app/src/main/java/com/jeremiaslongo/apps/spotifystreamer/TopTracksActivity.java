package com.jeremiaslongo.apps.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ListView;

import com.jeremiaslongo.apps.spotifystreamer.adapters.TracksAdapter;
import com.jeremiaslongo.apps.spotifystreamer.data.TrackModel;
import com.jeremiaslongo.apps.spotifystreamer.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


public class TopTracksActivity extends ActionBarActivity {

    private static final String KEY_TRACKS_LIST = "tracks";
    private static final String KEY_ARTIST_NAME = "artist";
    private TracksAdapter mTracksAdapter;
    private ArrayList<TrackModel> mTracks;
    private String mArtistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);


        // Read Saved Instance State
        if(savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(KEY_TRACKS_LIST);
            mArtistName = savedInstanceState.getString(KEY_ARTIST_NAME);
        } else {
            String[] data = getIntent().getExtras().getStringArray(Intent.EXTRA_TEXT);

            // initialize artist name
            mArtistName = data[1];

            // initialize tracks
            mTracks = new ArrayList<TrackModel>();
            updateTopTracks(data[0]);
        }

        // set subtitle in the actionbar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setSubtitle(mArtistName);

        // Initialize the adapter
        mTracksAdapter = new TracksAdapter(
                this,
                R.layout.list_item_tracks,
                mTracks);

        // Get a reference to the ListView, and attach the adapter to it.
        ListView listView = (ListView) this.findViewById(R.id.list_toptracks_view);
        listView.setAdapter(mTracksAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_TRACKS_LIST, mTracks);
        outState.putString(KEY_ARTIST_NAME, mArtistName);
    }

    // updateTopTracks Action
    private void updateTopTracks(String artistId) {
        new FetchTracksTask().execute(artistId);
    }

    // FetchArtistsTask
    public class FetchTracksTask extends AsyncTask<String, Void, String> {

        private final String LOG_TAG = FetchTracksTask.class.getSimpleName();

        private ArrayList<TrackModel> mFetchedTracks;

        @Override
        protected String doInBackground(String... params) {
            /**
             * REVIEW
             *
             * Check if there's network connectivity before calling into the spotify API.
             * This gives a more responsive output to the user rather than them having to wait
             * everytime you go through the API and hit an exception.
             */
            if ( !Utils.isNetworkAvailable(getApplicationContext()) ){
                return getString(R.string.network_unavailable);
            }

            // If there is an empty search query, return
            if (params.length == 0) {
                return getString(R.string.empty_id);
            }

            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                // add location info
                Map<String, Object> options = new HashMap<String, Object>();
                options.put(spotify.COUNTRY, "AR");

                Tracks results = spotify.getArtistTopTrack( params[0], options );

                if(results.tracks.size() > 0){
                    ArrayList<TrackModel> tracks = new ArrayList<TrackModel>();

                    for (Track track : results.tracks) {
                        String album_icon_image = null;
                        String album_large_image = null;
                        for (Image image : track.album.images ){
                            if (image.width >= 150 && image.width <= 300){
                                album_icon_image = image.url;
                                continue;
                            }
                            if (image.width >= 500 && image.width <= 640){
                                album_large_image = image.url;
                                continue;
                            }
                        }

                        TrackModel trackModel = new TrackModel(
                                track.name,
                                track.album.name,
                                mArtistName,
                                album_large_image,
                                album_icon_image,
                                track.preview_url);
                        tracks.add(trackModel);
                    }
                    Log.d(LOG_TAG, "Tracks fetched: " + tracks.size());
                    mFetchedTracks = tracks;
                    return null;

                }else{
                    return getString(R.string.no_tracks_found);
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
                return getString(R.string.spotify_api_error);
            }
        }

        @Override
        public void onPreExecute(){
            Utils.alert(getApplicationContext(), getString(R.string.loading));
        }

        @Override
        public void onPostExecute(String error){
            if ( error != null ) {
                Utils.alert(getApplicationContext(), error);
            } else {
                mTracks = mFetchedTracks;
                if (mTracksAdapter.getCount() > 0 ) {
                    mTracksAdapter.clear();
                }
                mTracksAdapter.addAll(mTracks);
            }
        }
    }
}
