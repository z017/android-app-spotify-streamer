package com.jeremiaslongo.apps.spotifystreamer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.fragment.PlayerFragment;
import com.jeremiaslongo.apps.spotifystreamer.fragment.TopTracksFragment;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.model.TrackModel;

import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;


public class TopTracksActivity extends ActionBarActivity implements TopTracksFragment.Callback{
    // TAG
    public static final String TAG = makeLogTag(TopTracksActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);

        // if we're being restored from a previous state, then we don't need to do anything and
        // should return or else we could end up with overlapping fragments.
        if (savedInstanceState == null) {
            // Create the top tracks fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putParcelable(TopTracksFragment.KEY_ARTIST,
                    getIntent().getParcelableExtra(TopTracksFragment.KEY_ARTIST));

            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment, TopTracksFragment.TAG)
                    .commit();
        }

    }

    @Override
    public void onItemSelected(ArtistModel artist, ArrayList<TrackModel> tracks, int trackIndex) {
        Intent intent = new Intent(this, PlayerActivity.class)
                .putExtra(PlayerFragment.KEY_ARTIST, artist)
                .putParcelableArrayListExtra(PlayerFragment.KEY_TRACKS, tracks)
                .putExtra(PlayerFragment.KEY_TRACK_INDEX, trackIndex);
        startActivity(intent);
    }

    /**
     * Set ActionBar Up button work like back button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
