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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import co.arcs.groove.thresher.Song;


public class SearchSongs extends Activity {

    private queryPopSongs searchPopSongs;
    private querySongs searchSongs;
    private Handler queryHandler;
    private ProgressBar indeterminateProgress;
    private EditText searchTermsET;
    private ActionBar actionbar;
    private ListView songsLV;

    public static ArrayList<Song> foundSongs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_songs);

        actionbar = getActionBar();
        searchPopSongs = new queryPopSongs();
        searchSongs = new querySongs();
        queryHandler = new Handler();

        actionbar.setTitle(R.string.add_song);

        // Get ListView object from xml
        songsLV = (ListView) findViewById(R.id.songsLV);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);
        searchTermsET = (EditText)findViewById(R.id.searchTermsET);

        searchPopSongs.execute();

        indeterminateProgress.setVisibility(View.VISIBLE);

        searchTermsET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {

                //This will handle tapping the "Done" or "Enter" button on the keyboard after entering text.


                indeterminateProgress.setVisibility(View.VISIBLE);
                songsLV.setVisibility(View.GONE);

                boolean handled = false;
                if(actionId == EditorInfo.IME_ACTION_DONE);
                {
                    InputMethodManager inputManager = (InputMethodManager) SearchSongs.this.getSystemService(Context.INPUT_METHOD_SERVICE); // All this ridiculousness to hide the keyboard so that the user can see if an error has occurred in text validation.
                    inputManager.hideSoftInputFromWindow(SearchSongs.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                    if(searchTermsET.getText().length() != 0 && searchTermsET.getText().toString().matches("^[a-zA-Z0-9 ]*$"))
                    {
                        searchSongs = new querySongs();
                        searchSongs.execute(searchTermsET.getText().toString());
                        postHandleSearchTask();
                    }

                    handled = true;

                }

                return handled;
            }
        });


        postHandleSearchTask();


        return;

    }

    public void postHandleSearchTask()
    {

        queryHandler.postDelayed(new Runnable() {

            public void run() {
                if (foundSongs == null)
                {
                    //If searchPopSongs did not return a list of found songs, then some sort of error occurred.
                    Intent provideErrorInfo = new Intent(SearchSongs.this, MoreInfo.class);
                    provideErrorInfo.putExtra("START_REASON", Mixen.GENERIC_NETWORK_ERROR);
                    SearchSongs.this.finish();
                    startActivity(provideErrorInfo);
                }
                else
                {
                    if (foundSongs.size() != 0)
                    {
                        populateListView(foundSongs);
                        songsLV.setVisibility(View.VISIBLE);

                    }
                    else
                    {
                        songsLV.setVisibility(View.GONE);
                        Intent provideErrorInfo = new Intent(SearchSongs.this, MoreInfo.class); //Totally lazy, but really easy.
                        provideErrorInfo.putExtra("START_REASON", Mixen.SONG_NOT_FOUND);
                        startActivity(provideErrorInfo);
                        Log.i(Mixen.TAG, "Nothing could be found for the provided query.");
                    }

                }

                indeterminateProgress.setVisibility(View.GONE);


            }
        }, 2500);

    }

    public void populateListView(ArrayList<Song> listOfSongs)
    {
        String[] values = new String[listOfSongs.size()];
        int count = 0;

        for(Song song : listOfSongs)
        {

            values[count] = song.getName();
            count++;
        }



        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);


        // Assign adapter to ListView
        songsLV.setAdapter(adapter);

        // ListView Item Click Listener
        songsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item value
                String userSelection = (String) songsLV.getItemAtPosition(position);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Adding " + userSelection + " to song queue.", Toast.LENGTH_SHORT)
                        .show();

                Log.i(Mixen.TAG, "User selected: " + userSelection);

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
            MixenPlayer.postHandlePlayback();

        }
        else if(!MixenPlayer.mixenStreamer.isPlaying() && !MixenPlayer.queueHasNextTrack())
        {
            MixenPlayer.preparePlayback();
            MixenPlayer.postHandlePlayback();
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
