/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.querying;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.sifassociation.util.SIFFileUtil;
import org.sifassociation.util.SIFXOMUtil;

/**
 *
 * @author jlovell
 */
public class EXistXQueryREST implements IXQuery {

    private String target;  // So we can confirm what collection we are targeting.
    private PoolingHttpClientConnectionManager mgr;
    private CloseableHttpClient httpClient;
    private BasicHttpContext sharedContext;
    private int currentIndex;
    private Element currentResult;
    private String query;
    private int hits;
    

    public EXistXQueryREST(String target) {
        this.target = target;
        this.query = "";
        this.mgr = new PoolingHttpClientConnectionManager();
        this.mgr.setMaxTotal(200);
        this.mgr.setDefaultMaxPerRoute(20);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(mgr)
                .build();
        this.sharedContext = new BasicHttpContext();
        this.reset();
    }
    
    // So we can start over when the query changes.
    private void reset() {
        this.currentIndex = 1;
        this.currentResult = null;
        this.hits = 0;
    }
    
    @Override
    public void run(String query) throws Exception {
        // So we know when to start over.
        if(0 != query.compareTo(this.query) || !this.hasNext()) {
            this.reset();
        }
        
        // So we know the current query.
        this.query = query;
        
        // So we scope things to the expected port.
        URL url = new URL(this.target);
        int port =  url.getPort();
        if(-1 == port) {
            port = url.getDefaultPort();
        }

        // So we have the core parts of the URL for use.
        HttpHost targetHost = new HttpHost(url.getHost(), port, url.getProtocol());
        
        // So we have the proper URL components.
        String pathPlus = this.target.substring(this.target.indexOf("/", 7));

        /**********************************************************************/
        
        // So we have an XML payload that carries the desired XQuery.
        Element template;
        template = EXistXQueryREST.getResourceRoot("XQuery.xml");
        ArrayList<Element> limits = new ArrayList<Element>();
        String ns = "http://exist.sourceforge.net/NS/exist";
        SIFXOMUtil.editAttribute(template, "query", ns, "start", 
                String.valueOf(this.currentIndex), limits);
        SIFXOMUtil.editValue(template, "text", ns, query, limits);
        
        /**********************************************************************/
        
        // So we set the expected headers.
        HttpPost httpPost = new HttpPost(pathPlus);
        httpPost.setHeader("Accept", "application/xml");
        
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
        this.hits = Integer.parseInt(responseRoot.getAttribute(
                "hits", "http://exist.sourceforge.net/NS/exist").getValue());
        Elements results = responseRoot.getChildElements();
        if(0 < results.size()) {
            this.currentResult = results.get(0);
        }
        else {
            this.currentResult = null;
        }
    }

    @Override
    public String getNext() throws Exception {
        String result = "";
        if(null != this.currentResult) {
            result = SIFXOMUtil.pretty(this.currentResult);
        }

        if(this.hasNext()) {
            this.currentIndex++;
        }
        if(this.hasNext()) {
            this.run(this.query);
        }
        else{
            this.currentResult = null;
        }
        
        return result;
    }

    @Override
    public boolean hasNext() throws Exception {
        return this.currentIndex <= this.hits;
    }
    
    /**
     * Quickly check if two instances target the same collection.
     * 
     * Note:  So that UNIX paths may be used comparison is case sensitive!
     * 
     * @param o (Object to compare.)
     * @return True if the two objects targets match, otherwise false.
     * @since 3.0
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EXistXQueryREST))
            return false;
        return o.toString().equals(target);
    }
    
    /**
     * So we support hash collections properly.
     * 
     * @return The hash of the target collection.
     * @since 3.0
     */
    @Override
    public int hashCode() {
        return target.hashCode();
    }
    
    /**
     * So the collection targeted by this object can be expressed to the user.
     * 
     * @return The target path to the collection.
     * @see getNext()
     * @since 3.0
     */
    @Override
    public String toString() {
        return this.target;
    }
    
    // So we can get our XML templates from the classpath with one line.
    private static Element getResourceRoot(String filename) {
        String resource = "";
        
        // So we can get the file from our classpath.
        InputStream resourceStream = null;
        try {
            resourceStream = Class.forName("org.sifassociation.querying.EXistXQueryREST").getClassLoader().getResourceAsStream(filename);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(EXistXQueryREST.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            resource = SIFFileUtil.readInputStream(resourceStream);
        } catch (IOException ex) {
            Logger.getLogger(EXistXQueryREST.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // So we can work with it as a XOM XML tree.
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(resource, null);
        } catch (ParsingException ex) {
            Logger.getLogger(EXistXQueryREST.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EXistXQueryREST.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return doc.getRootElement();
    }
    
}
