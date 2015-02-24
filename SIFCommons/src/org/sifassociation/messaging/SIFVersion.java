package org.sifassociation.messaging;

import java.util.Arrays;

/**
 * Immutable class for storing and comparing SIF Versions.
 * 
 * Note: This class has a natural ordering that is inconsistent with equals.
 * 
 * Note: Note places in the version of a value of 200 or more are reserved.
 * 
 * 1.5r1 < 2.0r1
 * 2.0 != 2.0r1
 * 2.0 < 2.0r1
 * 2.5 > 2.0r1
 * 2.* == 2.0r1
 * 2.2 == 2.2r*
 * 3.* == 3.0.1
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public final class SIFVersion {
    private String major = "";  // The first number in the version. 2 in 2.0r1
    private String minor = "";  // The second number in the version. 0 in 2.0r1
    private String revision = "";  // The last number in the version. 1 in 2.0r1
    
    // So we can calculate the hash (once).
    private volatile int hashCode = 0;
    
   /**
     * Store each component of the version as its own string.
     * 
     * Note:  If you first created then parsed the version it would be mutable.
     * 
     * @param version   The SIF_Version as it appears on the wire (i.e. 2.0r1).
     * @throws NullPointerException if version is null.
     * @throws IllegalArgumentException if version is not of the correct form.
     * @since 3.0
     */ 
    public SIFVersion(String version) {
        // So we validate the supplied version.
        if(null == version) {
            throw new NullPointerException("A String was expected, null was "
                    + "passed.");
        }
        if(!version.matches("\\*|([0-9]+[.]\\*)|([0-9]+[.][0-9]+r\\*)"
                + "|([0-9]+[.][0-9]+[.]\\*)|([0-9]+[.][0-9]+(r[0-9]+)?)"
                + "|([0-9]+[.][0-9]+([.][0-9]+)?)")) {
            throw new IllegalArgumentException(version + " is not in the"
                    + " correct form.");
        }

        int period = version.indexOf(".");
        
        // Major
        if(-1 != period) {
            major = version.substring(0, period);
        }
        else {
            major = version;
            return;
        }
        
        if(0 != "*".compareTo(major)) {
            if(3 > Integer.parseInt(major)) {
                int r = version.indexOf("r", period);

                // Minor
                if(-1 != r) {
                    minor = version.substring(period+1, r);
                }
                else {
                    minor = version.substring(period+1);
                    return;
                }

                // Revision
                if(-1 != r) {
                    revision = version.substring(r+1);
                }
            }
            else {
                String[] parts = version.split("\\.");
                
                // Minor
                if(2 <= parts.length) {
                    minor = parts[1];
                }
                        
                // Revision
                if(3 == parts.length) {
                    revision = parts[2];
                }
            }
        }
    }
    
   /**
     * Store a copy each component of the version.
     * 
     * @param v The SIFVersion to be copied.
     * @throws NullPointerException if v is null.
     * @since 3.0
     */ 
    public SIFVersion(SIFVersion v) {
        // So we validate the supplied version.
        if(null == v) {
            throw new NullPointerException("A SIFVersion was expected, null "
                    + "was passed.");
        }
        
        // So we have all the data.
        major = v.getMajorVersion();
        minor = v.getMinorVersion();
        revision = v.getRevision();
    }    
    
    /**
     * Returns the major version of the current instance.
     * 
     * @return String
     * @since 3.0
     */
    public String getMajorVersion() {
        return major;
    }

    /**
     * Returns the minor version of the current instance.
     * 
     * @return String
     * @since 3.0
     */
    public String getMinorVersion() {
        return minor;
    }

    /**
     * Returns the revision of the current instance.
     * 
     * @return String
     * @since 3.0
     */
    public String getRevision() {
        return revision;
    }   
    
   /**
     * Checks if the supplied SIFVersion matches this one exactly.
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
        if(!(o instanceof SIFVersion)) return false;
        
        // So we can access SIFVersion class member functions.
        SIFVersion v = (SIFVersion)o;
        
        // So we know if the major version does NOT match.
        if(0 != major.compareTo(v.getMajorVersion())) return false;
        
        // So we know if the minor version does NOT match.
        if(0 != minor.compareTo(v.getMinorVersion())) return false;

        // So we know if the revision does NOT match.
        if(0 != revision.compareTo(v.getRevision())) return false;
        
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
        int result = hashCode;
        
        // So we calculate the hash only when necessary.
        if (0 == result) {
            result = 17;
            
            // So we treat each data variable the same.
            String[] dataVariables = {major, minor, revision};
            for (String current : dataVariables) {
                // So empty strings effect the hash.
                if (current.isEmpty()) {
                    current = "200";
                }
                // So that wild cards are allowed and impact the hash.
                else if (0 == "*".compareTo(current)) {
                    current = "201";
                }
                
                result = (31 * result) + Integer.parseInt(current);
            }
        }
        
        return result;
    }
    
   /**
     * Compares two instances of SIFVersion.
     * 
     * Return Values (relative to:  this compared to parameter v):
     * - : <
     * 0 : ==
     * + : >
     * 
     * Note: Supports the concept of equality through wildcards "*."
     * Therefore this does NOT implement nor fulfill the comparable interface. 
     * 
     * @param v SIFVersion to compare to this one.
     * @throws NullPointerException if v is null.
     * @return int See above comments.
     * @since 3.0
     */ 
    public int compareTo_Wild(SIFVersion v) {
        // So we validate the supplied version.
        if(null == v) {
            throw new NullPointerException("A SIFVersion was expected, null "
                    + "was passed.");
        }        
        
        // So we know the results of each places comparison.
        int compared = 0;
        
        // So we know if we have a major wildcard in either object.
        if(0 == "*".compareTo(v.getMajorVersion())) return 0;
        if(0 == "*".compareTo(major)) return 0;
        
        // So we know how the major versions compare.
        compared = major.compareTo(v.getMajorVersion());
        if (0 != compared) return compared;
        
        // So we know if we have a minor wildcard in either object.
        if(0 == "*".compareTo(v.getMinorVersion())) return 0;
        if(0 == "*".compareTo(minor)) return 0;
        
        // So we know how the minor versions compare.
        compared = minor.compareTo(v.getMinorVersion());
        if (0 != compared) return compared;        
        
        // So we know if we have a revision wildcard in either object.
        if(0 == "*".compareTo(v.getRevision())) return 0;
        if(0 == "*".compareTo(revision)) return 0;
        
        // So we know how the minor revisions compare.
        compared = revision.compareTo(v.getRevision());
        if (0 != compared) return compared;        
       
        // Since we did NOT find an non-equal parts, they must be identical.
        return 0;
    }
    
   /**
     * Compares the passed in version to this one using the supplied opcode.
     * 
     * v s this
     * 
     * @param s  Opcode string ("==", ">", "<", "!=", ">=", "<=").
     * @param v  Left hand SIFVersion.
     * @throws NullPointerException if s is null.
     * @throws IllegalArgumentException if s is not a supported opcode.
     * @throws NullPointerException if v is null.
     * @return If the above expresion holds True or False.
     * @see compareTo_Wild
     * @since 3.0
     */ 
    public boolean compareBySymbol(String s, SIFVersion v) {
        // So we only support the symbols we expect.
        if(null == s) {
            throw new NullPointerException("A String was expected, null was"
                    + " passed.");
        }   
        String[] allowedSymbols = {"==", ">", "<", "!=", ">=", "<="};
        if (!Arrays.asList(allowedSymbols).contains(s)) {
            throw new IllegalArgumentException(s + " is not a supported"
                    + " opcode.");
        }
        
        // So we validate the supplied version.
        if(null == v) {
            throw new NullPointerException("A SIFVersion was expected, null "
                    + "was passed.");
        } 
        
        int compared = compareTo_Wild(v);
        
        // So we know when the statement (v s this) is true (or false).
        if (s.contains("!")) {
            if (0 != compared) {
                return true;
            }
            else {
                return false;
            }
        }
        if (s.contains("=") && 0 == compared) {
            return true;
        }
        if (s.contains(">") && 0 < compared) {
            return true;
        }
        if (s.contains("<") && 0 > compared) {
            return true;
        }
        
        // So we return false when we do not find truth.
        return false;
    }
    
   /**
     * Construct the string representation of the version in the form: 1.2r3
     * 1 = major
     * 2 = minor
     * 3 = revision
     * If one of these variables is blank any proceeding delimiter is omitted
     * 
     * @return String
     * @since 3.0
     */    
    @Override public String toString() {
        String result = "";
        
        // So we include any specified major version number or wildcard.
        if (major.isEmpty()) return result;
        result = result + major;
        
        // So we include any specified minor version number or wildcard.
        if (minor.isEmpty()) return result;
        result = result + "." + minor;
        
        // So we include an specified revision number or wildcard.
        if (revision.isEmpty()) return result;
        if(0 != "*".compareTo(major) && 3 > Integer.parseInt(major)) {
            result = result + "r" + revision;
        }
        else {
            result = result + "." + revision;
        }
        
        
        return result;
    }
}
