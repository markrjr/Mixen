package com.peak.mixen;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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


import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import co.arcs.groove.thresher.Song;


public class MixenPlayerFrag extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    public static ImageButton playPauseButton;
    private ImageButton fastForwardIB;
    private ImageButton rewindIB;
    private RelativeLayout playerControls;
    private static ProgressBar bufferPB;
    private static getStreamURLAsync retrieveURLsAsync;

    private static Drawable playDrawable;
    private static Drawable pauseDrawable;

    public static TextView upNextTV;
    public static TextView titleTV;
    public static TextView artistTV;
    public static ImageView albumArtIV;
    private View currentView;
    public boolean stoppedPlayingUnexpectedly;

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


        setupMediaPlayerListeners();

        playDrawable = getResources().getDrawable(R.drawable.play);
        pauseDrawable = getResources().getDrawable(R.drawable.pause);

        return relativeLayout;
    }

    public void prepareUI()
    {
        titleTV.setText(Mixen.currentSong.getName());
        artistTV.setText(Mixen.currentSong.getArtistName());

        if (hasAlbumArt()) {
            //TODO Check for same album.
            if(Mixen.previousAlbumArt.equals(Mixen.currentAlbumArt))
            {
                Log.i(Mixen.TAG, "Album art is same.");
            }
            else
            {
                new DownloadAlbumArt(albumArtIV, currentView).execute();
                Log.i(Mixen.TAG, "Will download album art.");
            }

        } else {
            albumArtIV.setBackgroundColor(Mixen.appColors[new Random().nextInt(Mixen.appColors.length)]);

            Log.i(Mixen.TAG, "Setting random color.");

        }

        Log.d(Mixen.TAG, "Current Song Info: " + Mixen.currentSong.getName() + " : " + Mixen.currentSong.getArtistName());


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

    public void showOrHidePlayBtn()
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

    void setPlaybackState()
    {
        if(Mixen.player.isPlaying() && playerHasTrack())
        {
            Mixen.currentSongProgress = Mixen.player.getCurrentPosition();

            Mixen.player.pause();
            stoppedPlayingUnexpectedly = false;
            showOrHidePlayBtn();
            //Log.i(Mixen.TAG, "Music playback has been paused.");
        }
        else if(playerHasTrack())
        {
            Mixen.player.seekTo(Mixen.currentSongProgress);
            //Mixen.player.start();
            showOrHidePlayBtn();
            //Log.i(Mixen.TAG, "Music playback has resumed.");

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


    public void setupMediaPlayerListeners()
    {
        //How the hell do these even work if I'm running the media player in a separate thread?
        //Shouldn't this require "runOnUIThread" anytime I want to make an update to the UI?

        Mixen.player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //After the music player is ready to go, restore UI controls to the user,
                //setup some nice UI stuff, and finally, start playing music.

                if(MixenPlayerFrag.this.isVisible())
                {
                    prepareUI();
                    restoreUIControls();
                }
                mediaPlayer.start();
                Log.i(Mixen.TAG, "Playback has been prepared, now playing.");
            }
        });

        Mixen.player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            //After the current track has finished, begin preparations for the playing the next song.

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {


                cleanUpUI();

                if (!queueHasNextTrack()) {
                    //If the queue does not have a track after this one, stop everything.
                    mediaPlayer.reset();
                    return;
                }

                hideUIControls();

                Mixen.previousAlbumArt = Mixen.currentAlbumArt;

                Mixen.currentSongAsInt = 0;
                Mixen.queuedSongs.remove(Mixen.queuedSongs.indexOf(Mixen.currentSong));
                SongQueueFrag.updateQueueUI();
                Mixen.currentSong = Mixen.queuedSongs.get(Mixen.currentSongAsInt);
                mediaPlayer.reset();


                preparePlayback();
            }
        });

        Mixen.player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int action, int extra) {

                //If some error happens while attempting to listen to music, start the error handling activity.

                mediaPlayer.reset();

                showOrHidePlayBtn();

                Log.e(Mixen.TAG, "An error occurred whilst trying to stream down music.");

                Intent provideMoreInfo = new Intent(getActivity(), MoreInfo.class);

                provideMoreInfo.putExtra("START_REASON", Mixen.GENERIC_STREAMING_ERROR);

                startActivity(provideMoreInfo);

                return false;
            }
        });

        Mixen.player.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override

            //Check for buffering of the track and show an indeterminate progress bar if buffering is happening.

            public boolean onInfo(MediaPlayer mediaPlayer, int action, int extra) {

                if (action == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    Mixen.bufferTimes++;
                    //If the Mixen.player has buffered more than 3 times recently.

                    if (Mixen.bufferTimes >= 3) {
                        mediaPlayer.pause();
                        stoppedPlayingUnexpectedly = false;
                        Mixen.bufferTimes = 0;
                        Log.d(Mixen.TAG, "Max buffer times exceeded.");

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Wait 5 seconds to buffer.
                                Mixen.player.start();
                            }
                        }, 5000);
                    }

                    if (MixenPlayerFrag.this.isVisible())
                    {
                        hideUIControls();
                    }
                    Log.i(Mixen.TAG, "Buffering of media has begun.");

                } else if (action == MediaPlayer.MEDIA_INFO_BUFFERING_END && Mixen.player.isPlaying()) {

                    if (MixenPlayerFrag.this.isVisible())
                    {
                        restoreUIControls();
                    }

                    Log.i(Mixen.TAG, "Buffering has stopped, and playback should have resumed.");
                }
                return false;
            }
        });

        Mixen.player.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
                //If the user fast forwards on rewinds, after the required seeking operating completes, restart the media Mixen.player at
                //the seek-ed to position.

                restoreUIControls();
                mediaPlayer.start();
                //restoreUIControls();

            }
        });


    }


    public static boolean playerHasTrack()
    {
        try
        {
            Mixen.queuedSongs.get(Mixen.currentSongAsInt);
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
    }

    public boolean hasAlbumArt()
    {
        if(Mixen.currentSong.getCoverArtFilename() == "" || Mixen.currentSong.getCoverArtFilename() == null)
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
    }


    public static void preparePlayback()
    {
        //Get all the necessary things to stream the song.

        retrieveURLsAsync = new getStreamURLAsync();
        retrieveURLsAsync.execute(Mixen.currentSong);

        Log.i(Mixen.TAG, "Grabbing URL for next song and signaling playback, it should begin shortly.");
        hideUIControls();
    }

    public static void beginPlayback()
    {
        Uri streamURI;

        try
        {
            streamURI = Uri.parse(retrieveURLsAsync.get().toString());
            //Album art should be set here.
            Log.i(Mixen.TAG, "Stream URL is " + streamURI.toString());
            Log.i(Mixen.TAG, "Track ID is " + Mixen.currentSong.getId());
            Mixen.player.setDataSource(Mixen.currentContext, streamURI);
        }
        catch (Exception ex)
        {
            Log.e(Mixen.TAG, "An error occurred, playback could not be started.");
            ex.printStackTrace();
            return;
        }

        Mixen.player.prepareAsync();

    }
    
    public static Song getNextTrack()
    {
        return Mixen.queuedSongs.get(Mixen.currentSongAsInt + 1);
    }


    @Override
    public boolean onLongClick(View view)
    {
        switch(view.getId())
        {
            case R.id.fastForwardIB:
            {
                if (queueHasNextTrack())
                {
                    Song nextSong = getNextTrack();
                    cleanUpUI();
                    Mixen.player.reset();
                    Mixen.currentSong = nextSong;
                    Mixen.currentSongAsInt = Mixen.currentSongAsInt + 1;
                    Mixen.currentAlbumArt = Mixen.COVER_ART_URL + nextSong.getCoverArtFilename();
                    MixenPlayerFrag.preparePlayback();
                    Log.d(Mixen.TAG, "Skipping songs to " + nextSong.getName());
                }
                break;
            }
        }

        return true;
    }


    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.playPauseButton:
            {
                setPlaybackState();
                return;
            }

            case R.id.fastForwardIB:
            {
                if (Mixen.player.isPlaying() || playerHasTrack())
                {
                    Mixen.currentSongProgress = Mixen.player.getCurrentPosition();

                    if(Mixen.currentSongProgress + 30000 > Mixen.player.getDuration())
                    {
                        Log.d(Mixen.TAG, "User tried to seek past track length.");
                        return;
                    }

                    Mixen.player.pause();
                    stoppedPlayingUnexpectedly = false;
                    hideUIControls();
                    Mixen.player.seekTo(Mixen.currentSongProgress + 30000); //Fast forward 30 seconds.
                    Log.i(Mixen.TAG, "Seeking forward 30 seconds.");
                }
                return;
            }

            case R.id.rewindIB:
            {
                if (Mixen.player.isPlaying() || playerHasTrack())
                {
                    Mixen.currentSongProgress = Mixen.player.getCurrentPosition();
                    Mixen.player.pause();
                    stoppedPlayingUnexpectedly = false;
                    hideUIControls();
                    Mixen.player.seekTo(Mixen.currentSongProgress - 30000);
                    Log.i(Mixen.TAG, "Seeking backwards 30 seconds.");
                }
                return;
            }

        }
    }


}

