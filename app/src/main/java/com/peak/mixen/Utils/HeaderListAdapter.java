package com.peak.mixen.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.peak.mixen.R;

import java.util.ArrayList;

public class HeaderListAdapter extends ArrayAdapter<HeaderListCell> {

	LayoutInflater inflater;
	public HeaderListAdapter(Context context, ArrayList<HeaderListCell> items) {
		super(context, 0, items);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		HeaderListCell cell = getItem(position);
		
		//If the cell is a section header we inflate the header layout 
		if(cell.isSectionHeader())
		{
			v = inflater.inflate(R.layout.section_header, null);
			
			v.setClickable(false);
			
			TextView header = (TextView) v.findViewById(R.id.section_header);
			header.setText(cell.getName());
		}
		else
		{
			v = inflater.inflate(R.layout.list_item, null);
			TextView name = (TextView) v.findViewById(R.id.name);
			TextView category = (TextView) v.findViewById(R.id.extra);
			
			name.setText(cell.getName());
			category.setText(cell.getCategory());
			
		}
		return v;
	}
}
