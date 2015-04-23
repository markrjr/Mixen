package com.peak.mixen;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.afollestad.materialdialogs.MaterialDialog;

import co.arcs.groove.thresher.Song;
import wseemann.media.FFmpegMediaPlayer;

public class MixenPlayerService extends Service implements  FFmpegMediaPlayer.OnPreparedListener, FFmpegMediaPlayer.OnErrorListener,
                                                            FFmpegMediaPlayer.OnCompletionListener, FFmpegMediaPlayer.OnInfoListener,
                                                            FFmpegMediaPlayer.OnSeekCompleteListener, AudioManager.OnAudioFocusChangeListener{

    public static final String play = "ACTION_PLAY";
    public static final String pause = "ACTION_PAUSE";
    public static final String rewind = "ACTION_REWIND";
    public static final String fastForward = "ACTION_FAST_FORWARD";
    public static final String skipToNext = "ACTION_SKIP_NEXT";
    public static final String skipToLast = "ACTION_SKIP_LAST";
    public static final String reset = "ACTION_RESET_PLAYER";
    public static final String preparePlayback = "ACTION_SETUP";
    public static final String getSongStreamURL = "ACTION_GET_SONG_URL";
    public static final String changePlayBackState = "ACTION_CHANGE_PLAYBACK_STATE";
    public static final String replayTrack = "ACTION_RESTART_TRACK_FROM_BEGINNING";
    public static final String init = "ACTION_INIT_MIXEN_PLAYER_SERVICE";


    public static MixenPlayerService instance;

    private FFmpegMediaPlayer player;
    private MediaSessionCompat mediaSession;

    private NoisyAudioReciever noisyAudioReciever;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private AudioManager audioManager;
    private int bufferTimes;
    private int originalMusicVolume;

    public boolean playerHasTrack = false;
    //A catch all boolean for when the player may not actually be playing, but still has a track loaded.

    public int queueSongPosition;

    public boolean pausedForPhoneCall = false;
    public boolean pausedUnexpectedly = false;
    public boolean isRunning = false;
    public boolean serviceIsBusy = true;
    //Another catch all boolean for when the service is fetching data, and cannot handle another request.

    public Map<String, Bitmap> previousAlbumArt;
    public Song currentSong;
    public MetaSong currentMetaSong;

    public ArrayList<Song> queuedSongs;
    public ArrayList<MetaSong> proposedSongs;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent == null || intent.getAction() == null)
            return super.onStartCommand(intent, flags, startId);

        if(player == null)
        {
            initService();
        }

        handleIntent(intent);

        return START_NOT_STICKY;
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

    public FFmpegMediaPlayer getPlayer() {
        return player;
    }
    public void initService()
    {

        queuedSongs = new ArrayList<Song>();
        proposedSongs = new ArrayList<MetaSong>();
        previousAlbumArt = new LinkedHashMap<>(10);

        if(Mixen.isHost)
        {
            initMusicPlayer();
            setupPhoneListener();
            audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            noisyAudioReciever = new NoisyAudioReciever();
        }

        if(Mixen.debugFeaturesEnabled)
        {
            mediaSession = new MediaSessionCompat(getApplicationContext(), "Mixen Player Service");
            mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        }

        isRunning = true;
        instance = this;

    }

    public void setMediaMetaData(int playbackState)
    {

        MediaMetadataCompat mediaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.getAlbumName())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentSong.getArtistName())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, currentMetaSong.albumArtURL)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.getArtistName())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getName())

                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentSong.getName())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentSong.getArtistName())

                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentMetaSong.albumArt)
                .build();

        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder();
                state.setActions(
                        PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
                state.setState(playbackState, (long)getCurrentSongProgress(), 1f);


        mediaSession.setPlaybackState(state.build());

        mediaSession.setMetadata(mediaData);
        if(!mediaSession.isActive())
        {
            mediaSession.setActive(true);

        }
    }

    public void initMusicPlayer(){

        player = new FFmpegMediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
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
            case preparePlayback:
                preparePlayback();
                return;
            case getSongStreamURL:
                retrieveSongStreamURL();
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
                return;
            case changePlayBackState:
                setPlaybackState();
            case init:
                return;
            case replayTrack:
                restartTrackFromBeginning();
                return;
        }
    }

    public void retrieveSongStreamURL()
    {
        MixenBase.mixenPlayerFrag.hideUIControls(false);
        currentSong = queuedSongs.get(queueSongPosition);
        currentMetaSong = proposedSongs.get(queueSongPosition);
        serviceIsBusy = true;
        currentMetaSong.getStreamURLForService();
        Log.i(Mixen.TAG, "Grabbing URL for next song and signaling playback, it should begin shortly.");
    }

    public int getCurrentSongProgress()
    {
        return (int)player.getCurrentPosition();
    }

    public int getCurrentSongDuration()
    {
        return (int)player.getDuration();
    }

    public boolean playerIsPlaying(){
        try{
            return player.isPlaying();
        }
        catch(Exception ex)
        {
            return false;
        }
    }

    public boolean queueIsEmpty()
    {
        return queuedSongs.isEmpty();
    }

    public boolean queueHasASingleTrack()
    {
        if (queuedSongs.size() == 1) return true;
        else return false;
    }

    public Song getNextTrack()
    {
        try
        {
            return queuedSongs.get(instance.queueSongPosition + 1);
        }
        catch (Exception ex)
        {
            Log.i(Mixen.TAG, "No song was found after the current one in the queue.");
        }

        return null;
    }

    public Song getLastTrack()
    {
        if(queueSongPosition == 0)
        {
            return null;
        }
        else
        {
            try
            {
                return queuedSongs.get(queueSongPosition - 1);
            }
            catch (Exception ex)
            {
                Log.i(Mixen.TAG, "The queue does not have a track before this one.");
                return null;
            }
        }

    }

    private void resetAndStopPlayer()
    {
        if(playerHasTrack)
        {
            if(playerIsPlaying())
            {
                player.pause();
                player.stop();
                stopForeground(true);
            }
            currentMetaSong.setAlreadyPlayed();
            player.reset();
            MixenBase.mixenPlayerFrag.cleanUpUI();
            audioManager.abandonAudioFocus(this);
            if(Mixen.debugFeaturesEnabled)
            {
                setMediaMetaData(PlaybackStateCompat.STATE_STOPPED);
                mediaSession.setActive(false);
            }
        }

        playerHasTrack = false;

    }

    private void skipToNextSong()
    {
        resetAndStopPlayer(); //TODO Redundant call from SongQueueFrag.
        playerHasTrack = false; //TODO Should be false?
        queueSongPosition +=1;
        retrieveSongStreamURL();
        Log.d(Mixen.TAG, "Skipping songs to " + currentSong.getName());
    }

    private void skipToLastSong()
    {
        resetAndStopPlayer();
        playerHasTrack = false; //TODO Should be false?
        queueSongPosition -=1;
        retrieveSongStreamURL();
        Log.d(Mixen.TAG, "Going back to " + currentSong.getName());
    }

    private void preparePlayback(){

        try
        {
            //Album art should be set here.
            player.setDataSource(currentMetaSong.streamURL);
            currentMetaSong.downloadAlbumArtForService(true);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setLooping(false);
        }
        catch (Exception ex)
        {
            Log.e(Mixen.TAG, "An error occurred, playback could not be started.");
            if(MixenBase.mixenPlayerFrag.isRunning)
            {
                MixenBase.mixenPlayerFrag.cleanUpUI();
            }
            new MaterialDialog.Builder(MixenBase.mixenPlayerFrag.getActivity())
                    .title("Bummer :(")
                    .content(R.string.generic_network_error)
                    .neutralText("Okay")
                    .show();
            ex.printStackTrace();
            MixenBase.mixenPlayerFrag.restoreUIControls();
            return;
        }

        MixenBase.mixenPlayerFrag.prepareHostUI();
        player.prepareAsync();
    }

    private void rewindPlayer()
    {
        if (player.isPlaying() || playerHasTrack)
        {
            player.pause();
            MixenBase.mixenPlayerFrag.hideUIControls(true);
            if(player.getCurrentPosition() - 30000 < 0)
            {
                player.seekTo(0);
            }
            else
            {
                player.seekTo(player.getCurrentPosition() - 30000);
            }
            //Log.i(Mixen.TAG, "Seeking backwards 30 seconds.");
        }

    }

    private void restartTrackFromBeginning()
    {
        if (player.isPlaying() || playerHasTrack) {
            player.pause();
            MixenBase.mixenPlayerFrag.hideUIControls(true);

            player.seekTo(0);
            Log.d(Mixen.TAG, "Restarting track from the beginning.");
        }
    }


    private void fastForwardPlayer()
    {
        if (player.isPlaying() || playerHasTrack)
        {

            if(player.getCurrentPosition() + 30000 > player.getDuration())
            {
                Log.d(Mixen.TAG, "User tried to seek past track length.");
                return;
            }

            player.pause();
            MixenBase.mixenPlayerFrag.hideUIControls(true);
            player.seekTo(player.getCurrentPosition() + 30000); //Fast forward 30 seconds.
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
        if(MixenPlayerService.instance.playerIsPlaying())
        {
            if(audioChange == AudioManager.AUDIOFOCUS_LOSS)
            {
                doAction(getApplicationContext(), MixenPlayerService.pause);
                pausedUnexpectedly = true;
                audioManager.abandonAudioFocus(this);
                Log.d(Mixen.TAG, "Abandoning audio focus.");
            }
            else if(audioChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
            {
                originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume / 2, AudioManager.FLAG_SHOW_UI);
                Log.v(Mixen.TAG, "Ducking Audio");
            }
            else if(audioChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            {
                //originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); //Also works, which is better?
                doAction(getApplicationContext(), MixenPlayerService.pause);
                pausedUnexpectedly = true;
            }
        }
        else
        {
            if(audioChange == AudioManager.AUDIOFOCUS_GAIN)
            {
                if(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != originalMusicVolume && !pausedUnexpectedly)
                {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, AudioManager.FLAG_SHOW_UI);
                    Log.v(Mixen.TAG, "Unducking Audio");
                }
                else if(pausedUnexpectedly && !pausedForPhoneCall)
                {
                    doAction(getApplicationContext(), MixenPlayerService.play);
                    pausedUnexpectedly = false;
                    Log.d(Mixen.TAG, "Resuming playback.");
                }
            }
        }
    }

    @Override
    public boolean onInfo(FFmpegMediaPlayer mediaPlayer, int action, int extra) {

        if (action == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            if(!playerIsPlaying())
            {
                MixenBase.mixenPlayerFrag.hideUIControls(false);
            }
            else
            {
                MixenBase.mixenPlayerFrag.hideUIControls(true);
            }

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
        return true;
    }

    @Override
    public void onSeekComplete(FFmpegMediaPlayer mediaPlayer) {
        //If the user fast forwards on rewinds, after the required seeking operating completes, restart the media player at
        //the seek-ed to position.

        Log.d(Mixen.TAG, "Seek operation complete.");
        if (hasAudioFocus())
        {
            mediaPlayer.start();
            if(Mixen.debugFeaturesEnabled)
            {
                setMediaMetaData(PlaybackStateCompat.STATE_PLAYING);
            }

            MixenBase.mixenPlayerFrag.restoreUIControls();
            MixenBase.mixenPlayerFrag.updateProgressBar();
            if(MixenBase.userHasLeftApp)
            {
                startForeground(Mixen.MIXEN_NOTIFY_CODE, updateNotification());
            }
        }

    }

    public void updateAlbumArtCache()
    {
        if(previousAlbumArt.size() > 9)
        {
            //Removes the oldest album art from the cache.
            for(Map.Entry<String, Bitmap> entry : previousAlbumArt.entrySet())
            {
                if(entry.getKey() != null)
                {
                    previousAlbumArt.remove(entry.getKey());
                    break;
                }

            }

            Log.d(Mixen.TAG, "Removed oldest album art");
        }

        if(!previousAlbumArt.containsKey(currentMetaSong.albumArtURL))
        {
            previousAlbumArt.put(currentMetaSong.albumArtURL, currentMetaSong.albumArt);
        }

    }


    @Override
    public void onCompletion(FFmpegMediaPlayer mediaPlayer) {

            unregisterReceiver(noisyAudioReciever);

            Log.d(Mixen.TAG, "Playback has completed.");
            resetAndStopPlayer();

            //TODO Delete x amount of tracks if after x amount of completions.

            if (getNextTrack() == null) {
                //If the queue does not have a track after this one, stop everything.
                stopForeground(true);
                return;
            }

            queueSongPosition++;
            retrieveSongStreamURL();

    }

    @Override
    public boolean onError(FFmpegMediaPlayer mediaPlayer, int action, int extra) {

        resetAndStopPlayer();
        serviceIsBusy = false;
        MixenBase.mixenPlayerFrag.bufferPB.setVisibility(View.INVISIBLE);

        Log.e(Mixen.TAG, "An error occurred whilst trying to stream down music.");

        new MaterialDialog.Builder(MixenBase.mixenPlayerFrag.getActivity())
                .title("Bummer :(")
                .content(R.string.generic_streaming_error)
                .neutralText("Okay")
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if(getNextTrack() != null)
                        {
                           skipToNextSong();
                        }
                    }
                })
                .show();

        return true;

    }

    @Override
    public void onPrepared(FFmpegMediaPlayer mediaPlayer) {
        //After the music player is ready to go, restore UI controls to the user,
        //setup some nice UI stuff, and finally, start playing music.

        serviceIsBusy = false;
        MixenBase.mixenPlayerFrag.restoreUIControls();

        if(hasAudioFocus())
        {
            mediaPlayer.start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MixenBase.mixenPlayerFrag.updateProgressBar();
                }
            }, 100);
            MixenBase.mixenPlayerFrag.showSongProgressViews();
            if(Mixen.debugFeaturesEnabled)
            {
                setMediaMetaData(PlaybackStateCompat.STATE_PLAYING);
            }
            playerHasTrack = true;
            currentMetaSong.setNowPlaying();
            registerReceiver(noisyAudioReciever, intentFilter);
            if(MixenBase.userHasLeftApp)
            {
                startForeground(Mixen.MIXEN_NOTIFY_CODE, updateNotification());
            }
            Log.i(Mixen.TAG, "Playback has been prepared, now playing.");
        }

    }

    public Notification updateNotification(){

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
            bigContentView.setImageViewBitmap(R.id.status_bar_album_art, currentMetaSong.albumArt);
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
        //PAUSE
        if(player.isPlaying())
        {
            player.pause();
            MixenBase.mixenPlayerFrag.showOrHidePlayBtn();

            if(MixenBase.userHasLeftApp)
            {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(Mixen.MIXEN_NOTIFY_CODE, updateNotification());
                stopForeground(false);
            }
            if(Mixen.debugFeaturesEnabled)
            {
                setMediaMetaData(PlaybackStateCompat.STATE_PAUSED);
            }
        }
        else if(playerHasTrack)
        {
            //PLAY
            if (hasAudioFocus())
            {
                player.start();

                MixenBase.mixenPlayerFrag.showOrHidePlayBtn();
                MixenBase.mixenPlayerFrag.updateProgressBar();

                if(MixenBase.userHasLeftApp)
                {
                    startForeground(Mixen.MIXEN_NOTIFY_CODE, updateNotification());
                }
                if(Mixen.debugFeaturesEnabled)
                {
                    setMediaMetaData(PlaybackStateCompat.STATE_PLAYING);
                }
            }
        }
    }


    public void setupPhoneListener() {

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call.

                    if (isRunning && MixenPlayerService.instance.playerIsPlaying()) {
                        doAction(getApplicationContext(), pause);
                        audioManager.abandonAudioFocus(MixenPlayerService.this);
                        pausedForPhoneCall = true;
                        Log.d(Mixen.TAG, "Incoming call, pausing playback.");

                    }

                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    if (isRunning && pausedForPhoneCall) {

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    doAction(getApplicationContext(), play);
                                    pausedForPhoneCall = false;
                                    Log.d(Mixen.TAG, "Resuming playback.");
                                }
                            }, 1000);
                        //Accounts for the delay in switching states from a phone call that has just ended.
                    }

                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                    //do all necessary action to pause the audio

                    if (isRunning && MixenPlayerService.instance.playerIsPlaying()) {
                        doAction(getApplicationContext(), pause);
                        audioManager.abandonAudioFocus(MixenPlayerService.this);
                        pausedForPhoneCall = true;
                        Log.d(Mixen.TAG, "Ongoing call, pausing playback.");
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

    public void cleanUpAndShutdown()
    {
        Log.d(Mixen.TAG, "Ending Mixen Service.");
        if (player != null)
        {
            if(playerHasTrack)
            {
                unregisterReceiver(noisyAudioReciever);
            }
            isRunning = false;
            resetAndStopPlayer();
            player.release();
            player = null;
            stopForeground(true);
        }
        if(Mixen.debugFeaturesEnabled)
        {
            if(Mixen.isHost)
            {
                Mixen.network.stopNetworkService();
            }
        }
        Log.d(Mixen.TAG, "Stopped Mixen Service.");
    }

    @Override
    public void onTaskRemoved (Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        cleanUpAndShutdown();
    }



        @Override
    public void onDestroy() {
        super.onDestroy();
        cleanUpAndShutdown();
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