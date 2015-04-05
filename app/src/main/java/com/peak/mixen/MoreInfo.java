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

        emoticonTV = (TextView)findViewById(R.id.emoticonTV);
        moreInfoTV = (TextView)findViewById(R.id.moreInfoTV);
        backButton = (Button)findViewById(R.id.createMixenButton);
        baseLayout = (RelativeLayout) findViewById(R.id.baseLayout);



    public void onBtnClicked(View v)
    {
        this.finish();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mixen_base, menu);
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
