/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sifassociation.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author jlovell
 */
public class SIFAuthUtil {
    
    private static final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    
    public static XMLGregorianCalendar stringToXMLGregorianCalendar(String timestamp) throws DatatypeConfigurationException {
        Calendar calendar = DatatypeConverter.parseDateTime(timestamp);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(calendar.getTimeInMillis());
        return DatatypeFactory.newInstance().
                    newXMLGregorianCalendar(gregorianCalendar);
    }
 
    public static XMLGregorianCalendar getNow() throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        return DatatypeFactory.newInstance().
                newXMLGregorianCalendar(gregorianCalendar);
    }
    
    // Timestamp (used with SIF_HMACSHA256, required on all messages)
    public static String getTimestamp() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat(ISO8601);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(new Date());
    }
    
    public static String getBasicHash(String token, String secret) {
        String combined = token + ':' + secret;
        String encoded = Base64.encodeBase64String(combined.getBytes());
        return encoded;
    }

    public static String getSIF_HMACSHA256Hash(
            String token, 
            String secret, 
            String timestamp) {
        String combined = token + ':' + timestamp;
        Mac HMACSHA256 = null;
        try {
            HMACSHA256 = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SIFAuthUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        SecretKeySpec secret_key = new SecretKeySpec(
                secret.getBytes(), "HmacSHA256");
        try {
            HMACSHA256.init(secret_key);  // The secret used indirectly.
        } catch (InvalidKeyException ex) {
            Logger.getLogger(SIFAuthUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        String encoded = null;
        try {
            encoded = Base64.encodeBase64String(
                    HMACSHA256.doFinal(combined.getBytes("UTF8")));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SIFAuthUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        String reencoded = null;
        try {
            reencoded = Base64.encodeBase64String(
                    (token + ':' + encoded).getBytes("UTF8"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SIFAuthUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return reencoded;
    }
}
