package com.peak.mixen;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.support.annotation.Nullable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.util.Random;

import co.arcs.groove.thresher.Song;


public class MixenPlayerFrag extends Fragment implements View.OnClickListener {

    public static ImageButton playPauseButton;
    private static ImageButton fastForwardIB;
    private static ImageButton rewindIB;
    private RelativeLayout playerControls;
    private static ProgressBar bufferPB;
    public static getStreamURLAsync retrieveURLsAsync;
    public static boolean isRunning;


    private static Drawable playDrawable;
    private static Drawable pauseDrawable;

    public static TextView upNextTV;
    public static TextView titleTV;
    public static TextView artistTV;
    public static ImageView albumArtIV;
    private static View currentView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        currentView = inflater.inflate(R.layout.fragment_mixen_player, container, false);

        RelativeLayout relativeLayout = (RelativeLayout) currentView.findViewById(R.id.baseLayout);
        titleTV = (TextView) currentView.findViewById(R.id.titleTV);
        playPauseButton = (ImageButton) currentView.findViewById(R.id.playPauseButton);
        fastForwardIB = (ImageButton) currentView.findViewById(R.id.fastForwardIB);
        rewindIB = (ImageButton) currentView.findViewById(R.id.rewindIB);
        bufferPB = (ProgressBar) currentView.findViewById(R.id.bufferingPB);
        artistTV = (TextView) currentView.findViewById(R.id.artistTV);
        albumArtIV = (ImageView) currentView.findViewById(R.id.albumArtIV);
        upNextTV = (TextView) currentView.findViewById(R.id.upNextTV);
        playerControls = (RelativeLayout) currentView.findViewById(R.id.playerControls);

        playPauseButton.setOnClickListener(this);
        fastForwardIB.setOnClickListener(this);
        rewindIB.setOnClickListener(this);


        bufferPB.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.Snow_White),
                android.graphics.PorterDuff.Mode.SRC_IN);



        playDrawable = getResources().getDrawable(R.drawable.play);
        pauseDrawable = getResources().getDrawable(R.drawable.pause);

        isRunning = true;

        return relativeLayout;

    }



    public static void prepareUI()
    {
        titleTV.setText(MixenPlayerService.currentSong.getName());
        artistTV.setText(MixenPlayerService.currentSong.getArtistName());

        if (hasAlbumArt()) {
//            //TODO Check for same album using ArrayList.
//            if(MixenPlayerService.previousAlbumArt.equals(MixenPlayerService.currentAlbumArtURL))
//            {
//                Log.i(Mixen.TAG, "Album art is same.");
//            }
//            else
//            {
                new DownloadAlbumArt(albumArtIV, currentView).execute();
                Log.i(Mixen.TAG, "Will download album art.");
//            }

        } else {
            albumArtIV.setBackgroundColor(Mixen.appColors[new Random().nextInt(Mixen.appColors.length)]);

            Log.i(Mixen.TAG, "Setting random color.");

        }

        Log.d(Mixen.TAG, "Current Song Info: " + MixenPlayerService.currentSong.getName() + " : " + MixenPlayerService.currentSong.getArtistName());


        if (queueHasNextTrack()) {
            upNextTV.setText("Up Next: " + getNextTrack().getName());
        }

    }

    public static void cleanUpUI()
    {
        titleTV.setText("");
        artistTV.setText("");
        albumArtIV.setImageResource(0);
    }

    public static void showOrHidePlayBtn()
    {
        if (playPauseButton.getDrawable() == pauseDrawable)
        {
            playPauseButton.setImageDrawable(playDrawable);
            Log.d(Mixen.TAG, "PAUSED");
        }
        else
        {
            playPauseButton.setImageDrawable(pauseDrawable);
            Log.d(Mixen.TAG, "PLAYING");

        }
    }



    public static void showOrHideProgressBar()
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


    public static boolean playerHasTrack()
    {
        try
        {
            MixenPlayerService.queuedSongs.get(MixenPlayerService.currentSongAsInt);
            return true;
        }
        catch (IndexOutOfBoundsException e)
        {
            Log.i(Mixen.TAG, "No song was found in the queue.");
        }
        catch (NullPointerException e)
        {
            Log.i(Mixen.TAG, "Queue not yet initialized.");
        }

        return false;
    }

    public static boolean queueHasNextTrack()
    {
        try
        {
            getNextTrack();
            return true;
        }
        catch (IndexOutOfBoundsException e)
        {
            playPauseButton.setImageDrawable(playDrawable);
            upNextTV.setText("");
            Log.i(Mixen.TAG, "No song was found after the current one in the queue.");
        }

        return false;
    }


    public static void hideUIControls()
    {
        //Show an indeterminate progress bar.

        bufferPB.setVisibility(View.VISIBLE);
        playPauseButton.setVisibility(View.INVISIBLE);
        playPauseButton.setImageDrawable(playDrawable);
        fastForwardIB.setClickable(false);
        rewindIB.setClickable(false);

    }

    public static boolean hasAlbumArt()
    {
        if(MixenPlayerService.currentSong.getCoverArtFilename() == "" || MixenPlayerService.currentSong.getCoverArtFilename() == null)
        {
            return false;
        }

        return true;
    }

    public static void restoreUIControls()
    {
        //Show the media controls.

        bufferPB.setVisibility(View.GONE);
        playPauseButton.setVisibility(View.VISIBLE);
        playPauseButton.setImageDrawable(pauseDrawable);
        fastForwardIB.setClickable(true);
        rewindIB.setClickable(true);
    }


    public static void preparePlayback()
    {
        //Get all the necessary things to stream the song.

        retrieveURLsAsync = new getStreamURLAsync();
        retrieveURLsAsync.execute(MixenPlayerService.currentSong);

        Log.i(Mixen.TAG, "Grabbing URL for next song and signaling playback, it should begin shortly.");
        hideUIControls();
    }

    public static Song getNextTrack()
    {
        return MixenPlayerService.queuedSongs.get(MixenPlayerService.currentSongAsInt + 1);
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
                if(MixenPlayerService.isRunning && MixenPlayerService.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.pause);

                }
                else if (MixenPlayerService.isRunning)
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.play);
                }

                return;
            }

            case R.id.fastForwardIB:
            {
                //updateUI
                if(MixenPlayerService.isRunning && MixenPlayerService.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.fastForward);
                }
                return;
            }

            case R.id.rewindIB:
            {
                //updateUI
                if(MixenPlayerService.isRunning && MixenPlayerService.playerIsPlaying())
                {
                    MixenPlayerService.doAction(getActivity().getApplicationContext(), MixenPlayerService.rewind);
                }
                return;
            }

        }
    }


}

