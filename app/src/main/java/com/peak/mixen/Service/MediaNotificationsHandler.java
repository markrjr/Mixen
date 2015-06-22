package com.peak.mixen.Service;

import android.support.v4.media.session.MediaSessionCompat;

public class MediaNotificationsHandler extends MediaSessionCompat.Callback{

    MixenPlayerService service;

    public MediaNotificationsHandler(MixenPlayerService service) {
        this.service = service;
    }


    @Override
    public void onPlay() {
        super.onPlay();
        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.play);
    }

    @Override
    public void onPause() {
        super.onPause();
        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.pause);
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.skipToNext);
    }

    @Override
    public void onSkipToPrevious() {
        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.skipToLast);
    }

    @Override
    public void onFastForward() {
        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.fastForward);
    }

    @Override
    public void onRewind() {
        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.rewind);
    }

    @Override
    public void onStop() {
        MixenPlayerService.doAction(service.getApplicationContext(), MixenPlayerService.reset);
    }
}
