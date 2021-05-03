/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.sifassociation.goessner.XmlJsonNative;

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

    /**
     * Finds the first application/ld+json tag in page and copies its contents.
     * 
     * @param page
     * @return found application/ld+json contents or ""
     * @since 4.2
     */
    public static String getPageJSON(String page) {
        if(null == page || page.isEmpty()) {
            return "";
        }
        
        String json = "";
        // So we know where to begin.
        String match = "<script type=\"application/ld+json\">";
        int begin = page.indexOf(match);
        // So the lack of a match does NOT create problems.
        if(-1 == begin) {
            return "";
        }
        // So we start at the beggining of the contents.
        begin = begin + match.length();
        // So we know where the contents end.
        int end = page.indexOf("</script>", begin);
        if(0 <= end) {
            // So we have only the contents.
            json = page.substring(begin, end);
        }
        return json;
    }
    
    /**
     * Converts CEDS JSON into actual JSON.
     * - 
     * 
     * @param json JSON that needs to be cleaned before it is parsed.
     * @return 
     * @since 4.2
     */
    public static String getCleanJSON(String json) {
        if(null == json || json.isEmpty()) {
            return "";
        }
        
        // So get rid of known problems.
        //json = json.replace("<span>", "");
        return json;
    }
    
    /**
     * Gets the codes (enumerations) out of the supplied JSON-LD.
     * 
     * @param json JSON-LD to extract codes from
     * @return comma space delimited codes
     * @since 4.2
     */
    public static String getCodesJSON(String json) {
        if(null == json || json.isEmpty()) {
            return "";
        }
        
        /* Save: This doesn't work currently, but it should in the future.
        // So we have an accurate representation of the JSON as a tree.
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode jsonRoot;
        try {
            jsonRoot = jsonMapper.readTree(json);
        } catch (IOException ex) {
            Logger.getLogger(XmlJsonNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        /* Start Debug
        // So we have the resulting JSON.
        try {
            json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonRoot);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(XmlJsonNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }        
        System.out.println(json);
        /* End Debug */
        
        String codes = "";
        List <String> results = new ArrayList<>();

        // To Do: Rely on parsed JSON.
        int index = 0;
        int start = 0;
        int last = json.lastIndexOf("#");
        String code = "";  // Current code.
        index = json.indexOf("https://ceds.ed.gov/element/", index);  // Consume the JSON-LD namespce declaration.
        while(index < last) {
            index = json.indexOf("https://ceds.ed.gov/element/", index);  // Start of code value.
            start = json.indexOf("#", index)+1;  // Skip code ID.  Start of what we want.
            index = json.indexOf("\"", start);  // End of what we want.
            code = json.substring(start, index);
            if(!code.isEmpty()) {
                // So we don't get a trailing comma.
                if(!codes.isEmpty()) {
                    codes = codes + ", ";
                }
                codes = codes + code;
            }
        }        

        return codes;
    }
}
