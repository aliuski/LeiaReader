package com.leiamedia.leiareader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class Utilities {
	
    public static String[][] orderby(File f[]) {
    	String out[][] = new String[f.length][2];
    	for(int i=0;i<f.length;i++){
    		out[i][1] = f[i].getAbsolutePath();
    		try{
    			FileInputStream fis = new FileInputStream(f[i].getAbsolutePath()+"/DATE");
    			InputStreamReader isr = new InputStreamReader(fis);
    			char[] inputBuffer = new char[10];
    			isr.read(inputBuffer,0,10);
    			out[i][0] = new String(inputBuffer,0,10);
            	isr.close();
            	fis.close();
    		} catch(Exception e){
    			out[i][0] = new String("");
    		}
    	}

	    for(int x=0;x<f.length;x++){
		    for(int y=0;y<(f.length-1);y++){
			    if(out[y][0].compareTo(out[y+1][0])<0){
			    	String t0 = out[y][0];
			    	String t1 = out[y][1];
			    	out[y][0] = out[y+1][0];
			    	out[y][1] = out[y+1][1];
			    	out[y+1][0] = t0;
			    	out[y+1][1] = t1;
			    }
		    }
	    }
	    return out;
    }
}
