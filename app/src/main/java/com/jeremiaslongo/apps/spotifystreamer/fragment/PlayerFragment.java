package com.jeremiaslongo.apps.spotifystreamer.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.model.TrackModel;
import com.jeremiaslongo.apps.spotifystreamer.service.MusicService;
import com.jeremiaslongo.apps.spotifystreamer.util.AppUtils;
import com.jeremiaslongo.apps.spotifystreamer.util.ImageLoader;

import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGD;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGE;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGI;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

/**
 * PlayerFragment: shows media player buttons. This Fragment shows the media player buttons and
 * lets the user click them. No media handling is done here, everything is done by passing
 * Intents to our MusicService.
 */
public class PlayerFragment extends DialogFragment implements OnClickListener, SeekBar.OnSeekBarChangeListener {
    // TAG
    public static final String TAG = makeLogTag(PlayerFragment.class);

    // Artist Data
    private ArtistModel mArtist;
    public static final String KEY_ARTIST = "artist";

    // Tracks
    private ArrayList<TrackModel> mTracks;
    public static final String KEY_TRACKS = "tracks";
    private int mTrackIndex;
    public static final String KEY_TRACK_INDEX = "track_index";
    private int mTrackDuration = 0;
    public static final String KEY_TRACK_DURATION = "track_duration";

    // Viewholder
    private ViewHolder mViewHolder;

    // Seekbar
    private int mProgress = 0;

    // Media Service is playing?
    private boolean mPlaying = false;

    // Messages with service
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshTrackData(intent);
        }
    };


    /**
     * Create a new instance of PlayerFragment
     */
    public static PlayerFragment newInstance(ArtistModel artist, ArrayList<TrackModel> tracks, int trackIndex) {
        // New instance
        PlayerFragment f = new PlayerFragment();

        // Create arguments bundle
        Bundle args = new Bundle();
        args.putParcelable(KEY_ARTIST, artist);
        args.putParcelableArrayList(KEY_TRACKS, tracks);
        args.putInt(KEY_TRACK_INDEX, trackIndex);

        // Supply arguments.
        f.setArguments(args);
        // Return fragment
        return f;
    }

    @Override
    public void onStop(){
        super.onStop();
        // If not stopped by a configuration change
        if( !getActivity().isChangingConfigurations() ) {
            // Stop Media Service
            Intent intent = new Intent(getActivity(), MusicService.class);
            getActivity().startService(intent.setAction(MusicService.ACTION_STOP));
            LOGD(TAG, "Stop Music Service");
        }
    }


    @Override
    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).
                registerReceiver(mBroadcastReceiver,
                        new IntentFilter(MusicService.BROADCAST_STATE_UPDATE));
    }

    @Override
    public void onPause(){
        // Unregister the broadcast receiver since the activity is not visible
        try {
            LocalBroadcastManager.getInstance(getActivity()).
                    unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
        }
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // dialog without title
        if( getDialog() != null ) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        // Read Saved Instance State
        if(savedInstanceState != null) {
            mArtist = savedInstanceState.getParcelable(KEY_ARTIST);
            mTracks = savedInstanceState.getParcelableArrayList(KEY_TRACKS);
            mTrackIndex = savedInstanceState.getInt(KEY_TRACK_INDEX);
            mTrackDuration = savedInstanceState.getInt(KEY_TRACK_DURATION);
        } else {
            // Read Arguments
            Bundle arguments = getArguments();
            if (arguments != null) {
                // initialize artist
                mArtist = arguments.getParcelable(KEY_ARTIST);

                // tracks
                mTracks = arguments.getParcelableArrayList(KEY_TRACKS);
                mTrackIndex = arguments.getInt(KEY_TRACK_INDEX);

                // Init service
                Intent intent = new Intent(getActivity(), MusicService.class);
                intent.putParcelableArrayListExtra(MusicService.KEY_TRACKS, mTracks);
                intent.putExtra(MusicService.KEY_TRACK_INDEX, mTrackIndex);
                getActivity().startService(intent.setAction(MusicService.ACTION_INIT));
            }
        }

        // Initialize ViewHolder
        mViewHolder = new ViewHolder(rootView);

        // Set buttons listeners
        mViewHolder.playPauseButton.setOnClickListener(this);
        mViewHolder.previousButton.setOnClickListener(this);
        mViewHolder.nextButton.setOnClickListener(this);

        // Set Seekbar Listener
        mViewHolder.seekBarView.setOnSeekBarChangeListener(this);

        // Refresh view
        loadTrackView();
        refreshTrackData(null);

        // Return fragment view
        return rootView;
    }


    @Override
    public void onSaveInstanceState(Bundle outState){
        // Save artist & tracks data
        outState.putParcelable(KEY_ARTIST, mArtist);
        outState.putParcelableArrayList(KEY_TRACKS, mTracks);
        outState.putInt(KEY_TRACK_INDEX, mTrackIndex);
        outState.putInt(KEY_TRACK_DURATION, mTrackDuration);

        super.onSaveInstanceState(outState);
    }

    /**
     * Load Track View
     */
    private void loadTrackView(){
        TrackModel track = mTracks.get(mTrackIndex);

        mViewHolder.artistNameView.setText(mArtist.getName());
        mViewHolder.albumNameView.setText(track.getAlbumName());
        mViewHolder.trackNameView.setText(track.getName());

        ImageLoader.drawWithDummy(getActivity(), track.getAlbumArtLarge(), mViewHolder.albumImageView);

        mViewHolder.seekBarView.setMax(mTrackDuration / 1000);
        mViewHolder.trackMaxTimeView.setText( AppUtils.timeText(mTrackDuration) );
    }

    /**
     * Refresh Track Data
     */
    private void refreshTrackData(Intent intent) {
        int trackPosition = 0;
        if (intent != null) {
            if (intent.hasExtra(MusicService.KEY_TRACK_INDEX)) {
                mTrackIndex = intent.getIntExtra(MusicService.KEY_TRACK_INDEX, 0);
                mTrackDuration = intent.getIntExtra(MusicService.KEY_TRACK_DURATION, 0);

                loadTrackView();
            } else {
                trackPosition = intent.getIntExtra(MusicService.KEY_TRACK_POS, 0);
                mPlaying = intent.getBooleanExtra(MusicService.KEY_TRACK_IS_PLAYING, false);
            }
        }

        // Buttons
        if (mPlaying) {
            mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }

        // Current Track Position
        mViewHolder.seekBarView.setProgress(trackPosition / 1000);
        mViewHolder.trackTimeView.setText( AppUtils.timeText(trackPosition) );
    }


    // Button Touch listener
    public void onClick(View target) {
        Intent intent = new Intent(getActivity(), MusicService.class);
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == mViewHolder.playPauseButton) {
            if (mPlaying) {
                getActivity().startService(intent.setAction(MusicService.ACTION_PAUSE));
            } else {
                getActivity().startService(intent.setAction(MusicService.ACTION_PLAY));
            }
        } else if (target == mViewHolder.nextButton) {
            getActivity().startService(intent.setAction(MusicService.ACTION_NEXT));
        } else if (target == mViewHolder.previousButton) {
            getActivity().startService(intent.setAction(MusicService.ACTION_PREVIOUS));
        }

    }

    /**
     * Seekbar change listener
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if (fromTouch) {
            mProgress = progress * 1000;
        }
    }

    /**
     * When user starts moving the progress handler
     * */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    /**
     * When user stops moving the progress handler
     * */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Intent intent = new Intent(getActivity(), MusicService.class);
        intent.putExtra(MusicService.KEY_GOTO, mProgress);
        getActivity().startService(intent.setAction(MusicService.ACTION_GOTO));
    }

    /**
     * ViewHolder
     */
    class ViewHolder {
        public final ImageView   albumImageView;
        public final ImageButton previousButton, playPauseButton, nextButton;
        public final SeekBar     seekBarView;
        public final TextView    artistNameView, albumNameView, trackNameView, trackTimeView,
                                 trackMaxTimeView;

        public ViewHolder(View view) {
            artistNameView = (TextView) view.findViewById(R.id.artist_name_textview);
            albumNameView = (TextView) view.findViewById(R.id.artist_album_name_textview);
            albumImageView = (ImageView) view.findViewById(R.id.artist_album_image_imageview);
            trackNameView = (TextView)view.findViewById(R.id.track_name_textview);
            seekBarView = (SeekBar) view.findViewById(R.id.track_seek_bar);
            trackTimeView = (TextView) view.findViewById(R.id.track_current_time_textview);
            trackMaxTimeView = (TextView) view.findViewById(R.id.track_max_time_textview);

            previousButton = (ImageButton) view.findViewById(R.id.previous_button);
                previousButton.setBackgroundColor(getResources().getColor(R.color.primary));
            playPauseButton = (ImageButton) view.findViewById(R.id.play_pause_button);
                playPauseButton.setBackgroundColor(getResources().getColor(R.color.primary));
            nextButton = (ImageButton) view.findViewById(R.id.next_button);
                nextButton.setBackgroundColor(getResources().getColor(R.color.primary));
        }
    }
}
