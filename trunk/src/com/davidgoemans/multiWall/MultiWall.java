
package com.davidgoemans.multiWall;
import java.util.ArrayList;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/*
 * This animated wallpaper draws a rotating wireframe shape. It is similar to
 * example #1, but has a choice of 2 shapes, which are user selectable and
 * defined in resources instead of in code.
 */

public class MultiWall extends WallpaperService 
{
	public static final String MULTI_WALL = "MultiWall";

    public static final String SHARED_PREFS_NAME="multiWallSettings";

    static class ThreeDPoint 
    {
        float x;
        float y;
        float z;
    }

    static class ThreeDLine 
    {
        int startPoint;
        int endPoint;
    }

    @Override
    public void onCreate() 
    {
        super.onCreate();
    }

    @Override
    public void onDestroy() 
    {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() 
    {
        return new RomanWallpaperEngine();
    }

    class RomanWallpaperEngine extends Engine 
        implements SharedPreferences.OnSharedPreferenceChangeListener 
    {

		private final Handler mHandler = new Handler();

        private final Paint paint = new Paint();
        private float offset;
        private float mTouchX = -1;
        private float mTouchY = -1;
        private long mStartTime;
        private float mCenterX;
        private float mCenterY;
        
        private ArrayList<Bitmap> wallpapers;

        private final Runnable drawScene = new Runnable() 
        {
            public void run() {
                drawFrame();
            }
        };
        
        private boolean mVisible;
        private SharedPreferences mPrefs;

        RomanWallpaperEngine() 
        {
            // Create a Paint to draw the lines for our cube
            paint.setColor(0xffffffff);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            mStartTime = SystemClock.elapsedRealtime();

            mPrefs = MultiWall.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);
        }
        
        public int NumScreens = 0;
        Bitmap currentScreen;
        ArrayList<String> imagePaths;

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
        {
        	String keyBase = new String("image_");
        	String imageKey = new String("");
        	Log.d(MULTI_WALL, imageKey);
        	
        	NumScreens = prefs.getInt("numScreens", 0);
        	wallpapers = new ArrayList<Bitmap>();
        	imagePaths = new ArrayList<String>();
        	
        	
        	imageKey = keyBase + (screenIndex + 1);
        	String currentPath = prefs.getString(imageKey, "");

        	for( int i=1; i<=NumScreens; i++ )
        	{
        		wallpapers.add(null);
        		
        		imageKey = keyBase + i;
        		imagePaths.add(prefs.getString(imageKey, ""));
        		Log.d(MULTI_WALL, "Getting Key: " + imageKey + " and Path: " + currentPath);        		
        	}
        	
        }
        
        Rect src = new Rect();
        Rect dst = new Rect();

        void drawFrame() 
        {
            final SurfaceHolder holder = getSurfaceHolder();
            final Rect frame = holder.getSurfaceFrame();
            final int width = frame.width();
            final int height = frame.height();
            
            Canvas c = null;
            try 
            {
                c = holder.lockCanvas();
                if (c != null) 
                {
                    // draw something
                    drawScene(c, width, height);
                    //drawTouchPoint(c);
                }
            } 
            finally 
            {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(drawScene);
            if (mVisible) 
            {
                mHandler.postDelayed(drawScene, 1000 / 25);
            }
        }

        int screenIndex = 0;
        
        void drawScene(Canvas c, int width, int height) 
        {
        	c.save();
        	
        	int oldScreenIndex = screenIndex;
        	screenIndex = Math.min((int)( (float)NumScreens * offset ), NumScreens-1);
        	
        	if( currentScreen == null || oldScreenIndex != screenIndex)
        	{
        		currentScreen = wallpapers.get(screenIndex);
        		
        		if(currentScreen == null )
        		{
        			currentScreen = BitmapFactory.decodeFile(imagePaths.get(screenIndex));
        			currentScreen = Bitmap.createScaledBitmap(currentScreen, c.getWidth(), c.getHeight(), false);
        			wallpapers.set(screenIndex, currentScreen);
        		}
        	}
        	
        	src.set(0,0, currentScreen.getWidth(), currentScreen.getHeight());
        	dst.set(0,0,c.getWidth(),c.getHeight());
        	c.drawBitmap(currentScreen, 
        			src,
        			dst, 
        			paint);
        	
        	c.restore();
        }
 
        void drawTouchPoint(Canvas c) 
        {
            if (mTouchX >=0 && mTouchY >= 0) 
            {
                c.drawCircle(mTouchX, mTouchY, 80, paint);
            }
        }
        

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) 
        {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() 
        {
            super.onDestroy();
            mHandler.removeCallbacks(drawScene);
        }

        @Override
        public void onVisibilityChanged(boolean visible) 
        {
            mVisible = visible;
            if (visible) 
            {
                drawFrame();
            } 
            else 
            {
                mHandler.removeCallbacks(drawScene);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) 
        {
            super.onSurfaceChanged(holder, format, width, height);
            // store the center of the surface, so we can draw the cube in the right spot
            mCenterX = width/2.0f;
            mCenterY = height/2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) 
        {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) 
        {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(drawScene);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) 
        {
            offset = xOffset;
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) 
        {
            if (event.getAction() == MotionEvent.ACTION_MOVE) 
            {
                mTouchX = event.getX();
                mTouchY = event.getY();
            } 
            else 
            {
                mTouchX = -1;
                mTouchY = -1;
            }
            super.onTouchEvent(event);
        }
    }
}
