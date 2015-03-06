package com.peak.mixen;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.util.ArrayList;

import co.arcs.groove.thresher.Song;


public class SearchSongs extends ActionBarActivity{

    private ProgressBar indeterminateProgress;
    private ListView songsLV;

    public static ArrayList<Song> foundSongs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_songs);

        getSupportActionBar().setTitle(Mixen.username + "'s Mixen.");

        // Boilerplate.
        songsLV = (ListView) findViewById(R.id.songsLV);
        indeterminateProgress = (ProgressBar)findViewById(R.id.progressBar);
        indeterminateProgress.setVisibility(View.GONE);

        indeterminateProgress.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);

        this.setResult(Activity.RESULT_OK);
        }

    private void handleIntent(Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {

            String query = intent.getStringExtra(SearchManager.QUERY);

            if(query.length() != 0 && query.matches("^[a-zA-Z0-9 ]*$"))
            {
                indeterminateProgress.setVisibility(View.VISIBLE);


                GrooveSharkRequests.findSong(query, new SimpleCallback() {
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
        }
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

                SnackbarManager.show(
                        Snackbar.with(getApplicationContext())
                                .text("Added " + selected.getName())
                                .actionLabel("Undo")
                                .actionColor(Color.YELLOW)
                                .actionListener(new ActionClickListener() {
                                    @Override
                                    public void onActionClicked(Snackbar snackbar) {
                                        MixenPlayerService.queuedSongs.remove(MixenPlayerService.queuedSongs.indexOf(selected));
                                    }
                                })
                        , SearchSongs.this);
            }

        });

        //Log.d(Mixen.TAG, "Updating Queue");
    }

    public void addSongToQueue(Song song)
    {
        if(MixenPlayerService.queuedSongs.isEmpty())
        {
            Log.i(Mixen.TAG, "First song added to queue.");
            MixenPlayerService.queuedSongs.add(song);
            MixenPlayerService.currentSongAsInt = 0;
            MixenPlayerService.currentSong = MixenPlayerService.queuedSongs.get(MixenPlayerService.currentSongAsInt);
            MixenPlayerFrag.preparePlayback();
            return;
        }
        else
        {
            MixenPlayerService.queuedSongs.add(song);

            if(MixenPlayerFrag.upNextTV.getText().equals(""))
            {
                MixenPlayerFrag.upNextTV.setText(MixenPlayerFrag.getNextTrack().getName());
            }

            if(!MixenPlayerService.playerIsPlaying() && !MixenPlayerFrag.queueHasNextTrack())
            {
                //If songs are in the queue, but have completed playback and a new one is suddenly added.
                MixenPlayerService.currentSongAsInt++;
                MixenPlayerService.currentSong = MixenPlayerService.queuedSongs.get(MixenPlayerService.currentSongAsInt);
                MixenPlayerFrag.preparePlayback();
            }

        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

        searchView.setOnCloseListener(new android.support.v7.widget.SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {

                SearchSongs.this.finish();

                return false;
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
