package com.peak.mixen;


import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.afollestad.materialdialogs.MaterialDialog;

import co.arcs.groove.thresher.Song;

public class MixenPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
                                                            MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener, 
                                                            MediaPlayer.OnSeekCompleteListener, AudioManager.OnAudioFocusChangeListener {

    public static final String play = "ACTION_PLAY";
    public static final String pause = "ACTION_PAUSE";
    public static final String rewind = "ACTION_REWIND";
    public static final String fastForward = "ACTION_FAST_FORWARD";
    public static final String skipToNext = "ACTION_SKIP_NEXT";
    public static final String skipToLast = "ACTION_SKIP_LAST";
    public static final String reset = "ACTION_RESET_PLAYER";
    public static final String setup = "ACTION_SETUP";
    public static final String changePlayBackState = "CHANGE_PLAYBACK_STATE";
    public static final String init = "INIT_MIXEN_PLAYER_SERVICE";


    public static MixenPlayerService instance;

    private MediaPlayer player;

    private NoisyAudioReciever noisyAudioReciever;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private AudioManager audioManager;
    private int bufferTimes;

    private boolean playerHasStartedSong;
    public boolean playerHasFinishedSong;

    public int currentSongProgress;
    public int currentSongAsInt;

    public boolean stoppedPlayingUnexpectedly;
    public boolean isRunning = false;

    public Bitmap currentAlbumArt;
    public String currentAlbumArtURL;
    public String previousAlbumArtURL = "";
    public Song currentSong;

    public ArrayList<Song> queuedSongs;
    public ArrayList<Song> proposedSongs;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent == null || intent.getAction() == null)
            return super.onStartCommand(intent, flags, startId);

        if(player == null)
        {
            instance = this;
            audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            noisyAudioReciever = new NoisyAudioReciever();
            queuedSongs = new ArrayList<Song>();
            proposedSongs = new ArrayList<Song>();
            setupPhoneListener();
            initMusicPlayer();
            isRunning = true;
            Log.d(Mixen.TAG, "Mixen Player Service successfully initialized.");
        }
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public static void doAction(Context context, String action)
    {
        Intent intent = new Intent(context, MixenPlayerService.class);
        intent.setAction(action);
        context.startService(intent);
    }
    public void initMusicPlayer(){

        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setLooping(false);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnInfoListener(this);
    }

    public void handleIntent(Intent intent)
    {
        switch (intent.getAction())
        {
            case setup:
                beginPlayback();
                return;
            case play:
                setPlaybackState();
                return;
            case pause:
                setPlaybackState();
                return;
            case rewind:
                rewindPlayer();
                return;
            case fastForward:
                fastForwardPlayer();
                return;
            case reset:
                resetAndStopPlayer();
                return;
            case skipToNext:
                skipToNextSong();
                return;
            case skipToLast:
                skipToLastSong();
            case changePlayBackState:
                setPlaybackState();
            case init:
                return;
        }
    }

    public void preparePlayback()
    {
        //Get all the necessary things to stream the song.

        MixenBase.mixenPlayerFrag.retrieveURLsAsync = new getStreamURLAsync();
        MixenBase.mixenPlayerFrag.retrieveURLsAsync.execute(currentSong);

        Log.i(Mixen.TAG, "Grabbing URL for next song and signaling playback, it should begin shortly.");
        MixenBase.mixenPlayerFrag.hideUIControls();
    }

    public int getCurrentSongProgress()
    {
        return player.getCurrentPosition();
    }

    public int getCurrentSongDuration()
    {
        return player.getDuration();
    }

    public boolean playerIsPlaying(){
        try{
            return player.isPlaying();
        }
        catch(IllegalStateException ex)
        {
            return false;
        }

    }

    public Song getNextTrack()
    {
        return queuedSongs.get(instance.currentSongAsInt + 1);
    }

    public Song getLastTrack()
    {
        if(currentSongAsInt == 0)
        {
            return null;
        }
        else
        {
            try
            {
                return queuedSongs.get(currentSongAsInt - 1);
            }
            catch (Exception ex)
            {
                return null;
            }
        }

    }



    public boolean queueHasNextTrack()
    {
        try
        {
            getNextTrack();
            return true;
        }
        catch (IndexOutOfBoundsException e)
        {
            //MixenBase.mixenPlayerFrag.cleanUpUI();
            Log.i(Mixen.TAG, "No song was found after the current one in the queue.");
        }

        return false;
    }

    public boolean playerHasTrack()
    {
        try
        {
            queuedSongs.get(instance.currentSongAsInt);
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


    private void resetAndStopPlayer()
    {
        if(playerHasTrack())
        {

            if(playerIsPlaying())
            {
                player.pause();
                player.stop();
            }

            player.reset();
            MixenBase.mixenPlayerFrag.cleanUpUI();
            audioManager.abandonAudioFocus(this);
        }

    }

    private void skipToNextSong()
    {
        Song nextSong = getNextTrack();
        MixenBase.mixenPlayerFrag.cleanUpUI();
        //Update UI
        resetAndStopPlayer();
        currentSong = nextSong;
        currentSongAsInt = queuedSongs.indexOf(nextSong);
        currentAlbumArtURL = Mixen.COVER_ART_URL + nextSong.getCoverArtFilename();
        preparePlayback();
        Log.d(Mixen.TAG, "Skipping songs to " + nextSong.getName());
    }

    private void skipToLastSong()
    {
        Song previousSong = getLastTrack();
        if(previousSong == null)
        {
            return;
        }

        MixenBase.mixenPlayerFrag.cleanUpUI();
        //Update UI
        resetAndStopPlayer();
        currentSong = previousSong;
        currentSongAsInt = queuedSongs.indexOf(previousSong);
        currentAlbumArtURL = Mixen.COVER_ART_URL + previousSong.getCoverArtFilename();
        preparePlayback();
        Log.d(Mixen.TAG, "Going back to " + previousSong.getName());
    }


    private void beginPlayback(){

        Uri streamURI;

        try
        {
            streamURI = Uri.parse(MixenBase.mixenPlayerFrag.retrieveURLsAsync.get().toString());
            //Album art should be set here.
            Log.i(Mixen.TAG, "Stream URL is " + streamURI.toString());
            Log.i(Mixen.TAG, "Track ID is " + currentSong.getId());
            player.setDataSource(getApplicationContext(), streamURI);
        }
        catch (Exception ex)
        {
            Log.e(Mixen.TAG, "An error occurred, playback could not be started.");
            if(MixenBase.mixenPlayerFrag.isRunning)
            {
                MixenBase.mixenPlayerFrag.cleanUpUI();
            }
            ex.printStackTrace();
            return;
        }

        MixenBase.mixenPlayerFrag.prepareUI();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (playerHasTrack() && !playerHasStartedSong)
                {
                    startActivity(Mixen.moreInfoDialog(MixenPlayerService.this, Mixen.GENERIC_STREAMING_ERROR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    MixenPlayerService.doAction(getApplicationContext(), MixenPlayerService.reset);
                    MixenBase.mixenPlayerFrag.cleanUpUI();
                }
            }
        }, 8000);
        player.prepareAsync();

    }

    private void rewindPlayer()
    {
        if (player.isPlaying() || playerHasTrack())
        {
            currentSongProgress = player.getCurrentPosition();
            player.pause();
            stoppedPlayingUnexpectedly = false;
            MixenBase.mixenPlayerFrag.hideUIControls();
            if(player.getCurrentPosition() - 30000 < 0)
            {
                player.seekTo(0);
            }
            else
            {
                player.seekTo(currentSongProgress - 30000);
            }
            //Log.i(Mixen.TAG, "Seeking backwards 30 seconds.");
        }

    }

    private void fastForwardPlayer()
    {
        if (player.isPlaying() || playerHasTrack())
        {
            currentSongProgress = player.getCurrentPosition();

            if(currentSongProgress + 30000 > player.getDuration())
            {
                Log.d(Mixen.TAG, "User tried to seek past track length.");
                return;
            }

            player.pause();
            stoppedPlayingUnexpectedly = false;
            MixenBase.mixenPlayerFrag.hideUIControls();
            player.seekTo(currentSongProgress + 30000); //Fast forward 30 seconds.
            //Log.i(Mixen.TAG, "Seeking forward 30 seconds.");
        }

    }

    public boolean hasAudioFocus() {
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(Mixen.TAG, "Audio Focus GRANTED.");
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void onAudioFocusChange(int audioChange) {
        if(audioChange == AudioManager.AUDIOFOCUS_GAIN)
        {
            if(!MixenPlayerService.instance.playerIsPlaying() && playerHasTrack())
            {
                MixenPlayerService.doAction(getApplicationContext(), MixenPlayerService.play);
                Log.d(Mixen.TAG, "Loop");
            }
        }
        else if(audioChange == AudioManager.AUDIOFOCUS_LOSS);
        {
            if(MixenPlayerService.instance.playerIsPlaying())
            {
                MixenPlayerService.doAction(getApplicationContext(), MixenPlayerService.pause);
                audioManager.abandonAudioFocus(this);
            }
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int action, int extra) {

        if (action == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            //TODO Fix buffering a lot.
            MixenBase.mixenPlayerFrag.hideUIControls();

            bufferTimes++;
            if(bufferTimes >= 3)
            {
                bufferTimes = 0;
                player.pause();
                Log.d(Mixen.TAG, "Buffer Times Exceeded.");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        player.seekTo(player.getCurrentPosition());
                    }
                }, 5000);
            }


            Log.i(Mixen.TAG, "Buffering of media has begun.");

        } else if (action == MediaPlayer.MEDIA_INFO_BUFFERING_END && player.isPlaying()) {

            MixenBase.mixenPlayerFrag.restoreUIControls();

            Log.i(Mixen.TAG, "Buffering has stopped, and playback should have resumed.");
        }
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        //If the user fast forwards on rewinds, after the required seeking operating completes, restart the media player at
        //the seek-ed to position.

        //Log.d(Mixen.TAG, "Seek operation complete.");
        if (hasAudioFocus())
        {
            MixenBase.mixenPlayerFrag.restoreUIControls();
            mediaPlayer.start();
        }

    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

            unregisterReceiver(noisyAudioReciever);

            Log.d(Mixen.TAG, "Playback has completed.");
            MixenBase.mixenPlayerFrag.cleanUpUI();
            playerHasStartedSong = false;
            playerHasFinishedSong = true;

            //TODO Delete x amount of tracks if after x amount of completions.

            if (!queueHasNextTrack()) { //TODO Implement other check here to fix bug.
                //If the queue does not have a track after this one, stop everything.
                stopForeground(true);
                mediaPlayer.reset();

                return;
            }

            currentSongAsInt++;
            MixenBase.mixenPlayerFrag.hideUIControls();
            previousAlbumArtURL = currentAlbumArtURL;
            currentSong = queuedSongs.get(currentSongAsInt);
            mediaPlayer.reset();
            
            preparePlayback();

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int action, int extra) {

        if(extra == 0)
        {
            //TODO Research
            //Operation is pending.
            return true;
        }

        mediaPlayer.reset();

        MixenBase.mixenPlayerFrag.cleanUpUI();

        Log.e(Mixen.TAG, "An error occurred whilst trying to stream down music.");

        new MaterialDialog.Builder(MixenBase.mixenPlayerFrag.getActivity())
                .title("Bummer :(")
                .content(R.string.generic_streaming_error)
                .neutralText("Okay")
                .show();

        return true;

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //After the music player is ready to go, restore UI controls to the user,
        //setup some nice UI stuff, and finally, start playing music.

        MixenBase.mixenPlayerFrag.restoreUIControls();

        if(hasAudioFocus())
        {
            mediaPlayer.start();
            MixenBase.mixenPlayerFrag.updateProgressBar();
            playerHasStartedSong = true;
            playerHasFinishedSong = false;
            registerReceiver(noisyAudioReciever, intentFilter);
            if(MixenBase.userHasLeftApp)
            {
                startForeground(Mixen.MIXEN_NOTIFY_CODE, prepareNotif());
            }
            Log.i(Mixen.TAG, "Playback has been prepared, now playing.");
        }

    }

    private void updateNotification() {

        Notification notification = prepareNotif();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Mixen.MIXEN_NOTIFY_CODE, notification);
    }


    public Notification prepareNotif(){

        Intent changePlayBack = new Intent(getApplicationContext(), MixenPlayerService.class);
        Intent rewindIntent = new Intent(getApplicationContext(), MixenPlayerService.class);
        Intent fastForwardIntent = new Intent(getApplicationContext(), MixenPlayerService.class);


        Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.mixen_notification_icon);


        changePlayBack.setAction(changePlayBackState);
        rewindIntent.setAction(rewind);
        fastForwardIntent.setAction(fastForward);


        PendingIntent changePlaybackPendingIntent = PendingIntent.getService(getApplicationContext(), 11, changePlayBack, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent rewindPendingIntent = PendingIntent.getService(getApplicationContext(), 11, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent fastForwardPendingIntent = PendingIntent.getService(getApplicationContext(), 11, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        RemoteViews contentView = new RemoteViews(Mixen.currentContext.getPackageName(), R.layout.player_notification);
        contentView.setImageViewResource(R.id.icon, R.drawable.mixen_icon);
        contentView.setTextViewText(R.id.title, currentSong.getName());
        contentView.setTextViewText(R.id.text, currentSong.getArtistName());

        RemoteViews bigContentView = new RemoteViews(Mixen.currentContext.getPackageName(), R.layout.player_notification_big);
        bigContentView.setTextViewText(R.id.status_bar_track_name, currentSong.getName());
        bigContentView.setTextViewText(R.id.status_bar_artist_name, currentSong.getArtistName());
        bigContentView.setTextViewText(R.id.status_bar_album_name, currentSong.getAlbumName());

        if(MixenPlayerFrag.hasAlbumArt())
        {
            bigContentView.setImageViewBitmap(R.id.status_bar_album_art, currentAlbumArt);
        }

        if(player.isPlaying())
        {
            contentView.setImageViewResource(R.id.playbackState, R.drawable.pause);
            bigContentView.setImageViewResource(R.id.status_bar_play, R.drawable.pause);
            mBuilder.setOngoing(true);

        }
        else
        {
            contentView.setImageViewResource(R.id.playbackState, R.drawable.play);
            bigContentView.setImageViewResource(R.id.status_bar_play, R.drawable.play);
            mBuilder.setOngoing(false);

        }

        contentView.setOnClickPendingIntent(R.id.playbackState, changePlaybackPendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.status_bar_play, changePlaybackPendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.status_bar_next, fastForwardPendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.status_bar_prev, rewindPendingIntent);


        final Intent notificationIntent = new Intent(getApplicationContext(), MixenBase.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        mBuilder.setContent(contentView);
        mBuilder.setContentIntent(contentIntent);

        Notification notification = mBuilder.build();
        notification.bigContentView = bigContentView;

        return notification;
    }

    void setPlaybackState()
    {
        if(player.isPlaying() && playerHasTrack())
        {
            currentSongProgress = player.getCurrentPosition();

            player.pause();
            stoppedPlayingUnexpectedly = false;
            MixenBase.mixenPlayerFrag.showOrHidePlayBtn();
            if(MixenBase.userHasLeftApp)
            {
                updateNotification();
            }
            //Log.i(Mixen.TAG, "Music playback has been paused.");
        }
        else if(playerHasTrack())
        {
            player.seekTo(currentSongProgress);

            if(hasAudioFocus())
            {
                player.start();
                MixenBase.mixenPlayerFrag.showOrHidePlayBtn();
                if(MixenBase.userHasLeftApp)
                {
                    updateNotification();
                }
                //Log.i(Mixen.TAG, "Music playback has resumed.");
            }

        }
    }


    public void setupPhoneListener() {

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //INCOMING call
                    //Do all necessary action to pause the audio
                    if (isRunning  && MixenBase.mixenPlayerFrag.isRunning) {

                        if (MixenPlayerService.instance.playerIsPlaying()) {
                            doAction(getApplicationContext(), pause);
                            stoppedPlayingUnexpectedly = true;
                        }
                    }

                } else if (state == TelephonyManager.CALL_STATE_IDLE && isRunning) {

                    if (MixenPlayerService.instance.playerIsPlaying() && MixenBase.mixenPlayerFrag.isRunning) {

                        if (!MixenPlayerService.instance.playerIsPlaying() && stoppedPlayingUnexpectedly) {
                            doAction(getApplicationContext(), play);
                            Log.d(Mixen.TAG, "Resuming playback.");
                            //This might be an issue with audio focus and not require set the audio mode.
                            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                            audioManager.setMode(AudioManager.MODE_NORMAL);
                        }
                    }

                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                    //do all necessary action to pause the audio
                    if (isRunning && MixenBase.mixenPlayerFrag.isRunning) {

                        if (MixenPlayerService.instance.playerIsPlaying()) {

                            doAction(getApplicationContext(), pause);
                            stoppedPlayingUnexpectedly = true;
                        }
                    }

                    super.onCallStateChanged(state, incomingNumber);
                }
            }
        }; //End PhoneStateListener

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(telephonyManager != null)
        {
            telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onTaskRemoved (Intent rootIntent) {

        Log.d(Mixen.TAG, "Ending Mixen Service.");
        if (player != null)
        {
            resetAndStopPlayer();
            player.release();
            isRunning = false;
            stopForeground(true);
            unregisterReceiver(noisyAudioReciever);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Mixen.MIXEN_NOTIFY_CODE);
        }
        super.onTaskRemoved(rootIntent);
    }



        @Override
    public void onDestroy() {
        Log.d(Mixen.TAG, "Ending Mixen Service.");
        if (player != null)
        {
            resetAndStopPlayer();
            player.release();
            isRunning = false;
            stopForeground(true);
            unregisterReceiver(noisyAudioReciever);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Mixen.MIXEN_NOTIFY_CODE);
        }
        super.onDestroy();
    }

    private class NoisyAudioReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
               doAction(getApplicationContext(), pause);
            }
        }
    }


}