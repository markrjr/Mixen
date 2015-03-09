package com.peak.mixen;


import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
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

import co.arcs.groove.thresher.Song;

public class MixenPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
                                                            MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener, 
                                                            MediaPlayer.OnSeekCompleteListener, AudioManager.OnAudioFocusChangeListener {

    public static final String play = "ACTION_PLAY";
    public static final String pause = "ACTION_PAUSE";
    public static final String rewind = "ACTION_REWIND";
    public static final String fastForward = "ACTION_FAST_FORWARD";
    public static final String skip = "ACTION_SKIP";
    public static final String reset = "ACTION_RESET_PLAYER";
    public static final String setup = "ACTION_SETUP";
    public static final String changePlayBackState = "CHANGE_PLAYBACK_STATE";

    public static MixenPlayerService instance;

    private MediaPlayer player;

    private NoisyAudioReciever noisyAudioReciever;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private AudioManager audioManager;
    private int bufferTimes;

    public static int currentSongProgress;
    public static int currentSongAsInt;

    public static boolean stoppedPlayingUnexpectedly;
    public static boolean isRunning = false;

    public static Bitmap currentAlbumArt;
    public static String currentAlbumArtURL;
    public static String previousAlbumArtURL = "";
    public static Song currentSong;

    public static ArrayList<Song> queuedSongs;
    public static ArrayList<Song> proposedSongs;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent == null || intent.getAction() == null)
            return super.onStartCommand(intent, flags, startId);

        if(player == null)
        {
            audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            noisyAudioReciever = new NoisyAudioReciever();
            instance = this;
            setupPhoneListener();
            initMusicPlayer();
            isRunning = true;
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

    public boolean playerIsPlaying(){
        return player.isPlaying();
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
            case skip:
                skipToNextSong();
                return;
            case changePlayBackState:
                setPlaybackState();
        }
    }


    private void resetAndStopPlayer()
    {
        if(MixenPlayerFrag.playerHasTrack())
        {
            player.stop();
            player.reset();
            MixenPlayerFrag.cleanUpUI();
            audioManager.abandonAudioFocus(this);
        }

    }

    private void skipToNextSong()
    {
        Song nextSong = MixenPlayerFrag.getNextTrack();
        MixenPlayerFrag.cleanUpUI();
        //Update UI
        resetAndStopPlayer();
        MixenPlayerService.currentSong = nextSong;
        MixenPlayerService.currentSongAsInt++;
        MixenPlayerService.currentAlbumArtURL = Mixen.COVER_ART_URL + nextSong.getCoverArtFilename();
        MixenPlayerFrag.preparePlayback();
        Log.d(Mixen.TAG, "Skipping songs to " + nextSong.getName());
    }


    private void beginPlayback(){

        Uri streamURI;

        try
        {
            streamURI = Uri.parse(MixenPlayerFrag.retrieveURLsAsync.get().toString());
            //Album art should be set here.
            Log.i(Mixen.TAG, "Stream URL is " + streamURI.toString());
            Log.i(Mixen.TAG, "Track ID is " + currentSong.getId());
            player.setDataSource(Mixen.currentContext, streamURI);
        }
        catch (Exception ex)
        {
            Log.e(Mixen.TAG, "An error occurred, playback could not be started.");
            if(MixenPlayerFrag.isRunning)
            {
                MixenPlayerFrag.cleanUpUI();
            }
            ex.printStackTrace();
            return;
        }

        MixenPlayerFrag.prepareUI();
        player.prepareAsync();

    }

    private void rewindPlayer()
    {
        if (player.isPlaying() || MixenPlayerFrag.playerHasTrack())
        {
            currentSongProgress = player.getCurrentPosition();
            player.pause();
            MixenPlayerService.stoppedPlayingUnexpectedly = false;
            MixenPlayerFrag.hideUIControls();
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
        if (player.isPlaying() || MixenPlayerFrag.playerHasTrack())
        {
            currentSongProgress = player.getCurrentPosition();

            if(currentSongProgress + 30000 > player.getDuration())
            {
                Log.d(Mixen.TAG, "User tried to seek past track length.");
                return;
            }

            player.pause();
            MixenPlayerService.stoppedPlayingUnexpectedly = false;
            MixenPlayerFrag.hideUIControls();
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
            if(!MixenPlayerService.instance.playerIsPlaying() && MixenPlayerFrag.playerHasTrack())
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
            MixenPlayerFrag.hideUIControls();

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

            MixenPlayerFrag.restoreUIControls();

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
            MixenPlayerFrag.restoreUIControls();
            mediaPlayer.start();
        }

    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

            unregisterReceiver(noisyAudioReciever);

            Log.d(Mixen.TAG, "Playback has completed.");
            MixenPlayerFrag.cleanUpUI();

            //TODO Delete x amount of tracks if after x amount of completions.

            if (!MixenPlayerFrag.queueHasNextTrack()) { //TODO Implement other check here to fix bug.
                //If the queue does not have a track after this one, stop everything.
                stopForeground(true);
                mediaPlayer.reset();

                return;
            }

            currentSongAsInt++;
            MixenPlayerFrag.hideUIControls();
            MixenPlayerService.previousAlbumArtURL = MixenPlayerService.currentAlbumArtURL;
            currentSong = queuedSongs.get(currentSongAsInt);
            mediaPlayer.reset();
            
            MixenPlayerFrag.preparePlayback();

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int action, int extra) {

        mediaPlayer.reset();

        MixenPlayerFrag.showOrHidePlayBtn();

        Log.e(Mixen.TAG, "An error occurred whilst trying to stream down music.");

        Intent provideMoreInfo = new Intent(MixenPlayerService.this, MoreInfo.class);

        provideMoreInfo.putExtra("START_REASON", Mixen.GENERIC_STREAMING_ERROR);

        getApplicationContext().startActivity(provideMoreInfo);

        return false;

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //After the music player is ready to go, restore UI controls to the user,
        //setup some nice UI stuff, and finally, start playing music.

        MixenPlayerFrag.restoreUIControls();

        if(hasAudioFocus())
        {
            mediaPlayer.start();
            registerReceiver(noisyAudioReciever, intentFilter);
            if(MixenBase.userHasLeftApp)
            {
                updateNotification();
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


        changePlayBack.setAction(changePlayBackState);
        rewindIntent.setAction(rewind);
        fastForwardIntent.setAction(fastForward);


        PendingIntent changePlaybackPendingIntent = PendingIntent.getService(getApplicationContext(), 11, changePlayBack, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent rewindPendingIntent = PendingIntent.getService(getApplicationContext(), 11, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent fastForwardPendingIntent = PendingIntent.getService(getApplicationContext(), 11, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        RemoteViews contentView = new RemoteViews(Mixen.currentContext.getPackageName(), R.layout.player_notification);
        contentView.setImageViewResource(R.id.icon, R.drawable.mixen_icon);
        contentView.setTextViewText(R.id.title, MixenPlayerService.currentSong.getName());
        contentView.setTextViewText(R.id.text, MixenPlayerService.currentSong.getArtistName());

        RemoteViews bigContentView = new RemoteViews(Mixen.currentContext.getPackageName(), R.layout.player_notification_big);
        bigContentView.setTextViewText(R.id.status_bar_track_name, MixenPlayerService.currentSong.getName());
        bigContentView.setTextViewText(R.id.status_bar_artist_name, MixenPlayerService.currentSong.getArtistName());
        bigContentView.setTextViewText(R.id.status_bar_album_name, MixenPlayerService.currentSong.getAlbumName());

        if(MixenPlayerFrag.hasAlbumArt())
        {
            bigContentView.setImageViewBitmap(R.id.status_bar_album_art, currentAlbumArt);
        }

        if(player.isPlaying())
        {
            contentView.setImageViewResource(R.id.playbackState, R.drawable.pause);
            bigContentView.setImageViewResource(R.id.status_bar_play, R.drawable.pause);
        }
        else
        {
            contentView.setImageViewResource(R.id.playbackState, R.drawable.play);
            bigContentView.setImageViewResource(R.id.status_bar_play, R.drawable.play);

        }

        contentView.setOnClickPendingIntent(R.id.playbackState, changePlaybackPendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.status_bar_play, changePlaybackPendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.status_bar_next, fastForwardPendingIntent);
        bigContentView.setOnClickPendingIntent(R.id.status_bar_prev, rewindPendingIntent);


        Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
                .setAutoCancel(true)
                .setOngoing(true)
                .setSmallIcon(R.drawable.mixen_notification_icon);

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
        if(player.isPlaying() && MixenPlayerFrag.playerHasTrack())
        {
            currentSongProgress = player.getCurrentPosition();

            player.pause();
            MixenPlayerService.stoppedPlayingUnexpectedly = false;
            MixenPlayerFrag.showOrHidePlayBtn();
            updateNotification();
            //Log.i(Mixen.TAG, "Music playback has been paused.");
        }
        else if(MixenPlayerFrag.playerHasTrack())
        {
            player.seekTo(currentSongProgress);

            if(hasAudioFocus())
            {
                player.start();
                MixenPlayerFrag.showOrHidePlayBtn();
                updateNotification();
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
                    if (MixenPlayerService.isRunning  && MixenPlayerFrag.isRunning) {

                        if (MixenPlayerService.instance.playerIsPlaying()) {
                            MixenPlayerFrag.showOrHidePlayBtn();
                            doAction(getApplicationContext(), pause);
                            MixenPlayerService.stoppedPlayingUnexpectedly = true;
                        }
                    }

                } else if (state == TelephonyManager.CALL_STATE_IDLE && MixenPlayerService.isRunning) {

                    if (MixenPlayerService.instance.playerIsPlaying() && MixenPlayerFrag.isRunning) {

                        if (!MixenPlayerService.instance.playerIsPlaying() && MixenPlayerService.stoppedPlayingUnexpectedly) {
                            MixenPlayerFrag.showOrHidePlayBtn();
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
                    if (MixenPlayerService.isRunning && MixenPlayerFrag.isRunning) {

                        if (MixenPlayerService.instance.playerIsPlaying()) {

                            MixenPlayerFrag.showOrHidePlayBtn();
                            doAction(getApplicationContext(), pause);
                            MixenPlayerService.stoppedPlayingUnexpectedly = true;
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