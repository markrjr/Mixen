package com.peak.mixen;


import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.mixen.Utils.SongQueueListAdapter;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MixenPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener, SalutDataCallback,
                                                            ConnectionStateCallback, PlayerNotificationCallback{

    public static final String play = "ACTION_PLAY";
    public static final String pause = "ACTION_PAUSE";
    public static final String rewind = "ACTION_REWIND";
    public static final String fastForward = "ACTION_FAST_FORWARD";
    public static final String skipToNext = "ACTION_SKIP_NEXT";
    public static final String skipToLast = "ACTION_SKIP_LAST";
    public static final String reset = "ACTION_RESET_PLAYER";
    public static final String preparePlayback = "ACTION_SETUP";
    public static final String changePlayBackState = "ACTION_CHANGE_PLAYBACK_STATE";
    public static final String replayTrack = "ACTION_RESTART_TRACK_FROM_BEGINNING";
    public static final String init = "ACTION_INIT_MIXEN_PLAYER_SERVICE";


    public static MixenPlayerService instance;
    public Player spotifyPlayer;
    public Track currentTrack;
    public boolean playerIsPlaying;
    private MediaSessionCompat mediaSession;

    private NoisyAudioReciever noisyAudioReciever;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private AudioManager audioManager;
    private int originalMusicVolume;
    private boolean isBuffering = false;

    public boolean playerHasTrack = false;
    //A catch all boolean for when the player may not actually be playing, but still has a track loaded.

    public int queueSongPosition;

    public boolean pausedForPhoneCall = false;
    public boolean pausedUnexpectedly = false;
    public boolean isRunning = false;
    public boolean serviceIsBusy = true;
    //Another catch all boolean for when the service is fetching data, and cannot handle another request.

    public MetaTrack currentMetaTrack;
    public ArrayList<Track> spotifyQueue;
    public ArrayList<MetaTrack> clientQueue;
    public PlaybackSnapshot playerServiceSnapshot;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent == null || intent.getAction() == null)
            return super.onStartCommand(intent, flags, startId);

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

    private void initService()
    {
        spotifyQueue = new ArrayList<>();
        clientQueue = new ArrayList<>();
        playerServiceSnapshot = new PlaybackSnapshot(PlaybackSnapshot.INIT);

        if(Mixen.isHost)
        {
            setupPhoneListener();
            audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            noisyAudioReciever = new NoisyAudioReciever();
        }

        ComponentName mixenService = new ComponentName(getApplicationContext(), MixenPlayerService.class);
        PendingIntent mixenIntent = PendingIntent.getService(getApplicationContext(), 11, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);


        mediaSession = new MediaSessionCompat(getApplicationContext(), "Mixen Player Service", mixenService, mixenIntent);

        isRunning = true;
        instance = this;

        Log.d(Mixen.TAG, "Mixen Player Service successfully initialized.");
        playerServiceSnapshot = new PlaybackSnapshot(PlaybackSnapshot.READY);
    }

    public void setMediaMetaData(int playbackState)
    {

        MediaMetadataCompat mediaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentMetaTrack.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentMetaTrack.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, currentMetaTrack.albumArtURL)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentMetaTrack.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentMetaTrack.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentMetaTrack.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentMetaTrack.albumName)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentMetaTrack.albumArt)
                .build();

        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder();
                state.setActions(
                        PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

                state.setState(playbackState, (long) MixenBase.mixenPlayerFrag.arcProgressBar.getProgress(), 1f);

        mediaSession.setPlaybackState(state.build());
        
        mediaSession.setMetadata(mediaData);

        if(!mediaSession.isActive())
        {
            mediaSession.setActive(true);

        }
    }

    public void handleIntent(Intent intent)
    {
        switch (intent.getAction())
        {
            case preparePlayback:
                preparePlayback();
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
                return;
            case init:
                initService();
                return;
            case replayTrack:
                //restartTrackFromBeginning();
                return;
        }
    }

    public boolean queueIsEmpty()
    {
        return spotifyQueue.isEmpty();
    }

    public boolean queueHasASingleTrack()
    {
        if (spotifyQueue.size() == 1) return true;
        else return false;
    }

    public MetaTrack getNextMetaTrack()
    {
        try
        {
            return clientQueue.get(instance.queueSongPosition + 1);
        }
        catch (Exception ex)
        {
            Log.i(Mixen.TAG, "No song was found after the current one in the queue.");
        }

        return null;
    }

    public Track getNextTrack()
    {
        try
        {
            return spotifyQueue.get(instance.queueSongPosition + 1);
        }
        catch (Exception ex)
        {
            Log.i(Mixen.TAG, "No song was found after the current one in the queue.");
        }

        return null;
    }

    public Track getLastTrack()
    {
        if(queueSongPosition == 0)
        {
            return null;
        }
        else
        {
            try
            {
                return spotifyQueue.get(queueSongPosition - 1);
            }
            catch (Exception ex)
            {
                Log.i(Mixen.TAG, "The queue does not have a track before this one.");
                return null;
            }
        }

    }

    private void setPlaybackState()
    {
        if(playerHasTrack)
        {
            if(playerIsPlaying)
            {
                pausePlayback();
            }
            else
            {
                resumePlayback();
            }
        }
    }

    private void resetAndStopPlayer()
    {
        if(playerHasTrack)
        {
            if(playerIsPlaying)
            {
                spotifyPlayer.pause();
                stopForeground(true);
            }
            if(serviceIsBusy)
            {
                MixenBase.mixenPlayerFrag.bufferPB.setVisibility(View.INVISIBLE);
            }
            unregisterReceiver(noisyAudioReciever);
            playerServiceSnapshot.updateNetworkPlayerState(PlaybackSnapshot.STOPPED);
            MixenBase.mixenPlayerFrag.cleanUpUI();
            audioManager.abandonAudioFocus(this);
            setMediaMetaData(PlaybackStateCompat.STATE_STOPPED);
            mediaSession.setActive(false);
        }

        playerHasTrack = false;

    }

    private void skipToNextSong()
    {
        resetAndStopPlayer(); //TODO Redundant call from SongQueueFrag?
        playerHasTrack = false;
        queueSongPosition +=1;
        preparePlayback();
        Log.d(Mixen.TAG, "Skipping songs to " + currentMetaTrack.name);
    }

    private void skipToLastSong()
    {
        resetAndStopPlayer();
        playerHasTrack = false;
        queueSongPosition -=1;
        preparePlayback();
        Log.d(Mixen.TAG, "Going back to " + currentMetaTrack.name);
    }

    private void preparePlayback(){

        if(Mixen.isHost)
        {
            serviceIsBusy = true;
            Log.d(Mixen.TAG, "Signaling playback, it should begin shortly.");
            MixenBase.mixenPlayerFrag.hideUIControls(false);
            currentTrack = spotifyQueue.get(queueSongPosition);
            currentMetaTrack = new MetaTrack(currentTrack);
            MixenBase.mixenPlayerFrag.prepareUI();
            if(hasAudioFocus())
            {
                spotifyPlayer.play(currentTrack.uri);
            }
        } else
        {
            MixenBase.songQueueFrag.updateClientQueueUI();
            MixenBase.mixenPlayerFrag.prepareUI();
            MixenBase.mixenPlayerFrag.showOrHidePlayBtn(playerServiceSnapshot);
            MixenBase.mixenPlayerFrag.showSongProgressViews();
            Log.d(Mixen.TAG, "Syncing playback, it should begin shortly.");

        }
    }

    private void rewindPlayer()
    {
        if (playerIsPlaying || playerHasTrack)
        {
            spotifyPlayer.getPlayerState(new PlayerStateCallback() {
                @Override
                public void onPlayerState(PlayerState playerState) {

                    if(playerState.positionInMs - 30000 < 0)
                    {
                        spotifyPlayer.seekToPosition(0);
                    }
                    else
                    {
                        spotifyPlayer.seekToPosition(playerState.positionInMs - 30000);
                    }
                }
            });
        }
    }

    private void fastForwardPlayer()
    {
        if (playerIsPlaying || playerHasTrack)
        {
            spotifyPlayer.getPlayerState(new PlayerStateCallback() {
                @Override
                public void onPlayerState(PlayerState playerState) {

                    if(playerState.positionInMs + 30000 > currentMetaTrack.duration)
                    {
                        Log.d(Mixen.TAG, "User tried to seek past track length.");
                        return;
                    }
                    else
                    {
                        spotifyPlayer.seekToPosition(playerState.positionInMs + 30000); //Fast forward 30 seconds.
                        Log.i(Mixen.TAG, "Seeking forward 30 seconds.");
                    }

                }
            });
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
    public void onLoggedIn() {
        Log.d(Mixen.TAG, "Successfully logged in user.");
        StartScreen.instance.restoreControls();
        StartScreen.instance.startActivity(StartScreen.createNewMixen);
    }

    @Override
    public void onLoggedOut() {
        Log.d(Mixen.TAG, "User has logged out.");
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
        Log.e(Mixen.TAG, "Failed to authenticate user. Reason: " + throwable.getMessage());

        StartScreen.instance.restoreControls();

        String content = "We had trouble logging into Spotify, please check your username and password and try again later.";

        if(throwable.getMessage().contains("Premium"))
        {
            content = "Right now you need a Spotify Premium account to stream songs with Mixen.";
        }

        new MaterialDialog.Builder(StartScreen.instance)
                .title("Bummer :(")
                .neutralText("Okay")
                .content(content)
                .build()
                .show();
    }

    @Override
    public void onTemporaryError() {
        Log.d(Mixen.TAG, "Failed to authenticate user, this is a temporary error.");
    }

    @Override
    public void onConnectionMessage(String s) {
        Log.d(Mixen.TAG, "Received connection message: " + s);
    }

    @Override
    public void onAudioFocusChange(int audioChange) {
        if(MixenPlayerService.instance.playerIsPlaying)
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

//    @Override
//    public boolean onInfo(MediaPlayer mediaPlayer, int action, int extra) {
//
//        if (action == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
//            isBuffering = true;
//            if(!playerIsPlaying())
//            {
//                MixenBase.mixenPlayerFrag.hideUIControls(false);
//            }
//            else
//            {
//                MixenBase.mixenPlayerFrag.hideUIControls(true);
//            }
//
//            bufferTimes++;
//            if(bufferTimes >= 3)
//            {
//                bufferTimes = 0;
//                player.pause();
//                Log.d(Mixen.TAG, "Buffer Times Exceeded.");
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        player.seekTo(player.getCurrentPosition());
//                    }
//                }, 5000);
//            }
//
//
//            Log.i(Mixen.TAG, "Buffering of media has begun.");
//
//        } else if (action == MediaPlayer.MEDIA_INFO_BUFFERING_END && player.isPlaying()) {
//            isBuffering = false;
//            MixenBase.mixenPlayerFrag.restoreUIControls();
//
//            Log.i(Mixen.TAG, "Buffering has stopped, and playback should have resumed.");
//        }
//        return true;
//    }

//    @Override
//    public void onSeekComplete(MediaPlayer mediaPlayer) {
//        //If the user fast forwards on rewinds, after the required seeking operating completes, restart the media player at
//        //the seek-ed to position.
//
//        Log.d(Mixen.TAG, "Seek operation complete.");
//        if (hasAudioFocus())
//        {
//            mediaPlayer.start();
//            setMediaMetaData(PlaybackStateCompat.STATE_PLAYING);
//
//            MixenBase.mixenPlayerFrag.restoreUIControls();
//            MixenBase.mixenPlayerFrag.updateProgressBar();
//            if(MixenBase.userHasLeftApp)
//            {
//                startForeground(Mixen.MIXEN_NOTIFY_CODE, updateNotification());
//            }
//        }
//
//    }

    public void onTrackCompletion()
    {
        Log.d(Mixen.TAG, "Playback has completed.");
        playerServiceSnapshot.updateNetworkPlayerState(PlaybackSnapshot.COMPLETED);

        if(!playerHasTrack)
        {
            //Skipping songs.
            return;
        }

        resetAndStopPlayer();

        //TODO Delete x amount of tracks if after x amount of completions.

        if (getNextTrack() == null) {
            //If the queue does not have a track after this one, stop everything.
            stopForeground(true);
            return;
        }
        else
        {
            skipToNextSong();
        }
    }

    public void onTrackStart()
    {
        serviceIsBusy = false;
        playerHasTrack = true;
        playerServiceSnapshot.updateNetworkPlayerState(PlaybackSnapshot.PLAYING, queueSongPosition, currentMetaTrack);
        MixenBase.mixenPlayerFrag.restoreUIControls();
        setMediaMetaData(PlaybackStateCompat.STATE_PLAYING);
        registerReceiver(noisyAudioReciever, intentFilter);

        if(MixenBase.userHasLeftApp)
        {
            startForeground(Mixen.MIXEN_NOTIFY_CODE, updateNotification(true));
        }

        Log.d(Mixen.TAG, "Playback has been prepared, now playing.");
        MixenBase.mixenPlayerFrag.updateProgressBar();
        MixenBase.mixenPlayerFrag.showSongProgressViews();

    }

    public void showLostPermissionError() {

        resetAndStopPlayer();
        serviceIsBusy = false;
        MixenBase.mixenPlayerFrag.bufferPB.setVisibility(View.INVISIBLE);

        Log.e(Mixen.TAG, "Lost permission to stream music from Spotify.");

        new MaterialDialog.Builder(MixenBase.mixenPlayerFrag.getActivity())
                .title("Bummer :(")
                .content(R.string.permission_error)
                .neutralText("Okay")
                .show();

    }

    public Notification updateNotification(boolean isPlaying){

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
        contentView.setTextViewText(R.id.title, currentMetaTrack.name);
        contentView.setTextViewText(R.id.text, currentMetaTrack.artist);

        RemoteViews bigContentView = new RemoteViews(Mixen.currentContext.getPackageName(), R.layout.player_notification_big);
        bigContentView.setTextViewText(R.id.status_bar_track_name, currentMetaTrack.name);
        bigContentView.setTextViewText(R.id.status_bar_artist_name, currentMetaTrack.artist);
        bigContentView.setTextViewText(R.id.status_bar_album_name, currentMetaTrack.albumName);

        if(isPlaying)
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

        Picasso.with(getApplicationContext())
                .load(currentMetaTrack.albumArtURL)
                .into(bigContentView, R.id.status_bar_album_art, Mixen.MIXEN_NOTIFY_CODE, notification);

        return notification;
    }

    public void resumePlayback()
    {
        if (hasAudioFocus())
        {
            spotifyPlayer.resume();
            MixenBase.mixenPlayerFrag.showOrHidePlayBtn(null);
            setMediaMetaData(PlaybackStateCompat.STATE_PLAYING);
            MixenBase.mixenPlayerFrag.updateProgressBar();
            if(MixenBase.userHasLeftApp)
            {
                startForeground(Mixen.MIXEN_NOTIFY_CODE, updateNotification(true));
            }
            if(Mixen.isHost)
            {
                playerServiceSnapshot.updateNetworkPlayerState(PlaybackSnapshot.RESUME);
            }
        }
    }

    public void pausePlayback()
    {
        spotifyPlayer.pause();
        MixenBase.mixenPlayerFrag.showOrHidePlayBtn(null);
        setMediaMetaData(PlaybackStateCompat.STATE_PAUSED);
        if(MixenBase.userHasLeftApp)
        {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(Mixen.MIXEN_NOTIFY_CODE, updateNotification(false));
            stopForeground(false);
        }
        if(Mixen.isHost)
        {
            playerServiceSnapshot.updateNetworkPlayerState(PlaybackSnapshot.PAUSED);
        }
    }


    public void setupPhoneListener() {

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call.

                    if (isRunning && MixenPlayerService.instance.playerIsPlaying) {
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

                    if (isRunning && MixenPlayerService.instance.playerIsPlaying) {
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
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    public void cleanUpAndShutdown()
    {
        Log.d(Mixen.TAG, "Ending Mixen Service.");
        if(spotifyPlayer != null)
        {
            isRunning = false;
            resetAndStopPlayer();
            stopForeground(true);

            try
            {
                Spotify.awaitDestroyPlayer(spotifyPlayer, 5, TimeUnit.SECONDS);
                //spotifyPlayer = null;
            }
            catch(Exception ex)
            {
                Log.d(Mixen.TAG, "Failed to shut down player correctly.");
            }
        }
        if(Mixen.network != null)
        {
            if(Mixen.isHost)
            {
                Mixen.network.stopNetworkService(false);
            }
            else if(Mixen.network.thisDevice.isRegistered)
            {
                Mixen.network.unregisterClient(null);
            }
        }

        Log.d(Mixen.TAG, "Stopped Mixen Service.");
    }

    public void handleNetworkData(PlaybackSnapshot hostPlaybackSnapshot)
    {
        playerServiceSnapshot = hostPlaybackSnapshot;

        if(playerServiceSnapshot.snapshotType == PlaybackSnapshot.QUEUE_UPDATE)
        {
            currentMetaTrack = hostPlaybackSnapshot.currentMetaTrack;
            queueSongPosition = hostPlaybackSnapshot.queueSongPosition;
            clientQueue.clear();
            clientQueue = hostPlaybackSnapshot.clientQueue;
            MixenBase.songQueueFrag.cellList.clear();
            MixenBase.songQueueFrag.cellList.addAll(SongQueueListAdapter.convertToListItems(clientQueue));
            MixenBase.mixenPlayerFrag.updateClientUpNext();
            MixenBase.songQueueFrag.updateClientQueueUI();
        }

        switch(hostPlaybackSnapshot.playServiceState)
        {
            case PlaybackSnapshot.PLAYING:
                preparePlayback();
                return;
            case PlaybackSnapshot.PAUSED:
                MixenBase.mixenPlayerFrag.showOrHidePlayBtn(hostPlaybackSnapshot);
                return;
            case PlaybackSnapshot.RESUME:
                MixenBase.mixenPlayerFrag.showOrHidePlayBtn(hostPlaybackSnapshot);
                return;
            case PlaybackSnapshot.COMPLETED:
                MixenBase.mixenPlayerFrag.cleanUpUI();
                return;

        }
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

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {

        switch (eventType)
        {
            case TRACK_END:
                onTrackCompletion();
                return;
            case TRACK_START:
                onTrackStart();
                return;
            case PAUSE:
                playerIsPlaying = false;
                return;
            case LOST_PERMISSION:
                showLostPermissionError();
                return;
            case PLAY:
                playerIsPlaying = true;
                return;
        }

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }

    @Override
    public void onDataReceived(Object data) {
        Log.d(Mixen.TAG, "Received network playback snapshot, now updating UI.");
        try
        {
            if(Mixen.isHost)
            {
                MetaTrack trackToAdd = LoganSquare.parse((String) data, MetaTrack.class);
                Mixen.spotify.getTrack(trackToAdd.spotifyID, new Callback<Track>() {
                    @Override
                    public void success(Track track, Response response) {
                        SearchSongs.addForHost(MixenBase.songQueueFrag.getActivity(), track);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        //TODO Send error to client that added song.
                    }
                });
            }
            else {

                final PlaybackSnapshot hostPlaybackState = LoganSquare.parse((String) data, PlaybackSnapshot.class);
                MixenBase.mixenPlayerFrag.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleNetworkData(hostPlaybackState);
                    }
                });
            }
        }
        catch (IOException ex)
        {
            Log.e(Mixen.TAG, "Failed to parse network data.");
            ex.printStackTrace();
        }
    }

    private class NoisyAudioReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()) && MixenPlayerService.instance != null && MixenPlayerService.instance.playerIsPlaying) {
               doAction(getApplicationContext(), pause);
            }
        }
    }


}