
package com.davidgoemans.multiWall;
import java.io.FileNotFoundException;
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
        Bitmap nextScreen;
        ArrayList<String> imagePaths;

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
        {
        	String keyBase = new String("image_");
        	String imageKey = new String("");
        	Log.d(MULTI_WALL, imageKey);
        	
        	NumScreens = prefs.getInt("numScreens", 0);
        	wallpapers = new ArrayList<Bitmap>();
        	imagePaths = new ArrayList<String>();
        	
        	
        	imageKey = keyBase + (currentIndex + 1);
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

        int currentIndex = 0;
        
        int getNextIndex()
        {        	
        	int nextIndex = currentIndex;
        	
        	float indexOffset = ((float)(NumScreens-1) * offset );
        	float offsetFromCurrentScreen = indexOffset - (float)currentIndex;
        	
        	if( offsetFromCurrentScreen != 0 )
        	{
            	// Depending on the direction we're moving, the next index is relative to the screen index
            	// clamped between 0 and NumScreens-1
	        	if( (int)Math.floor(indexOffset) ==  currentIndex )
	        	{
	        		nextIndex = Math.min(NumScreens-1, currentIndex+1);
	        	}
	        	else if( (int)Math.ceil(indexOffset) == currentIndex )
	        	{
	        		nextIndex = Math.max(0, currentIndex-1);
	        	}
        	}	
        	return nextIndex;
        }
        
        void drawScene(Canvas c, int width, int height) 
        {
        	c.save();
        	
        	int oldScreenIndex = currentIndex;
        	float indexOffset = ((float)(NumScreens-1) * offset );
        	
        	currentIndex = Math.min(Math.round(indexOffset), NumScreens-1);
        	
        	float offsetFromCurrentScreen = indexOffset - (float)currentIndex;

        	int nextIndex = getNextIndex();
        	
        	if( wallpapers.size() <= currentIndex )
        	{
        		// TOOD: Draw error paper
        	}
        	else if( currentScreen == null || oldScreenIndex != currentIndex)
        	{
        		currentScreen = loadScreen(currentIndex);
        	}
        	
			if( nextIndex != currentIndex )
			{
				nextScreen = loadScreen(nextIndex);  
			}
			
        	int xOffset = (int)((float)offsetFromCurrentScreen*c.getWidth());
        	
        	src.set(0,0, currentScreen.getWidth(), currentScreen.getHeight());
        	dst.set(-xOffset,0, c.getWidth() - xOffset,c.getHeight());
        	c.drawBitmap(currentScreen, 
        			src,
        			dst, 
        			paint);
        	
        	if( nextScreen != null && xOffset != 0 )
        	{
        		int nextXOffset = (int) Math.signum(xOffset)*(c.getWidth() - Math.abs(xOffset));

	        	src.set(0, 0, nextScreen.getWidth(), nextScreen.getHeight());
	        	dst.set(nextXOffset, 0, nextXOffset + c.getWidth(),c.getHeight());
	        	
	        	c.drawBitmap(nextScreen, 
	        			src,
	        			dst, 
	        			paint);
        	}
        	
        	c.restore();
        }
        
        Bitmap loadScreen(int screenIndex)
        {
        	Bitmap loaded = wallpapers.get(screenIndex);
        	
        	if( loaded == null )
	        {
	        	try 
	        	{
	        		loaded = BitmapFactory.decodeStream(openFileInput(imagePaths.get(screenIndex)));
	    			// And cache it
	    			wallpapers.set(screenIndex, loaded);
				} 
	        	catch (FileNotFoundException e) 
				{
					e.printStackTrace();
				}
	        }
			
        	return loaded;
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
