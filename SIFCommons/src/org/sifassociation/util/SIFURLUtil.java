/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sifassociation.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get part of the URL path based on position. 
 * Note:  The names of these functions are a bit deceptive.
 * 
 * @author jlovell
 */
public class SIFURLUtil {
    
    // Simple gets the second to last piece of the path.
    public static String getCollection(String url) {
        URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException ex) {
            Logger.getLogger(SIFURLUtil.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        String path = parsed.getPath();
        String[] parts = path.split("/");
        if(2 < parts.length) {
            return stripMatrixParameters(parts[parts.length-2]);
        }
        return "";
    }
    
    // Simple gets the last piece of the path.
    public static String getResource(String url) {
        URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException ex) {
            Logger.getLogger(SIFURLUtil.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        String path = parsed.getPath();
        String[] parts = path.split("/");
        if(1 < parts.length) {
            return stripMatrixParameters(parts[parts.length-1]);
        }
        return "";
    }

    // So we can do a quick comparison in our tests.
    public static String getServicePath(String url) {
        URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException ex) {
            Logger.getLogger(SIFURLUtil.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        String path = parsed.getPath();
        String[] parts = path.split("/");
        if(3 < parts.length) {
            return stripMatrixParameters(parts[parts.length-3]) + "/{}/" + 
                    stripMatrixParameters(parts[parts.length-1]);
        }
        return "";        
    }
    
    private static String stripMatrixParameters(String segment) {
        int index = segment.indexOf(";");
        if(-1 != index) {
            return segment.substring(0, index);
        }
        return segment;
    }
    
}
