package com.peak.mixen.Utils;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.TrackSimple;

public class HeaderListCell implements Comparable<HeaderListCell> {

	public String name;
	public String category;
	private boolean isSectionHeader;
	public TrackSimple trackSimple;
	public AlbumSimple albumSimple;
	public String hiddenCategory;

	public HeaderListCell(String name, String hiddenCategory)
	{
		this.name = name;
		this.hiddenCategory= hiddenCategory;
		isSectionHeader = false;
	}

	public HeaderListCell(TrackSimple track)
	{
		this.name = track.name;
		this.category = track.artists.get(0).name;
		this.hiddenCategory = "SONG";
		this.trackSimple = track;
		isSectionHeader = false;
	}

	public HeaderListCell(AlbumSimple album)
	{
		this.name = album.name;
		this.hiddenCategory = "ALBUM";
		this.albumSimple = album;
		isSectionHeader = false;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getCategory()
	{
		return category;
	}
	
	public void setToSectionHeader()
	{
		isSectionHeader = true;
	}
	
	public boolean isSectionHeader()
	{
		return isSectionHeader;
	}
	
	@Override
	public int compareTo(HeaderListCell other) {
		return this.category.compareTo(other.category);
	}
}
