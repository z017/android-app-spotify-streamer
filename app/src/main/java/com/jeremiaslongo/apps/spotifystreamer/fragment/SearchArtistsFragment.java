package com.jeremiaslongo.apps.spotifystreamer.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.adapter.ArtistsAdapter;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.task.FetchArtistsTask;

import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGD;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

public class SearchArtistsFragment extends Fragment implements SearchView.OnQueryTextListener, FetchArtistsTask.FetchArtistListener {
    // TAG
    public static final String TAG = makeLogTag(SearchArtistsFragment.class);

    // Adapter
    private ArtistsAdapter mArtistsAdapter;
    private ArrayList<ArtistModel> mArtists;
    private static final String KEY_ARTISTS = "artists";

    // SearchView
    private SearchView mSearchView;

    // ListView
    private ListView mListView;

    // ProgressBar - https://guides.codepath.com/android/Handling-ProgressBars
    private ProgressBar mProgress;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        void onItemSelected(ArtistModel artist);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_search_artists, container, false);

        // Initialize the adapter
        if (savedInstanceState != null) {
            mArtists = savedInstanceState.getParcelableArrayList(KEY_ARTISTS);
        } else {
            mArtists = new ArrayList<ArtistModel>();
        }
        mArtistsAdapter = new ArtistsAdapter(getActivity(), mArtists);

        // Get a reference to the ListView, and attach the adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.list_artists_view);
        mListView.setAdapter(mArtistsAdapter);

        // Click action on list items
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                ArtistModel artist = mArtists.get(position);
                if (artist != null) {
                    ((Callback) getActivity()).onItemSelected(artist);
                }
            }
        });

        // Search View
        mSearchView = (SearchView) rootView.findViewById(R.id.search_artist_view);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        /*
         * REVIEW
         *
         * If the searchView is iconified the user always needs to click on the search icon to start
         * using the app.
         * Keeping it open by default saves them a click since they would invariably always need to
         * do it before starting to search (Clicks are precious).
         */
        mSearchView.setIconifiedByDefault(false);

        // Progress View
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_view);

        // Return fragment view
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        // Save artist list
        outState.putParcelableArrayList(KEY_ARTISTS, mArtists);

        super.onSaveInstanceState(outState);
    }

    // SearchView.OnQueryTextListener function
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    // SearchView.OnQueryTextListener function
    public boolean onQueryTextSubmit(String query) {
        // This avoid the twice call of this function
        mSearchView.clearFocus();

        // Clear adapter
        if (mArtistsAdapter != null && mArtistsAdapter.getCount() > 0 ) {
            mArtistsAdapter.clear();
        }

        // Visible Progress
        mProgress.setVisibility(ProgressBar.VISIBLE);

        // Search Artist
        new FetchArtistsTask(getActivity(),this).execute(query);
        return true;
    }

    @Override
    public void onArtistsFetched(ArrayList<ArtistModel> artists) {
        // Remove Progress
        mProgress.setVisibility(ProgressBar.GONE);

        // If we get artists
        if (artists != null) {
            mArtists = artists;

            // Add Artists
            mArtistsAdapter.addAll(mArtists);
        }
    }
}
