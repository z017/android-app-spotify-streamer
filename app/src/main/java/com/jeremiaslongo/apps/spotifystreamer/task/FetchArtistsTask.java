package com.jeremiaslongo.apps.spotifystreamer.task;

import android.content.Context;
import android.os.AsyncTask;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.util.AppUtils;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGD;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.LOGE;
import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

/**
 * FetchArtistTask
 */
public class FetchArtistsTask extends AsyncTask<String, String, ArrayList<ArtistModel>> {
    // TAG
    public static final String TAG = makeLogTag(FetchArtistsTask.class);

    private Context mContext;
    private FetchArtistListener mListener;

    public FetchArtistsTask(Context context, FetchArtistListener listener) {
        mContext = context;
        mListener = listener;
    }

    // Interface used to return fetch result.
    public interface FetchArtistListener {
        void onArtistsFetched(ArrayList<ArtistModel> artists);
    }

    @Override
    protected ArrayList<ArtistModel> doInBackground(String... params) {
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

        // If there is an empty search query return
        if (params.length == 0) {
            publishProgress(mContext.getString(R.string.empty_query));
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
                LOGD(TAG, "Artists fetched: " + artists.size());
                return artists;
            }else{
                publishProgress(mContext.getString(R.string.no_artist_found));
                return null;
            }
        } catch (Exception e) {
            LOGE(TAG, e.toString());
            publishProgress(mContext.getString(R.string.spotify_api_error));
            return null;
        }
    }


    @Override
    protected void onPreExecute() {
        AppUtils.alert(mContext, mContext.getString(R.string.searching));
    }

    @Override
    protected void onProgressUpdate(String... states) {
        if (states.length > 0) {
            AppUtils.alert(mContext, states[0]);
        }
    }

    @Override
    protected void onPostExecute(ArrayList<ArtistModel> artists) {
        if ( artists != null ) {
            mListener.onArtistsFetched(artists);
        }
    }
}
