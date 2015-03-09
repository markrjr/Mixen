package com.peak.mixen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.support.annotation.Nullable;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import co.arcs.groove.thresher.Song;


public class SongQueueFrag extends Fragment implements View.OnClickListener {

    private static ListView queueLV;
    private FloatingActionButton addSongButton;
    private static TextView infoTV;
    private RelativeLayout relativeLayout;
    private Intent addSong;
    private static ArrayAdapter queueAdapter;
    public static final int ADD_SONG_REQUEST = 5;
    public static boolean snackBarVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_song_queue, container, false);

        relativeLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout);
        queueLV = (ListView) v.findViewById(R.id.queueLV);
        addSongButton = (FloatingActionButton) v.findViewById(R.id.fab);
        infoTV = (TextView) v.findViewById(R.id.infoTV);

        addSongButton.attachToListView(queueLV);
        addSongButton.setOnClickListener(this);

        addSong = new Intent(getActivity(), SearchSongs.class);

        setupQueueAdapter();

        return relativeLayout;
    }

    public static void updateQueueUI() {

        queueAdapter.notifyDataSetChanged();

        if (!MixenPlayerFrag.playerHasTrack()) {

            infoTV.setVisibility(View.VISIBLE);
            queueLV.setVisibility(View.GONE);
        }
        else
        {
            infoTV.setVisibility(View.GONE);
            queueLV.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateQueueUI();
    }

    private void setupQueueAdapter() {


        queueAdapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_2, android.R.id.text1, MixenPlayerService.queuedSongs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(MixenPlayerService.queuedSongs.get(position).getName());
                text2.setText(MixenPlayerService.queuedSongs.get(position).getArtistName());
                //text1.setTextSize(24);
                //text2.setTextSize(18);
                return view;
            }
        };

        // Assign adapter to ListView
        queueLV.setAdapter(queueAdapter);

        // ListView Item Click Listener
        queueLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item value
                final Song selected = (Song) queueLV.getItemAtPosition(position);

                if(!snackBarVisible)
                {
                    snackBarVisible = true;
                    addSongButton.setVisibility(View.INVISIBLE);

                    SnackbarManager.show(
                            Snackbar.with(getActivity().getApplicationContext())
                                    .text("SELECTED: " + selected.getName())
                                    .actionColor(Color.YELLOW)
                                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                                    .actionLabel("Remove")
                                    .actionListener(new ActionClickListener() {
                                        @Override
                                        public void onActionClicked(Snackbar snackbar) {

                                            boolean currentSongWasDeleted = false;

                                            if(MixenPlayerService.currentSong == selected)
                                            {
                                                if(!MixenPlayerService.queuedSongs.isEmpty())
                                                {
                                                    MixenPlayerService.currentSongAsInt = 0;
                                                }

                                                //Or, someone wants to delete the current playing song.
                                                MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.reset);
                                                currentSongWasDeleted = true;
                                                Log.d(Mixen.TAG, "Current song was deleted.");
                                            }

                                            MixenPlayerService.queuedSongs.remove(MixenPlayerService.queuedSongs.indexOf(selected));
                                            updateQueueUI();

                                            if(currentSongWasDeleted && !MixenPlayerService.queuedSongs.isEmpty())
                                            {
                                                MixenPlayerService.previousAlbumArtURL = MixenPlayerService.currentAlbumArtURL;
                                                MixenPlayerService.currentSong = MixenPlayerService.queuedSongs.get(MixenPlayerService.currentSongAsInt);
                                                MixenPlayerFrag.preparePlayback();
                                            }

                                        }
                                    })
                                    .eventListener(new EventListener() {
                                        @Override
                                        public void onShow(Snackbar snackbar) {

                                        }

                                        @Override
                                        public void onShowByReplace(Snackbar snackbar) {

                                        }

                                        @Override
                                        public void onShown(Snackbar snackbar) {

                                        }

                                        @Override
                                        public void onDismiss(Snackbar snackbar) {
                                        }

                                        @Override
                                        public void onDismissByReplace(Snackbar snackbar) {

                                        }

                                        @Override
                                        public void onDismissed(Snackbar snackbar) {
                                            snackBarVisible = false;
                                            addSongButton.setVisibility(View.VISIBLE);
                                        }
                                    })
                            , getActivity());
                }
                else
                {
                    SnackbarManager.dismiss();
                }

            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.fab:
            {
                startActivityForResult(addSong, 5);
                addSong.setAction(Intent.ACTION_SEARCH);
                return;
            }

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if(requestCode == ADD_SONG_REQUEST)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                updateQueueUI();
                //We really don't care about the Intent data here, we just need some way to know
                //when the user has come back from searching for songs so that we can update the UI.
            }
        }

    }


}

