package com.jeremiaslongo.apps.spotifystreamer.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.data.TrackModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class TracksAdapter extends ArrayAdapter<TrackModel> {

    private final String LOG_TAG = TracksAdapter.class.getSimpleName();

    public TracksAdapter(Context context, int resourceID, ArrayList<TrackModel> tracks) {
        super(context, resourceID, tracks);
    }

    // How each list item will look.
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        View view = convertView;

        if( view == null ) {
            // Inflate the ListView item layout
            LayoutInflater inflater = LayoutInflater.from( getContext() );
            view = inflater.inflate(R.layout.list_item_tracks, parent, false);

            // initialize the view holder
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (ViewHolder) view.getTag();
        }

        // Update the item view
        TrackModel track = getItem(position);

        /**
         * REVIEW
         *
         * Picasso can crash your app if you the url you pass it in empty/malformed.
         * Consider doing the check before calling into it.
         *
         * https://github.com/square/picasso/issues/609
         */
        String thumb = null;
        try {
            thumb = Uri.parse(track.getAlbumArtIcon()).toString();
        } catch (Exception e){
            Log.e(LOG_TAG, e.toString());
        }
        Picasso.with(getContext())
                .load(thumb)
                .placeholder(R.mipmap.ic_launcher)
                .into(viewHolder.iconView);

        viewHolder.nameView.setText(track.getName());

        viewHolder.albumNameView.setText(track.getAlbumName());

        // Return the view
        return view;
    }

    /**
     * Cache of the children views for an Artist list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView nameView;
        public final TextView albumNameView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_track_icon_imageview);
            nameView = (TextView) view.findViewById(R.id.list_item_track_name_textview);
            albumNameView = (TextView) view.findViewById(R.id.list_item_album_name_textview);
         }
    }
}