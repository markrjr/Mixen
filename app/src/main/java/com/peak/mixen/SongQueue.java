package com.peak.mixen;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.peak.salut.Salut;
import com.peak.salut.SalutCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.Song;


public class SongQueue extends Activity {

    private ListView queueLV;
    private FloatingActionButton addSongButton;
    private FloatingActionButton musicPlayerButton;
    private TextView infoTV;
    private RelativeLayout relativeLayout;
    private Intent addSong;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_queue);

        getActionBar().setTitle(" " + Mixen.username + " 's Mixen"); //Will have been set by create Mixen or from network discovery.

        queueLV = (ListView)findViewById(R.id.queueLV);
        addSongButton = (FloatingActionButton)findViewById(R.id.fab);
        musicPlayerButton = (FloatingActionButton)findViewById(R.id.musicPlayerButton);
        infoTV = (TextView)findViewById(R.id.infoTV);
        relativeLayout = (RelativeLayout)findViewById(R.id.relativeLayout);

        relativeLayout.setBackgroundColor(getResources().getColor(R.color.Jacksons_Purple));

        addSongButton.attachToListView(queueLV);

        //setupMixenNetwork();

        MixenPlayer.player = new MediaPlayer();

        MixenPlayer.player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        MixenPlayer.player.setLooping(false);


        if(Mixen.queuedSongs == null)
        {
            MixenPlayer.grooveSharkSession = new Client();
            Mixen.queuedSongs = new ArrayList<Song>();
            Mixen.proposedSongs = new ArrayList<Song>();

            Mixen.currentContext = this.getApplicationContext();
        }

    }

    public void setupMixenNetwork()
    {
        Map appData = new HashMap();
        appData.put("username", Mixen.username);
        appData.put("isHost", "TRUE");


        Mixen.network = new Salut(getApplicationContext(), "_mixen", appData);

        if(Mixen.isHost && !Mixen.network.serviceIsRunning)
        {


            Mixen.network.startNetworkService();
        }
        else
        {
            //Pass
        }

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

            case R.id.musicPlayerButton:
            {
                Intent musicPlayer = new Intent(this, MixenPlayer.class);
                //musicPlayer.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(musicPlayer);
                this.finish();
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
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(Mixen.network.receiver);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //registerReceiver(Mixen.network.receiver, Mixen.network.intentFilter);
        checkForSongsInQueue();

    }


    private void checkForSongsInQueue()
    {
        if(!MixenPlayer.playerHasTrack())
        {
            infoTV.setVisibility(View.VISIBLE);
            addSongButton.setVisibility(View.VISIBLE);
        }
        else
        {
            populateListView(Mixen.queuedSongs);
            infoTV.setVisibility(View.GONE);
            queueLV.setVisibility(View.VISIBLE);
            relativeLayout.setBackgroundColor(getResources().getColor(R.color.San_Marino));

        }

    }

    public void populateNetworkListView()
    {
        final String[] deviceNames;

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
                    MixenPlayer.player.reset();
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
        getMenuInflater().inflate(R.menu.mixen_stage, menu);

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!MixenPlayer.isRunning)
        {
            startActivity(new Intent(this, MixenPlayer.class));
        }
    }
}
