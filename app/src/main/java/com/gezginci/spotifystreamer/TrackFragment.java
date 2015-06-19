package com.gezginci.spotifystreamer;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackFragment extends Fragment {

    private static final String INTENT_ARTIST_ID = "INTENT_ARTIST_ID";
    private static final String INTENT_ARTIST_NAME = "INTENT_ARTIST_NAME";
    private static final String SPOTIFY_COUNTRY = "DE";
    private static final int SPOTIFY_NO_OF_TOP_TRACKS = 10;
    TrackAdapter trackAdapter;

    public TrackFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();

        /* Get artist name and artist id from Intend setted on MainActivity*/
        String artistName = getActivity().getIntent().getExtras().get(INTENT_ARTIST_NAME).toString();
        String artistId = getActivity().getIntent().getExtras().get(INTENT_ARTIST_ID).toString();

        actionBar.setSubtitle(artistName);

        /* Start asyn task for searching tracks*/
        FetchTrackDataFromSpotify artistTask = new FetchTrackDataFromSpotify();
        artistTask.execute(artistId);

        ArrayList<Track> tracks = new ArrayList<Track>();
        trackAdapter = new TrackAdapter(getActivity(), R.layout.list_item_track, tracks);

        View rootView = inflater.inflate(R.layout.fragment_track, container, false);

        ListView listView;
        listView = (ListView) rootView.findViewById(R.id.listview_tracks);
        listView.setAdapter(trackAdapter);

        return rootView;
    }

    /**
     * A custom ArrayAdapter accepting spotify.Track
     */
    public class TrackAdapter extends ArrayAdapter<Track> {

        public TrackAdapter(Context context, int textViewResourceId, ArrayList<Track> items) {
            super(context, textViewResourceId, items);
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            Track track = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);
            }

            TextView albumNameTrack = (TextView) convertView.findViewById(R.id.album_track_text);
            ImageView albumCover = (ImageView) convertView.findViewById(R.id.cover_image);

            /* Put album name and track name into the same TextView so the 'whole' text will be vertically centered
            *  right of the cover image */
            String albumText = track.album.name + "\n" + track.name;
            albumNameTrack.setText(albumText);

            /* If no cover image cover exists imageUrl is empty, in this case Glide uses automatically
            * an error image in this case no_image*/
            String imageUrl = "";
            if (track.album.images.size() > 0)
                imageUrl = track.album.images.get(0).url;

            /* Loading the cover image using centerCorp to have the same format for every cover*/
            Glide.with(getActivity())
                    .load(imageUrl)
                    .centerCrop()
                    .crossFade()
                    .error(R.drawable.no_image)
                    .into(albumCover);

            return convertView;
        }
    }

    /* Async function for fetching track data from spotiyf return type doInBackground:Tracks*/
    private class FetchTrackDataFromSpotify extends AsyncTask<String, Void, Tracks> {

        String searchString;

        protected Tracks doInBackground(String... artist) {
            try {

                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                searchString = artist[0];

                Map<String, Object> options = new HashMap<>();
                options.put(spotify.COUNTRY, SPOTIFY_COUNTRY);
                options.put(spotify.LIMIT, SPOTIFY_NO_OF_TOP_TRACKS);

                return spotify.getArtistTopTrack(artist[0], options);

            } catch (Exception e) {
                Log.e("FetchTrackData", "Error ", e);
                return null;
            }
        }

        /* put the result of doInBackground into trackAdapter*/
        @Override
        protected void onPostExecute(Tracks strings) {
            super.onPostExecute(strings);
            trackAdapter.clear();

            for (Iterator<Track> i = strings.tracks.iterator(); i.hasNext(); ) {
                Track item = i.next();
                trackAdapter.add(item);
            }
        }
    }
}
