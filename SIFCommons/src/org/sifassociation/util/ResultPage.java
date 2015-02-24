/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import nu.xom.Elements;

/**
 * So we can keep usefull metadata with the results.
 * 
 * @author jlovell
 * @since 3.0
 */
public class ResultPage {
    private int hits;
    private Elements results;

    public ResultPage() {
        this.hits = 0;
        this.results = null;
    }

    public ResultPage(int hits, Elements results) {
        this.hits = hits;
        this.results = results;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public Elements getResults() {
        return results;
    }

    public void setResults(Elements results) {
        this.results = results;
    }
    
}
