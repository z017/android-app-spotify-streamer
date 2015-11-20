package com.jeremiaslongo.apps.spotifystreamer.task;

import android.content.Context;
import android.os.AsyncTask;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.model.TrackModel;
import com.jeremiaslongo.apps.spotifystreamer.util.AppUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGD;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGE;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;


/**
 * FetchTracksTask
 */
public class FetchTracksTask extends AsyncTask<String, String, ArrayList<TrackModel>> {
    // TAG
    public static final String TAG = makeLogTag(FetchTracksTask.class);

    private Context mContext;
    private FetchTracksListener mListener;

    public FetchTracksTask(Context context, FetchTracksListener listener){
        super();
        mContext = context;
        mListener = listener;
    }

    // Interface used to return fetch result.
    public interface FetchTracksListener {
        void onTracksFetched(ArrayList<TrackModel> tracks);
    }

    @Override
    protected ArrayList<TrackModel> doInBackground(String... params) {
        /**
         * REVIEW
         *
         * Check if there's network connectivity before calling into the spotify API.
         * This gives a more responsive output to the user rather than them having to wait
         * everytime you go through the API and hit an exception.
         */
        if ( !AppUtils.isNetworkAvailable(mContext) ){
            publishProgress(mContext.getString(R.string.network_unavailable));
            return null;
        }

        // If there is an empty search query, return
        if (params.length == 0) {
            publishProgress(mContext.getString(R.string.empty_id));
            return null;
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
                            album_large_image,
                            album_icon_image,
                            track.preview_url);
                    tracks.add(trackModel);
                }
                LOGD(TAG, "Tracks fetched: " + tracks.size());
                return tracks;

            }else{
                publishProgress(mContext.getString(R.string.no_tracks_found));
                return null;
            }

        } catch (Exception e) {
            LOGE(TAG, e.toString());
            publishProgress(mContext.getString(R.string.spotify_api_error));
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(String... states) {
        if (states.length > 0) {
            AppUtils.alert(mContext, states[0]);
        }
    }

    @Override
    public void onPostExecute(ArrayList<TrackModel> tracks){
        mListener.onTracksFetched(tracks);
    }
}
