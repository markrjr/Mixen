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
import android.widget.Toast;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.peak.mixen.Mixen;
import com.peak.mixen.R;


public class MixenUsersFrag extends Fragment{

    private ListView queueLV;
    private TextView infoTV;
    public RelativeLayout baseLayout;
    private ArrayAdapter queueAdapter;
    public boolean snackBarIsVisible = false;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_mixen_users, container, false);

        baseLayout = (RelativeLayout)v.findViewById(R.id.relativeLayout);
        queueLV = (ListView)v.findViewById(R.id.queueLV);
        infoTV = (TextView)v.findViewById(R.id.infoTV);

        return baseLayout;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUser && snackBarIsVisible) {
            SnackbarManager.dismiss();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        //updateNetworkUsersQueue();
    }
}
//
//    public void updateNetworkUsersQueue()
//    {
////        if(Mixen.network != null && Mixen.network.isRunningAsHost)
////        {
////            if(Mixen.network.registeredClients.isEmpty())
////            {
////                infoTV.setText(R.string.empty_users_queue);
////            }
////            else if(queueAdapter == null)
////            {
////                setupNetworkList();
////            }
////            else
////            {
////                queueAdapter.notifyDataSetChanged();
////                queueLV.invalidate();
////            }
////        }
////        else
////        {
////            infoTV.setText("You're not hosting yet. :(");
////        }
//    }
//
//    private void setupNetworkList()
//    {
//
//        Log.d(Mixen.TAG, "Updating networked users queue.");
//
//        infoTV.setVisibility(View.GONE);
//
//        queueAdapter = new ArrayAdapter(Mixen.currentContext, android.R.layout.simple_list_item_1, android.R.id.text1, Mixen.network.getReadableRegisteredNames()) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                View view = super.getView(position, convertView, parent);
//                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
//
//                if(position == 0 && !Mixen.isHost)
//                {
//                    text1.setText(Mixen.network.getReadableRegisteredNames().get(position) + " - Host");
//                }
//                else
//                {
//                    text1.setText(Mixen.network.getReadableRegisteredNames().get(position));
//                }
//
//                return view;
//            }
//        };
//
//        // Assign adapter to ListView
//        queueLV.setAdapter(queueAdapter);
//
//        // ListView Item Click Listener
//        queueLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view,
//                                    int position, long id) {
//
//                SalutDevice selected = Mixen.network.registeredClients.get(position);
//
//                if(snackBarIsVisible)
//                {
//                    SnackbarManager.dismiss();
//                }
//                else
//                {
//                    showSnackBar(selected);
//                }
//        }
//
//    });
//
//        queueLV.setVisibility(View.VISIBLE);
//    }
//
//    private void showSnackBar(final SalutDevice selected)
//    {
//        SnackbarManager.show(
//                Snackbar.with(getActivity().getApplicationContext())
//                        .text("Selected: " + selected.readableName)
//                        .actionColor(Color.YELLOW)
//                        .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
//                        .actionLabel("Kick User")
//                        .actionListener(new ActionClickListener() {
//                            @Override
//                            public void onActionClicked(Snackbar snackbar) {
//
//                                Mixen.network.sendToDevice(selected, "FORCE_UNREGISTER", new SalutCallback() {
//                                    @Override
//                                    public void call() {
//                                        Toast.makeText(getActivity(), "Failed to kick user, please try again later.", Toast.LENGTH_SHORT);
//                                    }
//                                });
//                            }
//                        })
//                        .eventListener(new EventListener() {
//                            @Override
//                            public void onShow(Snackbar snackbar) {
//                                snackBarIsVisible = true;
//                            }
//
//                            @Override
//                            public void onShowByReplace(Snackbar snackbar) {
//
//                            }
//
//                            @Override
//                            public void onShown(Snackbar snackbar) {
//
//                            }
//
//                            @Override
//                            public void onDismiss(Snackbar snackbar) {
//                            }
//
//                            @Override
//                            public void onDismissByReplace(Snackbar snackbar) {
//
//                            }
//
//                            @Override
//                            public void onDismissed(Snackbar snackbar) {
//                                snackBarIsVisible = false;
//                            }
//                        })
//                , getActivity());
//    }
//
//}

