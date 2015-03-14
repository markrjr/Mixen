package com.peak.mixen;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
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


import com.github.lzyzsd.circleprogress.ArcProgress;

import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Random;



public class MixenPlayerFrag extends Fragment implements View.OnClickListener{

    public ImageButton playPauseButton;
    private ImageButton fastForwardIB;
    private ImageButton rewindIB;
    private ImageButton skipTrackBtn;
    private ImageButton previousTrackBtn;

    private RelativeLayout baseLayout;
    private ProgressBar bufferPB;
    public getStreamURLAsync retrieveURLsAsync;
    public boolean isRunning;


    private RotateAnimation recordPlayerAnim;
    private Drawable playDrawable;
    private Drawable pauseDrawable;

    public TextView upNextTV;
    public TextView titleTV;
    public TextView artistTV;
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


        recordPlayerAnim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        recordPlayerAnim.setDuration(60000);
        recordPlayerAnim.setRepeatCount(Animation.INFINITE);


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

        isRunning = true;

        return baseLayout;

    }

    public String humanReadableTimeString(int timeInMilliseconds)
    {
        Duration duration = new Duration(timeInMilliseconds);
        Period period = duration.toPeriod();
        PeriodFormatter minutesAndSeconds = new PeriodFormatterBuilder()
                .printZeroAlways()
                .appendMinutes()
                .appendSeparator(":")
                .appendSeconds()
                .toFormatter();
        return minutesAndSeconds.print(period);
    }


    public void updateProgressBar()
    {
        String songDuration = humanReadableTimeString(MixenPlayerService.instance.getCurrentSongDuration());

        songDurationTV.setText("" + songDuration);
        arcProgressBar.setMax(MixenPlayerService.instance.getCurrentSongDuration());

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenBase.mixenPlayerFrag.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            String songProgress = humanReadableTimeString(MixenPlayerService.instance.getCurrentSongProgress());

                            MixenBase.mixenPlayerFrag.songProgressTV.setText("" + songProgress);
                            MixenBase.mixenPlayerFrag.arcProgressBar.setProgress(MixenPlayerService.instance.getCurrentSongProgress());
                        }
                    });
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

    }

    public void prepareUI()
    {
        titleTV.setText(MixenPlayerService.instance.currentSong.getName());
        artistTV.setText(MixenPlayerService.instance.currentSong.getArtistName());
        arcProgressBar.setVisibility(View.VISIBLE);
        songDurationTV.setVisibility(View.VISIBLE);
        songProgressTV.setVisibility(View.VISIBLE);

        if (hasAlbumArt()) {

            new DownloadAlbumArt(albumArtIV, currentView).execute();
            Log.i(Mixen.TAG, "Will download album art.");

//            //TODO Check for same album using ArrayList, fix color palette generation.
//            if(MixenPlayerService.instance.previousAlbumArtURL.equals(MixenPlayerService.instance.currentAlbumArtURL))
//            {
//                Log.i(Mixen.TAG, "Album art is same.");
//            }
//            else
//            {
//                new DownloadAlbumArt(albumArtIV, currentView).execute();
//                Log.i(Mixen.TAG, "Will download album art.");
//            }

        } else {
            //albumArtIV.setImageResource(Mixen.appColors[new Random().nextInt(Mixen.appColors.length)]);

            Log.i(Mixen.TAG, "Setting random color.");

        }

        Log.d(Mixen.TAG, "Current Song Info: " + MixenPlayerService.instance.currentSong.getName() + " : " + MixenPlayerService.instance.currentSong.getArtistName());


        if (MixenPlayerService.instance.queueHasNextTrack()) {
            upNextTV.setText("Next: \n" + MixenPlayerService.instance.getNextTrack().getName());
        }

    }

    public void generateAlbumArtPalette()
    {

        albumArtIV.setAnimation(recordPlayerAnim);


        Palette.generateAsync(MixenPlayerService.instance.currentAlbumArt, new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {

                Log.d(Mixen.TAG, "Generated colors.");

                int vibrantColor = palette.getVibrantColor(Mixen.appColors[new Random().nextInt(Mixen.appColors.length)]);
                int darkMuted = palette.getDarkMutedColor(Mixen.appColors[new Random().nextInt(Mixen.appColors.length)]);

                titleTV.setBackgroundColor(vibrantColor);
                artistTV.setBackgroundColor(vibrantColor);
                arcProgressBar.setBackgroundColor(vibrantColor);
                playPauseButton.setBackgroundColor(vibrantColor);
                songDurationTV.setBackgroundColor(vibrantColor);
                songProgressTV.setBackgroundColor(vibrantColor);
                baseLayout.setBackgroundColor(vibrantColor);

                arcProgressBar.setUnfinishedStrokeColor(darkMuted);

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

        hideUIControls();
        showOrHideProgressBar();

    }

    public void showOrHidePlayBtn()
    {
        if (playPauseButton.getDrawable() == pauseDrawable)
        {
            playPauseButton.setImageDrawable(playDrawable);
            albumArtIV.setAlpha(0.3f);
            Log.d(Mixen.TAG, "PAUSED");
        }
        else
        {
            playPauseButton.setImageDrawable(pauseDrawable);
            albumArtIV.setAlpha(1f);
            Log.d(Mixen.TAG, "PLAYING");

        }
    }


    public void showOrHideProgressBar()
    {
        if (bufferPB.getVisibility() == View.VISIBLE)
        {
            bufferPB.setVisibility(View.GONE);
        }
        else
        {
            bufferPB.setVisibility(View.VISIBLE);
        }

    }


    public void hideUIControls()
    {
        //Show an indeterminate progress bar.

        bufferPB.setVisibility(View.VISIBLE);
        playPauseButton.setVisibility(View.INVISIBLE);
        playPauseButton.setImageDrawable(playDrawable);
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
        playPauseButton.setVisibility(View.VISIBLE);
        playPauseButton.setImageDrawable(pauseDrawable);
        fastForwardIB.setClickable(true);
        rewindIB.setClickable(true);
        skipTrackBtn.setClickable(true);
        previousTrackBtn.setClickable(true);
    }


    @Override
    public void onStop() {
        super.onStop();

        isRunning = false;
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.playPauseButton:
            {
                //updateUI
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.pause);
                }
                else if (MixenPlayerService.instance.isRunning && !MixenPlayerService.instance.playerHasFinishedSong)
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.play);
                }

                return;
            }

            case R.id.fastForwardBtn:
            {
                //updateUI
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.fastForward);
                }
                return;
            }

            case R.id.rewindBtn:
            {
                //updateUI
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.rewind);
                }
                return;
            }

            case R.id.skipTrackBtn:
            {
                //updateUI
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.skipToNext);
                }
                return;
            }

            case R.id.previousTrackBtn:
            {
                //updateUI
                if(MixenPlayerService.instance.isRunning && MixenPlayerService.instance.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.skipToLast);
                }
                return;
            }

        }
    }

}

