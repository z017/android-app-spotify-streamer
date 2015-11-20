package com.jeremiaslongo.apps.spotifystreamer.activity;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.fragment.PlayerFragment;
import com.jeremiaslongo.apps.spotifystreamer.fragment.SearchArtistsFragment;
import com.jeremiaslongo.apps.spotifystreamer.fragment.TopTracksFragment;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.model.TrackModel;

import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

public class MainActivity extends ActionBarActivity implements SearchArtistsFragment.Callback,
        TopTracksFragment.Callback{
    // TAG
    public static final String TAG = makeLogTag(MainActivity.class);

    // Two Pane Layout for Tablets
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check activity layout version
        if (findViewById(R.id.fragment_container) != null) {
            // The fragment_container view will be present only in the large-screen layouts.
            // If this view is present, then the activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, show the toptracks view in this activity by
            // adding or replacing the toptracks fragment using a fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TopTracksFragment(), TopTracksFragment.TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
    }

    // SearchArtistFragment Callback
    @Override
    public void onItemSelected(ArtistModel artist) {
        if (mTwoPane) {
            // In two-pane mode, show the toptracks view in this activity by
            // adding or replacing the toptracks fragment using a fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(TopTracksFragment.KEY_ARTIST, artist);

            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, TopTracksFragment.TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, TopTracksActivity.class)
                    .putExtra(TopTracksFragment.KEY_ARTIST, artist);
            startActivity(intent);
        }
    }

    // TopTracksFragment Callback
    @Override
    public void onItemSelected(ArtistModel artist, ArrayList<TrackModel> tracks, int trackIndex) {
        // Create and show the dialog.
        PlayerFragment fragment = PlayerFragment.newInstance(artist, tracks, trackIndex);
        fragment.show(getSupportFragmentManager(), PlayerFragment.TAG);
    }
}
