package com.a8lambda8.lambdaminimalwatchface;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.a8lambda8.lambdaminimalwatchface.MyWatchFace.TAG;
import static com.a8lambda8.lambdaminimalwatchface.MyWatchFace.update;

/**
 * Set Akkzent color
 * Set App Launcher App
 */
public class ConfigActivity extends WearableActivity {

    public static final String MY_PREFS_NAME = "ConfigPrefs";
    public static final String shortcutAppIconFileName = "shortcutAppIcon.png";

    SharedPreferences SP;
    SharedPreferences.Editor SP_E;

    ImageView IV_shortcut;

    PackageManager pm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        //WearableRecyclerView ConfigRecyclerView = findViewById(R.id.config_rec_view);


        pm = ConfigActivity.this.getPackageManager();

        ImageButton BTN_EditShortcut = findViewById(R.id.btn_shortcut_edit);
        ImageButton BTN_RemoveShortcut = findViewById(R.id.btn_shortcut_remove);
        IV_shortcut = findViewById(R.id.iv_shortcut);

        SP = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        SP_E = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();

        RadioGroup radioGroup = findViewById(R.id.radioGroup_color);
        RadioButton rb_red = findViewById(R.id.radioButton_red);
        RadioButton rb_green = findViewById(R.id.radioButton_green);
        RadioButton rb_blue = findViewById(R.id.radioButton_blue);


        rb_red.setButtonTintList(ColorStateList.valueOf(Color.RED));
        rb_green.setButtonTintList(ColorStateList.valueOf(Color.GREEN));
        rb_blue.setButtonTintList(ColorStateList.valueOf(Color.BLUE));


        //Load current config:
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


        String shortcutPackageName = SP.getString("shortcutApp","");

        if(shortcutPackageName!=null) {
            try {
                IV_shortcut.setImageDrawable(pm.getApplicationIcon(shortcutPackageName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        //Load current config\


        BTN_EditShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> pkgAppsList = pm.queryIntentActivities( mainIntent, 0);

                //Log.d(TAG,""+pkgAppsList);

                Intent i = new Intent(ConfigActivity.this, AppSelectorActivity.class);
                i.putExtra("AppList",(ArrayList<ResolveInfo>)pkgAppsList);
                startActivityForResult(i, 1);

            }
        });
        BTN_RemoveShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SP_E.putString("shortcutApp", "");
                SP_E.apply();



            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = findViewById(checkedId);

                SP_E.putInt("outlineColor", Objects.requireNonNull(rb.getButtonTintList()).getDefaultColor());
                SP_E.apply();
                update = true;

            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                ResolveInfo result = data.getParcelableExtra("result");

                Log.d(TAG,"back in config: "+result.loadLabel(pm)+" = "+result.activityInfo.packageName);

                IV_shortcut.setImageDrawable(result.loadIcon(pm));

                //SP_E.p
                SP_E.putString("shortcutApp", result.activityInfo.packageName);
                SP_E.apply();

                Bitmap bm = drawableToBitmap(result.loadIcon(pm));

                OutputStream fOut = null;

                try {
                    fOut = new FileOutputStream(new File(getFilesDir()/*root*/, shortcutAppIconFileName));
                } catch (Exception ignored){}

                assert fOut != null;

                bm.compress(Bitmap.CompressFormat.PNG, 100, fOut);

                try {
                    fOut.flush();
                    fOut.close();
                } catch (IOException ignored) {}

            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult

    public Bitmap drawableToBitmap(Drawable d) {

        Bitmap bm = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);

        d.setBounds(0, 0, 125, 125);
        d.draw(canvas);

        Paint P = new Paint();
        P.setColor(Color.RED);
        P.setStrokeWidth(10);

        //canvas.drawRect(new Rect(),P);
        //canvas.drawColor(Color.RED);

        /*for(int i = 0;i<bm.getWidth();i++){
            Log.d(TAG, "x="+i+" pixel="+bm.getPixel(i,10));
        }*/

        return bm;
    }
}
