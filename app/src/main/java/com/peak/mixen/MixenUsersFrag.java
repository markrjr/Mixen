package com.peak.mixen;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
    private ArrayAdapter queueAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_mixen_users, container, false);

        relativeLayout = (RelativeLayout)v.findViewById(R.id.relativeLayout);
        queueLV = (ListView)v.findViewById(R.id.queueLV);
        infoTV = (TextView)v.findViewById(R.id.infoTV);

        if(Mixen.network != null && Mixen.network.serviceIsRunning)
        {
            if(Mixen.network.registeredClients.isEmpty())
            {
                infoTV.setText(R.string.empty_users_queue);
            }
            else
            {
                populateNetworkListView();
            }
        }
        else
        {
            infoTV.setText("You're not hosting yet. :(");
        }

        return relativeLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        queueLV.invalidate();
    }

    public void setColors(int bgColor)
    {
        relativeLayout.setBackgroundColor(bgColor);
        queueLV.setBackgroundColor(bgColor);
        infoTV.setBackgroundColor(bgColor);
    }

    public void populateNetworkListView()
    {

        Log.d(Mixen.TAG, "Updating network queue.");

        infoTV.setVisibility(View.GONE);

        final ArrayList<String> nearbyUsers = Mixen.network.getReadableRegisteredNames();

        queueAdapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_1, android.R.id.text1, nearbyUsers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);

                if(position == 0 && !Mixen.isHost)
                {
                    text1.setText(nearbyUsers.get(position) + " - Host");
                }
                else
                {
                    text1.setText(nearbyUsers.get(position));
                }

                return view;
            }
        };

        // Assign adapter to ListView
        queueLV.setAdapter(queueAdapter);

        // ListView Item Click Listener
        queueLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {


            }

        });



        queueLV.setVisibility(View.VISIBLE);
    }




}

