package com.peak.mixen.Service;


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
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.mixen.Activities.MixenBase;
import com.peak.mixen.Activities.SearchSongs;
import com.peak.mixen.Activities.StartScreen;
import com.peak.mixen.MetaTrack;
import com.peak.mixen.Mixen;
import com.peak.mixen.R;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;
import com.squareup.picasso.Picasso;

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
    public MetaTrack currentTrack;
    public boolean playerIsPlaying;
    public MediaSessionCompat mediaSession;

    private NoisyAudioReciever noisyAudioReciever;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    protected AudioManager audioManager;
    private TelephonyManager telephonyManager;
    private PhoneListener checkForIncomingCalls;
    private int originalMusicVolume;
    private boolean pausedUnexpectedly = false;

    public boolean playerHasTrack = false;
    //A catch all boolean for when the player may not actually be playing, but still has a track loaded.

    public int queueSongPosition;

    public boolean pausedForPhoneCall = false;
    public boolean isRunning = false;
    public boolean serviceIsBusy = true;
    //Another catch all boolean for when the service is fetching data, and cannot handle another request.

    public ArrayList<MetaTrack> metaQueue;
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
        metaQueue = new ArrayList<>();
        playerServiceSnapshot = new PlaybackSnapshot(PlaybackSnapshot.INIT);

        if(Mixen.isHost && telephonyManager == null)
        {
            telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            noisyAudioReciever = new NoisyAudioReciever();
            checkForIncomingCalls = new PhoneListener(this);
            telephonyManager.listen(checkForIncomingCalls, PhoneStateListener.LISTEN_CALL_STATE);
        }

        ComponentName notificationsHandler = new ComponentName(getApplicationContext(), MediaNotificationsHandler.class);
        PendingIntent mixenIntent = PendingIntent.getService(getApplicationContext(), 11, new Intent(this, MixenBase.class), PendingIntent.FLAG_UPDATE_CURRENT);

        mediaSession = new MediaSessionCompat(getApplicationContext(), "Mixen Player Service", notificationsHandler, mixenIntent);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaNotificationsHandler(this));

        isRunning = true;
        instance = this;

        Log.d(Mixen.TAG, "Mixen Player Service successfully initialized.");
        playerServiceSnapshot = new PlaybackSnapshot(PlaybackSnapshot.READY);
    }

    public void setMetaDataAndState(int playbackState)
    {
        mediaSession.setPlaybackState(getMediaPlaybackState(playbackState));
        mediaSession.setMetadata(getMediaMetaData());
    }

    public void setMetaData()
    {
        mediaSession.setMetadata(getMediaMetaData());
    }

    private MediaMetadataCompat getMediaMetaData()
    {

        MediaMetadataCompat mediaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentTrack.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentTrack.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, currentTrack.albumArtURL)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentTrack.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTrack.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentTrack.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentTrack.albumName)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentTrack.albumArt)
                .build();

        return mediaData;
    }

    private PlaybackStateCompat getMediaPlaybackState(int playbackState)
    {
        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder();
        state.setActions(
                PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

        state.setState(playbackState, (long) MixenBase.mixenPlayerFrag.arcProgressBar.getProgress(), 1f);

        return state.build();
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

    private void removePlayedTracks()
    {
        if(metaQueue.size() > 5 && queueSongPosition > 2)
        {
            metaQueue.remove(0);
            metaQueue.remove(1);
            queueSongPosition--;
            queueSongPosition--;

            MixenBase.songQueueFrag.updateQueueUI();
        }
    }

    public boolean queueIsEmpty()
    {
        return metaQueue.isEmpty();
    }

    public boolean queueHasASingleTrack()
    {
        if (metaQueue.size() == 1) return true;
        else return false;
    }

    public MetaTrack getNextTrack()
    {
        try
        {
            return metaQueue.get(instance.queueSongPosition + 1);
        }
        catch (Exception ex)
        {
            Log.i(Mixen.TAG, "No song was found after the current one in the queue.");
        }

        return null;
    }

    public MetaTrack getLastTrack()
    {
        if(queueSongPosition == 0)
        {
            return null;
        }
        else
        {
            try
            {
                return metaQueue.get(queueSongPosition - 1);
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
            setMetaDataAndState(PlaybackStateCompat.STATE_STOPPED);
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
        Log.d(Mixen.TAG, "Skipping songs to " + currentTrack.name);
    }

    private void skipToLastSong()
    {
        resetAndStopPlayer();
        playerHasTrack = false;
        queueSongPosition -=1;
        preparePlayback();
        Log.d(Mixen.TAG, "Going back to " + currentTrack.name);
    }

    private void preparePlayback(){

        if(Mixen.isHost)
        {
            serviceIsBusy = true;
            Log.d(Mixen.TAG, "Signaling playback, it should begin shortly.");
            MixenBase.mixenPlayerFrag.hideUIControls(false);
            currentTrack = metaQueue.get(queueSongPosition);
            MixenBase.mixenPlayerFrag.prepareUI();
            if(hasAudioFocus())
            {
                spotifyPlayer.play(currentTrack.trackURI);
            }
        } else
        {
            MixenBase.songQueueFrag.updateQueueUI();
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

                    if(playerState.positionInMs + 30000 > currentTrack.duration)
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
                doAction(getApplicationContext(), MixenPlayerService.pause);
                pausedUnexpectedly = true;
                //We'll soon regain audio focus so do not abandon it.
            }
            else if(audioChange == AudioManager.AUDIOFOCUS_GAIN)
            {
                if(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != originalMusicVolume)
                {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, AudioManager.FLAG_SHOW_UI);
                    Log.v(Mixen.TAG, "Unducking Audio");
                }
            }
        }
        else
        {
            if(audioChange == AudioManager.AUDIOFOCUS_GAIN)
            {
                if(!pausedForPhoneCall && pausedUnexpectedly)
                {
                    doAction(getApplicationContext(), MixenPlayerService.play);
                    pausedUnexpectedly = false;
                    Log.d(Mixen.TAG, "Resuming playback.");
                }
            }
        }
    }

    public void onTrackCompletion()
    {
        Log.d(Mixen.TAG, "Playback has completed.");
        playerServiceSnapshot.updateNetworkPlayerState(PlaybackSnapshot.COMPLETED);

        removePlayedTracks();

        if(!playerHasTrack)
        {
            //Skipping songs.
            return;
        }

        resetAndStopPlayer();

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
        playerServiceSnapshot.updateNetworkPlayer(PlaybackSnapshot.PLAYING, queueSongPosition, currentTrack);
        MixenBase.mixenPlayerFrag.restoreUIControls();
        setMetaDataAndState(PlaybackStateCompat.STATE_PLAYING);
        mediaSession.setActive(true);
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
        contentView.setTextViewText(R.id.title, currentTrack.name);
        contentView.setTextViewText(R.id.text, currentTrack.artist);

        RemoteViews bigContentView = new RemoteViews(Mixen.currentContext.getPackageName(), R.layout.player_notification_big);
        bigContentView.setTextViewText(R.id.status_bar_track_name, currentTrack.name);
        bigContentView.setTextViewText(R.id.status_bar_artist_name, currentTrack.artist);
        bigContentView.setTextViewText(R.id.status_bar_album_name, currentTrack.albumName);

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
                .load(currentTrack.albumArtURL)
                .into(bigContentView, R.id.status_bar_album_art, Mixen.MIXEN_NOTIFY_CODE, notification);

        return notification;
    }

    public void resumePlayback()
    {
        if (hasAudioFocus())
        {
            spotifyPlayer.resume();
            MixenBase.mixenPlayerFrag.showOrHidePlayBtn(null);
            setMetaDataAndState(PlaybackStateCompat.STATE_PLAYING);
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
        setMetaDataAndState(PlaybackStateCompat.STATE_PAUSED);
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
                Log.d(Mixen.TAG, "Killed Spotify instance.");
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
                Mixen.network.stopNetworkService(StartScreen.wiFiBeforeLaunch);
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
        boolean explictAllowed = PlaybackSnapshot.explictAllowed;

        playerServiceSnapshot = hostPlaybackSnapshot;

        if(playerServiceSnapshot.snapshotType == PlaybackSnapshot.QUEUE_UPDATE)
        {
            currentTrack = hostPlaybackSnapshot.currentMetaTrack;
            queueSongPosition = hostPlaybackSnapshot.queueSongPosition;
            metaQueue = hostPlaybackSnapshot.remoteQueue;
            MixenBase.mixenPlayerFrag.updateUpNext();
            MixenBase.songQueueFrag.updateQueueUI();
        }
        else if (playerServiceSnapshot.snapshotType == PlaybackSnapshot.OTHER_DATA)
        {
            if(playerServiceSnapshot.explictAllowed != explictAllowed)
            {
                Toast.makeText(getApplicationContext(), "The host has restricted the party to clean songs only.", Toast.LENGTH_SHORT).show();
                playerServiceSnapshot.explictAllowed = explictAllowed;
            }

            return;
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
        try
        {
            if(Mixen.isHost)
            {
                Log.d(Mixen.TAG, "Received song request data, updating...");
                final PlaybackSnapshot clientPlaybackState = LoganSquare.parse((String) data, PlaybackSnapshot.class);
                SearchSongs.addTrackToQueue(MixenBase.songQueueFrag.getActivity(), clientPlaybackState.trackToAdd, false);
            }
            else {
                Log.d(Mixen.TAG, "Received network playback snapshot, now updating UI.");
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