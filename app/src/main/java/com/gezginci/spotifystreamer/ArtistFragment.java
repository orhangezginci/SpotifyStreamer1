package com.gezginci.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONException;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Artists;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.User;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistFragment extends Fragment {

    private static final String LAST_SEARCH_STRING_KEY = "prefs_artist_search_key";
    private static final String INTENT_ARTIST_ID = "INTENT_ARTIST_ID";
    private static final String INTENT_ARTIST_NAME = "INTENT_ARTIST_NAME";

    ArtistAdapter artistAdapter;
    String searchArtist;

    public ArtistFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ArrayList<Artist> artists = new ArrayList<Artist>();

        artistAdapter = new ArtistAdapter(getActivity(), R.layout.list_item_artist, artists);

        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        ListView listView;
        listView = (ListView) rootView.findViewById(R.id.listview_artists);
        listView.setAdapter(artistAdapter);

        /* If user touches/clicks an entry TrackActivity is started with
         * "artist_id" and "artist_name" */
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getActivity(), TrackActivity.class);
                intent.putExtra(INTENT_ARTIST_ID, artistAdapter.getItem(i).id);
                intent.putExtra(INTENT_ARTIST_NAME, artistAdapter.getItem(i).name);
                startActivity(intent);
            }
        });


        EditText searchEdit;
        searchEdit = (EditText) rootView.findViewById(R.id.search_artist);

        searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    searchArtist = v.getText().toString();

                    /* Save the search string as preferences, so it will be avaible on comming back from
                     * TrackActivity*/
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(LAST_SEARCH_STRING_KEY, searchArtist);
                    editor.commit();

                    /* Start search*/
                    updateArtists(searchArtist);

                    /* Hide soft keyboard */
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);

                    handled = true;
                }
                return handled;
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Hide soft keyboard on start*/
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onStart() {
        super.onStart();
        /* if search_artist is empty then check if there is a LAST_SEARCH_STRING_KEY in the preferences*/
        if (searchArtist != null && !searchArtist.isEmpty()) {
            updateArtists(searchArtist);
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            searchArtist = preferences.getString(LAST_SEARCH_STRING_KEY, "");
            if (searchArtist != null && !searchArtist.isEmpty()) {
                updateArtists(searchArtist);
            }
        }

        ((EditText) getActivity().findViewById(R.id.search_artist)).setText(searchArtist);
    }

    /* Start async task for searching*/
    public void updateArtists(String artist) {
        FetchArtistDataFromSpotify artistTask = new FetchArtistDataFromSpotify();
        artistTask.execute(artist);
    }

    /**
     * A custom ArrayAdapter accepting spotify.Artist
     */
    public class ArtistAdapter extends ArrayAdapter<Artist> {

        public ArtistAdapter(Context context, int textViewResourceId, ArrayList<Artist> items) {
            super(context, textViewResourceId, items);
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            Artist artist = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, parent, false);
            }

            TextView artistName = (TextView) convertView.findViewById(R.id.artist_name);
            ImageView artistCover = (ImageView) convertView.findViewById(R.id.cover_image);

            artistName.setText(artist.name);

            /* If no cover image cover exists imageUrl is empty, in this case Glide uses automatically
            * an error image in this case no_image*/
            String imageUrl = "";
            if (artist.images.size() > 0)
                imageUrl = artist.images.get(0).url;

            /* Loading artist image using centerCorp to have the same format for every cover*/
            Glide.with(getActivity())
                    .load(imageUrl)
                    .centerCrop()
                    .crossFade()
                    .error(R.drawable.no_image)
                    .into(artistCover);

            return convertView;
        }
    }

    /* Async function for fetching artist data from spotiyf return type doInBackground:ArtistPager*/
    private class FetchArtistDataFromSpotify extends AsyncTask<String, Void, ArtistsPager> {

        String searchString;

        protected ArtistsPager doInBackground(String... artist) {
            try {

                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                searchString = artist[0];

                return spotify.searchArtists(artist[0]);
            } catch (Exception e) {
                Log.e("FetchArtistData", "Error ", e);
                return null;
            }
        }

        /* put the result of doInBackground into artistAdapter*/
        @Override
        protected void onPostExecute(ArtistsPager strings) {
            super.onPostExecute(strings);
            artistAdapter.clear();

            for (Iterator<Artist> i = strings.artists.items.iterator(); i.hasNext(); ) {
                Artist item = i.next();
                artistAdapter.add(item);
            }

            String resultText;
            if (strings.artists.items.size() > 0)
                resultText = getString(R.string.found) + " " + Integer.toString(strings.artists.items.size()) + " "
                        + getString(R.string.entries_for) + " '" + searchString + "'";
            else
                resultText = getString(R.string.no_result) + " '" + searchString + "'";

            Toast.makeText(getActivity().getApplicationContext(), resultText, Toast.LENGTH_SHORT).show();
        }
    }
}

