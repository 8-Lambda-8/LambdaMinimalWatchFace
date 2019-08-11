package com.a8lambda8.lambdaminimalwatchface;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import static com.a8lambda8.lambdaminimalwatchface.MyWatchFace.TAG;



/**
 * Set Akkzent color
 * Set App Launcher App
 */
public class ConfigActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        //WearableRecyclerView ConfigRecyclerView = findViewById(R.id.config_rec_view);

        ImageButton ConfigRecyclerView = findViewById(R.id.btn_shortcut_edit);



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

        //TODO config saving



    }
}
