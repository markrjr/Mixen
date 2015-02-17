package com.peak.mixen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MoreInfo extends Activity {

    Intent startingIntent;
    TextView emoticonTV;
    TextView moreInfoTV;
    Button backButton;
    RelativeLayout baseLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_info);

        startingIntent = getIntent();

        getActionBar().hide();

        emoticonTV = (TextView)findViewById(R.id.emoticonTV);
        moreInfoTV = (TextView)findViewById(R.id.moreInfoTV);
        backButton = (Button)findViewById(R.id.createMixenButton);
        baseLayout = (RelativeLayout) findViewById(R.id.baseLayout);


        int startReason = startingIntent.getExtras().getInt("START_REASON");

        setEmoticon(startReason);

        switch(startReason)
        {

            case Mixen.NO_NETWORK:
                moreInfoTV.setText(R.string.no_internet_connection);
                return;

            case Mixen.GENERIC_NETWORK_ERROR:
                moreInfoTV.setText(R.string.generic_network_error);
                return;

            case Mixen.SONG_NOT_FOUND:
                moreInfoTV.setText(R.string.song_not_found);
                return;

            case Mixen.GENERIC_STREAMING_ERROR:
                moreInfoTV.setText(R.string.generic_streaming_error);
                return;

        }
    }

    public void setEmoticon(int startReason) {

        if (startReason <= 3)
        {
            int sunsetOrange = getResources().getColor(R.color.Sunset_Orange);
            //Errors
            emoticonTV.setText(R.string.frowny_face);
            baseLayout.setBackgroundColor(sunsetOrange);
            emoticonTV.setBackgroundColor(sunsetOrange);
            backButton.setBackgroundColor(sunsetOrange);
            moreInfoTV.setBackgroundColor(sunsetOrange);


        }
        else
        {
            //emoticonTV.setText(R.string.too lazy to make, or should use something better.);
            int sanMarino = getResources().getColor(R.color.San_Marino);
            //Errors
            emoticonTV.setText(R.string.happy_face);
            baseLayout.setBackgroundColor(sanMarino);
            emoticonTV.setBackgroundColor(sanMarino);
            backButton.setBackgroundColor(sanMarino);
            moreInfoTV.setBackgroundColor(sanMarino);

        }

    }

    public void onBtnClicked(View v)
    {
        this.finish();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mixen_stage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }
}
