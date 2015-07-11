package com.jeremiaslongo.apps.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.jeremiaslongo.apps.spotifystreamer.adapters.ArtistsAdapter;
import com.jeremiaslongo.apps.spotifystreamer.data.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.utils.Utils;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;


public class MainActivity extends ActionBarActivity implements SearchView.OnQueryTextListener {

    private static final String KEY_ARTIST_LIST = "artists";
    private ArtistsAdapter mArtistsAdapter;
    private ArrayList<ArtistModel> mArtists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Search View
        SearchView searchView = (SearchView) findViewById(R.id.search_artist_view);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.search_hint));
        /*
         * REVIEW
         *
         * If the searchView is iconified the user always needs to click on the search icon to start
         * using the app.
         * Keeping it open by default saves them a click since they would invariably always need to
         * do it before starting to search (Clicks are precious).
         */
        searchView.setIconifiedByDefault(false);

        // Read Saved Instance State
        if(savedInstanceState != null) {
            mArtists = savedInstanceState.getParcelableArrayList(KEY_ARTIST_LIST);
        } else {
            mArtists = new ArrayList<ArtistModel>();
        }

        // Initialize the adapter
        mArtistsAdapter = new ArtistsAdapter(
                this,
                R.layout.list_item_artists,
                mArtists);

        // Get a reference to the ListView, and attach the adapter to it.
        ListView listView = (ListView) this.findViewById(R.id.list_artists_view);
        listView.setAdapter( mArtistsAdapter );

        // Click action on list items
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                ArtistModel artist = mArtists.get(position);

                Intent intent = new Intent(adapterView.getContext(), TopTracksActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, new String[]{artist.getSpotifyId(), artist.getName()});
                startActivity(intent);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putParcelableArrayList(KEY_ARTIST_LIST, mArtists);
        super.onSaveInstanceState(outState);
    }

    // SearchView.OnQueryTextListener function
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    // SearchView.OnQueryTextListener function
    public boolean onQueryTextSubmit(String query) {
        updateArtists(query);
        return false;
    }

    // updateArtists Action
    private void updateArtists(String query) {
        new FetchArtistsTask().execute(query);
    }

    // FetchArtistsTask
    public class FetchArtistsTask extends AsyncTask<String, Void, String> {

        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();

        private ArrayList<ArtistModel> mFetchedArtists;

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
                return getString(R.string.empty_query);
            }

            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                ArtistsPager results = spotify.searchArtists( params[0] );

                if(results.artists.total > 0){
                    ArrayList<ArtistModel> artists = new ArrayList<ArtistModel>();
                    for (Artist artist : results.artists.items) {

                        String artist_image = null;
                        for (Image image : artist.images ){
                            if (image.width >= 150 && image.width <= 300){
                                artist_image = image.url;
                                break;
                            }
                        }

                        ArtistModel artistModel = new ArtistModel(artist.id, artist.name, artist_image);
                        artists.add(artistModel);
                    }
                    Log.d(LOG_TAG, "Artists fetched: " + artists.size());
                    mFetchedArtists = artists;
                    return null;

                }else{
                    return getString(R.string.no_artist_found);
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
                return getString(R.string.spotify_api_error);
            }
        }

        @Override
        public void onPreExecute(){
            Utils.alert(getApplicationContext(), getString(R.string.searching));
        }

        @Override
        public void onPostExecute(String error){
            if ( error != null ) {
                Utils.alert(getApplicationContext(), error);
            } else {
                mArtists = mFetchedArtists;
                if (mArtistsAdapter.getCount() > 0 ) {
                    mArtistsAdapter.clear();
                }
                mArtistsAdapter.addAll(mArtists);
            }
        }
    }
}
