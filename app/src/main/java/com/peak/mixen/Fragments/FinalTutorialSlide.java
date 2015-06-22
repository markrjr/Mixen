package com.peak.mixen.Fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.peak.mixen.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class FinalTutorialSlide extends android.support.v4.app.Fragment {


    public FinalTutorialSlide() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_final_tutorial_slide, container, false);

        ImageView googlePlus = (ImageView) v.findViewById(R.id.google_plus_link);
        googlePlus.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://plus.google.com/u/0/communities/105179969153892633137/members?cfem=1"));
                startActivity(intent);
            }
        });

        return v;
    }


}
