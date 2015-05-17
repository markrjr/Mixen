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
import kaaes.spotify.webapi.android.models.TrackSimple;

public class SongQueueFrag extends Fragment implements View.OnClickListener {

    public ListView queueLV;
    private FloatingActionButton addSongButton;
    private static TextView infoTV;
    public RelativeLayout baseLayout;
    private Intent addSong;
    private static ArrayAdapter queueAdapter;
    public static final int ADD_SONG_REQUEST = 5;
    public boolean snackBarIsVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_song_queue, container, false);

        baseLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout);
        queueLV = (ListView) v.findViewById(R.id.queueLV);
        addSongButton = (FloatingActionButton) v.findViewById(R.id.fab);
        infoTV = (TextView) v.findViewById(R.id.infoTV);

        addSongButton.attachToListView(queueLV);
        addSongButton.setOnClickListener(this);

        addSong = new Intent(getActivity(), SearchSongs.class);

        setupQueueAdapter();

        return baseLayout;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(queueAdapter == null && Mixen.isHost)
        {
            setupQueueAdapter();
        }

        updateHostQueueUI();
    }

    public void updateHostQueueUI() {

        queueAdapter.notifyDataSetChanged();

        if (MixenPlayerService.instance.queueIsEmpty()) {

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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUser && snackBarIsVisible) {
            SnackbarManager.dismiss();
        }

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(Mixen.isHost)
        {
            updateHostQueueUI();
        }

    }

    private void setupQueueAdapter() {

        if(MixenPlayerService.instance == null)
        {
            return;
        }

        queueAdapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_2, android.R.id.text1, MixenPlayerService.instance.spotifyQueue) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(MixenPlayerService.instance.spotifyQueue.get(position).name);
                text2.setText(MixenPlayerService.instance.spotifyQueue.get(position).artists.get(0).name);
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
                                    final int position, long id) {

                // ListView Clicked item value
                final TrackSimple selected = (TrackSimple) queueLV.getItemAtPosition(position);

                if(!snackBarIsVisible)
                {
                    snackBarIsVisible = true;
                    addSongButton.setVisibility(View.INVISIBLE);

                    SnackbarManager.show(
                            Snackbar.with(getActivity().getApplicationContext())
                                    .text("Selected: " + selected.name)
                                    .actionColor(Color.YELLOW)
                                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                                    .actionLabel("Remove")
                                    .actionListener(new ActionClickListener() {
                                        @Override
                                        public void onActionClicked(Snackbar snackbar) {

                                            MixenPlayerService.instance.spotifyQueue.remove(position);
                                            updateHostQueueUI();

                                            if(MixenPlayerService.instance.currentTrack.id.equals(selected.id))
                                            {
                                                //If someone wants to delete the currently playing song, stop everything.
                                                MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.reset);
                                                Log.d(Mixen.TAG, "Current song was deleted.");

                                                if(MixenPlayerService.instance.getNextTrack() != null || !MixenPlayerService.instance.queueIsEmpty())
                                                {
                                                    if(MixenPlayerService.instance.queueHasASingleTrack())
                                                    {
                                                        MixenPlayerService.instance.queueSongPosition = 0;
                                                    }
                                                    //We use preparePlayback here because we don't need to modify the counter, because the ArrayList will move around the counter.
                                                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.preparePlayback);
                                                }

                                            }
                                            else if(position < MixenPlayerService.instance.queueSongPosition)
                                            {
                                                MixenPlayerService.instance.queueSongPosition--;
                                            }

                                            MixenBase.mixenPlayerFrag.updateUpNext();
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
                                            snackBarIsVisible = false;
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

        if(v.getId() == R.id.fab || v.getId() == R.id.mixenBaseLayout)
        {
            startActivityForResult(addSong, 5);
            addSong.setAction(Intent.ACTION_SEARCH);
            return;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if(requestCode == ADD_SONG_REQUEST)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                if(Mixen.isHost)
                {
                    updateHostQueueUI();
                }
                //We really don't care about the Intent data here, we just need some way to know
                //when the user has come back from searching for songs so that we can update the UI.
            }
        }

    }


}

