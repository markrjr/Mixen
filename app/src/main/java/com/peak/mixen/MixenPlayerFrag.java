package com.peak.mixen;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.afollestad.materialdialogs.MaterialDialog;
import com.github.lzyzsd.circleprogress.ArcProgress;
import com.melnykov.fab.FloatingActionButton;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.ByteArrayOutputStream;
import java.util.Random;


public class MixenPlayerFrag extends Fragment implements View.OnClickListener{

    private ImageButton playPauseButton;
    private ImageButton fastForwardIB;
    private ImageButton rewindIB;
    private ImageButton skipTrackBtn;
    private ImageButton previousTrackBtn;

    private RelativeLayout baseLayout;
    private FloatingActionButton upVoteBtn;
    private FloatingActionButton downVoteBtn;
    public ProgressBar bufferPB;
    public boolean isRunning;
    public boolean progressBarThreadIsRunning = false;

    private boolean pressedPreviousBefore = false;
    public RotateAnimation recordPlayerAnim;
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

        bufferPB = (ProgressBar) currentView.findViewById(R.id.bufferingPB);
        artistTV = (TextView) currentView.findViewById(R.id.artistTV);
        albumArtIV = (ImageView) currentView.findViewById(R.id.albumArtIV);
        upNextTV = (TextView) currentView.findViewById(R.id.upNextTV);
        songProgressTV = (TextView) currentView.findViewById(R.id.songProgressTV);
        songDurationTV = (TextView) currentView.findViewById(R.id.songDurationTV);
        arcProgressBar = (ArcProgress) currentView.findViewById(R.id.arc_progress_bar);
        upVoteBtn = (FloatingActionButton) currentView.findViewById(R.id.upVoteBtn);
        downVoteBtn = (FloatingActionButton) currentView.findViewById(R.id.downVoteBtn);


        titleTV.setSelected(true);
        titleTV.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        titleTV.setSingleLine(true);

        artistTV.setSelected(true);
        artistTV.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        artistTV.setSingleLine(true);

        recordPlayerAnim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        recordPlayerAnim.setRepeatCount(Animation.INFINITE);
        recordPlayerAnim.setDuration(120000);

        playPauseButton.setOnClickListener(this);
        fastForwardIB.setOnClickListener(this);
        rewindIB.setOnClickListener(this);
        skipTrackBtn.setOnClickListener(this);
        previousTrackBtn.setOnClickListener(this);


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
            upVoteBtn.setVisibility(View.VISIBLE);
            upVoteBtn.setClickable(false);
            downVoteBtn.setClickable(false);
            downVoteBtn.setVisibility(View.VISIBLE);
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
            String songDuration = humanReadableTimeString(MixenPlayerService.instance.currentMetaTrack.duration);

            songDurationTV.setText("" + songDuration);
            arcProgressBar.setMax(MixenPlayerService.instance.currentMetaTrack.duration);

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
        titleTV.setText(MixenPlayerService.instance.currentMetaTrack.name);
        artistTV.setText(MixenPlayerService.instance.currentMetaTrack.artist);

        Picasso.with(getActivity().getApplicationContext())
                .load(MixenPlayerService.instance.currentMetaTrack.albumArtURL)
                .placeholder(getResources().getDrawable(R.drawable.mixen_icon))
                .into(albumArtIV);

        Log.d(Mixen.TAG, "Current Song Info: " + MixenPlayerService.instance.currentMetaTrack.name + " : " + MixenPlayerService.instance.currentMetaTrack.artist);

        setRotateAnimation();

        if(Mixen.isHost)
        {
            updateUpNext();
        }
        else
        {
            updateClientUpNext();
        }

    }

    public void updateClientUpNext()
    {
        if(MixenPlayerService.instance.clientQueue.isEmpty())
        {
            MixenBase.mixenPlayerFrag.upNextTV.setText("Nothing Is Playing");
            MixenBase.mixenPlayerFrag.upNextTV.setVisibility(View.VISIBLE);
        }
        else if(MixenPlayerService.instance.getNextMetaTrack() != null)
        {
            MixenBase.mixenPlayerFrag.upNextTV.setText("Next: \n" + MixenPlayerService.instance.getNextMetaTrack().name);
        }
        else
        {
            upNextTV.setText("");
        }
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
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                progressBarThreadIsRunning = false;
                Log.i(Mixen.TAG, "Stopped Progress Bar Updates.");

            }
        });

    }

    public void generateAlbumArtPalette(MetaTrack song)
    {

        Palette.generateAsync(song.albumArt, new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {

                Log.d(Mixen.TAG, "Generated colors.");

                int vibrantColor = palette.getVibrantColor(Mixen.appColors[new Random().nextInt(Mixen.appColors.length)]);
                int arcBarRandom = palette.getDarkVibrantColor(getResources().getColor(R.color.Tundora));


                titleTV.setBackgroundColor(vibrantColor);
                artistTV.setBackgroundColor(vibrantColor);
                arcProgressBar.setBackgroundColor(vibrantColor);
                playPauseButton.setBackgroundColor(vibrantColor);
                songDurationTV.setBackgroundColor(vibrantColor);
                songProgressTV.setBackgroundColor(vibrantColor);
                baseLayout.setBackgroundColor(vibrantColor);

                arcProgressBar.setUnfinishedStrokeColor(arcBarRandom);

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

