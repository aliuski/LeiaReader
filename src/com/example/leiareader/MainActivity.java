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
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Reader;

public class MainActivity extends Activity {
    
	static final String GETLIST = "";
	static final String DOWNLOAD = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_main);
/*        
        TouchImageView img = (TouchImageView) findViewById(R.id.img);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        
        img.setFileList(PublicMaterial.material("/storage/emulated/0/Download/test/"), width, height);
*/
        
        if((new Date()).after(new Date(getCacheDir().lastModified() + 43200000)))
            new ReadPapyrosTask().execute(GETLIST,DOWNLOAD);
        else
        	openReader();
    }
    
    @Override
    protected void onStart(){
    	super.onStart();
        if((new Date()).after(new Date(getCacheDir().lastModified() + 43200000)))
            new ReadPapyrosTask().execute(GETLIST,DOWNLOAD);
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    private void openReader(){
        TouchImageView img = (TouchImageView) findViewById(R.id.img);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        File fl[] = PublicMaterial.material(getCacheDir().getAbsolutePath()+"/");
        if(fl.length > 0)
        	img.setFileList(fl, width, height);
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
        	openReader();
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

		PublicMaterial.deleteMaterial(getCacheDir().getAbsolutePath()+"/");
		
		SimpleDateFormat sim = new SimpleDateFormat("yyyyMMdd");
		
		URL url = new URL(ulist+sim.format(new Date())+"000000?deviceid=0");
		URLConnection con = url.openConnection();
		Reader reader = new InputStreamReader(con.getInputStream());
		StringBuilder stringBuilder = new StringBuilder();
		while (true) {
			int ch = reader.read();
			if (ch==-1)
				break;
			stringBuilder.append((char)ch);
		}
		
    	JSONObject jsonObject = new JSONObject("{\"data\":"+stringBuilder.toString()+"}");
    	JSONArray postalCodesItems = new JSONArray(jsonObject.getString("data"));
    	JSONObject postalCodesItem = postalCodesItems.getJSONObject(0);
    	String id = postalCodesItem.getString("id");
    	
		url = new URL(udata+id+"?deviceid=0");
		con = url.openConnection();

		InputStream input = con.getInputStream();
		byte[] buffer = new byte[4096];
		int n = - 1;
		OutputStream output = new FileOutputStream(new File(getCacheDir(),PublicMaterial.ZIPFILENAME));
		while ( (n = input.read(buffer)) != -1)
		{
			if (n > 0)
				output.write(buffer, 0, n);
		}
		output.close();
		
		PublicMaterial.unzupMaterial(getCacheDir().getAbsolutePath()+"/");
		
	}catch(Exception e){
		return "FALSE";
		}
	return "TRUE";
}
}