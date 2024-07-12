package org.sifassociation.messaging;

import java.util.UUID;
import java.net.NetworkInterface;
import java.net.InetAddress;

import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import jakarta.xml.bind.DatatypeConverter;

import java.lang.StringBuffer;

/**
 * Mutable class for handling SIF RefIds.
 * Should always produce a version one UUID (with SIF modifications).
 * Must be able to store, inspect, and retrieve version four UUIDs.
 * 
 * To Do:  Add notes on thread safety.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public final class SIFRefId {
    private String hardware = "";  // Hardware address.
    private int version = 1;  // UUID version (1 or 4).
    private int sequence = 0;  // Ensure supplied sequence is 0 - 0x3FFF
    private long timestamp = 0;  // UTC milliseconds from the epoch.
    
    private boolean generic = true;  // RFC 4122 compliant UUID or with SIFisms
    
    private static long lastTimestamp = 0;  // So we don't reissue an ID.
    
    /**
     * Default constructor.
     * 
     * Note:  Default parameters are not enough to generate a unique UUID.
     * 
     * @since 3.0
     */
    public SIFRefId() {
        setHardware("");
        setVersion(1);
        setSequence(0);
        setTimestamp(0);
    }

    /**
     * Minimal constructor for creating a unique UUID.
     * 
     * @param hostname  Hostname or IP address of a local network adapter.
     * @param clockSequence  Number that must be incremented when the clock is
     * set back or the network adapter is moved to a new computer.
     * @see retrieveHardware
     * @see setHardware
     * 
     * @since 3.0
     */
    public SIFRefId(String hostname, int clockSequence)
            throws UnknownHostException, SocketException {
        setHardware(retrieveHardware(hostname));
        setVersion(1);  // Default!
        setSequence(clockSequence);
        Long currentTimestamp;
        do {
            currentTimestamp = generateTimestamp();
        } while(lastTimestamp == currentTimestamp);
        lastTimestamp = currentTimestamp;
        setTimestamp(currentTimestamp);  // Now!
    }
    
    /**
     * Constructor based on existing RefId (string).
     * 
     * @param identifier  UUID string.
     * @see parse
     * @since 3.0
     */
    public SIFRefId(String identifier) {
        parse(identifier);
    }
    
    /**
     * Commit an existing RefId string to the member variables.
     * 
     * @param identifier  UUID string.
     * @throws IllegalArgumentException  If included version is not one or four.
     * @since 3.0
     */
    public void parse(String identifier) {
        // So we work with the actual identifier.
        identifier = identifier.substring(identifier.lastIndexOf(':') + 1);
        
        // If classic SIF RefId instead of standard UUID, convert!
        if(32 == identifier.length()) {
            // So we can serialize the RefId in its given form.
            setGeneric(false);
            // So we have dashes where expected for a standard UUID.
            StringBuilder id = new StringBuilder(identifier);
            id.insert(8, "-");
            id.insert(13, "-");
            id.insert(18, "-");
            id.insert(23, "-");
            identifier = id.toString();
        }
        
        char v = identifier.charAt(14);
        if('4' == v) {
            // So we can serilize the RefId in its given version.
            setVersion(4);
            // So we can parse the random UUID with the time-based library.
            StringBuilder id = new StringBuilder(identifier);
            id.replace(14, 15, "1");
            identifier = id.toString();
        }
        else if ('1' != v) {
            throw new IllegalArgumentException("SIFRefID: " + v +
                    " is not a valid version (1 or 4) expected.");
        }
        else {
            // So we keep that this was a time-based UUID.
            setVersion(1);
        }
        
        // So we do not rely on our own understanding to parse the UUID.
        UUID messageId = UUID.fromString(identifier);
        setHardware(Long.toHexString(messageId.node()));
        setSequence(messageId.clockSequence());
        setTimestamp(messageId.timestamp());
    }
    
    /**
     * Directly sets the hardware address to a valid hex string.
     * 
     * @param hardwareAddress 
     * @throws IllegalArgumentException  If hardwareAddress is longer than 12.
     * @throws IllegalArgumentException  If non-hex characters are present.
     * @since 3.0
     */
    public void setHardware(String hardwareAddress) {
        // So the hardware address is not too long.
        if(12 < hardwareAddress.length()) {
            throw new IllegalArgumentException("SIFRefID: " + hardwareAddress + 
                    " is not a valid hardware address (too long).");
        }
        // So the hardware address is really hexadecimal.
        if(!hardwareAddress.matches("[0-9a-fA-F]*")) {
            throw new IllegalArgumentException("SIFRefID: " + hardwareAddress + 
                    " is not a valid hardware address (not a hex string).");
        }
        // So we keep the specified valid address.
        hardware = hardwareAddress.toLowerCase();
    }
    
    /**
     * Recalls the hardware address.
     * 
     * @return Padded hexadecimal string of length 12.
     * @see pad
     * @since 3.0
     */
    public String getHardware() {
        return pad(12, hardware);
    }
    
    /**
     * Sets the version to a valid one.
     * 
     * @param v  The desired version.
     * @throws IllegalArgumentException  If v is not one or four.
     * @since 3.0
     */
    public void setVersion(int v) {
        // So only supported version are valid.
        if(1 != v && 4 != v) {
            throw new IllegalArgumentException("SIFRefID: " + v + 
                    " is not a valid version (1 or 4).");
        }
        // So we keep the specified valid version.
        version = v;
    }
    
    /**
     * Recalls the version.
     * 
     * @return Hexadecimal string of the stored version.
     * @since 3.0
     */
    public String getVersion() {
        return Integer.toHexString(version);
    }
    
    /**
     * Sets the clock sequence to a valid 14bit (unsigned) number.
     * 
     * @param clockSequence
     * @throws IllegalArgumentException  If clockSequence is not 0 = 0x3FFF.
     * @since 3.0
     */
    public void setSequence(int clockSequence) {
        // So only supported clock sequences are valid.
        if(0 > clockSequence || 0x3FFF < clockSequence) {
            throw new IllegalArgumentException("SIFRefID: " + 
                    Integer.toHexString(clockSequence) +
                    " is not a valid clock sequence (0 - 0x3FFF).");
        }
        // So we keep the specified valid clock sequence.
        sequence = clockSequence;
    }
    
    /**
     * Recalls the clock sequence.
     * 
     * @return Hexadecimal string of the stored clock sequence.
     * @since 3.0
     */
    public String getSequence() {
        return pad(4, Integer.toHexString(sequence));
    }
    
    /**
     * Sets the timestamp to a valid 60bit (unsigned) number.
     * 
     * @param time
     * @throws IllegalArgumentException  If time is not a 60bit number.
     * @since 3.0
     */
    public void setTimestamp(long time) {
        // So only supported timestamps are valid.
        if(0 > time || 0xFFFFFFFFFFFFFFFL < time) {
            throw new IllegalArgumentException("SIFRefID: " + 
                    Long.toHexString(time) +
                    " is not a valid timestamp (0 - 0xFFFFFFFFFFFFFFF).");
        }
        
        // So we keep the specified valid clock sequence.
        timestamp = time;
    }
    
    /**
     * Recalls the timestamp.
     * 
     * @return Hexadecimal string of the stored timestamp.
     * @see toString
     * @since 3.0
     */
    public String getTimestamp() {
        return Long.toHexString(timestamp);
    }
    
    /**
     * Sets whether to generate a generic UUID or one in classic SIF style.
     * 
     * @param g  True = UUID.  False = SIF.
     * @since 3.0
     */
    public void setGeneric(boolean g) {
        generic = g;
    }
    
    /**
     * Retrieves whether to generate a generic UUID or one in classic SIF style.
     * 
     * Note:  Is impacted whenever a RefId is parsed.
     * 
     * @return Boolean  True = UUID.  False = SIF.
     * @since 3.0
     */
    public boolean getGeneric() {
        return generic;
    }
    
   /**
     * Checks if the supplied SIFRefId's data matches this one exactly.
     * 
     * @param o Object to inspect.
     * @return boolean On equality <code>true</code>, otherwise 
     * <code>false</code>.
     * @since 3.0
     */
    @Override public boolean equals(Object o) {
        // So nothing never equals something.
        if (o == null) return false;

        // So we know if we have an object of the same type.
        if(!(o instanceof SIFRefId)) return false;
        
        // So we can access SIFVersion class member functions.
        SIFRefId id = (SIFRefId)o;
        
        // So we know if the node does NOT match.
        if(0 != getHardware().compareTo(id.getHardware())) return false;
        
        // So we know if the version does NOT match.
        if(0 != getVersion().compareTo(id.getVersion())) return false;

        // So we know if the sequence does NOT match.
        if(0 != getSequence().compareTo(id.getSequence())) return false;
        
        // So we know if the timestamp does NOT match.
        if(0 != getTimestamp().compareTo(id.getTimestamp())) return false;
        
        // Since we did NOT find an non-equal parts, they must be identical.
        return true;        
    }
    
   /**
     * If necessary calculates a hash based on the data member variables.
     * 
     * @return int
     * @since 3.0
     */    
    @Override public int hashCode() {
        int result = 17;

        // So we treat each data variable the same.
        String[] dataVariables = {getHardware(), getVersion(), getSequence(), 
            getTimestamp()};
        for (String current : dataVariables) {
            // So empty strings effect the hash.
            if (current.isEmpty()) {
                current = "200";
            }
            
            // So we trim any large numbers down to integers (32bit hex).
            // Note: Keeps the least signifigant bits.
            int start = 0;
            int end = current.length();
            if(end > 7) start = end - 7;
            current = current.substring(start, end);
            
            // So the hash is impacted by the current string.
            result = (31 * result) + Integer.parseInt(current, 16);
        }
        
        return result;
    }
    
    @Override public String toString() {
        String result = "";
        
        // So we pad the hardware address as necessary.
        String nodeHex = getHardware();
        
        // So we have the UUID version in the the needed format.
        String versionHex = getVersion();
        
        // So we have the clock sequence in the needed format.
        String sequenceHex = getSequence();
        
        // So we break the timestamp into its component parts.
        String timestampHex = getTimestamp();
        // Low
        int e = timestampHex.length();  // end
        int l = e - 8;  // low
        if(0 > l)  {l = 0;}
        String low = "";
        low = timestampHex.substring(l, e);
        low = pad(8, low);
        // Mid
        int m = l - 4;  // mid
        if(0 > m)  {m = 0;}
        String mid = "";
        mid = timestampHex.substring(m, l);
        mid = pad(4, mid);
        // High
        int h = m - 3;  // high
        if(0 > h)  {h = 0;}
        String high = "";
        high = timestampHex.substring(h, m);
        high = pad(3, high);
        // So we put it all together (00000000-0000-0000-0000-000000000000).
        result = low + "-" + mid + "-" + versionHex + high + "-" + sequenceHex + "-" + nodeHex;
        
        // So we produce classic SIF RefIds when called for.
        if(!getGeneric()) {
            result = result.replaceAll("-", "");
            result = result.toUpperCase();
        }
        
        return result;
    }
    
    
    /**
     * Takes a byte and converts it into its hexadecimal string representation.
     * 
     * @param b
     * @return Hexadecimal string representation of b.
     * @since 3.0
     */
    private static String byteToHex(byte b) {
        // So we can retrieve hex representation by index.
        char hexDigit[] = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };
        // So we have the appropriate index of the above array.
        char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
        return new String(array);
    }
    
    /**
     * Pad the unpadded string with leading zeros until it obtains length.
     * 
     * Note:  Leaves string longer than the indicated length unchanged.
     * 
     * @param length  Desired length.
     * @param unpadded  Original string.
     * @return The padded string.
     * @since 3.0
     */
    private static String pad(int length, String unpadded) {
        String result = unpadded;
        
        while(length > result.length()) {
            result = "0" + result;
        }
        
        return result;
    }
    
    /**
     * Get the hardware address of the specified adapter.
     * 
     * To Do:  Get the host name form the servlet or listenHostOverride.
     * 
     * @param hostname  Hostname or IP address of a local network adapter.
     * @return The hex string related to the specified hostname.
     * @since 3.0
     */
    private static String retrieveHardware(String hostname) 
            throws UnknownHostException, SocketException {
        // So we have a local network interface.
        NetworkInterface net;
        net = NetworkInterface.getByInetAddress(
                InetAddress.getByName(hostname));
        // So we have the hardware address for creating type 1 UUIDs.
        byte[] hardware;
        hardware = net.getHardwareAddress();
        String nodeHex = "";
        for(byte b : hardware)  {
            nodeHex = nodeHex + byteToHex(b);
        }
        return nodeHex;
    }
    
    /**
     * So we can get the current timestamp for use in UUIDs.
     * 
     * @return  The current time in milliseconds, 0 on failure.
     * @since 3.0
     */
    private Long generateTimestamp() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        XMLGregorianCalendar now = null;
        try {
            now = DatatypeFactory.newInstance().
                    newXMLGregorianCalendar(gregorianCalendar);
        } catch(DatatypeConfigurationException msg) {
            return 0L;
        }
        Calendar calendar = DatatypeConverter.parseDateTime(now.toString());
        return calendar.getTimeInMillis();
    }
}
