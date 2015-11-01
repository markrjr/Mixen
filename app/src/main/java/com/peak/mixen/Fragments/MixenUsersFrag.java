package com.peak.mixen.Fragments;

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

import com.peak.mixen.Mixen;
import com.peak.mixen.R;

import java.util.ArrayList;
import java.util.Arrays;

public class MixenUsersFrag extends Fragment{

    private ListView queueLV;
    private TextView infoTV;
    public RelativeLayout baseLayout;
    private ArrayAdapter queueAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_mixen_users, container, false);

        baseLayout = (RelativeLayout)v.findViewById(R.id.relativeLayout);
        queueLV = (ListView)v.findViewById(R.id.queueLV);
        infoTV = (TextView)v.findViewById(R.id.infoTV);

        return baseLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNetworkUsersQueue();
    }

    public void updateNetworkUsersQueue()
    {
        if(Mixen.network != null && Mixen.network.isRunningAsHost)
        {
            if(Mixen.network.registeredClients.isEmpty())
            {
                infoTV.setText(R.string.empty_users_queue);
            }
            else if(queueAdapter == null)
            {
                setupNetworkList();
            }
            else
            {
                queueAdapter.notifyDataSetChanged();
                queueLV.invalidate();
            }
        }
        else
        {
            infoTV.setText("You're not hosting yet. :(");
        }
    }

    private void setupNetworkList()
    {

        Log.d(Mixen.TAG, "Updating networked users queue.");

        infoTV.setVisibility(View.GONE);

        queueAdapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_1, android.R.id.text1, Mixen.network.getReadableRegisteredNames()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);

                if(position == 0 && !Mixen.isHost)
                {
                    text1.setText(Mixen.network.getReadableRegisteredNames().get(position) + " - Host");
                }
                else
                {
                    text1.setText(Mixen.network.getReadableRegisteredNames().get(position));
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

