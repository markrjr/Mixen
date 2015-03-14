package com.peak.mixen;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.support.annotation.Nullable;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class MixenUsersFrag extends Fragment{

    private ListView queueLV;
    private TextView infoTV;
    private RelativeLayout relativeLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_mixen_users, container, false);

        relativeLayout = (RelativeLayout)v.findViewById(R.id.relativeLayout);
        queueLV = (ListView)v.findViewById(R.id.queueLV);
        infoTV = (TextView)v.findViewById(R.id.infoTV);

        return relativeLayout;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isAdded() && isVisibleToUser) {
            populateNetworkListView();
        }

    }

    public void setColors(int bgColor)
    {
        relativeLayout.setBackgroundColor(bgColor);
        queueLV.setBackgroundColor(bgColor);
        infoTV.setBackgroundColor(bgColor);
    }

    public void populateNetworkListView()
    {
        if(Mixen.network.foundDevices.isEmpty())
        {
            return;
        }

        infoTV.setVisibility(View.GONE);

        final ArrayList<Object> nearbyUsers = new ArrayList<>(Arrays.asList(Mixen.network.foundDevices.keySet().toArray()));

        ArrayAdapter adapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_1, android.R.id.text1, nearbyUsers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);

                text1.setText((String)nearbyUsers.get(position));
                return view;
            }
        };




        // Assign adapter to ListView
        queueLV.setAdapter(adapter);

        // ListView Item Click Listener
        queueLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item value
                //Song userSelection = (Song) songsLV.getItemAtPosition(position);

                //Log.i(Mixen.TAG, "Adding " + userSelection.getName() + " to song queue.");

            }

        });



        queueLV.setVisibility(View.VISIBLE);
    }




}

