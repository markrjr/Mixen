package com.peak.mixen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;

@JsonObject
public class MetaTrack {

    @JsonField
    public String name;
    @JsonField
    public String artist;
    @JsonField
    public String albumName;
    @JsonField
    public String albumArtURL;
    @JsonField
    public String spotifyID;
    @JsonField
    public String trackURI;
    @JsonField
    public int duration;
    @JsonField
    public int upVotes = 1;
    @JsonField
    public int downVotes = 0;
    @JsonField
    public String addedBy;
    @JsonField
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
