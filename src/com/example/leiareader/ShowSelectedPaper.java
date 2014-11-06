package com.example.leiareader;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class ShowSelectedPaper extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_showselectedpage);

	    Intent intent = getIntent();
	    
        TouchImageView img = (TouchImageView) findViewById(R.id.img);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        File fl[] = PublicMaterial.material(intent.getStringExtra(MainActivity.EXTRA_SELECTEDDIR));
        if(fl.length > 0)
        	img.setFileList(fl, width, height);
    }
    /*
    @Override
    protected void onStart(){
    	super.onStart();
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }
*/
}