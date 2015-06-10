package com.peak.mixen.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.peak.mixen.MetaTrack;
import com.peak.mixen.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class SongQueueListAdapter extends ArrayAdapter<SongQueueListItem> {

	LayoutInflater inflater;
	public SongQueueListAdapter(Context context, ArrayList<SongQueueListItem> items) {
		super(context, 0, items);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public static ArrayList<SongQueueListItem> convertToListItems(ArrayList<MetaTrack> metaTracks)
	{
		ArrayList<SongQueueListItem> listItems = new ArrayList<>(metaTracks.size());

		for(MetaTrack track : metaTracks)
		{
			listItems.add(new SongQueueListItem(track));
		}

		return listItems;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View v = convertView;
		SongQueueListItem listItem = getItem(position);

		if(convertView == null) {
			v = inflater.inflate(R.layout.song_list_item, null);
			TextView songName = (TextView) v.findViewById(R.id.songName);
			TextView artistName = (TextView) v.findViewById(R.id.artistName);
			TextView addedBy = (TextView) v.findViewById(R.id.addedBy);
			ImageView downIcon = (ImageView) v.findViewById(R.id.downIcon);
			ImageView upIcon = (ImageView) v.findViewById(R.id.upIcon);
//			Picasso.with(getContext())
//					.load(R.drawable.ic_action_up)
//					.resize(R.dimen.vote_buttons_small, R.dimen.vote_buttons_small)
//					.into(upIcon);
//			Picasso.with(getContext())
//					.load(R.drawable.ic_action_down)
//					.resize(R.dimen.vote_buttons_small, R.dimen.vote_buttons_small)
//					.into(downIcon);
			songName.setText(listItem.songName);
			artistName.setText(listItem.songArtist);
			addedBy.setText("Added by " + listItem.addedBy);
		}

		return v;
	}
}
