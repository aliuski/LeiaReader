package com.leiamedia.leiareader;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class TouchImageView extends ImageView {
    private static final String TAG = "Touch";
    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    
    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    Context context;

	int counter = 0;
	File filelist[];
    int sizey = 0;
    int sizex = 0;
    int bitmapwidth;
    float animatestep;
    float redundantYSpace;
    float redundantXSpace;
    Timer timer = null;
    MoveTimerTask moveTimerTask;
    Bitmap bm;
    
	public TouchImageView(Context context, AttributeSet set) {
    	super(context,set);
        init(context);
    }
	
    public TouchImageView(Context context) {
        super(context);
        init(context);
    }
	
	private void init(Context context){
        super.setClickable(true);
        this.context = context;

        matrix.setTranslate(1f, 1f);
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
           
        setOnTouchListener(new OnTouchListener() {
        	float[] matrixvalues = new float[9];
        	boolean newtouch = false;
            @Override
            public boolean onTouch(View v, MotionEvent rawEvent) {
            	if(filelist == null)
            		return true;
                WrapMotionEvent event = WrapMotionEvent.wrap(rawEvent);
                // Handle touch events here...
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    Log.d(TAG, "mode=DRAG");
                    mode = DRAG;
                    newtouch = true;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    Log.d(TAG, "oldDist=" + oldDist);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                        Log.d(TAG, "mode=ZOOM");
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    int xDiff = (int) Math.abs(event.getX() - start.x);
                    int yDiff = (int) Math.abs(event.getY() - start.y);
                    if (xDiff < 8 && yDiff < 8){
                        performClick();
                    }
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    Log.d(TAG, "mode=NONE");
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                    	if(!newtouch)
                    		return true;
                    	
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                        matrix.getValues(matrixvalues);
                        
                        if(((matrixvalues[Matrix.MTRANS_X] - sizex/2) > 0)){
                            	newPage(false);
                            	newtouch = false;
                            }
                        if((matrixvalues[Matrix.MTRANS_X] + (int)(matrixvalues[Matrix.MSCALE_X]*(float)bitmapwidth)) < sizex/2){
                            	newPage(true);
                            	newtouch = false;
                            }
                        
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        Log.d(TAG, "newDist=" + newDist);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
                }

                setImageMatrix(matrix);
                return true; // indicate event was handled
            }

        });
    }

	private void newPage(boolean up){
        matrix = new Matrix();
        savedMatrix = new Matrix();
	    start = new PointF();
	    mid = new PointF();
	    int add = 1;
	    if(sizex > sizey)
	    	add = 2;
		if(up){
			counter+=add;
			if(counter>=filelist.length)
				counter = 0;
		    createBitmap();
			setImage(1);
		} else {
			counter-=add;
			if(counter<0)
				counter = filelist.length-add;
		    createBitmap();
			setImage(-1);
		}
	}
	
	public int getCounter(){
		return counter;
	}
	
	public void setFileList(File filelist[], int sizex, int sizey, int page){
        matrix = new Matrix();
        savedMatrix = new Matrix();
	    start = new PointF();
	    mid = new PointF();
		this.filelist = filelist;
		this.sizex = sizex;
		this.sizey = sizey;
		if(sizex > sizey) {
			if(page % 2 == 0)
				this.counter = page;
			else
				this.counter = page-1;
		} else
			this.counter = page;
		createBitmap();
		setImage(0);
	}
	
	private void createBitmap(){
		if(bm!=null){
			bm.recycle();
			bm=null;
		}
		if(sizex > sizey){
			Bitmap bm1 = BitmapFactory.decodeFile(filelist[counter].getAbsolutePath());
			Bitmap bm2 = BitmapFactory.decodeFile(filelist[counter+1].getAbsolutePath());
			bm = Bitmap.createBitmap(bm1.getWidth()*2, bm1.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bm);
			canvas.drawBitmap(bm1, 0, 0, new Paint());
			canvas.drawBitmap(bm2, bm1.getWidth(), 0, new Paint());
		} else
			bm = BitmapFactory.decodeFile(filelist[counter].getAbsolutePath());
	}
	    
    public void setImage(int animate) {

    	bitmapwidth = bm.getWidth();
        super.setImageBitmap(bm);
        float scale;
        //Fit to screen.
        if ((sizey / bm.getHeight()) >= (sizex / bm.getWidth())){
            scale =  (float)sizex / (float)bm.getWidth();
        } else {
            scale = (float)sizey / (float)bm.getHeight();
        }

        savedMatrix.set(matrix);
        matrix.set(savedMatrix);
        matrix.postScale(scale, scale, mid.x, mid.y);
        setImageMatrix(matrix);

        // Center the image
        redundantYSpace = ((float)sizey - (scale * (float)bm.getHeight()))/(float)2;
        redundantXSpace = ((float)sizex - (scale * (float)bm.getWidth()))/(float)2;
        
        if(animate > 0){
        	animatestep = (redundantXSpace - (float)sizex) / (float)10;
        	redundantXSpace = (float)sizex + animatestep;
        	startAnimateTimmer();
        } else if(animate < 0){
        	animatestep = (redundantXSpace + (float)(scale * (float)bm.getWidth())) / (float)10;
        	redundantXSpace = -(float)(scale * (float)bm.getWidth()) + animatestep;
        	startAnimateTimmer();
        }

        savedMatrix.set(matrix);
        matrix.set(savedMatrix);
        matrix.postTranslate(redundantXSpace, redundantYSpace);
        setImageMatrix(matrix);
    }
    
    private void startAnimateTimmer(){
        if(timer != null)
        	timer.cancel();
        timer = new Timer();
        moveTimerTask = new MoveTimerTask();
        timer.schedule(moveTimerTask, 20, 20);
    }

    class MoveTimerTask extends TimerTask {
    	int counter = 2;
    	@Override
    	public void run() {
    		((android.app.Activity)context).runOnUiThread(new Runnable() {
    		@Override
    	    public void run() {
        		matrix.set(savedMatrix);
        		matrix.postTranslate(redundantXSpace + animatestep*counter, redundantYSpace);
        		TouchImageView.this.setImageMatrix(matrix);        		
        		if (counter > 8){
        			if(timer != null)
        				timer.cancel();
        			timer = null;
        		}
        	    counter++;
    	    }});
    	  }
    }

    /** Determine the space between the first two fingers */
    private float spacing(WrapMotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    /** Calculate the mid point of the first two fingers */
    private void midPoint(PointF point, WrapMotionEvent event) {
        // ...
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
