/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Collection of common file actions.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public class SIFFileUtil {

    public static String readFile(String path) throws IOException {
      FileInputStream stream = new FileInputStream(new File(path));
      try {
        FileChannel fc = stream.getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        return Charset.defaultCharset().decode(bb).toString();
      }
      finally {
        stream.close();
      }
    }
    
    public static void writeFile(String path, String content) throws FileNotFoundException, IOException {
        FileOutputStream stream = new FileOutputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            stream.write(content.getBytes());
        }
        finally {
            stream.close();
        }
    }
    
    public static String readURL(String path) throws MalformedURLException, IOException {
        URL url = new URL(path);
        URLConnection con = url.openConnection();
        Reader r = new InputStreamReader(con.getInputStream(), "UTF-8");
        StringBuilder buf = new StringBuilder();
        while (true) {
            int ch = r.read();
            if (ch < 0) {
                break;
            }
          buf.append((char) ch);
        }
        return buf.toString();        
    }
    
    public static String readInputStream(InputStream is) throws IOException {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {
                br = new BufferedReader(new InputStreamReader(is));
                while((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                }
        } finally {
                if (br != null) {
                        br.close();
                }
        }

        return sb.toString();
    }

}
