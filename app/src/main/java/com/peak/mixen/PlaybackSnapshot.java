package com.peak.mixen;

import android.util.Log;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.peak.mixen.Utils.SongQueueListAdapter;
import com.peak.salut.Callbacks.SalutCallback;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

@JsonObject
public class PlaybackSnapshot {

    public static final int PLAYING = 0;
    public static final int PAUSED = 1;
    public static final int STOPPED = 2;
    public static final int PREPARING = 3;
    public static final int COMPLETED = 4;
    public static final int BUSY = 5;
    public static final int READY = 6;
    public static final int RESUME = 7;
    public static final int INIT = 47;


    public static final int QUEUE_UPDATE = 23;
    public static final int PLAYBACK_UPDATE = 93;

    @JsonField
    public ArrayList<MetaTrack> remoteQueue;
    @JsonField
    public int playServiceState;
    @JsonField
    public MetaTrack currentMetaTrack;
    @JsonField
    public int queueSongPosition;
    @JsonField
    public int snapshotType;
    @JsonField
    public MetaTrack trackToAdd;

    public PlaybackSnapshot(){}

    public PlaybackSnapshot(int playerServiceState)
    {
        this.playServiceState = playerServiceState;
    }

    private void updateNetworkPlaybackData()
    {
        if(Mixen.isHost && Mixen.network != null && Mixen.network.isRunningAsHost && !Mixen.network.registeredClients.isEmpty())
        {
            Mixen.network.sendToAllDevices(this, new SalutCallback() {
                @Override
                public void call() {
                    Log.e(Mixen.TAG, "Failed to send network queue data.");
                }
            });
        }
        else if(Mixen.network != null && !Mixen.isHost && Mixen.network.thisDevice.isRegistered)
        {
            trackToAdd = MixenPlayerService.instance.metaQueue.get(MixenPlayerService.instance.metaQueue.size() - 1);
            Mixen.network.sendToHost(this, new SalutCallback() {
                @Override
                public void call() {
                    Log.e(Mixen.TAG, "Failed to send network queue data.");
                }
            });
        }
    }

    public void updateNetworkQueue()
    {
        this.snapshotType = QUEUE_UPDATE;
        this.remoteQueue = MixenPlayerService.instance.metaQueue;
        updateNetworkPlaybackData();
    }

    public void updateNetworkPlayerState(int playerServiceState)
    {
        this.playServiceState = playerServiceState;
        this.snapshotType = PLAYBACK_UPDATE;
        updateNetworkPlaybackData();
    }

    public void updateNetworkPlayer()
    {
        this.currentMetaTrack = MixenPlayerService.instance.currentTrack;
        this.playServiceState = MixenPlayerService.instance.playerServiceSnapshot.playServiceState;
        this.queueSongPosition = MixenPlayerService.instance.queueSongPosition;
        updateNetworkQueue();
    }

    public void updateNetworkPlayer(int playerServiceState, int queueSongPosition, MetaTrack currentMetaTrack)
    {
        this.currentMetaTrack = currentMetaTrack;
        this.playServiceState = playerServiceState;
        this.queueSongPosition = queueSongPosition;
        updateNetworkQueue();
    }

}
