package com.peak.mixen.Fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.peak.mixen.Activities.TutorialScreen;
import com.peak.mixen.MetaTrack;
import com.peak.mixen.Mixen;
import com.peak.mixen.Activities.MixenBase;
import com.peak.mixen.Service.MixenPlayerService;
import com.peak.mixen.R;
import com.peak.mixen.Activities.SearchSongs;
import com.peak.mixen.Activities.SettingsScreen;
import com.peak.mixen.Service.PlaybackSnapshot;
import com.peak.mixen.Utils.ActivityAnimator;
import com.peak.mixen.Utils.FABScrollListener;
import com.peak.mixen.Utils.ShowHideOnScroll;
import com.peak.mixen.Utils.SongQueueListAdapter;
import com.peak.mixen.Utils.SongQueueListItem;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;

public class SongQueueFrag extends Fragment implements View.OnClickListener {

    public static final int ADD_SONG_REQUEST = 5;
    public static final int TUTORIAL_REQUEST = 87;
    public ListView queueLV;
    public RelativeLayout baseLayout;
    public ArrayList<SongQueueListItem> cellList;
    public boolean snackBarIsVisible = false;
    private FloatingActionButton addSongBtn;
    private FloatingActionButton settingsBtn;
    private MaterialDialog connectingProgress;
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
        settingsBtn = (FloatingActionButton) v.findViewById(R.id.settings);
        infoTV = (TextView) v.findViewById(R.id.infoTV);

        addSongBtn.setOnClickListener(this);
        settingsBtn.setOnClickListener(this);

        addSong = new Intent(getActivity(), SearchSongs.class);
        liveDrawable = getResources().getDrawable(R.drawable.ic_live);
        notLiveDrawable = getResources().getDrawable(R.drawable.ic_not_live);

        setupDiags();

        cellList = new ArrayList<>();

        FABScrollListener fabScrollListener = new FABScrollListener(queueLV);
        fabScrollListener.addFab(addSongBtn);
        fabScrollListener.addFab(settingsBtn);

        if(Mixen.amoledMode)
        {
            //Set FAB background tint colors to black.
        }

        if(!Mixen.hasSeenTutorial)
        {
            Intent tutorialIntent = new Intent(this.getActivity(), TutorialScreen.class);
            startActivityForResult(tutorialIntent, TUTORIAL_REQUEST);
        }
        else
        {
            setupFragment();
        }

        setupNetwork();

        return baseLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(queueAdapter != null)
        {
            updateQueueUI();
        }
    }

    private void setupNetwork()
    {
        if(Mixen.isHost)
        {
            Mixen.thisUser = new ParseObject("Hosts");
            Mixen.thisUser.put("username", Mixen.username);
            Mixen.thisUser.put("partyCreated", true);
            Mixen.thisUser.put("partySize", 0);
            Mixen.thisUser.put("partyID", RandomStringUtils.randomAlphanumeric(6));
            Mixen.thisUser.saveInBackground();

            new MaterialDialog.Builder(getActivity())
                    .title("Congrats!")
                    .content("You're now hosting your very own Mixen!")
                    .neutralText("Okay")
                    .build()
                    .show();

            infoTV.setText("Others can join with this code:" + Mixen.thisUser.get("partyID"));
        }
        else
        {
            new MaterialDialog.Builder(getActivity())
                    .title("Find a Mixen")
                    .cancelable(false)
                    .autoDismiss(false)
                    .content(R.string.join_hint)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(R.string.blank_string, R.string.blank_string, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                            if (charSequence.length() != 0 && charSequence.toString().matches("^[a-zA-Z0-9]*$")) {

                                ParseQuery<ParseObject> query = ParseQuery.getQuery("Hosts");
                                query.whereEqualTo("partyID", charSequence.toString());
                                query.findInBackground(new FindCallback<ParseObject>() {
                                    public void done(List<ParseObject> possibleHosts, ParseException e) {
                                        if (e == null) {
                                            Log.d(Mixen.TAG, "Congrats! You're connected.");
                                            connectingProgress.dismiss();
                                            new MaterialDialog.Builder(getActivity())
                                                    .title("Congrats!")
                                                    .content("You're now connected to " + possibleHosts.get(0).get("username") + "'s mixen.")
                                                    .neutralText("Okay")
                                                    .build()
                                                    .show();
                                        } else {
                                            connectingProgress.dismiss();
                                            cleanUpDialog.show();
                                        }
                                    }
                                });
                                materialDialog.dismiss();
                                connectingProgress.show();

                            } else {
                                materialDialog.getContentView().setText("That code doesn't look right, please try again.");
                                materialDialog.getContentView().setTextColor(getResources().getColor(R.color.Radical_Red));
                            }
                        }
                    })
                    .show();

        }
    }


    private void setupFragment()
    {
        if(Mixen.isHost)
        {
            setupQueueAdapter(false);
            if(Mixen.username == null || Mixen.username.equals("Anonymous")) {
                setUsername();
            }
        }
        else
        {
            setupQueueAdapter(true);
            addSongBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void setupDiags() {

        connectingProgress = new MaterialDialog.Builder(getActivity())
                .title("Trying to connect...")
                .content("Please wait...")
                .negativeText("Stop")
                .progress(true, 0)
                .cancelable(false)
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

    public void updateQueueUI() {

        this.cellList.clear();
        this.cellList.addAll(SongQueueListAdapter.convertToListItems(MixenPlayerService.instance.metaQueue));

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

    private void setUsername()
    {

        new MaterialDialog.Builder(getActivity())
                .title("Who are you?")
                .cancelable(false)
                .autoDismiss(false)
                .content(R.string.createDescript)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.username_hint, R.string.blank_string, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                        if (charSequence.length() != 0 && charSequence.toString().matches("^[a-zA-Z0-9]*$")) {

                            Mixen.username = charSequence.toString();

                            SharedPreferences.Editor prefs = Mixen.sharedPref.edit();
                            prefs.putString("username", Mixen.username).apply();
                            prefs.commit();

                            materialDialog.dismiss();
                        } else {
                            materialDialog.getContentView().setText(getResources().getString(R.string.username_protocol));
                            materialDialog.getContentView().setTextColor(getResources().getColor(R.color.Radical_Red));
                        }
                    }
                })
                .show();

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
                settingsBtn.setVisibility(View.INVISIBLE);

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
                                        settingsBtn.setVisibility(View.VISIBLE);
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
                addSongBtn.setVisibility(View.INVISIBLE);
                settingsBtn.setVisibility(View.INVISIBLE);

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

                                        if (MixenPlayerService.instance.currentTrack.spotifyID.equals(selected.spotifyID)) {
                                            //If someone wants to delete the currently playing song, stop everything.
                                            MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.reset);
                                            Log.d(Mixen.TAG, "Current song was deleted.");

                                            MixenPlayerService.instance.playerServiceSnapshot.updateNetworkPlayer(PlaybackSnapshot.STOPPED);

                                            if (MixenPlayerService.instance.getNextTrack() != null || !MixenPlayerService.instance.queueIsEmpty()) {
                                                if (MixenPlayerService.instance.queueHasASingleTrack()) {
                                                    MixenPlayerService.instance.queueSongPosition = 0;
                                                }
                                                //We use preparePlayback here because we don't need to modify the counter, because the ArrayList will move around the counter.
                                                MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.preparePlayback);
                                            }

                                        } else if (position < MixenPlayerService.instance.queueSongPosition) {
                                            MixenPlayerService.instance.queueSongPosition--;
                                            MixenPlayerService.instance.playerServiceSnapshot.updateNetworkQueue();
                                        }

                                        MixenBase.mixenPlayerFrag.updateUpNext();

                                    }
                                })
                                .eventListener(new EventListener() {
                                    @Override
                                    public void onShow(Snackbar snackbar) {
                                        snackBarIsVisible = true;
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
                                        settingsBtn.setVisibility(View.VISIBLE);
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

        if(v.getId() == R.id.add_song_button)
        {
            addSong.setAction(Intent.ACTION_SEARCH);
            startActivityForResult(addSong, ADD_SONG_REQUEST);
            new ActivityAnimator().fadeAnimation(this.getActivity());
            return;
        }
        else if(v.getId() == R.id.settings)
        {
            startActivity(new Intent(getActivity(), SettingsScreen.class));
            new ActivityAnimator().fadeAnimation(this.getActivity());
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

        if(requestCode == TUTORIAL_REQUEST){

            if(resultCode == Activity.RESULT_OK){
                //We've finished the tutorial.
                Mixen.hasSeenTutorial = true;
                SharedPreferences.Editor prefs = Mixen.sharedPref.edit();
                prefs.putBoolean("hasSeenTutorial", Mixen.hasSeenTutorial).apply();
                setupFragment();
            }
        }

    }


}

