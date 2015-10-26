package com.peak.mixen.Service;

import com.peak.mixen.MetaTrack;

import java.util.ArrayList;

public class PlaybackSnapshot {

    public static final int PLAYING = 0;
    public static final int PAUSED = 1;
    public static final int STOPPED = 2;
    public static final int NONE = 5;
    public static final int READY = 6;
    public static final int RESUME = 7;
    public static final int INIT = 47;

    public static final int GENERAL_UPDATE = 101;
    public static final int QUEUE_UPDATE = 102;
    public static final int PLAYBACK_UPDATE = 103;
    public static final int OTHER_DATA = 104;


    public ArrayList<MetaTrack> remoteQueue;
    public int playServiceState;
    public MetaTrack currentMetaTrack;
    public int queueSongPosition;
    public int snapshotType;
    public MetaTrack trackToAdd;
    public static boolean explictAllowed = true;

    public PlaybackSnapshot(){}

    public PlaybackSnapshot(int playerServiceState)
    {
        this.playServiceState = playerServiceState;
    }

    private void updateNetworkPlaybackData()
    {
//        if(Mixen.isHost && Mixen.network != null && Mixen.network.isRunningAsHost && !Mixen.network.registeredClients.isEmpty())
//        {
//            Mixen.network.sendToAllDevices(this, new SalutCallback() {
//                @Override
//                public void call() {
//                    Log.e(Mixen.TAG, "Failed to send network queue data.");
//                }
//            });
//        }
//        else if(Mixen.network != null && !Mixen.isHost && Mixen.network.thisDevice.isRegistered)
//        {
//            trackToAdd = MixenPlayerService.instance.metaQueue.get(MixenPlayerService.instance.metaQueue.size() - 1);
//            Mixen.network.sendToHost(this, new SalutCallback() {
//                @Override
//                public void call() {
//                    Log.e(Mixen.TAG, "Failed to send network queue data.");
//                }
//            });
//        }
    }

    public void updateNetworkQueue()
    {
        this.snapshotType = QUEUE_UPDATE;
        this.remoteQueue = MixenPlayerService.instance.metaQueue;
        updateNetworkPlaybackData();
    }

    public void updateNetworkPlayerState(int playerServiceState)
    {

        this.snapshotType = PLAYBACK_UPDATE;
        this.playServiceState = playerServiceState;
        updateNetworkPlaybackData();
    }

    public void updateNetworkPlayerSettings()
    {
        this.snapshotType = OTHER_DATA;
        this.playServiceState = NONE;
        updateNetworkPlaybackData();
    }

    public void updateNetworkPlayer(int playerServiceState)
    {
        this.snapshotType = GENERAL_UPDATE;
        this.currentMetaTrack = MixenPlayerService.instance.currentTrack;
        this.playServiceState = playerServiceState;
        this.queueSongPosition = MixenPlayerService.instance.queueSongPosition;
        this.remoteQueue = MixenPlayerService.instance.metaQueue;
        updateNetworkPlaybackData();
    }
}
