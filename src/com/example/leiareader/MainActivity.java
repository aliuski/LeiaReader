package com.example.leiareader;

import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Reader;

import com.example.leiareader.ShowSelectedPaper;

import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {
	public final static String EXTRA_SELECTEDDIR = "com.example.leiareader.SELECTEDDIR";
	
	static final String GETLIST = "";
	static final String DOWNLOAD = "";
    
    private Vector <Bitmap>imageIDs = new Vector<Bitmap>();
    private String extimageorder[][];
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

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
                Intent intent = new Intent(MainActivity.this, ShowSelectedPaper.class);
        		intent.putExtra(EXTRA_SELECTEDDIR,extimageorder[position][1]+"/");
        		startActivity(intent);  
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	
        	if(IsExternalStorageAvailableAndWriteable()){
        		new ReadPapyrosTask().execute(GETLIST,DOWNLOAD);
        	}
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private class ReadPapyrosTask extends AsyncTask
    <String, Void, String> {
    	ProgressDialog progressDialog;
        @Override
        protected void onPreExecute()
        {
            progressDialog = ProgressDialog.show(MainActivity.this, "Leia reader",
            		"Haetaan aineisto. Toimenpide kestää muutaman minuutin.", true);
        };
        @Override
        protected String doInBackground(String... urls) {
            return getMaterialFromLeia(urls[0],urls[1]);
        }
        @Override
        protected void onPostExecute(String result) {
        	progressDialog.dismiss();
        	loadSelectedPaperList();
        }
    }

	public String getMaterialFromLeia(String ulist, String udata) {
		
		try{
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}
		};

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

		SimpleDateFormat sim = new SimpleDateFormat("yyyyMMdd");
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, -10);
		
		URL url = new URL(ulist+sim.format(cal.getTime())+"000000?deviceid=0");
		URLConnection con = url.openConnection();
		Reader reader = new InputStreamReader(con.getInputStream());
		StringBuilder stringBuilder = new StringBuilder();
		while (true) {
			int ch = reader.read();
			if (ch==-1)
				break;
			stringBuilder.append((char)ch);
		}

		File f[] = new File[4];
		f[0] = getExternalFilesDir("paper1");
		f[1] = getExternalFilesDir("paper2");
		f[2] = getExternalFilesDir("paper3");
		f[3] = getExternalFilesDir("paper4");
    	String[][] fileorder = orderby(f);
		
    	JSONObject jsonObject = new JSONObject("{\"data\":"+stringBuilder.toString()+"}");
    	JSONArray postalCodesItems = new JSONArray(jsonObject.getString("data"));

    	Log.d("KOE","AML: size: "+ postalCodesItems.length());
    	
    	int papercounter = 0;
    	for(int loop=postalCodesItems.length()-1;loop > 0;loop-=2){
    		Log.d("KOE","AML: loop: "+ loop);
	    	JSONObject postalCodesItem = postalCodesItems.getJSONObject(loop-1);
	    	String id = postalCodesItem.getString("id");
	    	java.text.DateFormat df = new SimpleDateFormat("yyyyMMddkkmmss");
	    	Date date = df.parse(postalCodesItem.getString("createdate"));
	    	boolean find = true;
	    	for(int i=0 ; i<f.length ; i++){
	    		
	    		Log.d("KOE","AML: Test: "+ fileorder[i][0] +" "+ (new java.sql.Date(date.getTime())).toString());
	    		if(fileorder[i][0].equals((new java.sql.Date(date.getTime())).toString())){
	    			find = false;
	    			Log.d("KOE","AML: Document found: "+ fileorder[i][0]);
	    			break;
	    		}
	    	}
	    	if(find){
	    		Log.d("KOE","AML: Document Make: "+ date.toString() +" File: "+fileorder[f.length-1][1]);
	    		loadRowData(udata, id, fileorder[f.length-1][1], date);
	    		fileorder = orderby(f);
	    	}
	    	
	    	papercounter++;
	    	Log.d("KOE","AML: papercounter: "+ papercounter);
	    	if(papercounter >= 4)
	    		break;
    	}
    	
	}catch(Exception e){
		Log.d("KOE","AML: "+ e);
		return "FALSE";
		}
	return "TRUE";
	}

	private void loadRowData(String udata, String id, String extStorage, Date date) throws Exception{
		
		PublicMaterial.deleteMaterial(extStorage+"/");
		
		URL url = new URL(udata+id+"?deviceid=0");
		URLConnection con = url.openConnection();

		InputStream input = con.getInputStream();
		byte[] buffer = new byte[4096];
		int n = - 1;
		OutputStream output = new FileOutputStream(extStorage+"/"+PublicMaterial.ZIPFILENAME);
		while ( (n = input.read(buffer)) != -1)
		{
			if (n > 0)
				output.write(buffer, 0, n);
		}
		output.close();
		
		if(PublicMaterial.unzupMaterial(extStorage+"/")){
			OutputStream outputStream = new FileOutputStream(extStorage+"/DATE");
			outputStream.write((new java.sql.Date(date.getTime())).toString().getBytes());
			outputStream.close();
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
                imageView.setLayoutParams(new
                        GridView.LayoutParams(195, 195));
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

    private void getPaperList(){
    	if(IsExternalStorageAvailableAndWriteable()){
    		extimageorder = orderby(getExternalFilesDir(null).listFiles());
    		imageIDs = new Vector<Bitmap>();
    		for(int loop=0 ; loop<extimageorder.length ; loop++){
	    		File fl[] = PublicMaterial.material(extimageorder[loop][1]+"/");	    		
	            if(fl.length > 0)
	    			imageIDs.add(BitmapFactory.decodeFile(fl[0].getAbsolutePath()));
    		}    		
		}
    }

    public String[][] orderby(File f[]) {
    	
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
    
    public boolean IsExternalStorageAvailableAndWriteable() {
        boolean externalStorageAvailable = false;
        boolean externalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            //---you can read and write the media---
            externalStorageAvailable = externalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            //---you can only read the media---
            externalStorageAvailable = true;
            externalStorageWriteable = false;
        } else {
            //---you cannot read nor write the media---
            externalStorageAvailable = externalStorageWriteable = false;
        }
        return externalStorageAvailable && externalStorageWriteable;
    }
}