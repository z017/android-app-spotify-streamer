package com.jeremiaslongo.apps.spotifystreamer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jeremiaslongo.apps.spotifystreamer.R;
import com.jeremiaslongo.apps.spotifystreamer.data.ArtistModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ArtistsAdapter extends ArrayAdapter<ArtistModel> {

    public ArtistsAdapter(Context context, int resourceID, ArrayList<ArtistModel> artists) {
        super(context, resourceID, artists);
    }

    // How each list item will look.
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        View view = convertView;

        if( view == null ) {
            // Inflate the ListView item layout
            LayoutInflater inflater = LayoutInflater.from( getContext() );
            view = inflater.inflate(R.layout.list_item_artists, parent, false);

            // initialize the view holder
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (ViewHolder) view.getTag();
        }

        // Update the item view
        ArtistModel artist = getItem(position);

        Picasso.with(getContext())
                .load(artist.getImage())
                .placeholder(R.mipmap.ic_launcher)
                .into(viewHolder.imageView);

        viewHolder.nameView.setText(artist.getName());

        // Return the view
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