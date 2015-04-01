package com.peak.mixen;


import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


import android.support.annotation.Nullable;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.afollestad.materialdialogs.MaterialDialog;
import com.github.lzyzsd.circleprogress.ArcProgress;

import org.apache.commons.lang3.time.DurationFormatUtils;
import java.util.Random;



public class MixenPlayerFrag extends Fragment implements View.OnClickListener{

    private ImageButton playPauseButton;
    private ImageButton fastForwardIB;
    private ImageButton rewindIB;
    private ImageButton skipTrackBtn;
    private ImageButton previousTrackBtn;

    private RelativeLayout baseLayout;
    public ProgressBar bufferPB;
    public boolean isRunning;
    public boolean progressBarThreadIsRunning = false;

    private boolean pressedPreviousBefore = false;
    private RotateAnimation recordPlayerAnim;
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

        //setupSkipControlListeners();


        bufferPB.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);



        playDrawable = getResources().getDrawable(R.drawable.play_circle);
        pauseDrawable = getResources().getDrawable(R.drawable.pause_circle);

        //For first run. This cannot be set in the designer because the drawable set there will get
        //a different memory location than the above, making the showHidePlayBtn method not work.
        playPauseButton.setImageDrawable(pauseDrawable);

        showOrHidePlayBtn();
        playPauseButton.setClickable(false);
        fastForwardIB.setClickable(false);
        rewindIB.setClickable(false);
        skipTrackBtn.setClickable(false);
        previousTrackBtn.setClickable(false);


        return baseLayout;

    }

    public String humanReadableTimeString(int timeInMilliseconds)
    {
        return DurationFormatUtils.formatDuration(timeInMilliseconds, "m:ss");
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

        if(MixenPlayerService.instance.isRunning && MixenPlayerFrag.this.isRunning && MixenPlayerService.instance.playerIsPlaying())
        {
            String songDuration = humanReadableTimeString(MixenPlayerService.instance.getCurrentSongDuration());

            songDurationTV.setText("" + songDuration);
            arcProgressBar.setMax(MixenPlayerService.instance.getCurrentSongDuration());

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
        titleTV.setText(MixenPlayerService.instance.currentSong.getName());
        artistTV.setText(MixenPlayerService.instance.currentSong.getArtistName());

        if (hasAlbumArt()) {

            if(MixenPlayerService.instance.previousAlbumArt.containsKey(MixenPlayerService.instance.currentAlbumArtURL))
            {
                MixenPlayerService.instance.currentAlbumArt = MixenPlayerService.instance.previousAlbumArt.get(MixenPlayerService.instance.currentAlbumArtURL);
                albumArtIV.setImageBitmap(MixenPlayerService.instance.currentAlbumArt);
                generateAlbumArtPalette();
                Log.d(Mixen.TAG, "Using cached album art.");
            }
            else
            {
                new DownloadAlbumArt(albumArtIV, currentView).execute();
                Log.i(Mixen.TAG, "Will download album art.");
            }

        } else {

            Log.i(Mixen.TAG, "The current song does not have album art, will set a random color.");

        }

        Log.d(Mixen.TAG, "Current Song Info: " + MixenPlayerService.instance.currentSong.getName() + " : " + MixenPlayerService.instance.currentSong.getArtistName());

        startRotateAnimation();
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
            MixenBase.mixenPlayerFrag.upNextTV.setText("Next: \n" + MixenPlayerService.instance.getNextTrack().getName());
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
                    MixenBase.mixenPlayerFrag.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(MixenPlayerService.instance.playerIsPlaying())
                            {
                                String songProgress = humanReadableTimeString(MixenPlayerService.instance.getCurrentSongProgress());

                                MixenBase.mixenPlayerFrag.songProgressTV.setText("" + songProgress);
                                MixenBase.mixenPlayerFrag.arcProgressBar.setProgress(MixenPlayerService.instance.getCurrentSongProgress());
                            }

                        }
                    });
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                progressBarThreadIsRunning = false;
                Log.i(Mixen.TAG, "Stopped Progress Bar Updates.");

            }
        });

    }

    public void generateAlbumArtPalette()
    {

        Palette.generateAsync(MixenPlayerService.instance.currentAlbumArt, new Palette.PaletteAsyncListener() {
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
        arcProgressBar.setVisibility(View.INVISIBLE);
        songDurationTV.setVisibility(View.INVISIBLE);
        songProgressTV.setVisibility(View.INVISIBLE);
        MixenPlayerService.instance.currentAlbumArtURL = "";
        MixenPlayerService.instance.currentAlbumArt = null;
        playPauseButton.setImageDrawable(playDrawable);
        playPauseButton.animate()
                .alpha(1.0f)
                .setDuration(250);
        albumArtIV.animate()
                .alpha(0.3f)
                .setDuration(250);

        stopRotateAnimation();
        hideSongProgressViews();

    }

    public void showOrHidePlayBtn()
    {
        //The UI is already set to a paused state when UIIsClean, so calling this function would upset the order
        // for future calls to this function.

        if (playPauseButton.getDrawable() == pauseDrawable)
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
        else
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
    }

    public void stopRotateAnimation()
    {
        if(albumArtIV.getAnimation() != null)
        {
            albumArtIV.getAnimation().cancel();
        }
    }

    public void startRotateAnimation()
    {
        if(albumArtIV != null)
        {
            albumArtIV.startAnimation(recordPlayerAnim);
        }
    }

    public void hideUIControls(boolean changeDrawable)
    {
        //Show an indeterminate progress bar.

        bufferPB.setVisibility(View.VISIBLE);
        upNextTV.setVisibility(View.GONE);
        if(changeDrawable)
            showOrHidePlayBtn();
        playPauseButton.setClickable(false);
        fastForwardIB.setClickable(false);
        rewindIB.setClickable(false);
        skipTrackBtn.setClickable(false);
        previousTrackBtn.setClickable(false);

    }

    public static boolean hasAlbumArt()
    {
        if(MixenPlayerService.instance.currentSong.getCoverArtFilename().equals("") || MixenPlayerService.instance.currentSong.getCoverArtFilename() == null)
        {
            return false;
        }

        return true;
    }

    public void restoreUIControls()
    {
        //Show the media controls.

        bufferPB.setVisibility(View.GONE);
        upNextTV.setVisibility(View.VISIBLE);
        showOrHidePlayBtn();
        playPauseButton.setClickable(true);
        fastForwardIB.setClickable(true);
        rewindIB.setClickable(true);
        skipTrackBtn.setClickable(true);
        previousTrackBtn.setClickable(true);
    }

    public void setupSkipControlListeners()
    {
        fastForwardIB.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                v.setClickable(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerHasTrack)
                        {
                            MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.pause);
                            MixenPlayerService.instance.getPlayer().seekTo(MixenPlayerService.instance.getCurrentSongProgress() + (int)event.getEventTime()/100000);
                            Log.d(Mixen.TAG, "Attempting to skip forward " + event.getEventTime()/100000 + " seconds");
                        }

                    }
                }).start();
                v.setClickable(true);
                return true;
            }
        });
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.playPauseButton:
            {
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.pause);

                }
                else if (MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerHasTrack)
                {
                    //To fix a fringe bug.
                    if(MixenPlayerService.instance.getCurrentSongDuration() == 0)
                    {
                        MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.reset);
                        new MaterialDialog.Builder(MixenBase.mixenPlayerFrag.getActivity())
                                .title("Bummer :(")
                                .content(R.string.generic_streaming_error)
                                .neutralText("Okay")
                                .show();

                    }

                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.play);

                }

                return;
            }

            case R.id.fastForwardBtn:
            {
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.fastForward);
                }
                return;
            }

            case R.id.rewindBtn:
            {
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
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

