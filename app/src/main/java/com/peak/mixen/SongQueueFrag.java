package com.peak.mixen;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
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
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.github.clans.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.SalutP2P;

import java.util.HashMap;

import kaaes.spotify.webapi.android.models.TrackSimple;

public class SongQueueFrag extends Fragment implements View.OnClickListener {

    public static final int ADD_SONG_REQUEST = 5;
    public ListView queueLV;
    public RelativeLayout baseLayout;
    public boolean snackBarIsVisible = false;
    private FloatingActionButton addSongButton;
    private FloatingActionButton networkBtn;
    private MaterialDialog findingMixensProgress;
    private MaterialDialog cleanUpDialog;
    private MaterialDialog wiFiFailureDiag;
    private MaterialDialog foundMixensDialog;
    private Intent addSong;
    private Drawable liveDrawable;
    private Drawable notLiveDrawable;
    private static TextView infoTV;
    private static ArrayAdapter queueAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_song_queue, container, false);

        baseLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout);
        queueLV = (ListView) v.findViewById(R.id.queueLV);
        addSongButton = (FloatingActionButton) v.findViewById(R.id.addSongFab);
        networkBtn = (FloatingActionButton) v.findViewById(R.id.goLiveBtn);
        infoTV = (TextView) v.findViewById(R.id.infoTV);

        //addSongButton.attachToListView(queueLV);
        //networkBtn.attachToListView(queueLV);
        addSongButton.setOnClickListener(this);
        networkBtn.setOnClickListener(this);

        addSong = new Intent(getActivity(), SearchSongs.class);
        liveDrawable = getResources().getDrawable(R.drawable.ic_live);
        notLiveDrawable = getResources().getDrawable(R.drawable.ic_not_live);

        setupDiags();

        setupQueueAdapter();

        Intent startingIntent = getActivity().getIntent();

        if(startingIntent != null && startingIntent.getExtras().getBoolean("FIND"))
        {
            setupMixenNetwork();
        }

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

    private void setupDiags() {

        findingMixensProgress = new MaterialDialog.Builder(getActivity())
                .title("Searching for nearby Mixens...")
                .content("Please wait...")
                .theme(Theme.DARK)
                .progress(true, 0)
                .build();

        cleanUpDialog = new MaterialDialog.Builder(getActivity())
                .title("Bummer :(")
                .content(R.string.discover_p2p_error)
                .neutralText("Okay")
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        SongQueueFrag.this.getActivity().finish();
                    }
                })
                .build();

        wiFiFailureDiag = new MaterialDialog.Builder(getActivity())
                .title("Bummer :(")
                .content("We had trouble checking if WiFi was on, please double check your settings.")
                .neutralText("Okay")
                .build();

    }

    private void findMixen()
    {
        findingMixensProgress.show();

        Mixen.network.discoverNetworkServicesWithTimeout(new SalutCallback() {
            @Override
            public void call() {
                findingMixensProgress.dismiss();

                if (Mixen.network.foundDevices.size() == 1) {

                    Mixen.network.connectToHostDevice(Mixen.network.foundDevices.get(0), new SalutCallback() {
                        @Override
                        public void call() {
                            Toast.makeText(getActivity(), "You're now connected to " + Mixen.network.foundDevices.get(0).readableName + "'s Mixen.", Toast.LENGTH_LONG).show();
                            networkBtn.setImageDrawable(liveDrawable);
                        }
                    }, new SalutCallback() {
                        @Override
                        public void call() {
                            cleanUpDialog.setContent("We had a problem connection to " + Mixen.network.foundDevices.get(0).readableName + "'s Mixen. Please try again momentarily.");
                            cleanUpDialog.show();
                        }
                    });
                } else {

                    String[] foundNames = Mixen.network.getReadableFoundNames().toArray(new String[Mixen.network.foundDevices.size()]);

                    foundMixensDialog = new MaterialDialog.Builder(getActivity())
                            .title("We found a few people nearby.")
                            .theme(Theme.DARK)
                            .items(foundNames)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, final int i, CharSequence charSequence) {

                                    Mixen.network.connectToHostDevice(Mixen.network.foundDevices.get(i), new SalutCallback() {
                                        @Override
                                        public void call() {
                                            Toast.makeText(getActivity(), "You're now connected to " + Mixen.network.foundDevices.get(i).readableName + "'s Mixen.", Toast.LENGTH_LONG).show();
                                            networkBtn.setImageDrawable(liveDrawable);
                                        }
                                    }, new SalutCallback() {
                                        @Override
                                        public void call() {
                                            cleanUpDialog.setContent("We had a problem connection to " + Mixen.network.foundDevices.get(i).readableName + " 's Mixen. Please try again momentarily.");
                                            cleanUpDialog.show();
                                        }
                                    });
                                }
                            })
                            .build();
                    foundMixensDialog.show();
                }

            }
        }, new SalutCallback() {
            @Override
            public void call() {
                findingMixensProgress.dismiss();
                cleanUpDialog.show();
            }
        }, 3000);
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

    public void setUsername()
    {

        new MaterialDialog.Builder(getActivity())
                .title("Who are you?")
                .content(R.string.createDescript)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.username_hint, R.string.blank_string, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                        if (charSequence.length() != 0 && charSequence.toString().matches("^[a-zA-Z0-9]*$")) {

                            Log.i(Mixen.TAG, "Creating a Mixen service for: " + charSequence.toString());
                            Mixen.username = charSequence.toString();

                            SharedPreferences.Editor prefs = Mixen.sharedPref.edit();
                            prefs.putString("username", Mixen.username).apply();

                            setupMixenNetwork();
                        }
                    }
                })
                .show();

    }

    public void setupMixenNetwork()
    {

        if(Mixen.username == null || Mixen.username.equals("Anonymous"))
        {
            setUsername();
            return;
        }

        if(Mixen.network == null)
        {
            HashMap serviceData = new HashMap<String, String>();

            serviceData.put("SERVICE_NAME", "_mixen");
            serviceData.put("SERVICE_PORT", "" + Mixen.MIXEN_SERVICE_PORT);
            serviceData.put("INSTANCE_NAME", Mixen.username);
            Mixen.network = new SalutP2P(this.getActivity(), serviceData, new SalutCallback() {
                @Override
                public void call() {
                    wiFiFailureDiag.show();
                }
            });
        }

        if(Mixen.isHost)
        {
            if(Mixen.network.serviceIsRunning)
            {
                networkBtn.setImageDrawable(notLiveDrawable);
                Toast.makeText(getActivity(), "We're no longer live.", Toast.LENGTH_SHORT).show();
                Mixen.network.stopNetworkService(false);

            }
            else
            {
                networkBtn.setImageDrawable(liveDrawable);
                Toast.makeText(getActivity(), "We're now live.", Toast.LENGTH_SHORT).show();
                Mixen.network.startNetworkService();

            }
        }
        else
        {
            if(Mixen.network.thisDevice.isRegistered)
            {
                networkBtn.setImageDrawable(notLiveDrawable);
//                    Mixen.network.unregisterClient();

            }
            else
            {
                findMixen();
            }
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

        if(v.getId() == R.id.addSongFab || v.getId() == R.id.mixenBaseLayout)
        {
            startActivityForResult(addSong, 5);
            addSong.setAction(Intent.ACTION_SEARCH);
            return;
        }
        else if(v.getId() == R.id.goLiveBtn)
        {
            setupMixenNetwork();
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

