package com.jeremiaslongo.apps.spotifystreamer.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.model.TrackModel;
import com.jeremiaslongo.apps.spotifystreamer.util.AppUtils;
import com.jeremiaslongo.apps.spotifystreamer.util.AudioFocusHelper;

import java.io.IOException;
import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

/**
 * Service that handles media playback. This is the service through which we perform all the media
 * handling in our application. Upon initialization, it starts a MusicRetriever to scan the user's
 * media, which signal the service to perform specific operations: Play, Pause, Rewind, Skip, etc.
 */
public class MusicService extends Service implements OnPreparedListener, OnCompletionListener,
                    OnErrorListener, AudioFocusHelper.MusicFocusable {
    // TAG
    public static final String TAG = makeLogTag(MusicService.class);

    // Service Actions
    public static final String ACTION_PLAY = "com.jeremiaslongo.musicservice.action.PLAY";
    public static final String ACTION_PAUSE = "com.jeremiaslongo.musicservice.action.PAUSE";
    public static final String ACTION_NEXT = "com.jeremiaslongo.musicservice.action.NEXT";
    public static final String ACTION_PREVIOUS = "com.jeremiaslongo.musicservice.action.PREVIOUS";
    public static final String ACTION_TOGGLE = "com.jeremiaslongo.musicservice.action.TOGGLE";
    public static final String ACTION_STOP = "com.jeremiaslongo.musicservice.action.STOP";
    public static final String ACTION_GOTO = "com.jeremiaslongo.musicservice.action.GOTO";
    public static final String ACTION_INIT =  "com.jeremiaslongo.musicservice.action.INIT";

    // Broadcast Message
    public static final String BROADCAST_STATE_UPDATE = "com.jeremiaslongo.musicservice.ST_UPDATE";

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    // Media Player
    private MediaPlayer mPlayer = null;

    // AudioFocusHelper
    private AudioFocusHelper mAudioFocusHelper = null;

    // indicates the state our service:
    enum State {
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    private State mState = State.Stopped;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiManager.WifiLock mWifiLock;

    // Audio Manager
    AudioManager mAudioManager;

    // Music
    private ArrayList<TrackModel> mTracks;
    private int mCurrentTrackIndex;

    public static final String KEY_TRACKS = "tracks";
    public static final String KEY_TRACK_POS = "track_pos";
    public static final String KEY_TRACK_INDEX = "track_index";
    public static final String KEY_TRACK_DURATION = "track_duration";
    public static final String KEY_TRACK_IS_PLAYING = "is_playing";
    public static final String KEY_GOTO = "goto";

    // Status Messaging
    private final Handler mStatusHandler = new Handler();

    private Runnable statusEcho = new Runnable() {
        @Override
        public void run() {
            broadcastStatus(false);
            mStatusHandler.postDelayed(this, 500);
        }
    };

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    private void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        } else {
            mPlayer.reset();
        }
    }


    @Override
    public void onCreate() {
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, TAG + "_lock");
        // Set Audio Manager
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // Create the Audio Focus Helper
        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null){
            String action = intent.getAction();
            if      (action.equals(ACTION_TOGGLE))      processTogglePlayRequest();
            else if (action.equals(ACTION_PLAY))        processPlayRequest();
            else if (action.equals(ACTION_PAUSE))       processPauseRequest();
            else if (action.equals(ACTION_STOP))        processStopRequest();
            else if (action.equals(ACTION_NEXT))        processNextRequest();
            else if (action.equals(ACTION_PREVIOUS))    processPreviousRequest();
            else if (action.equals(ACTION_GOTO))        processGoToRequest(intent);
            else if (action.equals(ACTION_INIT))        processInitRequest(intent);
        }
        // Means we started the service, but don't want it to restart in case it's killed.
        return START_NOT_STICKY;
    }

    // Action -> Init
    void processInitRequest(Intent intent) {
        mCurrentTrackIndex = intent.getIntExtra(KEY_TRACK_INDEX, 0);
        mTracks = intent.getParcelableArrayListExtra(KEY_TRACKS);
        processPlayRequest();
    }

    // Action -> Toggle Play
    void processTogglePlayRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    // Action -> Play
    void processPlayRequest() {
        tryToGetAudioFocus();

        if (mState == State.Stopped) {
            // If we're stopped, prepare and start music
            prepareAndPlayTrack();
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback.
            mState = State.Playing;
            configAndStartMediaPlayer();
        }
    }

    // Action -> Pause
    void processPauseRequest() {
        if (mState == State.Playing) {
            // Pause media player.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            broadcastStatus(false);
        }
    }

    // Action -> Previous
    void processPreviousRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            mCurrentTrackIndex--;
            if (mCurrentTrackIndex < 0) {
                mCurrentTrackIndex = mTracks.size() - 1;
            }
            prepareAndPlayTrack();
        }
    }

    // Action -> Next
    void processNextRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            mCurrentTrackIndex++;
            if (mCurrentTrackIndex >= mTracks.size()){
                mCurrentTrackIndex = 0;
            }
            prepareAndPlayTrack();
        }
    }

    // Action -> Stop
    void processStopRequest() {
        processStopRequest(false);
    }
    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;
            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }


    // Action - Go To
    void processGoToRequest(Intent intent) {
        mPlayer.seekTo(intent.getIntExtra(KEY_GOTO, 0));
    }

    // Broadcast Media Player Status
    private void broadcastStatus(boolean once) {
        Intent intent = new Intent(BROADCAST_STATE_UPDATE);

        if (once) {
            intent.putExtra(KEY_TRACK_INDEX, mCurrentTrackIndex);
            intent.putExtra(KEY_TRACK_DURATION, mPlayer.getDuration());
        } else {
            intent.putExtra(KEY_TRACK_IS_PLAYING, mState.equals(State.Playing));
            intent.putExtra(KEY_TRACK_POS, mPlayer.getCurrentPosition());
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    /**
     * Starts playing the song at position.
     */
    void prepareAndPlayTrack() {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
        try {
            createMediaPlayerIfNeeded();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // set the source of the media player
            Uri trackUri = Uri.parse(mTracks.get(mCurrentTrackIndex).getPreviewUrl());
            mPlayer.setDataSource(trackUri.toString());

            mState = State.Preparing;

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing.
            mWifiLock.acquire();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
            }
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if ( !mPlayer.isPlaying() ){
            mPlayer.start();
        }
        mStatusHandler.removeCallbacks(statusEcho);
        mStatusHandler.postDelayed(statusEcho, 500);
    }

    /**
     * Releases resources used by the service for playback. This includes
     * the wake locks and possibly the MediaPlayer.
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
        //
        mStatusHandler.removeCallbacks(statusEcho);
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;
        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    // Called when Media Player is done preparing.
    @Override
    public void onPrepared(MediaPlayer mp) {
        // The Media Player is done preparing. That means we can start playing.
        mState = State.Playing;
        broadcastStatus(true);
        configAndStartMediaPlayer();
    }

    // Called when Media Player is done playing current song.
    @Override
    public void onCompletion(MediaPlayer player) {
        // The Media Player finished playing the current song, so we go ahead and start next.
        mCurrentTrackIndex++;
        if (mCurrentTrackIndex >= mTracks.size()) {
            mCurrentTrackIndex = 0;
        }
        prepareAndPlayTrack();
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        AppUtils.alert(getApplicationContext(), getString(R.string.media_player_error));
        Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        // true indicates we handled the error
        return true;
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we released our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Receives broadcasted intents. In particular, we are interested in the
     * android.media.AUDIO_BECOMING_NOISY and android.intent.action.MEDIA_BUTTON intents, which is
     * broadcast, for example, when the user disconnects the headphones. This class works because we are
     * declaring it in a &lt;receiver&gt; tag in AndroidManifest.xml.
     */
    public class MusicIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                AppUtils.alert(context, getString(R.string.headphones_out));
                // send an intent to our MusicService to telling it to pause the audio
                context.startService(new Intent(MusicService.ACTION_PAUSE));

            } else if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
                KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                    return;
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        context.startService(new Intent(MusicService.ACTION_TOGGLE));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        context.startService(new Intent(MusicService.ACTION_PLAY));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        context.startService(new Intent(MusicService.ACTION_PAUSE));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        context.startService(new Intent(MusicService.ACTION_STOP));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        context.startService(new Intent(MusicService.ACTION_NEXT));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        context.startService(new Intent(MusicService.ACTION_PREVIOUS));
                        break;
                }
            }
        }
    }
}
