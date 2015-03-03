package com.peak.mixen;

/**
 * Created by markrjr on 3/1/15.
 */
import java.util.ArrayList;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RemoteViews;

import co.arcs.groove.thresher.Song;

public class MixenPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
                                                            MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener {

    private static final String COVER_ART_URL = "http://images.gs-cdn.net/static/albums/";
    private static final String playAction = "ACTION_PLAY";
    private static final String pauseAction = "ACTION_PAUSE";
    private static final String stopAction = "ACTION_STOP";
    private static final String rewindAction = "ACTION_REWIND";
    private static final String fastForwardAction = "ACTION_FAST_FORWARD";
    private static final String skipAction = "ACTION_SKIP";
    private static final String resetPlayer = "ACTION_RESET_PLAYER";
    private static final String setupPlayer = "ACTION_SETUP";


    public static final Intent play = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(playAction);
    public static final Intent pause = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(pauseAction);
    public static final Intent stop = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(stopAction);
    public static final Intent rewind = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(rewindAction);
    public static final Intent fastForward = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(fastForwardAction);
    public static final Intent skip = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(skipAction);
    public static final Intent reset = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(resetPlayer);
    public static final Intent setup = new Intent(Mixen.currentContext, MixenPlayerService.class).setAction(setupPlayer);


    private static MediaPlayer player;

    private static final int NOTIFICATION_ID = 11;

    public static int currentSongProgress;
    public static int currentSongAsInt;
    public static int bufferTimes = 0;

    public static boolean stoppedPlayingUnexpectedly;
    public static boolean isRunning = false;


    public static String currentAlbumArt;
    public static String previousAlbumArt = "";
    public static Song currentSong;

    public static ArrayList<Song> queuedSongs;
    public static ArrayList<Song> proposedSongs;




    public void onCreate(){
        super.onCreate();

        setupPhoneListener();
        initMusicPlayer();
        isRunning = true;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent == null || intent.getAction() == null)
            return super.onStartCommand(intent, flags, startId);


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

    public static boolean playerIsPlaying(){
        return player.isPlaying();
    }


    public void handleIntent(Intent intent)
    {
        switch (intent.getAction())
        {
            case setupPlayer:
                beginPlayback();
                return;
            case playAction:
                Log.d(Mixen.TAG, "HANDLING PLAY INTENT.");
                setPlaybackState();
                return;
            case pauseAction:
                setPlaybackState();
                return;
            case rewindAction:
                rewindPlayer();
                return;
            case fastForwardAction:
                fastFowardPlayer();
                return;
            case resetPlayer:
                resetAndStopPlayer();
                return;
            case skipAction:
                skipToNextSong();
                return;
        }
    }

    private void resetAndStopPlayer()
    {
        if(MixenPlayerFrag.playerHasTrack())
        {
            player.stop();
            player.reset();
            MixenPlayerFrag.cleanUpUI();
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
        MixenPlayerService.currentAlbumArt = Mixen.COVER_ART_URL + nextSong.getCoverArtFilename();
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
            Log.i(Mixen.TAG, "Seeking backwards 30 seconds.");
        }

    }

    private void fastFowardPlayer()
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
            Log.i(Mixen.TAG, "Seeking forward 30 seconds.");
        }

    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int action, int extra) {

        if (action == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            bufferTimes++;
            //If the player has buffered more than 3 times recently.
            if (bufferTimes >= 3) {
                mediaPlayer.pause();
                stoppedPlayingUnexpectedly = false;
                bufferTimes = 0;
                Log.d(Mixen.TAG, "Max buffer times exceeded.");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Wait 5 seconds to buffer.
                        player.start();
                    }
                }, 5000);
            }

            if (MixenPlayerFrag.isRunning)
            {
                MixenPlayerFrag.hideUIControls();
            }

            Log.i(Mixen.TAG, "Buffering of media has begun.");

        } else if (action == MediaPlayer.MEDIA_INFO_BUFFERING_END && player.isPlaying()) {

            if (MixenPlayerFrag.isRunning)
            {
                MixenPlayerFrag.restoreUIControls();
            }

            Log.i(Mixen.TAG, "Buffering has stopped, and playback should have resumed.");
        }
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        //If the user fast forwards on rewinds, after the required seeking operating completes, restart the media player at
        //the seek-ed to position.

        Log.d(Mixen.TAG, "Seek operation complete.");
        MixenPlayerFrag.restoreUIControls();
        mediaPlayer.start();
    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

            MixenPlayerFrag.cleanUpUI();

            if (!MixenPlayerFrag.queueHasNextTrack()) {
                //If the queue does not have a track after this one, stop everything.
                mediaPlayer.reset();
                return;
            }

            MixenPlayerFrag.hideUIControls();

            MixenPlayerService.previousAlbumArt = MixenPlayerService.currentAlbumArt;

            queuedSongs.remove(queuedSongs.indexOf(currentSong));
            SongQueueFrag.updateQueueUI();
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

        startActivity(provideMoreInfo);

        return false;

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //After the music player is ready to go, restore UI controls to the user,
        //setup some nice UI stuff, and finally, start playing music.

        MixenPlayerFrag.restoreUIControls();

        mediaPlayer.start();
        startForeground(Mixen.MIXEN_NOTIFY_CODE, prepareNotif());

        Log.i(Mixen.TAG, "Playback has been prepared, now playing.");
    }

    private void updateNotification() {

        Notification notification = prepareNotif();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Mixen.MIXEN_NOTIFY_CODE, notification);
    }


    public Notification prepareNotif(){

        PendingIntent playPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, play, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, pause, PendingIntent.FLAG_UPDATE_CURRENT);


        RemoteViews contentView = new RemoteViews(Mixen.currentContext.getPackageName(), R.layout.player_notification);
        contentView.setImageViewResource(R.id.icon, R.drawable.mixen_icon);
        contentView.setTextViewText(R.id.title, MixenPlayerService.currentSong.getName());
        contentView.setTextViewText(R.id.text, MixenPlayerService.currentSong.getArtistName());


        if(player.isPlaying())
        {
            contentView.setImageViewResource(R.id.playbackState, R.drawable.pause);
            contentView.setOnClickPendingIntent(R.id.playbackState, pausePendingIntent);
        }
        else
        {
            contentView.setImageViewResource(R.id.playbackState, R.drawable.play);
            contentView.setOnClickPendingIntent(R.id.playbackState, playPendingIntent);
        }

        Notification.Builder mBuilder = new Notification.Builder(getApplicationContext())
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.mixen_notification_icon);


        final Intent notificationIntent = new Intent(getApplicationContext(), MixenBase.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        BroadcastReceiver recieveNotificationClicks = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action_name = intent.getAction();
                if (action_name.equals(playAction) || action_name.equals(pauseAction)) {
                    setPlaybackState();
                }
            };
        };

        //registerReceiver(recieveNotificationClicks, new IntentFilter(playAction));


        mBuilder.setContent(contentView);

        mBuilder.setContentIntent(contentIntent);

        return mBuilder.build();
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
            player.start();
            MixenPlayerFrag.showOrHidePlayBtn();
            updateNotification();
            //Log.i(Mixen.TAG, "Music playback has resumed.");

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

                        if (MixenPlayerService.playerIsPlaying()) {
                            MixenPlayerFrag.showOrHidePlayBtn();
                            startService(MixenPlayerService.pause);
                            MixenPlayerService.stoppedPlayingUnexpectedly = true;
                        }
                    }

                } else if (state == TelephonyManager.CALL_STATE_IDLE && MixenPlayerService.isRunning) {

                    if (MixenPlayerService.playerIsPlaying() && MixenPlayerFrag.isRunning) {

                        if (!MixenPlayerService.playerIsPlaying() && MixenPlayerService.stoppedPlayingUnexpectedly) {
                            MixenPlayerFrag.showOrHidePlayBtn();
                            startService(MixenPlayerService.play);
                            Log.d(Mixen.TAG, "Resuming playback.");

                            AudioManager audioManager =  (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                            audioManager.setMode(AudioManager.MODE_NORMAL);
                        }
                    }

                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                    //do all necessary action to pause the audio
                    if (MixenPlayerService.isRunning && MixenPlayerFrag.isRunning) {

                        if (MixenPlayerService.playerIsPlaying()) {

                            MixenPlayerFrag.showOrHidePlayBtn();
                            startService(MixenPlayerService.pause);
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
    public void onDestroy() {
        Log.d(Mixen.TAG, "Ending Mixen Service.");
        if (player != null)
        {
            //Assuming reset has been called before.
            player.release();
        }

        stopForeground(true);
    }


}