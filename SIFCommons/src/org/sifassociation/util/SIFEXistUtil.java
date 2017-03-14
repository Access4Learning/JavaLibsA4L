/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;

/**
 * So we have one connection for many eXist REST operations.
 * 
 * @author jlovell
 * @since 3.0
 */
public class SIFEXistUtil {
    
    private String target;  // So we can confirm what collection we are targeting.
    String username;
    String password;
    private PoolingHttpClientConnectionManager mgr;
    private CloseableHttpClient httpClient;
    private BasicHttpContext sharedContext;
    
    public SIFEXistUtil(String target, String username, String password) {
        this.target = target;
        this.username = username;
        this.password = password;
        this.mgr = new PoolingHttpClientConnectionManager();
        this.mgr.setMaxTotal(200);
        this.mgr.setDefaultMaxPerRoute(20);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(mgr)
                .build();
        this.sharedContext = new BasicHttpContext();
    }
    
    public ResultPage readCollection(String relativePath) throws MalformedURLException, IOException, ParsingException {
        String absolute = this.target + relativePath;
        
        // So we scope things to the expected port.
        URL url = new URL(absolute);
        int port =  url.getPort();
        if(-1 == port) {
            port = url.getDefaultPort();
        }

        // So we have the core parts of the URL for use.
        HttpHost targetHost = new HttpHost(url.getHost(), port, url.getProtocol());
        
        // So we have the proper URL components.
        String pathPlus = absolute.substring(absolute.indexOf("/", 7));        
        
        /**********************************************************************/
        
        // So we set the expected headers.
        HttpGet httpGet = new HttpGet(pathPlus);
        httpGet.setHeader("Accept", "application/xml");
        if(0 < this.username.length()) {
            httpGet.setHeader("Authorization", this.getBasicEncoded());
        }
        
        // So we request the XQuery be executed (with a POST).
        String responseBody = "";
        HttpResponse httpResponse = null;
        httpResponse = httpClient.execute(targetHost, httpGet, sharedContext);
        responseBody = SIFFileUtil.readInputStream(
                httpResponse.getEntity().getContent());
        
        if(400 <= httpResponse.getStatusLine().getStatusCode()) {
            return null;
        }
        
        // So we have the response as an XML tree we can work with.
        Element responseRoot = null;
        Builder parser = new Builder();
        Document doc = parser.build(responseBody, null);
        responseRoot = doc.getRootElement();
        
        // So we know the results.
        Elements results = responseRoot.getChildElements().get(0).getChildElements();
        int hits = results.size();
        
        return new ResultPage(hits, results);
    }
    
    public String readResource(String relativePath) throws MalformedURLException, IOException, ParsingException {
        String absolute = this.target + relativePath;
        
        // So we scope things to the expected port.
        URL url = new URL(absolute);
        int port =  url.getPort();
        if(-1 == port) {
            port = url.getDefaultPort();
        }

        // So we have the core parts of the URL for use.
        HttpHost targetHost = new HttpHost(url.getHost(), port, url.getProtocol());
        
        // So we have the proper URL components.
        String pathPlus = absolute.substring(absolute.indexOf("/", 7));        
        
        /**********************************************************************/
        
        // So we set the expected headers.
        HttpGet httpGet = new HttpGet(pathPlus);
        httpGet.setHeader("Accept", "application/xml");
        if(0 < this.username.length()) {
            httpGet.setHeader("Authorization", this.getBasicEncoded());
        }
        
        // So we request the XQuery be executed (with a POST).
        String responseBody = "";
        HttpResponse httpResponse = null;
        httpResponse = httpClient.execute(targetHost, httpGet, sharedContext);
        responseBody = SIFFileUtil.readInputStream(
                httpResponse.getEntity().getContent());
        
        if(400 <= httpResponse.getStatusLine().getStatusCode()) {
            return null;
        }
        
        // So we have the response as an XML tree we can work with.
        Element responseRoot = null;
        Builder parser = new Builder();
        Document doc = parser.build(responseBody, null);
        responseRoot = doc.getRootElement();
        
        return SIFXOMUtil.pretty(responseRoot);
    }
    
    public int deleteResource(String relativePath) throws MalformedURLException, IOException, ParsingException {
        String absolute = this.target + relativePath;
        
        // So we scope things to the expected port.
        URL url = new URL(absolute);
        int port =  url.getPort();
        if(-1 == port) {
            port = url.getDefaultPort();
        }

        // So we have the core parts of the URL for use.
        HttpHost targetHost = new HttpHost(url.getHost(), port, url.getProtocol());
        
        // So we have the proper URL components.
        String pathPlus = absolute.substring(absolute.indexOf("/", 7));        
        
        /**********************************************************************/
        
        // So we set the expected headers.
        HttpDelete httpDelete = new HttpDelete(pathPlus);
        httpDelete.setHeader("Accept", "application/xml");
        if(0 < this.username.length()) {
            httpDelete.setHeader("Authorization", this.getBasicEncoded());
        }
        
        // So we request the XQuery be executed (with a POST).
        HttpResponse httpResponse = null;
        httpResponse = httpClient.execute(targetHost, httpDelete, sharedContext);
        
        // So we consume the response (and move on the to the next request).
        String responseBody = "";
        if(null != httpResponse) {
            responseBody = SIFFileUtil.readInputStream(httpResponse.getEntity().getContent());
        }
        else {
            return 500;
        }
    
        return httpResponse.getStatusLine().getStatusCode();
    }
    
    public int createResource(String relativePath, String content) throws MalformedURLException, IOException, ParsingException {
        String absolute = this.target + relativePath;
        
        // So we scope things to the expected port.
        URL url = new URL(absolute);
        int port =  url.getPort();
        if(-1 == port) {
            port = url.getDefaultPort();
        }

        // So we have the core parts of the URL for use.
        HttpHost targetHost = new HttpHost(url.getHost(), port, url.getProtocol());
        
        // So we have the proper URL components.
        String pathPlus = absolute.substring(absolute.indexOf("/", 7));
        
        /**********************************************************************/
        
        // So we set the expected headers.
        HttpPut httpPut = new HttpPut(pathPlus);
        httpPut.setHeader("Accept", "application/xml");
        if(0 < this.username.length()) {
            httpPut.setHeader("Authorization", this.getBasicEncoded());
        }
        
        // So we include the body to create.
        ContentType type = ContentType.create("application/xml", "utf-8");
        StringEntity request = new StringEntity(content, type);
        httpPut.setEntity(request);
        
        // So we request the create be executed (with a put).
        HttpResponse httpResponse = null;
        httpResponse = httpClient.execute(targetHost, httpPut, sharedContext);
        
        // So we consume the response (and move on the to the next request).
        String responseBody = "";
        if(null != httpResponse) {
            responseBody = SIFFileUtil.readInputStream(httpResponse.getEntity().getContent());
        }
        else {
            return 500;
        }
        
        return httpResponse.getStatusLine().getStatusCode();
    }
    
    public int updateResource(String relativePath, String content) throws MalformedURLException, IOException, ParsingException {
        // So we have the current state of the resource to update.
        String current = this.readResource(relativePath);
        // So we don't update resource that are not there.
        if(null == current || current.isEmpty()) {
            return 404;
        }
        
        /**********************************************************************/
        
        String absolute = this.target + relativePath;
        
        // So we scope things to the expected port.
        URL url = new URL(absolute);
        int port =  url.getPort();
        if(-1 == port) {
            port = url.getDefaultPort();
        }

        // So we have the core parts of the URL for use.
        HttpHost targetHost = new HttpHost(url.getHost(), port, url.getProtocol());
        
        // So we have the proper URL components.
        String pathPlus = absolute.substring(absolute.indexOf("/", 7));
        
        /**********************************************************************/
        
        // So we can work with both the original and content documents.
        Builder parser = new Builder();
        Element original = parser.build(current, null).getRootElement();
        Element partial = parser.build(content, null).getRootElement();
        
        // So we know what chunks to work with.
        Set<String> fields = new LinkedHashSet<String>();
        Set<String> lists = new LinkedHashSet<String>();
        Set<String> originalLists = SIFXOMUtil.getListXPaths(original);
        Set<String> partialLists = SIFXOMUtil.getListXPaths(partial);
        for(String xpath : SIFXOMUtil.getXPaths(partial)) {
            String match = "";
            String originalMatch = SIFXOMUtil.startsWithList(xpath, originalLists);
            String partialMatch = SIFXOMUtil.startsWithList(xpath, partialLists);
            // So we keep the highest level (shortest) match.
            if(!originalMatch.isEmpty() && !partialMatch.isEmpty()) {
                if(originalMatch.length() < partialMatch.length()) {
                    match = originalMatch;
                }
                match = partialMatch;
            }
            else if(!originalMatch.isEmpty()) {
                match = originalMatch;
            }
            else if(!partialMatch.isEmpty()) {
                match = partialMatch;
            }
            if(!match.isEmpty()) {
                lists.add(match);
            }
            else {
                fields.add(xpath);
            }
        }
                
        /**********************************************************************/
        
        // So we have an XUpdate document to send.
        Element root = new Element("modifications", "http://www.xmldb.org/xupdate");
        for(int i = 0; i < original.getNamespaceDeclarationCount(); i++)
        {
            String prefix = original.getNamespacePrefix(i);
            if(!prefix.isEmpty()) {
                root.addNamespaceDeclaration(prefix, 
                        original.getNamespaceURI(prefix));
            }
        }
        // To Do:  Explicitly find and utilize an unused prefix throughout.
        root.addNamespaceDeclaration("d", original.getNamespaceURI());
        root.addAttribute(new Attribute("version", "1.0"));
        
        for(String field : fields) {
            root.appendChild(getUpdateField(field, partial));
        }
        for(String list : lists) {
            root.appendChild(getUpdateList(list, partial));
        }
        
        /**********************************************************************/
        
        // So we set the expected headers.
        HttpPost httpPost = new HttpPost(pathPlus);
        httpPost.setHeader("Accept", "application/xml");
        if(0 < this.username.length()) {
            httpPost.setHeader("Authorization", this.getBasicEncoded());
        }
        
        // So we include the modification to make.
        ContentType type = ContentType.create("application/xml", "utf-8");
        StringEntity request = new StringEntity(SIFXOMUtil.pretty(root), type);
        httpPost.setEntity(request);
        
        // So we request the XUpdate be executed (with a POST).
        HttpResponse result = httpClient.execute(targetHost, httpPost, 
                sharedContext);
        
        // So we consume the response (and move on the to the next request).
        String responseBody = "";
        if(null != result) {
            responseBody = SIFFileUtil.readInputStream(result.getEntity().getContent());
        }
        else {
            return 500;
        }
        
        return result.getStatusLine().getStatusCode();
    }

    // So we update consistently.
    private static Element getUpdateField(String xpath, Element partial) {
        Element update = new Element("update", "http://www.xmldb.org/xupdate");
        XPathContext namespaces = new XPathContext("d", partial.getNamespaceURI());
        xpath = toQualifiedXPath(xpath.substring(1).split("/"), "d");
        update.addAttribute(new Attribute("select", xpath));
        Nodes children = partial.query(xpath, namespaces);
        for(int i = 0; i < children.size(); i++) {
            update.appendChild(children.get(i).getValue());
        }
        return update;
    }
    
    // So we update consistently.
    private static Element getUpdateList(String xpath, Element partial) {
        Element update = new Element("update", "http://www.xmldb.org/xupdate");
        XPathContext namespaces = new XPathContext("d", partial.getNamespaceURI());
        xpath = toQualifiedXPath(xpath.substring(1).split("/"), "d");
        update.addAttribute(new Attribute("select", xpath));
        Nodes children = partial.query(xpath + "/*", namespaces);
        for(int i = 0; i < children.size(); i++) {
            update.appendChild(new Element((Element)children.get(i)));
        }
        return update;
    }
    
    // So we can quickly and consistently combine the pieces of an XPath.
    private static String toQualifiedXPath(String[] parts, String prefix) {
        String combined = "";
        for(String part : parts) {
            // So we don't qualify attributes, prequalified parts, or with nothing.
            if(!part.startsWith("@") && !part.contains(":") && !prefix.isEmpty()) {
                combined = combined + "/" + prefix +":" + part;
            }
            else {
                combined = combined + "/" + part;
            }
        }
        return combined;
    }
    
    // To Do:  Figure out if we can use eXist sessions to improve performance.
    //         Don't forget to switch caching on.
    //         Only add if session is previously set.
    //         If returned session is diffrent, change.
    //         Unset session/don't add, if query changes.
    public ResultPage getXQueryPage(String relativePath, String query, int page, int pageSize) throws MalformedURLException, IOException, ParsingException {
        String absolute = this.target + relativePath;
        
        // So we scope things to the expected port.
        URL url = new URL(absolute);
        int port =  url.getPort();
        if(-1 == port) {
            port = url.getDefaultPort();
        }

        // So we have the core parts of the URL for use.
        HttpHost targetHost = new HttpHost(url.getHost(), port, url.getProtocol());
        
        // So we have the proper URL components.
        String pathPlus = absolute.substring(absolute.indexOf("/", 7));

        /**********************************************************************/
        
        // So we have an XML payload that carries the desired XQuery.
        Element template;
        template = SIFEXistUtil.getResourceRoot("XQuery.xml");
        ArrayList<Element> limits = new ArrayList<Element>();
        String ns = "http://exist.sourceforge.net/NS/exist";
        SIFXOMUtil.editAttribute(template, "query", ns, "start", 
                String.valueOf(page), limits);
        SIFXOMUtil.editAttribute(template, "query", ns, "max", 
                String.valueOf(pageSize), limits);
        SIFXOMUtil.editValue(template, "text", ns, query, limits);
        
        /**********************************************************************/
        
        // So we set the expected headers.
        HttpPost httpPost = new HttpPost(pathPlus);
        httpPost.setHeader("Accept", "application/xml");
        if(0 < this.username.length()) {
            httpPost.setHeader("Authorization", this.getBasicEncoded());
        }
        
        // So we include the body to create.
        ContentType type = ContentType.create("application/xml", "utf-8");
        StringEntity request = new StringEntity(template.toXML(), type);
        httpPost.setEntity(request);
        
        // So we request the XQuery be executed (with a POST).
        String responseBody = "";
        HttpResponse httpResponse = null;
        httpResponse = httpClient.execute(targetHost, httpPost, sharedContext);
        responseBody = SIFFileUtil.readInputStream(
                httpResponse.getEntity().getContent());
        
        // So we have the response as an XML tree we can work with.
        Element responseRoot = null;
        Builder parser = new Builder();
        Document doc = parser.build(responseBody, null);
        responseRoot = doc.getRootElement();
        
        // So we know the results.
        int hits = 0;
        Attribute ha = responseRoot.getAttribute(
                "hits", "http://exist.sourceforge.net/NS/exist");
        if(null != ha) {
            hits = Integer.parseInt(ha.getValue());
        }
        Elements results = responseRoot.getChildElements();
        
        return new ResultPage(hits, results);
    }
    
    // So we can get our XML templates from the classpath with one line.
    private static Element getResourceRoot(String filename) {
        String resource = "";
        
        // So we can get the file from our classpath.
        InputStream resourceStream = null;
        try {
            resourceStream = Class.forName("org.sifassociation.util.SIFEXistUtil").getClassLoader().getResourceAsStream(filename);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SIFEXistUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            resource = SIFFileUtil.readInputStream(resourceStream);
        } catch (IOException ex) {
            Logger.getLogger(SIFEXistUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // So we can work with it as a XOM XML tree.
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(resource, null);
        } catch (ParsingException ex) {
            Logger.getLogger(SIFEXistUtil.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SIFEXistUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return doc.getRootElement();
    }

    public String getBasicEncoded() {
        return "Basic " + SIFAuthUtil.getBasicHash(this.username, this.password);
    }
}
