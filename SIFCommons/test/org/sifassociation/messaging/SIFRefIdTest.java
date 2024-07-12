/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.net.InetAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sifassociation.messaging.SIFRefId;

/**
 *
 * @author jlovell
 */
public class SIFRefIdTest {
    
    /**
     * Since SIFRefId is a mutable class these need to be set before each test.
     */
    SIFRefId parsed = null;
    SIFRefId sif = null;
    SIFRefId created = null;
    SIFRefId minimum = null;
    SIFRefId maximum = null;
    SIFRefId random = null;
    
    private final String hostname;
    
    public SIFRefIdTest() {
        this.hostname = "192.168.4.84";
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws UnknownHostException, SocketException {
        parsed = new SIFRefId("uuid:fe886483-0132-1000-0003-002500f11f4e");
        sif    = new SIFRefId("FE886483F132100F00F3002500F11F4E");
        created = new SIFRefId(this.hostname, 127);
        minimum = new SIFRefId("urn:uuid:00000000-0000-1000-0000-000000000000");
        maximum = new SIFRefId("urn:uuid:ffffffff-ffff-1fff-3fff-ffffffffffff");
        random  = new SIFRefId("urn:uuid:ffffffff-ffff-4fff-3fff-ffffffffffff");
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of parse method, of class SIFRefId.
     */
    @Test
    public void testParse() {
        System.out.println("parse");
        assertNotNull(parsed);
        assertNotNull(sif);
        assertNotNull(minimum);
        assertNotNull(maximum);
        assertNotNull(random);
    }

    /**
     * Test of setHardware method, of class SIFRefId.
     */
    @Test
    public void testSetHardware() {
        System.out.println("setHardware");
        String tooLong = "123456789ABCD";
        String tooBad  = "GFDCBA987654";
        String justRight = "123456789ABC";
        SIFRefId instance = new SIFRefId();
        
        // Too long.
        try {
            instance.setHardware(tooLong);
            fail("SetHardware accepted a hardware address that is too long.");
        } catch (IllegalArgumentException ex) {
          // String containing an illegally long hardware address was rejected!  
        }
        
        // Too bad (invalid character used).
        try {
            instance.setHardware(tooBad);
            fail("SetHardware accepted a hardware address that is not hex char.");
        } catch (IllegalArgumentException ex) {
          // String containing an illegally character was rejected!  
        } 

        // Just right.
        instance.setHardware(justRight);
    }

    /**
     * Test of getHardware method, of class SIFRefId.
     */
    @Test
    public void testGetHardware() {
        System.out.println("getHardware");        
        assertEquals("002500f11f4e", parsed.getHardware());
        assertEquals("002500f11f4e", sif.getHardware());
        assertEquals("000000000000", minimum.getHardware());
        assertEquals("ffffffffffff", maximum.getHardware());
        assertEquals("ffffffffffff", random.getHardware());
    }

    /**
     * Test of setVersion method, of class SIFRefId.
     */
    @Test
    public void testSetVersion() {
        System.out.println("setVersion");
        SIFRefId instance = new SIFRefId();
    
        // Too low.
        try {
            instance.setVersion(0);
            fail("SetVersion accepted a version that is too low (<1).");
        } catch (IllegalArgumentException ex) {
          // Too small a version number was rejected!  
        } 
        
        // Preferred.
        instance.setVersion(1);
        
        // Unsupported.
        try {
            instance.setVersion(2);
            instance.setVersion(3);
            fail("SetVersion accepted a version that is unsupported (2-3).");
        } catch (IllegalArgumentException ex) {
          // Too large a version number was rejected!  
        }
        
        // Acceptable.
        instance.setVersion(4);
        
        // Too high.
        try {
            instance.setVersion(5);
            fail("SetVersion accepted a version that is too high (>4).");
        } catch (IllegalArgumentException ex) {
          // Too large a version number was rejected!  
        } 
    }

    /**
     * Test of getVersion method, of class SIFRefId.
     */
    @Test
    public void testGetVersion() {
        System.out.println("getVersion");
        assertEquals("1", parsed.getVersion());
        assertEquals("1", sif.getVersion());
        assertEquals("1", created.getVersion());
        assertEquals("1", minimum.getVersion());
        assertEquals("1", maximum.getVersion());
        assertEquals("4", random.getVersion());
    }

    /**
     * Test of setSequence method, of class SIFRefId.
     */
    @Test
    public void testSetSequence() {
        System.out.println("setSequence");
        SIFRefId instance = new SIFRefId();
        
        // Too low.
        try {
            instance.setSequence(-1);
            fail("SetSequence accepted a version that is too low (<0).");
        } catch (IllegalArgumentException ex) {
          // Too small a sequence number was rejected!  
        } 
        
        // Just right.
        instance.setSequence(0);
        instance.setSequence(0x3FFF);
        
        // Too high.
        try {
            instance.setSequence(0x4000);
            fail("SetSequence accepted a version that is too high (>0x3FFF).");
        } catch (IllegalArgumentException ex) {
          // Too large a sequence number was rejected!  
        } 
    }

    /**
     * Test of getSequence method, of class SIFRefId.
     */
    @Test
    public void testGetSequence() {
        System.out.println("getSequence");
        assertEquals("0003", parsed.getSequence());
        assertEquals("00f3", sif.getSequence());
        assertEquals("007f", created.getSequence());
        assertEquals("0000", minimum.getSequence());
        assertEquals("3fff", maximum.getSequence());
        assertEquals("3fff", random.getSequence());
    }

    /**
     * Test of setTimestamp method, of class SIFRefId.
     */
    @Test
    public void testSetTimestamp() {
        System.out.println("setTimestamp");
        SIFRefId instance = new SIFRefId();
        
        // Too small.
        try {
            instance.setTimestamp(-1);
            fail("SetTimestamp accepted a time that is too small (<0).");
        } catch (IllegalArgumentException ex) {
          // Too small a sequence number was rejected!  
        }
        
        // Just right.
        instance.setTimestamp(0);
        instance.setTimestamp(0xFFFFFFFFFFFFFFFL);
        
        // Too large.
        try {
            instance.setTimestamp(0x1000000000000000L);
            fail("SetTimestamp accepted a time that is too large "
                    + "(>0xFFFFFFFFFFFFFFFL).");
        } catch (IllegalArgumentException ex) {
          // Too large a time was rejected!  
        }
    }    

    /**
     * Test of getTimestamp method, of class SIFRefId.
     */
    @Test
    public void testGetTimestamp() {
        System.out.println("getTimestamp");
        assertEquals("132fe886483", parsed.getTimestamp());
        assertEquals("ff132fe886483", sif.getTimestamp());
        assertEquals("0", minimum.getTimestamp());
        assertEquals("fffffffffffffff", maximum.getTimestamp());
        assertEquals("fffffffffffffff", random.getTimestamp());
    }

    /**
     * Test of setGeneric method, of class SIFRefId.
     */
    @Test
    public void testSetGeneric() {
        System.out.println("setGeneric");
        // Boolean!
    }

    /**
     * Test of getGeneric method, of class SIFRefId.
     */
    @Test
    public void testGetGeneric() {
        System.out.println("getGeneric");
        // Boolean!
    }

    /**
     * Test of equals method, of class SIFRefId.
     */
    @Test
    public void testEquals() throws UnknownHostException, SocketException {
        System.out.println("equals");

        // Should be equal.
        SIFRefId again = new SIFRefId(
                "urn:uuid:fe886483-0132-1000-0003-002500f11f4e");
        if(!again.equals(parsed)) {
            fail("Parsing the same string resulted in an unequal objects.");
        }
        again = new SIFRefId("FE886483F132100F00F3002500F11F4E");
        if(!again.equals(sif)) {
            fail("Parsing the same string resulted in an unequal objects.");
        }
        again = new SIFRefId("urn:uuid:00000000-0000-1000-0000-000000000000");
        if(!again.equals(minimum)) {
            fail("Parsing the same string resulted in an unequal objects.");
        }        
        again = new SIFRefId("urn:uuid:ffffffff-ffff-1fff-3fff-ffffffffffff");
        if(!again.equals(maximum)) {
            fail("Parsing the same string resulted in an unequal objects.");
        }        
        again = new SIFRefId("urn:uuid:ffffffff-ffff-4fff-3fff-ffffffffffff");
        if(!again.equals(random)) {
            fail("Parsing the same string resulted in an unequal objects.");
        }
       
        // Should always be diffrent!
        again = new SIFRefId(this.hostname, 127);        
        if(again.equals(created)) {
            fail("Generating two unique objects were tested equal.");
        }        
    }

    /**
     * Test of hashCode method, of class SIFRefId.
     */
    @Test
    public void testHashCode() throws UnknownHostException, SocketException {
//        System.out.println("hashCode");
        
        // Should be equal.
        SIFRefId again = new SIFRefId(
                "urn:uuid:fe886483-0132-1000-0003-002500f11f4e");
        if(again.hashCode() != parsed.hashCode()) {
            fail("Parsing the same string resulted in an unequal hashes.");
        }
        again = new SIFRefId("FE886483F132100F00F3002500F11F4E");
        if(again.hashCode() != sif.hashCode()) {
            fail("Parsing the same string resulted in an unequal hashes.");
        }
        again = new SIFRefId("urn:uuid:00000000-0000-1000-0000-000000000000");
        if(again.hashCode() != minimum.hashCode()) {
            fail("Parsing the same string resulted in an unequal hashes.");
        }   
        again = new SIFRefId("urn:uuid:ffffffff-ffff-1fff-3fff-ffffffffffff");
        if(again.hashCode() != maximum.hashCode()) {
            fail("Parsing the same string resulted in an unequal hashes.");
        }
        again = new SIFRefId("ffffffff-ffff-4fff-3fff-ffffffffffff");
        if(again.hashCode() != random.hashCode()) {
            fail("Parsing the same string resulted in an unequal hashes.");
        }
        
        // Should always be diffrent!
        again = new SIFRefId(this.hostname, 127);
        if(again.hashCode() == created.hashCode()) {
            fail("Generating two unique objects had the same hash.");
        }      
    }

    /**
     * Test of toString method, of class SIFRefId.
     */
    @Test
    public void testToString() throws UnknownHostException, SocketException {
        // Should be equal.
        if(!parsed.toString().equals(
                "fe886483-0132-1000-0003-002500f11f4e")) {
            fail("Parsed and serialized string does not equal the string.");
        }
        if(!sif.toString().equals("FE886483F132100F00F3002500F11F4E")) {
            fail("Parsed and serialized string does not equal the string.");
        }
        if(!minimum.toString().equals(
                "00000000-0000-1000-0000-000000000000")) {
            fail("Parsed and serialized string does not equal the string.");
        }
        if(!maximum.toString().equals(
                "ffffffff-ffff-1fff-3fff-ffffffffffff")) {
            fail("Parsed and serialized string does not equal the string.");
        }
        if(!random.toString().equals(
                "ffffffff-ffff-4fff-3fff-ffffffffffff")) {
            fail("Parsed and serialized string does not equal the string.");
        }

        // Should always be diffrent!
        SIFRefId again = new SIFRefId(this.hostname, 127);
        if(again.toString().equals(created.toString())) {
            fail("Generating two unique objects produced the same string.");
        }
    }
}
