package com.peak.salut;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.os.Parcelable;
import android.util.Log;

import java.nio.charset.Charset;

/**
 * Created by Mark on 4/5/2015.
 */
public class SalutBeam {

    private static final String TAG = "SalutBeam";
    private static boolean nfcIsSupported;
    private Activity beamActivity;
    private IntentFilter intentFilter = new IntentFilter();
    private String mimeType;
    private NfcManager nfcManager;
    private NfcAdapter nfcAdapter;

    public SalutBeam(Activity activity) throws NFCNotEnabledException, NFCNotSupportedException
    {
        this.beamActivity = activity;

        nfcManager = (NfcManager) activity.getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager.getDefaultAdapter();

        if (nfcAdapter == null) {
            nfcIsSupported = false;
            throw(new NFCNotSupportedException("NFC is not supported on this device."));
        }
        else if(!nfcAdapter.isEnabled())
        {
            throw(new NFCNotEnabledException("NFC is not enabled."));
        }

        nfcIsSupported = true;
        mimeType = "application/" + beamActivity.getPackageName();

        intentFilter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        try
        {
            intentFilter.addDataType(mimeType);
        }
        catch (Exception ex)
        {
            Log.d(TAG, "Failed to properly set a mimeType based on application name, will use default of application/salutbeam");
            //TODO Set default here.
        }
    }

    private NdefMessage createBeamPayload(String data)
    {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("UTF-8"));

        //GENERATE PAYLOAD
        byte[] payLoad = data.getBytes();

        return new NdefMessage(new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, null, payLoad), NdefRecord.createApplicationRecord(beamActivity.getPackageName()));
    }

    public void beamPayload(final String data)
    {
        if(nfcIsSupported && nfcAdapter.isEnabled())
        {
            nfcAdapter = NfcAdapter.getDefaultAdapter(beamActivity);
            assert nfcAdapter != null;
            nfcAdapter.setNdefPushMessageCallback(
                    new NfcAdapter.CreateNdefMessageCallback() {
                        @Override
                        public NdefMessage createNdefMessage(NfcEvent event) {
                            return createBeamPayload(data);
                        }
                    }, beamActivity);
        }
    }


    private String extractPayload(Intent beamIntent)
    {
        Parcelable[] messages = beamIntent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage message = (NdefMessage) messages[0];
        NdefRecord record = message.getRecords()[0];
        return new String(record.getPayload());
    }

    public void onBeamRecieved(Intent beamIntent, SalutBeamCallback handleBeam)
    {
        if(nfcIsSupported && nfcAdapter.isEnabled()) {
            //See if app got called by AndroidBeam intent.
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(beamIntent.getAction())) {
                handleBeam.call(extractPayload(beamIntent));
            }
        }
    }

    private class NFCNotEnabledException extends Exception
    {
        public NFCNotEnabledException(String message) {
            super(message);
        }
    }

    private class NFCNotSupportedException extends Exception
    {
        public NFCNotSupportedException(String message) {
            super(message);
        }
    }

}
