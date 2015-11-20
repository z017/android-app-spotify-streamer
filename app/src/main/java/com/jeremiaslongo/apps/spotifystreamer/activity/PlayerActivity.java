package com.jeremiaslongo.apps.spotifystreamer.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.fragment.PlayerFragment;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.model.TrackModel;

import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

public class PlayerActivity extends ActionBarActivity {
    // TAG
    public static final String TAG = makeLogTag(PlayerActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // if we're being restored from a previous state, then we don't need to do anything and
        // should return or else we could end up with overlapping fragments.
        if (savedInstanceState == null) {
            // Create the player fragment and add it to the activity
            // using a fragment transaction.
            ArtistModel artist = getIntent().getParcelableExtra(PlayerFragment.KEY_ARTIST);
            ArrayList<TrackModel> tracks = getIntent().getParcelableArrayListExtra(PlayerFragment.KEY_TRACKS);
            int trackIndex = getIntent().getIntExtra(PlayerFragment.KEY_TRACK_INDEX, 0);

            PlayerFragment fragment = PlayerFragment.newInstance(artist, tracks, trackIndex);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment, PlayerFragment.TAG)
                    .commit();
        }
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
