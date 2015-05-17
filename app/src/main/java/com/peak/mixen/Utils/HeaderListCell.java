package com.peak.mixen.Utils;

import com.peak.mixen.SearchSongs;

import kaaes.spotify.webapi.android.models.TrackSimple;

public class HeaderListCell implements Comparable<HeaderListCell> {

	private String name;
	private String category;
	private boolean isSectionHeader;
	public TrackSimple trackSimple;
	public String hiddenCategory;
	
	public HeaderListCell(String name, String category)
	{
		this.name = name;
		this.category = category;
		isSectionHeader = false;
	}

	public HeaderListCell(String name, String category, String hiddenCategory)
	{
		this.name = name;
		this.category = category;
		if(hiddenCategory == null)
		{
			this.hiddenCategory = "HEADER_OR_OTHER";
		}
		else
		{
			this.hiddenCategory = hiddenCategory;
		}
		isSectionHeader = false;
	}

	public HeaderListCell(TrackSimple track)
	{
		this.name = track.name;
		this.category = SearchSongs.humanReadableTimeString(track.duration_ms);
		this.hiddenCategory = "SONG";
		this.trackSimple = track;
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
