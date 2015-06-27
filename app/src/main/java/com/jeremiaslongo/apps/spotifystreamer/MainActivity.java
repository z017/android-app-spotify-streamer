package com.jeremiaslongo.apps.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.jeremiaslongo.apps.spotifystreamer.adapters.ArtistsAdapter;
import com.jeremiaslongo.apps.spotifystreamer.data.ArtistModel;

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
    public class FetchArtistsTask extends AsyncTask<String, Void, ArrayList<ArtistModel>> {

        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();

        @Override
        protected ArrayList<ArtistModel> doInBackground(String... params) {

            // If there is an empty search query, return
            if (params.length == 0) {
                return null;
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
                    return artists;

                }else{
                    return null;
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
                return null;
            }
        }

        @Override
        public void onPreExecute(){
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.searching), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
        }

        @Override
        public void onPostExecute(ArrayList<ArtistModel> artists){
            if ( artists != null ) {
                mArtists = artists;
                if (mArtistsAdapter.getCount() > 0 ) {
                    mArtistsAdapter.clear();
                }
                mArtistsAdapter.addAll(mArtists);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_artist_found), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
            }
        }
    }
}
