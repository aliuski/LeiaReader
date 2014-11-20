package com.leiamedia.leiareader;

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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Reader;

import com.leiamedia.leiareader.R;
import com.leiamedia.leiareader.SelectPaper;

public class MainActivity extends Activity {
	
	static final String GETLIST = "https://leiapapyros.com/leia/public/main/api/edition/list/";
	static final String DOWNLOAD = "https://leiapapyros.com/leia/public/main/api/download/";
	
	static final int SELECTPAPER = 1;
	private String directory;
    private int width;
    private int height;
    private boolean updatedone = true;
    
	private TouchImageView img;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_showselectedpage);
        
        img = (TouchImageView) findViewById(R.id.img);
        
        try{
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        
        File files[] = getExternalFilesDir(null).listFiles();
        if(files.length > 0){
        	directory = (savedInstanceState != null) ? savedInstanceState.getString("paper") : null;
	        if(directory == null)
	        	directory = Utilities.orderby(files)[0][1];
	        File fl[] = PublicMaterial.material(directory);
	        if(fl.length > 0)
	        	img.setFileList(fl, width, height, (savedInstanceState != null) ? savedInstanceState.getInt("page") : 0);
        }
        
        }catch(Exception e){
        	Log.d("Leiareader","onCreate() : "+e);
        }
        updatedone = true;
    }

	@Override
	public void onResume() {
	  super.onResume();
	  if(!updatedone){
    	  directory = Utilities.orderby(getExternalFilesDir(null).listFiles())[0][1];
    	  img.setFileList(PublicMaterial.material(directory), width, height, 0);
	  }
	  updatedone = true;
	}
    
	@Override
	protected void onPause() {
		super.onPause();
		updatedone = false;
	}
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      switch(requestCode) {
        case (1) : {
          if (resultCode == Activity.RESULT_OK) {
        	  directory = data.getStringExtra(SelectPaper.EXTRA_SELECTEDDIR);
              img.setFileList(PublicMaterial.material(directory), width, height, 0);
              updatedone = true;
          }
          break;
        } 
      }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putInt("page",img.getCounter());
      savedInstanceState.putString("paper",directory);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.removeItem(R.id.android_settings);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_load) {
        	if(IsExternalStorageAvailableAndWriteable()) {
        		new ReadPapyrosTask().execute(GETLIST,DOWNLOAD);
        	}
        	return true;
        } else if (id == R.id.action_select) {
        	directory = null;
            Intent intent = new Intent(MainActivity.this, SelectPaper.class);
    		startActivityForResult(intent,SELECTPAPER);
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private class ReadPapyrosTask extends AsyncTask
    <String, Void, String> {
    	ProgressDialog progressDialog;
    	PowerManager pm;
    	PowerManager.WakeLock wl;
        @Override
        protected void onPreExecute()
        {
        	pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        	wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Leia reader");
        	
            progressDialog = ProgressDialog.show(MainActivity.this, "Leia reader",
            		getResources().getString(R.string.download_material), true);
        };
        @Override
        protected String doInBackground(String... urls) {
        	wl.acquire();
            return getMaterialFromLeia(urls[0],urls[1]);
        }
        @Override
        protected void onPostExecute(String result) {
        	wl.release();
        	progressDialog.dismiss();
        	directory = Utilities.orderby(getExternalFilesDir(null).listFiles())[0][1];
            File fl[] = PublicMaterial.material(directory);
            if(fl.length > 0)
            	img.setFileList(fl, width, height, 0);
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
    	String[][] fileorder = Utilities.orderby(f);
		
    	JSONObject jsonObject = new JSONObject("{\"data\":"+stringBuilder.toString()+"}");
    	JSONArray postalCodesItems = new JSONArray(jsonObject.getString("data"));
    	int papercounter = 0;
    	for(int loop=postalCodesItems.length()-1;loop > 0;loop-=2){
	    	JSONObject postalCodesItem = postalCodesItems.getJSONObject(loop-1);
	    	if(postalCodesItem.getInt("filesize") < 1000)
	    		continue;
	    	String id = postalCodesItem.getString("id");
	    	java.text.DateFormat df = new SimpleDateFormat("yyyyMMddkkmmss");
	    	Date date = df.parse(postalCodesItem.getString("createdate"));
	    	boolean find = true;
	    	for(int i=0 ; i<f.length ; i++){
	    		if(fileorder[i][0].equals((new java.sql.Date(date.getTime())).toString())){
	    			find = false;
	    			break;
	    		}
	    	}
	    	if(find){
	    		loadRowData(udata, id, fileorder[f.length-1][1], date);
	    		fileorder = Utilities.orderby(f);
	    	}
	    	papercounter++;
	    	if(papercounter >= 4)
	    		break;
    	}
    	
	}catch(Exception e){
		Log.d("Leiareader","getMaterialFromLeia() : "+ e);
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
    
    private boolean IsExternalStorageAvailableAndWriteable() {
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