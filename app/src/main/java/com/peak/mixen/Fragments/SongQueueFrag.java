package com.peak.mixen.Fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.support.annotation.Nullable;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.peak.mixen.MetaTrack;
import com.peak.mixen.Mixen;
import com.peak.mixen.MixenBase;
import com.peak.mixen.MixenPlayerService;
import com.peak.mixen.R;
import com.peak.mixen.SearchSongs;
import com.peak.mixen.Utils.SongQueueListAdapter;
import com.peak.mixen.Utils.SongQueueListItem;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.Salut;
import com.peak.salut.SalutServiceData;

import java.util.ArrayList;

public class SongQueueFrag extends Fragment implements View.OnClickListener {

    public static final int ADD_SONG_REQUEST = 5;
    public ListView queueLV;
    public RelativeLayout baseLayout;
    public boolean snackBarIsVisible = false;
    public ArrayList<SongQueueListItem> cellList;
    private FloatingActionButton addSongBtn;
    private FloatingActionButton networkBtn;
    private MaterialDialog findingMixensProgress;
    private MaterialDialog cleanUpDialog;
    private MaterialDialog wiFiFailureDiag;
    private MaterialDialog foundMixensDialog;
    private Intent addSong;
    private Drawable liveDrawable;
    private Drawable notLiveDrawable;
    private TextView infoTV;
    private ArrayAdapter queueAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_song_queue, container, false);

        baseLayout = (RelativeLayout) v.findViewById(R.id.relativeLayout);
        queueLV = (ListView) v.findViewById(R.id.queueLV);
        addSongBtn = (FloatingActionButton) v.findViewById(R.id.add_song_button);
        networkBtn = (FloatingActionButton) v.findViewById(R.id.go_live_button);
        infoTV = (TextView) v.findViewById(R.id.infoTV);

        addSongBtn.setOnClickListener(this);
        networkBtn.setOnClickListener(this);

        addSong = new Intent(getActivity(), SearchSongs.class);
        liveDrawable = getResources().getDrawable(R.drawable.ic_live);
        notLiveDrawable = getResources().getDrawable(R.drawable.ic_not_live);

        setupDiags();

        cellList = new ArrayList<>();

        if(Mixen.isHost)
        {
            setupQueueAdapter(false);
        }
        else
        {
            setupQueueAdapter(true);
            addSongBtn.setVisibility(View.INVISIBLE);
        }

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
        if(queueAdapter != null)
        {
            updateQueueUI();
        }
    }

    private void setupDiags() {

        findingMixensProgress = new MaterialDialog.Builder(getActivity())
                .title("Searching for nearby Mixens...")
                .content("Please wait...")
                .negativeText("Stop")
                .progress(true, 0)
                .cancelable(false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        if(Mixen.network != null)
                        {
                            Mixen.network.cancelConnecting();
                            Mixen.network.stopServiceDiscovery();
                            SongQueueFrag.this.getActivity().finish();
                        }
                    }
                })
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
                .content("We had trouble setting things up. Try turning WiFi off and then back on again.")
                .neutralText("Okay")
                .build();

    }

    private void findMixen()
    {
        findingMixensProgress.show();

        Mixen.network.discoverNetworkServicesWithTimeout(new SalutCallback() {
            @Override
            public void call() {

                if (Mixen.network.foundDevices.size() == 1) {

                    findingMixensProgress.setTitle("Attempting to connect...");
                    findingMixensProgress.setContent("Trying to connect to " + Mixen.network.getReadableFoundNames().get(0) + "'s Mixen...");

                    Mixen.network.registerWithHost(Mixen.network.foundDevices.get(0), new SalutCallback() {
                        @Override
                        public void call() {
                            Toast.makeText(Mixen.currentContext, "You're now connected to " + Mixen.network.foundDevices.get(0).readableName + "'s Mixen.", Toast.LENGTH_LONG).show();
                            networkBtn.setImageDrawable(liveDrawable);
                            addSongBtn.setVisibility(View.VISIBLE);
                            findingMixensProgress.dismiss();

                        }
                    }, new SalutCallback() {
                        @Override
                        public void call() {
                            findingMixensProgress.dismiss();

                            findingMixensProgress.setTitle("Searching for nearby Mixens...");
                            findingMixensProgress.setContent("Please wait...");
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

                                    Mixen.network.registerWithHost(Mixen.network.foundDevices.get(i), new SalutCallback() {
                                        @Override
                                        public void call() {
                                            Toast.makeText(getActivity(), "You're now connected to " + Mixen.network.foundDevices.get(i).readableName + "'s Mixen.", Toast.LENGTH_LONG).show();
                                            networkBtn.setImageDrawable(liveDrawable);
                                            addSongBtn.setVisibility(View.VISIBLE);
                                            ;
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

                    findingMixensProgress.dismiss();
                    foundMixensDialog.show();
                }

            }
        }, new SalutCallback() {
            @Override
            public void call() {
                findingMixensProgress.dismiss();
                cleanUpDialog.show();
            }
        }, 5000);
    }

    public void updateQueueUI() {

        MixenBase.songQueueFrag.cellList.clear();
        MixenBase.songQueueFrag.cellList.addAll(SongQueueListAdapter.convertToListItems(MixenPlayerService.instance.metaQueue));

        queueAdapter.notifyDataSetChanged();
        queueLV.invalidate();

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
                            prefs.commit();

                            setupMixenNetwork();
                        }
                    }
                })
                .show();

    }

    public void setupMixenNetwork()
    {
        if(!Salut.isWiFiEnabled(getActivity()))
        {
            new MaterialDialog.Builder(getActivity())
                    .title("Enabling WiFi...")
                    .content("Mixen needs to turn on WiFi in order to communicate with other devices. ")
                    .positiveText("Okay")
                    .negativeText("Nevermind")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Salut.enableWiFi(getActivity());
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setupMixenNetwork();
                                }
                            }, 1500);
                        }
                    })
                    .show();
            return;
        }

        if(Mixen.username == null || Mixen.username.equals("Anonymous"))
        {
            setUsername();
            return;
        }

        if(Mixen.network == null)
        {
            SalutDataReceiver dataReceiver = new SalutDataReceiver(getActivity(), MixenPlayerService.instance);
            SalutServiceData serviceData = new SalutServiceData("mixen", Mixen.MIXEN_SERVICE_PORT, Mixen.username);

            Mixen.network = new Salut(dataReceiver, serviceData, new SalutCallback() {
                @Override
                public void call() {
                    wiFiFailureDiag.show();
                }
            });
        }

        if(Mixen.isHost)
        {
            if(Mixen.network.isRunningAsHost)
            {
                networkBtn.setImageDrawable(notLiveDrawable);
                Toast.makeText(getActivity(), "We're no longer live.", Toast.LENGTH_SHORT).show();
                Mixen.network.stopNetworkService(false);
                setupQueueAdapter(false);

            }
            else
            {
                networkBtn.setImageDrawable(liveDrawable);
                Mixen.network.startNetworkService(new SalutDeviceCallback() {
                    @Override
                    public void call(SalutDevice device) {
                        Toast.makeText(getActivity(), device.readableName + " is now connected.", Toast.LENGTH_SHORT).show();
                        MixenPlayerService.instance.playerServiceSnapshot.updateNetworkPlayer();
                    }
                }, new SalutCallback() {
                    @Override
                    public void call() {
                        wiFiFailureDiag.show();
                        networkBtn.setImageDrawable(notLiveDrawable);
                    }
                });
                setupQueueAdapter(true);
            }
        }
        else
        {
            if(Mixen.network.thisDevice.isRegistered)
            {
                networkBtn.setImageDrawable(notLiveDrawable);
                Toast.makeText(getActivity(), "Disconnected from " + Mixen.network.registeredHost.readableName + " 's Mixen.", Toast.LENGTH_SHORT).show();
                Mixen.network.unregisterClient(null);
                this.getActivity().finish();
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


    private void assignItemClickListener(boolean forNetwork, final int position)
    {
        if(forNetwork)
        {
            final SongQueueListItem selected = (SongQueueListItem) queueLV.getItemAtPosition(position);

            if (!snackBarIsVisible) {
                snackBarIsVisible = true;
                addSongBtn.setVisibility(View.INVISIBLE);
                networkBtn.setVisibility(View.INVISIBLE);

                SnackbarManager.show(
                        Snackbar.with(getActivity().getApplicationContext())
                                .text("Selected: " + selected.metaTrack.name)
                                .actionColor(Color.YELLOW)
                                .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                                .actionLabel("Remove")
                                .actionListener(new ActionClickListener() {
                                    @Override
                                    public void onActionClicked(Snackbar snackbar) {

                                        MixenPlayerService.instance.metaQueue.remove(position);

                                        updateQueueUI();
                                        MixenPlayerService.instance.playerServiceSnapshot.updateNetworkQueue();

                                        if (MixenPlayerService.instance.currentTrack.spotifyID.equals(selected.metaTrack.spotifyID)) {
                                            //If someone wants to delete the currently playing song, stop everything.
                                            MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.reset);
                                            Log.d(Mixen.TAG, "Current song was deleted.");

                                            if (MixenPlayerService.instance.getNextTrack() != null || !MixenPlayerService.instance.queueIsEmpty()) {
                                                if (MixenPlayerService.instance.queueHasASingleTrack()) {
                                                    MixenPlayerService.instance.queueSongPosition = 0;
                                                }
                                                //We use preparePlayback here because we don't need to modify the counter, because the ArrayList will move around the counter.
                                                MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.preparePlayback);
                                            }

                                        } else if (position < MixenPlayerService.instance.queueSongPosition) {
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
                                        addSongBtn.setVisibility(View.VISIBLE);
                                        networkBtn.setVisibility(View.VISIBLE);
                                    }
                                })
                        , getActivity());
            } else {
                SnackbarManager.dismiss();
            }
        }

        else
        {

            final MetaTrack selected = (MetaTrack) queueLV.getItemAtPosition(position);

            if (!snackBarIsVisible) {
                snackBarIsVisible = true;
                addSongBtn.setVisibility(View.INVISIBLE);
                networkBtn.setVisibility(View.INVISIBLE);

                SnackbarManager.show(
                        Snackbar.with(getActivity().getApplicationContext())
                                .text("Selected: " + selected.name)
                                .actionColor(Color.YELLOW)
                                .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                                .actionLabel("Remove")
                                .actionListener(new ActionClickListener() {
                                    @Override
                                    public void onActionClicked(Snackbar snackbar) {

                                        MixenPlayerService.instance.metaQueue.remove(position);

                                        updateQueueUI();
                                        MixenPlayerService.instance.playerServiceSnapshot.updateNetworkQueue();

                                        if (MixenPlayerService.instance.currentTrack.spotifyID.equals(selected.spotifyID)) {
                                            //If someone wants to delete the currently playing song, stop everything.
                                            MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.reset);
                                            Log.d(Mixen.TAG, "Current song was deleted.");

                                            if (MixenPlayerService.instance.getNextTrack() != null || !MixenPlayerService.instance.queueIsEmpty()) {
                                                if (MixenPlayerService.instance.queueHasASingleTrack()) {
                                                    MixenPlayerService.instance.queueSongPosition = 0;
                                                }
                                                //We use preparePlayback here because we don't need to modify the counter, because the ArrayList will move around the counter.
                                                MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.preparePlayback);
                                            }

                                        } else if (position < MixenPlayerService.instance.queueSongPosition) {
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
                                        addSongBtn.setVisibility(View.VISIBLE);
                                        networkBtn.setVisibility(View.VISIBLE);
                                    }
                                })
                        , getActivity());
            } else {
                SnackbarManager.dismiss();
            }
        }
    }


    private void setupQueueAdapter(final boolean forNetwork) {

        if(forNetwork)
        {
            queueAdapter = new SongQueueListAdapter(getActivity(), cellList);
        }
        else
        {
            queueAdapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_2, android.R.id.text1, MixenPlayerService.instance.metaQueue) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    //view.setBackground(getResources().getDrawable(R.drawable.song_queue_item_background));

                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                    text1.setText(MixenPlayerService.instance.metaQueue.get(position).name);
                    text2.setText(MixenPlayerService.instance.metaQueue.get(position).artist);
                    return view;
                }
            };
        }

        // Assign adapter to ListView
        queueLV.setAdapter(queueAdapter);

        if(Mixen.isHost)
        {
            // ListView Item Click Listener
            queueLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        final int position, long id) {

                    // ListView Clicked item value
                    assignItemClickListener(forNetwork, position);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.add_song_button || v.getId() == R.id.mixenBaseLayout)
        {
            startActivityForResult(addSong, 5);
            addSong.setAction(Intent.ACTION_SEARCH);
            return;
        }
        else if(v.getId() == R.id.go_live_button)
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
                updateQueueUI();
                //We really don't care about the Intent data here, we just need some way to know
                //when the user has come back from searching for songs so that we can update the UI.
            }
        }

    }


}

