package com.peak.mixen.Utils;

public class HeaderListCell implements Comparable<HeaderListCell> {

	private String name;
	private String category;
	private boolean isSectionHeader;
	public String hiddenCategory;
	
	public HeaderListCell(String name, String category)
	{
		this.name = name;
		this.category = category;
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
