package com.peak.mixen;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.util.ArrayList;

import co.arcs.groove.thresher.Song;


public class SearchSongs extends ActionBarActivity{

    private ProgressBar indeterminateProgress;
    private ListView songsLV;
    private boolean queryPending = false;
    private querySongs findSong;


    public static SearchSongs instance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_songs);

        getSupportActionBar().setTitle(Mixen.username + "'s Mixen");

        // Boilerplate.
        songsLV = (ListView) findViewById(R.id.songsLV);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);
        indeterminateProgress.setVisibility(View.GONE);

        indeterminateProgress.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);

        this.setResult(Activity.RESULT_OK);

        instance = this;

    }

    private void handleIntent(Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {

            if(queryPending)
            {
                findSong.cancel(true);
                queryPending = false;
            }

            String query = intent.getStringExtra(SearchManager.QUERY);

            if(query.length() != 0 && query.matches("^[a-zA-Z0-9 ]*$"))
            {
                queryPending = true;
                indeterminateProgress.setVisibility(View.VISIBLE);
                songsLV.setVisibility(View.INVISIBLE);
                findSong = new querySongs(query);
                findSong.execute();

            }
            else
            {
                SnackbarManager.show(Snackbar.with(this).text("Please use only letters and numbers in your query."));
            }
        }
    }

    public void postHandleSearchTask(ArrayList foundSongs, Integer requestStatus)
    {
        queryPending = false;
        indeterminateProgress.setVisibility(View.GONE);


        if(requestStatus.intValue() == querySongs.REQUEST_FAILED)
        {
            new MaterialDialog.Builder(this)
                    .title("Bummer :(")
                    .content(R.string.generic_network_error)
                    .neutralText("Okay")
                    .show();

            return;
        }
        else if(foundSongs.isEmpty())
        {
            new MaterialDialog.Builder(this)
                    .title("Bummer :(")
                    .content(R.string.song_not_found)
                    .neutralText("Okay")
                    .show();

            return;
        }

        populateListView(foundSongs);
        songsLV.setVisibility(View.VISIBLE);


    }

    private void populateListView(final ArrayList<Song> listOfSongs)
    {
        ArrayAdapter adapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_2, android.R.id.text1, listOfSongs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(listOfSongs.get(position).getName());
                text2.setText(listOfSongs.get(position).getArtistName());
                //text1.setTextSize(24);
                //text2.setTextSize(18);
                return view;
            }
        };


        // Assign adapter to ListView
        songsLV.setAdapter(adapter);

        // ListView Item Click Listener
        songsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {

                // ListView Clicked item value
                final Song selected = (Song) songsLV.getItemAtPosition(position);

                addSongToQueue(selected);
                //TODO Fix undo action flow.

                SnackbarManager.show(
                        Snackbar.with(getApplicationContext())
                                .text("Added " + selected.getName())
                        //.actionLabel("Undo")
                        //.actionColor(Color.YELLOW)
                        , SearchSongs.this);
            }

        });

        //Log.d(Mixen.TAG, "Updating Queue");
    }


    public void addSongToQueue(Song song)
    {
        if(MixenPlayerService.instance.queuedSongs.isEmpty())
        {
            Log.i(Mixen.TAG, "First song added to queue.");
            MixenPlayerService.instance.queuedSongs.add(song);
            MixenPlayerService.instance.currentSongAsInt = 0;
            MixenPlayerService.instance.currentSong = MixenPlayerService.instance.queuedSongs.get(MixenPlayerService.instance.currentSongAsInt);
            MixenPlayerService.doAction(getApplicationContext(), MixenPlayerService.getSongStreamURL);
        }
        else
        {
            MixenPlayerService.instance.queuedSongs.add(song);

            if(MixenBase.mixenPlayerFrag.upNextTV.getText().equals("") && MixenPlayerService.instance.getNextTrack() != null)
            {
                MixenBase.mixenPlayerFrag.upNextTV.setText("Next: \n" + MixenPlayerService.instance.getNextTrack().getName());
            }

            if(MixenPlayerService.instance.isRunning && !MixenPlayerService.instance.serviceIsBusy && !MixenPlayerService.instance.playerHasTrack && !MixenPlayerService.instance.playerIsPlaying())
            {
                //Condense if to playerIsReady?
                Log.d(Mixen.TAG, "This is being executed also.");
                //If songs are in the queue, but have completed playback and a new one is suddenly added.
                MixenPlayerService.instance.currentSongAsInt++;
                MixenPlayerService.instance.currentSong = MixenPlayerService.instance.queuedSongs.get(MixenPlayerService.instance.currentSongAsInt);
                MixenPlayerService.doAction(getApplicationContext(), MixenPlayerService.getSongStreamURL);
            }

        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_songs, menu);


        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        android.support.v7.widget.SearchView searchView =
                (android.support.v7.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        menu.findItem(R.id.search).expandActionView();

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(!hasFocus)
                {
                    SearchSongs.this.finish();
                }
            }
        });

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }


}
