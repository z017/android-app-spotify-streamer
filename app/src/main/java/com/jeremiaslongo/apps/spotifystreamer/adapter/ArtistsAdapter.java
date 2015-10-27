package com.jeremiaslongo.apps.spotifystreamer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.model.ArtistModel;
import com.jeremiaslongo.apps.spotifystreamer.util.ImageLoader;

import java.util.ArrayList;

import static com.jeremiaslongo.apps.spotifystreamer.util.LogUtils.makeLogTag;

public class ArtistsAdapter extends ArrayAdapter<ArtistModel> {
    // TAG
    public static final String TAG = makeLogTag(ArtistsAdapter.class);

    public ArtistsAdapter(Context context, ArrayList<ArtistModel> artists) {
        super(context, R.layout.list_item_artists, artists);
    }

    // How each list item will look.
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;
        //Check if an existing view is being reused, otherwise inflate the view.
        if( view == null ) {
            // Inflate the ListView item layout
            LayoutInflater inflater = LayoutInflater.from( getContext() );
            view = inflater.inflate(R.layout.list_item_artists, parent, false);

            // initialize view holder
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            // view already inflated, get viewHolder
            viewHolder = (ViewHolder) view.getTag();
        }

        // Get the data item for this position
        ArtistModel artist = getItem(position);

        ImageLoader.drawWithDummy(getContext(), artist.getImage(), viewHolder.imageView);
        viewHolder.nameView.setText(artist.getName());

        // Return the view to render on screen
        return view;
    }


    /**
     * Cache of the children views for an Artist list item.
     */
    public static class ViewHolder {
        public final ImageView imageView;
        public final TextView nameView;

        public ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.list_item_artist_image_imageview);
            nameView = (TextView) view.findViewById(R.id.list_item_artist_name_textview);
         }
    }
}