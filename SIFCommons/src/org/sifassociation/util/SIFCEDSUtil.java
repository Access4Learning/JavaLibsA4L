/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;

/**
 * Screen scrapping library for CEDS.
 * 
 * @author jlovell
 * @since 3.2
 */
public class SIFCEDSUtil {

    /**
     * Combines all the necessary steps to retrieve codes at a give URL.
     * Note:  Does NOT cache results, please don't abuse.
     * 
     * @param url  CEDS page to extract codes from.
     * @return comma space delimited codes
     * @since 3.2
     */
    public static String retrieveCodes(String url) {
        String codes = "";
        // So we have the CEDS page to work with.
        String page = "";
        try {
            page = SIFFileUtil.readURL(url);
        } catch (IOException ex) {
            Logger.getLogger(SIFCEDSUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(page.isEmpty()) {
            return "";
        }
        // So we have the contents of the "xml" variable on the page.
        String xml = getPageXML(page);
        // So we have the convered actual XML/XSD snippet.
        xml = getCleanXML(xml);
        // So we have the codes contained within the XML/XSD snippet.
        codes = getCodes(xml);
        return codes;
    }
    
    /**
     * Finds the first JavaScript xml variable in page and copies its contents.
     * 
     * @param page
     * @return found xml contents or ""
     * @since 3.2
     */
    public static String getPageXML(String page) {
        if(null == page || page.isEmpty()) {
            return "";
        }
        
        String xml = "";
        // So we know where to begin.
        String match = "var xml = '";
        int begin = page.indexOf(match);
        // So the lack of a match does NOT create problems.
        if(-1 == begin) {
            return "";
        }
        // So we start at the beggining of the contents.
        begin = begin + match.length();
        // So we know where the contents end.
        int end = page.indexOf("';", begin);
        if(0 <= end) {
            // So we have only the contents.
            xml = page.substring(begin, end);
        }
        return xml;
    }
    
    
    /**
     * Converts CEDS XML into actual XML/XSD.
     * - Strips known CEDS HTML tags. 
     * - Converts escaped XML characters.
     * - Adds namespace declaration.
     * 
     * @param html XML encoded in HTML
     * @return 
     * @since 3.2
     */
    public static String getCleanXML(String html) {
        if(null == html || html.isEmpty()) {
            return "";
        }

        // So get rid of the HTML tags.
        html = html.replace("<span>", "");
        html = html.replace("</span>", "");
        // So we have actual XML tags.
        html = html.replace("&lt;", "<");
        html = html.replace("&gt;", ">");
        // So we know we have XML Schema.
        html = html.replace("<xs:simpleType", "<xs:simpleType xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"");
        return html;
    }
    
    /**
     * Gets the codes (enumerations) out of the supplied XML Schema (snippet).
     * 
     * @param xml XML/XSD to extract codes from
     * @return comma space delimited codes
     * @since 3.2
     */
    public static String getCodes(String xml) {
        if(null == xml || xml.isEmpty()) {
            return "";
        }
        
        String codes = "";
        // So we can work with the XML.
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(xml, null);
        } catch (ParsingException | IOException ex) {
            Logger.getLogger(SIFCEDSUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(null == doc) {
            return "";
        }
        Element root = doc.getRootElement();
        // So we get just the codes.
        XPathContext context = new XPathContext("xs", "http://www.w3.org/2001/XMLSchema");
        Nodes results = root.query("//xs:enumeration/@value", context);
        for(int i = 0; i < results.size(); i++) {
            // So we don't get a trailing comma.
            if(!codes.isEmpty()) {
                codes = codes + ", ";
            }
            codes = codes + results.get(i).getValue();
        }        
        return codes;
    }

}
