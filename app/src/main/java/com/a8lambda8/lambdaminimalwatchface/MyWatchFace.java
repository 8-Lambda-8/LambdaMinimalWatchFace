package com.a8lambda8.lambdaminimalwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.a8lambda8.lambdaminimalwatchface.ConfigActivity.MY_PREFS_NAME;
import static com.a8lambda8.lambdaminimalwatchface.ConfigActivity.shortcutAppIconFileName;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class MyWatchFace extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    public static final String TAG = "XXX";

    public static boolean update = false;


    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                if (msg.what == MSG_UPDATE_TIME) {
                    engine.handleUpdateTimeMessage();
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        /*private static final float FIRST_STROKE_WIDTH = 5f;
        private static final float SECOND_STROKE_WIDTH = 3f;
        private static final float THIRD_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;*/

        private static final int SHADOW_RADIUS = 8;
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;

        private int mClockColor = Color.WHITE;
        private int mAccentColor = Color.GREEN;

        /*private int mSecondColor = Color.LTGRAY;
        private int mThirdColor = Color.GRAY;*/

        private int mClockColorAmbiant = Color.GRAY;
        /*private int mSecondColorA = Color.GRAY;
        private int mThirdColorA = Color.DKGRAY;*/


        private Paint mClockPaint;
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mDatumPaint;
        private Paint mBatteryPaint;
        private Paint mAccentPaint;
        private Paint mShortcutPaint;


        //private Paint mClockThirdPaint;
        //private Paint mTickAndCirclePaint;
        private Paint mBackgroundPaint;
        /*private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;*/
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private boolean shortcutIsSet;
        private boolean shortcutAppIconGrayscale = false;
        private String shortcutAppPacketName = "notSet";
        private Bitmap shortcutAppBitmap;
        private Rect shortcutRect;

        private float[] mBatPts;
        private Rect mBatRect;

        private BatteryManager bm;

        SharedPreferences SP;
        //SharedPreferences.Editor SP_E;




        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();

            //onSurfaceChanged(holder,);

            //Shared Prefs
            SP = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            
            initializeBackground();
            initializeWatchFace();

            //Bat
            bm = (BatteryManager)getSystemService(BATTERY_SERVICE);


            /*//steps
            final DataSource ds = new DataSource.Builder()
                    .setAppPackageName("com.google.android.gms")
                    .setDataType(Element.DataType.TYPE_STEP_COUNT_DELTA)
                    .setType(DataSource.TYPE_DERIVED)
                    .setStreamName("estimated_steps")
                    .build();

            final DataReadRequest req = new DataReadRequest.Builder()
                    .aggregate(ds, Element.DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(timeBounds[0], timeBounds[1], TimeUnit.MILLISECONDS)
                    .build();

            DataReadRequest readreq = new DataReadRequest.Builder()
                    .addAggregatedDefaultDataSource(DataTypes.STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime)
                    .build();*/
            shortcutAppPacketName = SP.getString("shortcutApp","notSet");
            shortcutIsSet = !Objects.requireNonNull(shortcutAppPacketName).equals("notSet");

            if(shortcutIsSet){

                File directory = getFilesDir();
                File file = new File(directory, shortcutAppIconFileName);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                shortcutAppBitmap = BitmapFactory.decodeFile(file.getPath(), options);

            }else {
                Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                shortcutAppBitmap = Bitmap.createBitmap(64, 64, conf);
            }


        }

        private void initializeBackground() {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            /*mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);

            *//* Extracts colors from background image to improve watchface style. *//*
            Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    if (palette != null) {
                        mWatchHandHighlightColor = palette.getVibrantColor(Color.RED);
                        mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
                        mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                        updateWatchHandStyle();
                    }
                }
            });*/
        }

        void initializeWatchFace() {
            /* Set defaults for colors */


            mAccentColor = SP.getInt("outlineColor",0);
            Log.d(TAG, "initializeWatchFace: col="+mAccentColor);


            mClockPaint = new Paint();
            mClockPaint.setColor(mClockColor);
            mClockPaint.setStrokeWidth(2);
            mClockPaint.setAntiAlias(true);
            mClockPaint.setStrokeCap(Paint.Cap.ROUND);
            mClockPaint.setTextSize(60);

            mClockPaint.setTextAlign(Paint.Align.CENTER);

            Typeface font = ResourcesCompat.getFont(getApplicationContext(), R.font.scifi_adventure/*_monospace//*/);

            mClockPaint.setTypeface(font);

            mHourPaint = new Paint();
            mMinutePaint = new Paint();
            mSecondPaint = new Paint();
            mDatumPaint = new Paint();
            mBatteryPaint = new Paint();
            mAccentPaint = new Paint();
            mShortcutPaint = new Paint();

            mHourPaint.set(mClockPaint);
            //mHourPaint.setTextAlign(Paint.Align.RIGHT);
            mMinutePaint.set(mClockPaint);
            //mMinutePaint.setTextSize((int)(mHourPaint.getTextSize()*0.8));

            mSecondPaint.set(mClockPaint);
            mSecondPaint.setTextSize((int)(mMinutePaint.getTextSize()*0.6));

            mDatumPaint.set(mClockPaint);

            mDatumPaint.setTextSize(20);

            mBatteryPaint.set(mClockPaint);
            mBatteryPaint.setTextSize(15);

            mAccentPaint.set(mClockPaint);
            mAccentPaint.setColor(mAccentColor);


            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);
            mDatumPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);
            mBatteryPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);


            //shortcut

            shortcutAppPacketName = SP.getString("shortcutApp","notSet");
            shortcutIsSet = !Objects.requireNonNull(shortcutAppPacketName).equals("notSet");

            Log.d(TAG, "initializeWatchFace: "+shortcutAppPacketName);

            if(shortcutIsSet){

                File directory = getFilesDir();
                File file = new File(directory, shortcutAppIconFileName);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                shortcutAppBitmap = BitmapFactory.decodeFile(file.getPath(), options);

                int x = (int)mCenterX+100;
                int y = (int)mCenterY;

                shortcutRect = new Rect(x,y-shortcutAppBitmap.getHeight()/2,x+shortcutAppBitmap.getWidth(),y+shortcutAppBitmap.getHeight()/2);

                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setDither(true);

                if(shortcutAppIconGrayscale) {
                    ColorMatrix cm = new ColorMatrix();
                    cm.setSaturation(0);
                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
                    paint.setColorFilter(filter);
                }

            }

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
            if(update){
                initializeBackground();
                initializeWatchFace();

                update = false;
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            updateWatchStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchStyle() {
            if (mAmbient) {
                //mHourPaint.setColor(Color.BLACK);
                mHourPaint.setColor(mClockColorAmbiant);
                mMinutePaint.setColor(mClockColorAmbiant);
                //mSecondPaint.setColor(mClockColorAmbiant);


                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                //mSecondPaint.setAntiAlias(false);

                mHourPaint.setStyle(Paint.Style.STROKE);
                mMinutePaint.setStyle(Paint.Style.STROKE);


                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                /*mSecondPaint.clearShadowLayer();
                mBatteryPaint.clearShadowLayer();//*/


            } else {
                mHourPaint.setColor(mClockColor);
                mMinutePaint.setColor(mClockColor);
                //mSecondPaint.setColor(mClockColor);


                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                //mSecondPaint.setAntiAlias(true);


                //mHourPaint.clearShadowLayer();

                mHourPaint.setStyle(Paint.Style.FILL);
                mMinutePaint.setStyle(Paint.Style.FILL);


                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);
                /*mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);
                mBatteryPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mAccentColor);*/
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 100 : 255);

                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);


            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;//-20;


            createBatShape(/*width,*/height);

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            /*mSecondHandLength = (float) (mCenterX * 0.875);
            sMinuteHandLength = (float) (mCenterX * 0.75);
            sHourHandLength = (float) (mCenterX * 0.5);


            *//* Scale loaded background image (more efficient) if surface dimensions change. *//*
            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);*/

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            /*if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap();
            }*/
        }

        private void createBatShape(/*int width,*/int height){

            Rect BatBounds = new Rect();
            mBatteryPaint.getTextBounds("100",0,3,BatBounds);

            BatBounds.offset((int)mCenterX-BatBounds.width()/2,(int)(height-15-mMinutePaint.descent()));

            int offset = 5;
            BatBounds.set(BatBounds.left-offset,BatBounds.top-offset,BatBounds.right+offset,BatBounds.bottom+offset);

            mBatPts = new float[]{

                    BatBounds.left,//x1
                    BatBounds.top,//y1
                    BatBounds.right,//x2
                    BatBounds.top,//y2

                    BatBounds.right,//x2
                    BatBounds.top,//y2
                    BatBounds.right,//x3
                    BatBounds.bottom,//y3

                    BatBounds.right,//x3
                    BatBounds.bottom,//y3
                    BatBounds.left,//x4
                    BatBounds.bottom,//y4

                    BatBounds.left,//x4
                    BatBounds.bottom,//y4
                    BatBounds.left,//x1
                    BatBounds.top//y1

            };

            mBatRect = new Rect(BatBounds.right,BatBounds.top+8,BatBounds.right+5,BatBounds.bottom-8);

        }

        /*private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }*/

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:

                    if(shortcutRect.contains(x,y)){
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(shortcutAppPacketName);
                        if (launchIntent != null) {
                            startActivity(launchIntent);//null pointer check in case package name was not found
                        }

                    }

                    /*if( x<mCenterX+xArea
                        &&
                        x>mCenterX-xArea
                        &&
                        y<yArea*2
                        &&
                        y>0){

                        Intent

                        Toast.makeText(getApplicationContext(), "Tapped "+x+":"+y, Toast.LENGTH_SHORT)
                                .show();

                    }*/

                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawWatchFace(canvas);
        }

        private void drawBackground(Canvas canvas) {

            /*if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }*/

            canvas.drawColor(Color.BLACK);

            /*canvas.drawLine(mCenterX,0,mCenterX,canvas.getHeight(),mAccentPaint);
            canvas.drawLine(0,mCenterY,canvas.getWidth(),mCenterY,mAccentPaint);//*/

        }

        private void drawWatchFace(Canvas canvas) {

            canvas.save();


            /*dd-MM-yyyy*/

            //mClockFirstPaint.setTextAlign(Paint.Align.RIGHT);

            /*Log.d(TAG,String.format("H: %f; %f; %f;",mHourPaint.getFontSpacing(),mHourPaint.ascent(),mHourPaint.descent()));
            Log.d(TAG,String.format("M: %f; %f; %f;",mMinutePaint.getFontSpacing(),mMinutePaint.ascent(),mMinutePaint.descent()));
            Log.d(TAG,String.format("S: %f; %f; %f;",mSecondPaint.getFontSpacing(),mSecondPaint.ascent(),mSecondPaint.descent()));*/

            Rect bounds = new Rect();
            mHourPaint.getTextBounds("00",0,2,bounds);

            //mClockPaint.setAlpha(40);

            //canvas.drawRect(mCenterX-5-bounds.width()/2f,mCenterY-mHourPaint.getFontSpacing(),mCenterX+5+bounds.width()/2f,mCenterY+mMinutePaint.getFontSpacing(),mClockPaint);


            canvas.drawText(DateFormat.format("HH", mCalendar).toString(),mCenterX,mCenterY-mHourPaint.descent()-20,mHourPaint);

            canvas.drawText(DateFormat.format("mm", mCalendar).toString(),mCenterX,mCenterY-mMinutePaint.ascent()-20,mMinutePaint);

            if(!mAmbient) {
                //Log.d(TAG,"second");
                canvas.drawText(DateFormat.format("ss", mCalendar).toString(), mCenterX, mCenterY + mMinutePaint.getFontSpacing() - mSecondPaint.ascent()-20, mSecondPaint);


                //Bat
                canvas.drawLines(mBatPts, mBatteryPaint);
                canvas.drawRect(mBatRect,mBatteryPaint);                

                int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                canvas.drawText(""+batLevel,mCenterX,canvas.getHeight()-15-mMinutePaint.descent(),mBatteryPaint);


                //TODO Add Step count viewer
                //step

                /*float[] pts = new float[]{

                        200,100,
                        200,0,
                        200,200,
                        100,200

                };
                canvas.drawLines(pts, mAccentPaint);*/

                //Datum
                canvas.rotate(-90,mCenterX,mCenterY);
                canvas.drawText(DateFormat.format("EEdd.MM", mCalendar).toString(),mCenterX,55+mDatumPaint.getFontSpacing(),mDatumPaint);
                canvas.restore();


                if(shortcutIsSet){

                    canvas.drawBitmap(shortcutAppBitmap,null/*new Rect(0,0,shortcutAppBitmap.getWidth(),shortcutAppBitmap.getHeight())*/,shortcutRect,mShortcutPaint);

                }


            }//if mAmbient

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
