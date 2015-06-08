package com.peak.mixen;

import android.util.Log;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
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

    @JsonField
    public ArrayList<MetaTrack> clientQueue;
    @JsonField
    public int playServiceState;
    @JsonField
    public MetaTrack currentMetaTrack;
    @JsonField
    public int queueSongPosition;

    public PlaybackSnapshot(){}

    public PlaybackSnapshot(int playerServiceState)
    {
        this.playServiceState = playerServiceState;
        if(playerServiceState != INIT && playerServiceState != READY)
            populateNetworkQueue();
    }

    private void populateNetworkQueue()
    {
        if(clientQueue == null)
        {
            clientQueue = new ArrayList<>(MixenPlayerService.instance.spotifyQueue.size());
        }

        if(!MixenPlayerService.instance.spotifyQueue.isEmpty())
        {
            for(Track track : MixenPlayerService.instance.spotifyQueue)
            {
                for(MetaTrack metaTrack : MixenPlayerService.instance.clientQueue)
                {
                    if(track.id.equals(metaTrack.spotifyID))
                        return;
                }

                MixenPlayerService.instance.clientQueue.add(new MetaTrack(track));
            }
        }
    }

    public void updateNetworkPlaybackData()
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
        else if(!Mixen.isHost && Mixen.network.thisDevice.isRegistered)
        {
            Mixen.network.sendToHost(this, new SalutCallback() {
                @Override
                public void call() {
                    Log.e(Mixen.TAG, "Failed to send network queue data.");
                }
            });
        }
    }

    public void updatePlayerServiceState(int playerServiceState)
    {
        this.playServiceState = playerServiceState;
        populateNetworkQueue();
        updateNetworkPlaybackData();
    }

    public void updatePlayerServiceState(int playerServiceState, int queueSongPosition, MetaTrack currentMetaTrack)
    {
        this.currentMetaTrack = currentMetaTrack;
        this.playServiceState = playerServiceState;
        this.queueSongPosition = queueSongPosition;
        populateNetworkQueue();
        updateNetworkPlaybackData();
    }

}
