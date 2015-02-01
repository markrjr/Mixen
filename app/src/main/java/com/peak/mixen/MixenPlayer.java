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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;


import co.arcs.groove.thresher.*;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MixenPlayer extends Activity {

    public static final int REQUEST_CODE = 5;
    public static Client grooveSharkSession;
    public static Intent viewQueue;
    public static MediaPlayer mixenStreamer;
    public static SpotifyService spotify;
    public static Artist currentArtist;


    private static ImageButton playPauseButton;
    private static ProgressBar bufferPB;
    private static getStreamURLAsync retrieveURLsAsync;
    private static TextView titleTV;
    private static TextView artistTV;
    private static SpotifyApi api;
    private static Drawable playDrawable;
    private static Drawable pauseDrawable;


    private boolean pressedBefore = false;
    private ImageView albumArtIV;
    private TextView songDuration;
    private TextView songProgress;
    private RelativeLayout baseLayout;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixen_player);

        Intent startingIntent = getIntent();
        getActionBar().hide();

        //Setup the GrooveShark and Spotify session as well as the media player.
        grooveSharkSession = new Client();

        api = new SpotifyApi();
        spotify = api.getService();

        //grooveSharkSession.setDebugLoggingEnabled(true);

        mixenStreamer = new MediaPlayer();

        mixenStreamer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mixenStreamer.setLooping(false);

        Mixen.username = startingIntent.getStringExtra("userName");

        //Setup the UI buttons.



        titleTV = (TextView) findViewById(R.id.titleTV);
        playPauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        bufferPB = (ProgressBar) findViewById(R.id.bufferingPB);
        artistTV = (TextView) findViewById(R.id.artistTV);
        albumArtIV = (ImageView) findViewById(R.id.albumArtIV);
        baseLayout = (RelativeLayout) findViewById(R.id.baseLayout);


        //Create the required list of songs.

        Mixen.queuedSongs = new ArrayList<Song>();
        Mixen.proposedSongs = new ArrayList<Song>();

        Mixen.currentContext = this.getApplicationContext();

        playPauseButton.setScaleX(1.5f);
        playPauseButton.setScaleY(1.5f);

        setupMediaPlayerListeners();

        playDrawable = getResources().getDrawable(R.drawable.play);
        pauseDrawable = getResources().getDrawable(R.drawable.pause);



    }


    public void setupMediaPlayerListeners()
    {
        mixenStreamer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //After the music player is ready to go, restore UI controls to the user,
                //setup some nice UI stuff, and finally, start playing music.

                titleTV.setText(Mixen.currentSong.getName());
                artistTV.setText(Mixen.currentSong.getArtistName());
                //new DownloadArtistArt(baseLayout).execute(Mixen.currentArtistArt);
                Log.i(Mixen.TAG, "Downloading artist art.");


                restoreUIControls();
                //playPauseButton.setChecked(false);
                mediaPlayer.start();
                Log.i(Mixen.TAG, "Playback has been prepared, now playing.");
            }
        });

        mixenStreamer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            //After the current track has finished, begin preparations for the playing the next song.

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

                if(!queueHasNextTrack())
                {
                    //If the queue does not have a track after this one, stop everything.
                    return;
                }

                Mixen.currentSongAsInt++;
                Mixen.currentSong = Mixen.queuedSongs.get(Mixen.currentSongAsInt);
                mediaPlayer.reset();
                titleTV.setText("");
                artistTV.setText("");
                bufferPB.setVisibility(View.VISIBLE);

                preparePlayback();
                postHandlePlayback();
            }
        });

        mixenStreamer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
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

        mixenStreamer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override

            //Check for buffering of the track and show an indeterminate progress bar if buffering is happening.

            public boolean onInfo(MediaPlayer mediaPlayer, int action, int extra) {

                if(action == MediaPlayer.MEDIA_INFO_BUFFERING_START)
                {
                    Mixen.bufferTimes++;
                    //If the player has buffered more than 3 times recently.

                    if(Mixen.bufferTimes >= 3)
                    {
                        mediaPlayer.pause();
                        Mixen.bufferTimes = 0;
                        Log.d(Mixen.TAG, "Max buffer times exceeded.");

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Wait 5 seconds to buffer.
                                 mixenStreamer.start();
                            }
                        }, 5000);
                    }

                    hideUIControls();
                    Log.i(Mixen.TAG, "Buffering of media has begun.");
                }
                else if(action == MediaPlayer.MEDIA_INFO_BUFFERING_END && MixenPlayer.mixenStreamer.isPlaying())
                {
                    restoreUIControls();
                    Log.i(Mixen.TAG, "Buffering has stopped, and playback should have resumed.");
                }
                return false;
            }
        });

        mixenStreamer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
                //If the user fast forwards on rewinds, after the required seeking operating completes, restart the media player at
                //the seek-ed to position.

                if(playPauseButton.getBackground() == getDrawable(R.drawable.play))
                {
                    //TODO Probably shouldn't use the equals operator above.
                    //If the player is paused, then change the icon.
                    playPauseButton.setBackground(pauseDrawable);
                }

                mediaPlayer.start();
                restoreUIControls();

            }
        });


    }

    public static void postHandlePlayback()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                MixenPlayer.beginPlayback();
            }
        }, 2000);
    }


    public boolean playerHasTrack()
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

        return false;
    }

    public static boolean queueHasNextTrack()
    {
        try
        {
            Mixen.queuedSongs.get(Mixen.currentSongAsInt + 1);
            return true;
        }
        catch (IndexOutOfBoundsException e)
        {
            playPauseButton.setBackground(playDrawable);
            titleTV.setText("");
            artistTV.setText("");
            Log.i(Mixen.TAG, "No song was found after the current one in the queue.");
        }

        return false;
    }


    public static void hideUIControls()
    {
        //Show an indeterminate progress bar.

        bufferPB.setVisibility(View.VISIBLE);
        playPauseButton.setVisibility(View.INVISIBLE);
        playPauseButton.setBackground(playDrawable);
    }

    public void restoreUIControls()
    {
        //Show the media controls.

        bufferPB.setVisibility(View.GONE);
        playPauseButton.setVisibility(View.VISIBLE);
        playPauseButton.setBackground(pauseDrawable);
    }


    public static void preparePlayback()
    {
        //Get all the necessary things to stream the song.

        hideUIControls();
        retrieveURLsAsync = new getStreamURLAsync();
        retrieveURLsAsync.execute(Mixen.currentSong);
        spotify.getArtist(Mixen.currentSong.getArtistName(), new Callback<Artist>() {
            @Override
            public void success(Artist artist, Response response) {

                Mixen.currentArtistArt = artist.images.get(new Random().nextInt(artist.images.size())).url;
                Log.d(Mixen.TAG, "Found artist art at " + artist.images.get(0).url);

            }

            @Override
            public void failure(RetrofitError error) {

            }
        });




        Log.i(Mixen.TAG, "Grabbing URL for next song and signaling playback, it should begin shortly.");
    }

    public static void beginPlayback()
    {
        //Begin playback of the song.

        //TODO Post delay this with a handler. Not everyone has Google Fiber.

        Uri streamURI;


        try
        {
            streamURI = Uri.parse(retrieveURLsAsync.get().toString());
            //Album art should be set here.
            Log.i(Mixen.TAG, "Stream URL is " + streamURI.toString());
            Log.i(Mixen.TAG, "Track ID is " + Mixen.currentSong.getId());
            mixenStreamer.setDataSource(Mixen.currentContext, streamURI);
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

        mixenStreamer.prepareAsync();

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
                if(mixenStreamer.isPlaying() && playerHasTrack())
                {
                    Mixen.currentSongProgress = mixenStreamer.getCurrentPosition();

                    mixenStreamer.pause();
                    playPauseButton.setBackground(playDrawable);
                    Log.i(Mixen.TAG, "Music playback has been paused.");

                    return;
                }
                else if(playerHasTrack())
                {
                    mixenStreamer.seekTo(Mixen.currentSongProgress);
                    mixenStreamer.start();
                    playPauseButton.setBackground(pauseDrawable);
                    Log.i(Mixen.TAG, "Music playback has resumed.");

                    return;
                }

                return;

            }

            case R.id.fastForwardIB:
            {
                if (mixenStreamer.isPlaying() || playerHasTrack())
                {
                    Mixen.currentSongProgress = mixenStreamer.getCurrentPosition();

                    if(Mixen.currentSongProgress + 30000 > mixenStreamer.getDuration())
                    {
                        Log.d(Mixen.TAG, "User tried to seek past track length.");
                        return;
                    }

                    mixenStreamer.pause();
                    playPauseButton.setBackground(pauseDrawable);
                    mixenStreamer.seekTo(Mixen.currentSongProgress + 30000); //Fast forward 30 seconds.
                    Log.i(Mixen.TAG, "Seeking forward 30 seconds.");
                }
                return;
            }

            case R.id.rewindIB:
            {
                if (mixenStreamer.isPlaying() || playerHasTrack())
                {
                    Mixen.currentSongProgress = mixenStreamer.getCurrentPosition();
                    mixenStreamer.pause();
                    playPauseButton.setBackground(playDrawable);
                    mixenStreamer.seekTo(Mixen.currentSongProgress - 30000);
                    Log.i(Mixen.TAG, "Seeking backwards 30 seconds.");
                }
                return;
            }

            case R.id.currentMusicBtn:

                viewQueue = new Intent(this, SongQueue.class);
                startActivity(viewQueue);

                return;
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
    public void onDestroy()
    {
        super.onDestroy();
        //StartScreen.restoreControls();
    }

    public void onBackPressed() {

        if (pressedBefore)
        {
            //If the user has pressed the back button twice at this point kill the player.
            if(mixenStreamer.isPlaying())
            {
                mixenStreamer.stop();
                mixenStreamer.release();
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
