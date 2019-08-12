package com.a8lambda8.lambdaminimalwatchface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.a8lambda8.lambdaminimalwatchface.MyWatchFace.TAG;

/**
 * Set Akkzent color
 * Set App Launcher App
 */
public class ConfigActivity extends WearableActivity {

    public static final String MY_PREFS_NAME = "ConfigPrefs";

    SharedPreferences SP;
    SharedPreferences.Editor SP_E;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        //WearableRecyclerView ConfigRecyclerView = findViewById(R.id.config_rec_view);

        ImageButton ConfigRecyclerView = findViewById(R.id.btn_shortcut_edit);

        SP = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        SP_E = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();

        RadioGroup radioGroup = findViewById(R.id.radioGroup_color);
        RadioButton rb_red = findViewById(R.id.radioButton_red);
        RadioButton rb_green = findViewById(R.id.radioButton_green);
        RadioButton rb_blue = findViewById(R.id.radioButton_blue);


        rb_red.setButtonTintList(ColorStateList.valueOf(Color.RED));
        rb_green.setButtonTintList(ColorStateList.valueOf(Color.GREEN));
        rb_blue.setButtonTintList(ColorStateList.valueOf(Color.BLUE));

        int outlineCol = SP.getInt("outlineColor",0);

        /*Log.d(TAG, "SP: "+outlineCol);

        Log.d(TAG, "C"+Color.RED+" "+Color.GREEN+" "+Color.BLUE);
        Log.d(TAG, "R"+getColor(R.color.red)+" "+getColor(R.color.green)+" "+getColor(R.color.blue));
        Log.d(TAG, "O"+Objects.requireNonNull(rb_red.getButtonTintList()).getDefaultColor()+
                " "+ Objects.requireNonNull(rb_green.getButtonTintList()).getDefaultColor()+
                " "+ Objects.requireNonNull(rb_blue.getButtonTintList()).getDefaultColor());*/

        switch(outlineCol) {
            case Color.RED:
                Log.d(TAG, "check red");
                radioGroup.check(R.id.radioButton_red);
                break;
            case Color.GREEN:
                Log.d(TAG, "check green");
                radioGroup.check(R.id.radioButton_green);
                break;
            case Color.BLUE:
                Log.d(TAG, "check blue");
                radioGroup.check(R.id.radioButton_blue);
                break;
        }

        ConfigRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG,""+v);

                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> pkgAppsList = ConfigActivity.this.getPackageManager().queryIntentActivities( mainIntent, 0);

                Log.d(TAG,""+pkgAppsList);

                Intent i = new Intent(ConfigActivity.this, AppSelectorActivity.class);
                i.putExtra("AppList",(ArrayList<ResolveInfo>)pkgAppsList);
                startActivityForResult(i, 1);

            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = findViewById(checkedId);

                SP_E.putInt("outlineColor", Objects.requireNonNull(rb.getButtonTintList()).getDefaultColor());
                SP_E.apply();
            }
        });

        //TODO config saving



    }
}
