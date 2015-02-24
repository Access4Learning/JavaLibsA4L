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
 *
 * @author jlovell
 */
public class SIFURLUtil {
    
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
    
    private static String stripMatrixParameters(String segment) {
        int index = segment.indexOf(";");
        if(-1 != index) {
            return segment.substring(0, index);
        }
        return segment;
    }
    
}
