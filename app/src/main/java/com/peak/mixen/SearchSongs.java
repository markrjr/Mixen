package com.peak.mixen;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import co.arcs.groove.thresher.Song;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SearchSongs extends Activity{

    private querySongs searchSongs;
    private Handler queryHandler;
    private ProgressBar indeterminateProgress;
    private EditText searchTermsET;
    private ListView songsLV;

    public static ArrayList<Song> foundSongs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_songs);

        getActionBar().hide();

        // Boilerplate.
        songsLV = (ListView) findViewById(R.id.songsLV);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);
        searchTermsET = (EditText)findViewById(R.id.searchTermsET);


        setupListListener();

        indeterminateProgress.setVisibility(View.GONE);
        return;

    }


    public void setupListListener()
    {
        searchTermsET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
                //This will handle tapping the "Done" or "Enter" button on the keyboard after entering text.
                songsLV.setVisibility(View.GONE);

                boolean handled = false;
                if(actionId == EditorInfo.IME_ACTION_DONE);
                {
                    InputMethodManager inputManager = (InputMethodManager) SearchSongs.this.getSystemService(Context.INPUT_METHOD_SERVICE); // All this ridiculousness to hide the keyboard so that the user can see if an error has occurred in text validation.
                    inputManager.hideSoftInputFromWindow(SearchSongs.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                    String searchTerms = searchTermsET.getText().toString();

                    if(searchTermsET.getText().length() != 0 && searchTerms.matches("^[a-zA-Z0-9 ]*$"))
                    {
                        indeterminateProgress.setVisibility(View.VISIBLE);

                        GrooveSharkRequests.findSong(searchTermsET.getText().toString(), new SimpleCallback() {
                            @Override
                            public void call() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        postHandleSearchTask();
                                    }
                                });
                            }
                        });

                    }

                    //We handled the input event.
                    handled = true;

                }

                //In the actual function body.
                return handled;
            }
        });
    }

    public void postHandleSearchTask()
    {

        indeterminateProgress.setVisibility(View.GONE);

        if (GrooveSharkRequests.searchResultCode != 99)
        {
            songsLV.setVisibility(View.GONE);
            Intent provideErrorInfo = new Intent(SearchSongs.this, MoreInfo.class); //Totally lazy, but really easy.
            provideErrorInfo.putExtra("START_REASON", GrooveSharkRequests.searchResultCode);
            startActivity(provideErrorInfo);
        }
        else
        {
            populateListView(foundSongs);
            songsLV.setVisibility(View.VISIBLE);

        }



    }

    public void populateListView(ArrayList<Song> listOfSongs)
    {
        String[] songNames = new String[listOfSongs.size()];
        String[] songArtists = new String[listOfSongs.size()];
        int names = 0;
        int artists = 0;

        for(Song song : listOfSongs)
        {

            songNames[names] = song.getName();
            songArtists[artists] = song.getArtistName();
            names++;
            artists++;
        }



        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, songNames);





        // Assign adapter to ListView
        songsLV.setAdapter(adapter);

        // ListView Item Click Listener
        songsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item value
                String userSelection = (String) songsLV.getItemAtPosition(position);

                Log.i(Mixen.TAG, "Adding " + userSelection + " to song queue.");

                addSongToQueue(userSelection);


            }

        });


    }


    public void addSongToQueue(String songName)
    {
        boolean firstSong = false;

        if(Mixen.queuedSongs.isEmpty())
        {
            firstSong = true;
            Log.i(Mixen.TAG, "First song added to queue.");
        }

        for(Song song : foundSongs)
        {
            if(song.getName() != null && song.getName().equals(songName))
            {
                Mixen.queuedSongs.add(song);
                break;
            }

        }

        if(firstSong)
        {
            Mixen.currentSongAsInt = 0;
            Mixen.currentSong = Mixen.queuedSongs.get(Mixen.currentSongAsInt);
            MixenPlayer.preparePlayback();

        }

        Log.i(Mixen.TAG, "Queue contains " + Mixen.queuedSongs.size() + " songs.");

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mixen_stage, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.exit_app) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }


}
