package com.leiamedia.leiareader;

import java.io.File;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;

public class SelectPaper extends Activity {
	public final static String EXTRA_SELECTEDDIR = "com.example.leiareader.SELECTEDDIR";
	
    private Vector <Bitmap>imageIDs = new Vector<Bitmap>();
    private String extimageorder[][];
	private int width;
	private int height;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_main);
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        
        loadSelectedPaperList();
    }
    
    private void loadSelectedPaperList(){
        getPaperList();

        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(new ImageAdapter(this));

        gridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent,
            View v, int position, long id) {
                Intent intent = new Intent(SelectPaper.this, MainActivity.class);
        		intent.putExtra(EXTRA_SELECTEDDIR,extimageorder[position][1]+"/");                
        		setResult(Activity.RESULT_OK, intent);
        		finish();
            }
        });
    }
    
    private void getPaperList(){
    	extimageorder = Utilities.orderby(getExternalFilesDir(null).listFiles());
    	imageIDs = new Vector<Bitmap>();
    	for(int loop=0 ; loop<extimageorder.length ; loop++){
	    	File fl[] = PublicMaterial.material(extimageorder[loop][1]+"/");	    		
	           if(fl.length > 0)
	        	   imageIDs.add(BitmapFactory.decodeFile(fl[0].getAbsolutePath()));
    	}
    }
    
    public class ImageAdapter extends BaseAdapter 
    {
        private Context context;

        public ImageAdapter(Context c)
        {
            context = c;
        }

        //---returns the number of images---
        public int getCount() {
            return imageIDs.size();
        }

        //---returns the item---
        public Object getItem(int position) {
            return position;
        }

        //---returns the ID of an item---
        public long getItemId(int position) {
            return position;
        }

        //---returns an ImageView view---
        public View getView(int position, View convertView,
                ViewGroup parent)
        {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
            	int size = ((((height > width) ? width : height)) - 60) / 2;
                imageView.setLayoutParams(new
                        GridView.LayoutParams(size, size));
                imageView.setScaleType(
                        ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(5, 5, 5, 5);
            } else {
                imageView = (ImageView) convertView;
            }
		    imageView.setImageBitmap(imageIDs.get(position));
            
            return imageView;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.removeItem(R.id.action_select);
        menu.removeItem(R.id.action_load);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.android_settings) {
            Intent i = getPackageManager().getLaunchIntentForPackage("com.android.settings");
            startActivity(i);
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
}