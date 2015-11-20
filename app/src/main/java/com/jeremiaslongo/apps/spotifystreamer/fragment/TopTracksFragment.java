package com.jeremiaslongo.apps.spotifystreamer.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.adapter.TracksAdapter;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.model.TrackModel;
import com.jeremiaslongo.apps.spotifystreamer.task.FetchTracksTask;

import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;


public class TopTracksFragment extends Fragment implements FetchTracksTask.FetchTracksListener{
    // TAG
    public static final String TAG = makeLogTag(TopTracksFragment.class);

    // Bundle Arguments Keys
    public static final String KEY_ARTIST = "artist";
    private ArtistModel mArtist;

    // Adapter
    private TracksAdapter mTracksAdapter;
    private ArrayList<TrackModel> mTracks;
    private static final String KEY_TRACKS = "tracks";

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
        void onItemSelected(ArtistModel artist, ArrayList<TrackModel> tracks, int trackIndex);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        // Progress View
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_view);

        // Read Saved Instance State
        if(savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(KEY_TRACKS);
            mArtist = savedInstanceState.getParcelable(KEY_ARTIST);
        } else {
            // initialize tracks
            mTracks = new ArrayList<TrackModel>();

            Bundle arguments = getArguments();
            if (arguments != null) {
                // initialize artist name
                mArtist = arguments.getParcelable(KEY_ARTIST);
                // Fetch Tracks
                fetchTopTracks();
            }
        }

        if (mArtist != null) {
            ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            assert actionBar != null;
            actionBar.setSubtitle(mArtist.getName());
        }

        // Initialize the adapter
        mTracksAdapter = new TracksAdapter(getActivity(), mTracks);

        // Get a reference to the ListView, and attach the adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.list_toptracks_view);
        mListView.setAdapter(mTracksAdapter);

        // Click action on list items
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                TrackModel track = mTracks.get(position);
                if (track != null) {
                    ((Callback) getActivity()).onItemSelected(mArtist, mTracks, position);
                }
            }
        });

        // Return fragment view
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        //Save artist data
        outState.putParcelable(KEY_ARTIST, mArtist);
        // Save track list
        outState.putParcelableArrayList(KEY_TRACKS, mTracks);

        super.onSaveInstanceState(outState);
    }

    private void fetchTopTracks() {
        if (mArtist != null) {
            // Clear adapter
            if (mTracksAdapter != null && mTracksAdapter.getCount() > 0 ) {
                mTracksAdapter.clear();
            }

            // Visible Progress
            mProgress.setVisibility(ProgressBar.VISIBLE);

            // Fetch Tracks
            new FetchTracksTask(getActivity(), this).execute(mArtist.getSpotifyId());
        }
    }

    @Override
    public void onTracksFetched(ArrayList<TrackModel> tracks) {
        // Remove Progress
        mProgress.setVisibility(ProgressBar.GONE);

        // If we get Tracks
        if (tracks != null) {
            mTracks = tracks;

            // Add Tracks Fetched
            mTracksAdapter.addAll(tracks);
        }
    }
}
