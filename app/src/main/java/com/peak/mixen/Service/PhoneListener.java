package com.peak.mixen.Service;

import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.peak.mixen.Mixen;

public class PhoneListener extends PhoneStateListener{

    MixenPlayerService service;

    public PhoneListener(MixenPlayerService service) {
        this.service = service;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {

        if (state == TelephonyManager.CALL_STATE_RINGING) {
            //Incoming call.

            if (service.isRunning && service.playerIsPlaying) {
                MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.pause);
                service.audioManager.abandonAudioFocus(service);
                service.pausedForPhoneCall = true;
                Log.d(Mixen.TAG, "Incoming call, pausing playback.");
            }

        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            if (service.isRunning && service.pausedForPhoneCall) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.play);
                        service.pausedForPhoneCall = false;
                        Log.d(Mixen.TAG, "Resuming playback.");
                    }
                }, 1000);
                //Accounts for the delay in switching states from a phone call that has just ended.
            }

        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            //A call is dialing, active or on hold
            //do all necessary action to pause the audio

            if (service.isRunning && service.playerIsPlaying && !service.pausedForPhoneCall) {
                MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.pause);
                service.audioManager.abandonAudioFocus(service);
                service.pausedForPhoneCall = true;
                Log.d(Mixen.TAG, "Ongoing call, pausing playback.");
            }

            super.onCallStateChanged(state, incomingNumber);
        }
    }
}
