package com.peak.mixen;

import android.content.Context;
import android.graphics.Bitmap;

import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;

public class MetaTrack {
    
    public String name;
    public String artist;
    public String albumName;
    public String albumArtURL;
    public String spotifyID;
    public String trackURI;
    public int duration;
    public int upVotes = 1;
    public int downVotes = 0;
    public String addedBy;
    public boolean explicit;
    public Bitmap albumArt;

    public MetaTrack(){}

    public MetaTrack(Track track)
    {
        this.name = track.name;
        this.artist = track.artists.get(0).name;
        this.trackURI = track.uri;
        this.albumName = track.album.name;
        this.albumArtURL = track.album.images.get(0).url;
        this.spotifyID = track.id;
        this.duration = (int)track.duration_ms;
        this.addedBy = Mixen.username;
        this.explicit = track.explicit;
    }

    public MetaTrack(TrackSimple track)
    {
        this.name = track.name;
        this.artist = track.artists.get(0).name;
        this.trackURI = track.uri;
        this.spotifyID = track.id;
        this.duration = (int)track.duration_ms;
        this.addedBy = Mixen.username;
        this.explicit = track.explicit;
    }
}
