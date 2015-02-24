/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Element;
import nu.xom.Elements;

/**
 *
 * @author jlovell
 */
public class SIFCertificateInfo {
    protected String subject;
    protected String algorithm;
    protected int keyLength;
    protected Date notBefore;
    protected Date notAfter;
    protected List<String> names = null;

    public SIFCertificateInfo() {
        this.subject = "";
        this.algorithm = "";
        this.keyLength = 0;
        this.notBefore = null;
        this.notAfter = null;
        this.names = new ArrayList<String>();
    }

    public void parse(Element certificate) {
        // Subject
        this.setSubject(
                certificate.getFirstChildElement("subject").getValue());

        // Algorithm
        this.setAlgorithm(
                certificate.getFirstChildElement("algorithm").getValue());

        // Key Length
        this.setKeyLength(Integer.parseInt(
                certificate.getFirstChildElement("keyLength").getValue()));
        
        // Dates
        SimpleDateFormat format = 
                new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        String before = certificate.getFirstChildElement("notBefore").getValue();
        try {
            this.setNotBefore(format.parse(before));
        } catch (ParseException ex) {
            Logger.getLogger(SIFCertificateInfo.class.getName()).log(Level.SEVERE, null, ex);
            this.notBefore = null;
        }
        String after = certificate.getFirstChildElement("notAfter").getValue();
        try {
            this.setNotAfter(format.parse(after));
        } catch (ParseException ex) {
            Logger.getLogger(SIFCertificateInfo.class.getName()).log(Level.SEVERE, null, ex);
            this.notAfter = null;
        }

        // Names
        Element namesRoot = certificate.getFirstChildElement("names");
        Elements ns = namesRoot.getChildElements();
        for(int i = 0; i < ns.size(); i++) {
            // Name
            Element n = ns.get(i);
            names.add(n.getValue());
        }
    }
    
    public Element toXOM() {
        Element cert = new Element("certificate");
        
        Element sub = new Element("subject");
        cert.appendChild(sub);
        sub.appendChild(this.subject);
        
        Element alg = new Element("algorithm");
        cert.appendChild(alg);
        alg.appendChild(this.algorithm);
        
        Element key = new Element("keyLength");
        cert.appendChild(key);
        key.appendChild(Integer.toString(this.keyLength));
        
        Element before = new Element("notBefore");
        cert.appendChild(before);
        before.appendChild(null != this.notBefore ? this.notBefore.toString() : "");            

        Element after = new Element("notAfter");
        cert.appendChild(after);
        after.appendChild(null != this.notAfter ? this.notAfter.toString() : "");            
        
        Element ns = new Element("names");
        cert.appendChild(ns);
        for(String name : names) {
            Element n = new Element("name");
            ns.appendChild(n);
            n.appendChild(name);
        }
        
        return cert;
    }
    
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public List<String> getNames() {
        return names;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }
    
    @Override
    public int hashCode() {
        return subject.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SIFCertificateInfo other = (SIFCertificateInfo) obj;
        if ((this.subject == null) ? (other.subject != null) : !this.subject.equals(other.subject)) {
            return false;
        }
        if ((this.algorithm == null) ? (other.algorithm != null) : !this.algorithm.equals(other.algorithm)) {
            return false;
        }
        if (this.keyLength != other.keyLength) {
            return false;
        }
        return true;
    }
    
}
