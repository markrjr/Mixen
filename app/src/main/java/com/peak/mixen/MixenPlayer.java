package com.peak.mixen;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.common.collect.Lists;
import com.peak.salut.Salut;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;


import co.arcs.groove.thresher.*;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;


public class MixenPlayer extends Activity {

    public static final int REQUEST_CODE = 5;
    public static Client grooveSharkSession;
    public static Intent viewQueue;
    public static MediaPlayer player;
    public static SpotifyService spotify;
    public static Artist currentArtist;
    public static TextView upNext;
    public static boolean isRunning = false;

    private static ImageButton playPauseButton;
    private static ProgressBar bufferPB;
    private static getStreamURLAsync retrieveURLsAsync;
    private static TextView titleTV;
    private static TextView artistTV;
    private static Drawable playDrawable;
    private static Drawable pauseDrawable;


    private boolean pressedBefore = false;
    private ImageView albumArtIV;
    private TextView songProgress;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixen_player);

        getActionBar().hide();

        //Setup the GrooveShark and Spotify session as well as the media player.

        //grooveSharkSession.setDebugLoggingEnabled(true);

        //Setup the UI buttons.

        titleTV = (TextView) findViewById(R.id.titleTV);
        playPauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        bufferPB = (ProgressBar) findViewById(R.id.bufferingPB);
        artistTV = (TextView) findViewById(R.id.artistTV);
        albumArtIV = (ImageView) findViewById(R.id.albumArtIV);
        songProgress = (TextView) findViewById(R.id.songProgressTV);
        upNext = (TextView) findViewById(R.id.upNextTV);


        //Create the required list of songs.


        setupMediaPlayerListeners();

        playDrawable = getResources().getDrawable(R.drawable.play);
        pauseDrawable = getResources().getDrawable(R.drawable.pause);


    }

    public void prepareUI()
    {
        titleTV.setText(Mixen.currentSong.getName());
        artistTV.setText(Mixen.currentSong.getArtistName());

        if (hasAlbumArt()) {
            //TODO Check for same album.
            new DownloadAlbumArt(albumArtIV).execute();
            //albumArtIV.setImageURI(Uri.parse(Mixen.currentAlbumArt));
            Log.i(Mixen.TAG, "Will download album art.");
        } else {
            albumArtIV.setBackgroundColor(Mixen.appColors[new Random().nextInt(Mixen.appColors.length)]);
            //albumArtIV.setImageDrawable(getDrawable(R.drawable.music)); TODO Find generic drawable.
            Log.i(Mixen.TAG, "Will set random color.");

        }

        Log.d(Mixen.TAG, "Current Song Info: " + Mixen.currentSong.getName() + " : " + Mixen.currentSong.getArtistName());


        if (queueHasNextTrack()) {
            upNext.setText("Up Next: " + Mixen.queuedSongs.get(Mixen.currentSongAsInt + 1).getName());
        }

    }

    public void cleanUpUI()
    {
        titleTV.setText("");
        artistTV.setText("");
        bufferPB.setVisibility(View.VISIBLE);

    }

    public void showOrHidePlayBtn()
    {
        if (playPauseButton.getDrawable() == pauseDrawable)
        {
            playPauseButton.setImageDrawable(playDrawable);
        }
        else
        {
            playPauseButton.setImageDrawable(pauseDrawable);
        }
    }


    public void setupMediaPlayerListeners()
    {
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //After the music player is ready to go, restore UI controls to the user,
                //setup some nice UI stuff, and finally, start playing music.



                //restoreUIControls();
                //playPauseButton.setChecked(false);
                mediaPlayer.start();
                Log.i(Mixen.TAG, "Playback has been prepared, now playing.");
            }
        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            //After the current track has finished, begin preparations for the playing the next song.

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

                if (!queueHasNextTrack()) {
                    //If the queue does not have a track after this one, stop everything.
                    return;
                }

                if(isRunning)
                {
                    showOrHidePlayBtn();
                }

                Mixen.currentSongAsInt++;
                Mixen.currentSong = Mixen.queuedSongs.get(Mixen.currentSongAsInt);
                mediaPlayer.reset();

                preparePlayback();
            }
        });

        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int action, int extra) {

                //If some error happens while attempting to listen to music, start the error handling activity.

                mediaPlayer.reset();

                Log.e(Mixen.TAG, "An error occurred whilst trying to stream down music.");

                Intent provideMoreInfo = new Intent(MixenPlayer.this, MoreInfo.class);

                provideMoreInfo.putExtra("START_REASON", Mixen.GENERIC_STREAMING_ERROR);

                startActivity(provideMoreInfo);

                return false;
            }
        });

        player.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override

            //Check for buffering of the track and show an indeterminate progress bar if buffering is happening.

            public boolean onInfo(MediaPlayer mediaPlayer, int action, int extra) {

                if (action == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    Mixen.bufferTimes++;
                    //If the player has buffered more than 3 times recently.

                    if (Mixen.bufferTimes >= 3) {
                        mediaPlayer.pause();
                        Mixen.bufferTimes = 0;
                        Log.d(Mixen.TAG, "Max buffer times exceeded.");

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Wait 5 seconds to buffer.
                                player.start();
                            }
                        }, 5000);
                    }

                    //hideUIControls();
                    Log.i(Mixen.TAG, "Buffering of media has begun.");
                } else if (action == MediaPlayer.MEDIA_INFO_BUFFERING_END && MixenPlayer.player.isPlaying()) {
                    //restoreUIControls();
                    Log.i(Mixen.TAG, "Buffering has stopped, and playback should have resumed.");
                }
                return false;
            }
        });

        player.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
                //If the user fast forwards on rewinds, after the required seeking operating completes, restart the media player at
                //the seek-ed to position.

                showOrHidePlayBtn();

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
        //TODO Fix function in program flow.
        try
        {
            Mixen.queuedSongs.get(Mixen.currentSongAsInt + 1);
            return true;
        }
        catch (IndexOutOfBoundsException e)
        {
            playPauseButton.setImageDrawable(playDrawable);
            upNext.setText("");
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

    public void restoreUIControls()
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
            player.setDataSource(Mixen.currentContext, streamURI);
        }
        catch (InterruptedException e)
        {
            Log.e(Mixen.TAG, "An error occurred, playback could not be started.");
            return;
        }
        catch (IOException e)
        {
            Log.e(Mixen.TAG, "An error occurred, playback could not be started.");
            return;
        }
        catch (ExecutionException e)
        {
            Log.e(Mixen.TAG, "An error occurred.");
            return;
        }

        player.prepareAsync();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mixen_stage, menu);
        return true;
    }

    public void onBtnClicked(View v)
    {


        switch(v.getId())
        {
            case R.id.playPauseButton:
            {
                if(player.isPlaying() && playerHasTrack())
                {
                    Mixen.currentSongProgress = player.getCurrentPosition();

                    player.pause();
                    playPauseButton.setImageDrawable(playDrawable);
                    Log.i(Mixen.TAG, "Music playback has been paused.");

                    return;
                }
                else if(playerHasTrack())
                {
                    player.seekTo(Mixen.currentSongProgress);
                    player.start();
                    playPauseButton.setImageDrawable(pauseDrawable);
                    Log.i(Mixen.TAG, "Music playback has resumed.");

                    return;
                }

                return;

            }

            case R.id.fastForwardIB:
            {
                if (player.isPlaying() || playerHasTrack())
                {
                    Mixen.currentSongProgress = player.getCurrentPosition();

                    if(Mixen.currentSongProgress + 30000 > player.getDuration())
                    {
                        Log.d(Mixen.TAG, "User tried to seek past track length.");
                        return;
                    }

                    player.pause();
                    playPauseButton.setImageDrawable(pauseDrawable);
                    player.seekTo(Mixen.currentSongProgress + 30000); //Fast forward 30 seconds.
                    Log.i(Mixen.TAG, "Seeking forward 30 seconds.");
                }
                return;
            }

            case R.id.rewindIB:
            {
                if (player.isPlaying() || playerHasTrack())
                {
                    Mixen.currentSongProgress = player.getCurrentPosition();
                    player.pause();
                    playPauseButton.setImageDrawable(playDrawable);
                    player.seekTo(Mixen.currentSongProgress - 30000);
                    Log.i(Mixen.TAG, "Seeking backwards 30 seconds.");
                }
                return;
            }

            case R.id.currentMusicBtn: {

                viewQueue = new Intent(this, SongQueue.class);
                startActivity(viewQueue);

                return;
            }

            case R.id.usersBtn:
            {
                Log.d(Mixen.TAG, "This method was called.");

                //Mixen.network.discoverNetworkService();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId())
        {

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(MixenPlayer.player.isPlaying())
        {
            prepareUI();
            showOrHidePlayBtn();
        }

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        StartScreen.restoreControls();
        //Mixen.network.disposeNetworkService();
    }


    public void onBackPressed() {

        if (pressedBefore)
        {
            //If the user has pressed the back button twice at this point kill the player.
            if(player.isPlaying())
            {
                player.stop();
                player.release();
            }
            this.finish();
            return; //Sometimes the app doesn't finish before the end of this function. Gosh is Android weird.
        }

        Toast.makeText(getApplicationContext(),
                "Press again to close the player.", Toast.LENGTH_SHORT)
                .show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                pressedBefore = false;
            }
        }, 5000);

        pressedBefore = true;
        return;

    }


}
