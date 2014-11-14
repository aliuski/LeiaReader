package com.leiamedia.leiareader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PublicMaterial {
	public static final String ZIPFILENAME = "menu.zip";
	
	public static void deleteMaterial(String directory) {
		File ft[] = material(directory);
		for(int loop=0;loop<ft.length;loop++)
			ft[loop].delete();
	}
	
	public static boolean unzupMaterial(String directory) {
		
    	File folder = new File(directory + ZIPFILENAME);
    	if(folder.exists()){
    		byte[] buffer = new byte[1024];

    		try {
    			ZipInputStream zis =
    					new ZipInputStream(new FileInputStream(directory + ZIPFILENAME));
    			ZipEntry ze = zis.getNextEntry();
    			while(ze!=null){
    				FileOutputStream fos = new FileOutputStream(new File(directory + ze.getName()));
    				int len;
    				while ((len = zis.read(buffer)) > 0)
    					fos.write(buffer, 0, len);
 
    				fos.close();   
    				ze = zis.getNextEntry();
    			}
    			zis.closeEntry();
    			zis.close();
    			folder.delete();
    			
    		} catch(IOException ex) {
    			ex.printStackTrace();
    			return false;
    		}
    	}
    	return true;
	}
    	
    public static File[] material(String directory) {
		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".png")) {
					return true;
				} else {
					return false;
				}
			}
		};
    	File f = new File(directory);
    	return f.listFiles(textFilter);
	}
}
