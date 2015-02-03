package com.peak.mixen;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;


public class SongQueue extends Activity {

    private ListView queueLV;
    private FloatingActionButton fab;
    private TextView infoTV;
    private RelativeLayout relativeLayout;
    private Intent addSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_queue);

        getActionBar().setTitle(" " + Mixen.username + " 's Mixen");

        queueLV = (ListView)findViewById(R.id.queueLV);
        fab = (FloatingActionButton)findViewById(R.id.fab);
        infoTV = (TextView)findViewById(R.id.infoTV);
        relativeLayout = (RelativeLayout)findViewById(R.id.relativeLayout);

        relativeLayout.setBackgroundColor(getResources().getColor(R.color.Jacksons_Purple));

        fab.attachToListView(queueLV);

        MixenPlayer.grooveSharkSession = new Client();
        //checkForSongsInQueue();


    }

    public void onBtnClicked(View v)
    {
        switch (v.getId())
        {
            case R.id.fab:
            {
                addSong();
                return;
            }
        }
    }

    private void addSong()
    {
        addSong = new Intent(this, SearchSongs.class);
        startActivity(addSong);

    }

    @Override
    protected void onResume() {

        checkForSongsInQueue();

        super.onResume();
    }

    private void checkForSongsInQueue()
    {
        if(!MixenPlayer.playerHasTrack())
        {
            infoTV.setVisibility(View.VISIBLE);
            fab.setVisibility(View.VISIBLE);
        }
        else
        {
            populateListView(Mixen.queuedSongs);
            infoTV.setVisibility(View.GONE);
            queueLV.setVisibility(View.VISIBLE);
            relativeLayout.setBackgroundColor(getResources().getColor(R.color.San_Marino));

        }

    }


    private void populateListView(final ArrayList<Song> listOfSongs)
    {

        final String[] songNames = new String[listOfSongs.size()];
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



        ArrayAdapter adapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_2, android.R.id.text1, listOfSongs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(listOfSongs.get(position).getName());
                text2.setText(listOfSongs.get(position).getArtistName());
                text1.setTextSize(24);
                text2.setTextSize(18);
                return view;
            }
        };





        // Assign adapter to ListView
        queueLV.setAdapter(adapter);

        // ListView Item Click Listener
        queueLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item value
                Song selected = (Song) queueLV.getItemAtPosition(position);


                Log.i(Mixen.TAG, "User selected: " + selected.getName());

                if (position > Mixen.currentSongAsInt)
                {
                    MixenPlayer.mixenStreamer.reset();
                    Mixen.currentSong = selected;
                    Mixen.currentSongAsInt = position;
                    Mixen.currentAlbumArt = Mixen.COVER_ART_URL + selected.getCoverArtFilename();
                    MixenPlayer.preparePlayback();
                    Log.d(Mixen.TAG, "Switching songs to " + selected.getName());
                }

                //addSongToQueue(userSelection);


            }

        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_song_queue, menu);
        return true;
    }


}
