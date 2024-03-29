
package com.davidgoemans.multiWall;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MultiWallSettings extends Activity implements OnSeekBarChangeListener
{
	int numScreens = 5;
	
	ArrayList<View> homeScreenPickers;
	LinearLayout homeScreenPickerLayout;
	TextView homeScreenCountLabel;
	ArrayList<String> images;
	final String  prefsBase = "image_";
	DisplayMetrics metrics;
	

    @Override
    protected void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        
        setContentView(R.layout.settings_screen);
        images = new ArrayList<String>();
        
        homeScreenPickers = new ArrayList<View>();
        homeScreenPickerLayout = (LinearLayout)findViewById(R.id.imageLayout);
        homeScreenCountLabel = (TextView)findViewById(R.id.numScreens);
        
        Load();
        
        SeekBar seeker = (SeekBar)findViewById(R.id.sbNumScreens);
        seeker.setMax(9);
        seeker.setProgress(numScreens - 1);
        
        seeker.setOnSeekBarChangeListener(this);

        UpdatePickers();
    }
    
    void Load()
    {
		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
    	SharedPreferences prefs = getSharedPreferences(MultiWall.SHARED_PREFS_NAME, 0);
    	numScreens = prefs.getInt("numScreens", 5);
    	UpdatePickers();
    	
    	String imageKey = new String("");
    	
    	for( int i=1; i<=numScreens; i++ )
    	{
    		imageKey = prefsBase + i;
    		String currentPath = prefs.getString(imageKey, "");
    		//Log.d(MULTI_WALL, "Getting Key: " + imageKey + " and Path: " + currentPath);
    		
    		images.set(i-1, currentPath);
    		if( currentPath != "")
    			setImageAt(i-1, currentPath);
    	}
    }
    
    void UpdatePickers()
    {
    	LayoutInflater inflater = LayoutInflater.from(this);
    	
    	//int numPickers = homeScreenPickers.size();
    	for(int i=numScreens; i<homeScreenPickers.size(); i++)
    	{
    		homeScreenPickerLayout.removeView(homeScreenPickers.get(i));
    		homeScreenPickers.remove(i);
    		images.remove(i);
    	}
    	
        for(int i=homeScreenPickers.size(); i<numScreens; i++)
        {
        	View v = inflater.inflate(R.layout.settings_row, null, false);
        	homeScreenPickers.add(v);
        	homeScreenPickerLayout.addView(v);
        	images.add(new String());
        }
        
        homeScreenCountLabel.setText(String.valueOf(numScreens));
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
    }

    @Override
    protected void onDestroy() 
    {
    	SharedPreferences prefs = getSharedPreferences(MultiWall.SHARED_PREFS_NAME, 0);
    	Editor ed = prefs.edit();
    	ed.putInt("numScreens", numScreens);
    	
    	for(int i=0; i<images.size(); i++)
    	{
    		ed.putString(prefsBase + String.valueOf(i+1), images.get(i));
    		Log.d(MultiWall.MULTI_WALL, "Putting path: " + images.get(i));
    	}
    	ed.commit();

    	Log.d(MultiWall.MULTI_WALL, "Destroy settings");
    	
        super.onDestroy();
    }

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) 
	{
		numScreens = progress;
		UpdatePickers();
	}
	
	public void pickImage(View v)
	{
		Intent intent = new Intent(Intent.ACTION_PICK,
	               android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		intent.setType("image/*");
		startActivityForResult(intent, homeScreenPickers.indexOf(v.getParent())); 
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{ 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

	    if(resultCode == RESULT_OK)
	    {  
            Uri selectedImage = imageReturnedIntent.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();      
    
            String localPath = copyScaledImageToLocalStorage(filePath);
            
            setImageAt(requestCode, localPath);
        }
	}
	
	
	// Returns the path where it's stored locally
	String copyScaledImageToLocalStorage(String imagePath)
	{
		Uri pathUri = Uri.parse(imagePath);
		String outputPath = null;
		FileOutputStream stream;
		
		try 
		{
			outputPath = pathUri.getLastPathSegment();
			stream = openFileOutput(outputPath, MODE_PRIVATE);
			Bitmap toScale = BitmapFactory.decodeFile(imagePath);
			
			Bitmap scaled = Bitmap.createScaledBitmap(toScale, metrics.widthPixels, metrics.heightPixels, true);
			// Some manual cleanup since we're fighting the vm here
			toScale.recycle();
			
			if( !scaled.compress(CompressFormat.PNG, 80, stream) )
			{
				outputPath = null;
			}

			// Some manual cleanup since we're fighting the vm here
			scaled.recycle();
			
			stream.getFD().sync();
			stream.close();
		} 
		catch (FileNotFoundException e) 
		{
			ShowErrorDialog("Couldn't find the image " + e.getMessage());
		}
		catch (NullPointerException e)
		{
			ShowErrorDialog("Couldn't open the image - " + e.getMessage());
		} 
		catch (SyncFailedException e) 
		{
			ShowErrorDialog("Couldn't sync the image to disk - " + e.getMessage());
		} 
		catch (IOException e) 
		{
			ShowErrorDialog("Couldn't write the image to disk - " + e.getMessage());
		}
		
		return outputPath;
		
	}
	
	void setImageAt(int index, String imagePath)
	{
		
		Bitmap loadedImage;
		try 
		{
			loadedImage = BitmapFactory.decodeStream(openFileInput(imagePath));
			loadedImage = Bitmap.createScaledBitmap(loadedImage, 120, 160, false);
	        ImageView image = (ImageView)homeScreenPickers.get(index).findViewById(R.id.pickedImage);
	        image.setImageBitmap(loadedImage);
	        images.set(index, imagePath);
		} 
		catch (FileNotFoundException e) 
		{
			ShowErrorDialog("Couldn't load image, saving may have failed - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	void ShowErrorDialog(String reason)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Couldn't load that image: " + reason)
		       .setCancelable(false)
		       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) 
		           {
		        	   dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) 
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) 
	{
	}
}
