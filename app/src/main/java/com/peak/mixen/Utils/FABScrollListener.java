package com.peak.mixen.Utils;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

public class FABScrollListener implements View.OnTouchListener{
    private ArrayList<View.OnTouchListener> listeners;
    private ListView listView;

    public FABScrollListener(ListView listView)
    {
        this.listView = listView;
        listeners = new ArrayList<>();
        listView.setOnTouchListener(this);
    }

    public void addFab(View v)
    {
        listeners.add(new ShowHideOnScroll(v));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        for(View.OnTouchListener listener : listeners)
        {
            listener.onTouch(v, event);
        }
        return false;
    }
}
