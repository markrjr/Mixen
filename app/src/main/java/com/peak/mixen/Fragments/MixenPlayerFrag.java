package com.peak.mixen.Fragments;


import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.github.lzyzsd.circleprogress.ArcProgress;
import com.peak.mixen.MetaTrack;
import com.peak.mixen.Mixen;
import com.peak.mixen.Activities.MixenBase;
import com.peak.mixen.Service.MixenPlayerService;
import com.peak.mixen.Service.PlaybackSnapshot;
import com.peak.mixen.R;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.Random;


public class MixenPlayerFrag extends Fragment implements View.OnClickListener{

    private ImageButton playPauseButton;
    private ImageButton fastForwardIB;
    private ImageButton rewindIB;
    private ImageButton skipTrackBtn;
    private ImageButton previousTrackBtn;
    private ImageButton upVoteBtn;
    private ImageButton downVoteBtn;

    private RelativeLayout baseLayout;
    public ProgressBar bufferPB;
    public boolean isRunning;
    public boolean progressBarThreadIsRunning = false;
    public RotateAnimation recordPlayerAnim;


    private boolean pressedPreviousBefore = false;
    private Drawable playDrawable;
    private Drawable pauseDrawable;
    private Thread progressBarUpdateThread;

    private TextView upNextTV;
    private TextView titleTV;
    private TextView artistTV;
    public TextView songProgressTV;
    public TextView songDurationTV;
    public ImageView albumArtIV;
    public ArcProgress arcProgressBar;
    private View currentView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        currentView = inflater.inflate(R.layout.fragment_mixen_player, container, false);
        baseLayout = (RelativeLayout) currentView.findViewById(R.id.baseLayout);

        titleTV = (TextView) currentView.findViewById(R.id.titleTV);
        playPauseButton = (ImageButton) currentView.findViewById(R.id.playPauseButton);
        fastForwardIB = (ImageButton) currentView.findViewById(R.id.fastForwardBtn);
        rewindIB = (ImageButton) currentView.findViewById(R.id.rewindBtn);
        skipTrackBtn = (ImageButton) currentView.findViewById(R.id.skipTrackBtn);
        previousTrackBtn = (ImageButton) currentView.findViewById(R.id.previousTrackBtn);
        upVoteBtn = (ImageButton) currentView.findViewById(R.id.upVoteBtn);
        downVoteBtn = (ImageButton) currentView.findViewById(R.id.downVoteBtn);

        bufferPB = (ProgressBar) currentView.findViewById(R.id.bufferingPB);
        artistTV = (TextView) currentView.findViewById(R.id.artistTV);
        albumArtIV = (ImageView) currentView.findViewById(R.id.albumArtIV);
        upNextTV = (TextView) currentView.findViewById(R.id.upNextTV);
        songProgressTV = (TextView) currentView.findViewById(R.id.songProgressTV);
        songDurationTV = (TextView) currentView.findViewById(R.id.songDurationTV);
        arcProgressBar = (ArcProgress) currentView.findViewById(R.id.arc_progress_bar);
        RelativeLayout voteControls = (RelativeLayout) currentView.findViewById(R.id.voteControls);
        RelativeLayout playerControls = (RelativeLayout) currentView.findViewById(R.id.playerControls);

        recordPlayerAnim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        recordPlayerAnim.setDuration(120000);
        recordPlayerAnim.setRepeatCount(Animation.INFINITE);

        playPauseButton.setOnClickListener(this);
        fastForwardIB.setOnClickListener(this);
        rewindIB.setOnClickListener(this);
        skipTrackBtn.setOnClickListener(this);
        previousTrackBtn.setOnClickListener(this);
        upVoteBtn.setOnClickListener(this);
        downVoteBtn.setOnClickListener(this);


        bufferPB.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);



        playDrawable = getResources().getDrawable(R.drawable.play_circle);
        pauseDrawable = getResources().getDrawable(R.drawable.pause_circle);

        //For first run. This cannot be set in the designer because the drawable set there will get
        //a different memory location than the above, making the showHidePlayBtn method not work.
        playPauseButton.setImageDrawable(pauseDrawable);

        showOrHidePlayBtn(null);
        playPauseButton.setClickable(false);
        fastForwardIB.setClickable(false);
        rewindIB.setClickable(false);
        skipTrackBtn.setClickable(false);
        previousTrackBtn.setClickable(false);

        if(!Mixen.isHost)
        {
            voteControls.setVisibility(View.VISIBLE);
            playerControls.setVisibility(View.INVISIBLE);
        }

        return baseLayout;

    }

    public String humanReadableTimeString(int timeInMilliseconds)
    {
        return DurationFormatUtils.formatDuration(timeInMilliseconds, "mm:ss");
    }

    public void hideSongProgressViews()
    {
        arcProgressBar.setVisibility(View.INVISIBLE);
        songProgressTV.setVisibility(View.INVISIBLE);
        songDurationTV.setVisibility(View.INVISIBLE);
        upNextTV.setVisibility(View.INVISIBLE);
    }

    public void showSongProgressViews()
    {
        arcProgressBar.setVisibility(View.VISIBLE);
        songProgressTV.setVisibility(View.VISIBLE);
        songDurationTV.setVisibility(View.VISIBLE);
        upNextTV.setVisibility(View.VISIBLE);
    }

    public void updateProgressBar()
    {
        if(progressBarUpdateThread == null)
        {
            setupProgressBarThread();
        }

        if(MixenPlayerService.instance.isRunning && MixenPlayerFrag.this.isRunning && MixenPlayerService.instance.playerIsPlaying)
        {
            String songDuration = humanReadableTimeString(MixenPlayerService.instance.currentTrack.duration);

            songDurationTV.setText("" + songDuration);
            arcProgressBar.setMax(MixenPlayerService.instance.currentTrack.duration);

            if(!progressBarThreadIsRunning)
            {
                setupProgressBarThread();
                progressBarUpdateThread.start();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isRunning = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        isRunning = true;
        if(MixenPlayerService.instance != null) //TODO Fix with real guard, bug from network?
        {
            updateProgressBar();
        }
    }

    public void prepareUI()
    {
        if(MixenPlayerService.instance.currentTrack == null)
        {
            //TODO Investigate. See if PlaybackSnapShot is getting past when should be flagged as an init or ready call.
            return;
        }

        Picasso.with(getActivity())
                .load(MixenPlayerService.instance.currentTrack.albumArtURL)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        MixenPlayerService.instance.currentTrack.albumArt = bitmap;
                        if(Mixen.isHost)
                        {
                            MixenPlayerService.instance.setMetaData();
                        }
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });

        titleTV.setText(MixenPlayerService.instance.currentTrack.name);
        artistTV.setText(MixenPlayerService.instance.currentTrack.artist);

        Picasso.with(getActivity().getApplicationContext())
                .load(MixenPlayerService.instance.currentTrack.albumArtURL)
                .placeholder(getResources().getDrawable(R.drawable.mixen_icon))
                .into(albumArtIV);

        Log.d(Mixen.TAG, "Current Song Info: " + MixenPlayerService.instance.currentTrack.name + " : " + MixenPlayerService.instance.currentTrack.artist);

        setRotateAnimation();

        updateUpNext();
    }

    public void updateUpNext()
    {
        if(MixenPlayerService.instance.queueIsEmpty())
        {
            MixenBase.mixenPlayerFrag.upNextTV.setText("Nothing Is Playing");
            MixenBase.mixenPlayerFrag.upNextTV.setVisibility(View.VISIBLE);
        }
        else if(MixenPlayerService.instance.getNextTrack() != null)
        {
            MixenBase.mixenPlayerFrag.upNextTV.setText("Next: \n" + MixenPlayerService.instance.getNextTrack().name);
        }
        else
        {
            upNextTV.setText("");
        }
    }

    public void setupProgressBarThread()
    {
        progressBarUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(Mixen.TAG, "Player Progress Bar thread created.");
                progressBarThreadIsRunning = true;
                while(MixenPlayerService.instance.isRunning && MixenPlayerFrag.this.isRunning)
                {

                    if(MixenPlayerService.instance.playerIsPlaying)
                    {

                        MixenPlayerService.instance.spotifyPlayer.getPlayerState(new PlayerStateCallback() {
                            @Override
                            public void onPlayerState(final PlayerState playerState) {
                                MixenBase.mixenPlayerFrag.getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        String songProgress = humanReadableTimeString(playerState.positionInMs);

                                        MixenBase.mixenPlayerFrag.songProgressTV.setText("" + songProgress);
                                        MixenBase.mixenPlayerFrag.arcProgressBar.setProgress(playerState.positionInMs);

                                    }
                                });
                            }
                        });

                    }
                    try {
                        Thread.sleep(125);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                progressBarThreadIsRunning = false;
                Log.i(Mixen.TAG, "Stopped Progress Bar Updates.");

            }
        });

    }

    public void cleanUpUI()
    {
        titleTV.setText("");
        artistTV.setText("");
        upNextTV.setText("");
        albumArtIV.setImageResource(R.drawable.mixen_icon);
        playPauseButton.setImageDrawable(playDrawable);
        playPauseButton.animate()
                .alpha(1.0f)
                .setDuration(250);
        albumArtIV.animate()
                .alpha(0.3f)
                .setDuration(250);

        setRotateAnimation();
        hideSongProgressViews();
    }

    protected void setUIToPaused()
    {
        playPauseButton.setImageDrawable(playDrawable);
        playPauseButton.animate()
                .alpha(1.0f)
                .setDuration(250);
        albumArtIV.animate()
                .alpha(0.3f)
                .setDuration(250);
        Log.d(Mixen.TAG, "PAUSED");
    }

    protected void setUIToPlaying()
    {
        playPauseButton.setImageDrawable(pauseDrawable);
        playPauseButton.animate()
                .alpha(0f)
                .setDuration(250);
        albumArtIV.animate()
                .alpha(1.0f)
                .setDuration(250);
        Log.d(Mixen.TAG, "PLAYING");
    }

    public void showOrHidePlayBtn(@Nullable PlaybackSnapshot playbackState)
    {

        if(playbackState != null)
        {
            if(playbackState.playServiceState == PlaybackSnapshot.PLAYING)
            {
                setUIToPlaying();
                return;
            }
            else if(playbackState.playServiceState == PlaybackSnapshot.PAUSED)
            {
                setUIToPaused();
                return;
            }
        }

        if (playPauseButton.getDrawable() == pauseDrawable)
        {
            setUIToPaused();
        }
        else
        {
            setUIToPlaying();
        }
    }

    public void setRotateAnimation()
    {
        if(albumArtIV != null)
        {
            if(albumArtIV.getAnimation() == null)
            {
                albumArtIV.startAnimation(recordPlayerAnim);
            }
            else
            {
                albumArtIV.setAnimation(null);
            }
        }
    }

    public void hideUIControls(boolean changeDrawable)
    {
        //Show an indeterminate progress bar.

        bufferPB.setVisibility(View.VISIBLE);
        upNextTV.setVisibility(View.INVISIBLE);
        if(changeDrawable)
            showOrHidePlayBtn(null);
        playPauseButton.setClickable(false);
        fastForwardIB.setClickable(false);
        rewindIB.setClickable(false);
        skipTrackBtn.setClickable(false);
        previousTrackBtn.setClickable(false);

    }

    public void restoreUIControls()
    {
        //Show the media controls.

        bufferPB.setVisibility(View.GONE);
        upNextTV.setVisibility(View.VISIBLE);
        showOrHidePlayBtn(null);
        playPauseButton.setClickable(true);
        fastForwardIB.setClickable(true);
        rewindIB.setClickable(true);
        skipTrackBtn.setClickable(true);
        previousTrackBtn.setClickable(true);
    }


    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.upVoteBtn:
            {
                MixenPlayerService.instance.playerServiceSnapshot.sendNetworkVote(true);
                return;
            }

            case R.id.downVoteBtn:
            {
                MixenPlayerService.instance.playerServiceSnapshot.sendNetworkVote(false);
                return;
            }

            case R.id.playPauseButton:
            {
                MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.changePlayBackState);
                return;
            }

            case R.id.fastForwardBtn:
            {

                MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.fastForward);
                return;
            }

            case R.id.rewindBtn:
            {
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying)
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.rewind);
                }
                return;
            }

            case R.id.skipTrackBtn:
            {
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerHasTrack && MixenPlayerService.instance.getNextTrack() != null)
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.skipToNext);
                }
                return;
            }

            case R.id.previousTrackBtn:
            {
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerHasTrack)
                {

                    if(pressedPreviousBefore && MixenPlayerService.instance.getLastTrack() != null)
                    {
                        MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.skipToLast);
                        pressedPreviousBefore = false;
                        return;
                    }

                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.replayTrack);
                    pressedPreviousBefore = true;
                    return;

                }
            }

        }
    }

}

