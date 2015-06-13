package com.peak.mixen.Utils;

import com.peak.mixen.MetaTrack;
import com.peak.mixen.SearchSongs;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.TrackSimple;

public class SongQueueListItem implements Comparable<SongQueueListItem> {

	protected String songName;
	protected String songArtist;
	protected int downVotes;
	protected int upVotes;
	protected String addedBy;
	public MetaTrack metaTrack;

	public SongQueueListItem(MetaTrack track)
	{
		this.songName = track.name;
		this.songArtist = track.artist;
		this.addedBy = track.addedBy;
		this.metaTrack = track;
		this.upVotes = track.upVotes;
        this.downVotes = track.downVotes;
	}

	@Override
	public int compareTo(SongQueueListItem other) {
		return this.songName.compareTo(other.songName);
	}
}
