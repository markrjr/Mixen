package com.peak.mixen.Service;

import android.app.Activity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.peak.mixen.Mixen;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark on 10/25/2015.
 */

public class CloudOps {

    public boolean canConnectToParty(CharSequence partyID) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Hosts");
        query.whereEqualTo("partyID", partyID.toString());
        try
        {
            List<ParseObject> possibleHosts = query.find();

            if(possibleHosts.size() > 0)
            {
                Log.d(Mixen.TAG, "Successfully found party.");
                Mixen.connectedHost = possibleHosts.get(0);
                return true;
            }
        }
        catch(Exception ex)
        {
            Log.e(Mixen.TAG, "Failed to connect to party");
        }

        return false;
    }
}

