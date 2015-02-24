package org.sifassociation.messaging;

import org.sifassociation.messaging.SIFVersion;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author jlovell
 */
public class SIFVersionTest {
    
    /**
     * Since SIFVersion is an immutable class, instances need only a single
     * initialization.
     */
    private SIFVersion star = new SIFVersion("*");
    private SIFVersion oneDotStar = new SIFVersion("1.*");
    private SIFVersion twoDotZeroROne = new SIFVersion("2.0r1");
    private SIFVersion twoDotStar = new SIFVersion("2.*");
    private SIFVersion twoDotTwo = new SIFVersion("2.2");
    private SIFVersion threeDotStar = new SIFVersion("3.*");
    private SIFVersion threeDotZeroDotOne = new SIFVersion("3.0.1");
    
    public SIFVersionTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getMajorVersion method, of class SIFVersion.
     */
    @Test
    public void testGetMajorVersion() {
        System.out.println("getMajorVersion");
        assertEquals("2", twoDotZeroROne.getMajorVersion());
    }

    /**
     * Test of getMinorVersion method, of class SIFVersion.
     */
    @Test
    public void testGetMinorVersion() {
        System.out.println("getMinorVersion");
        assertEquals("0", twoDotZeroROne.getMinorVersion());
    }

    /**
     * Test of getRevision method, of class SIFVersion.
     */
    @Test
    public void testGetRevision() {
        System.out.println("getRevision");
        assertEquals("1", twoDotZeroROne.getRevision());
    }

    /**
     * Test of equals method, of class SIFVersion.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");

        // So we have something that is equal to something else.
        System.out.println("(copy)");
        SIFVersion original = twoDotZeroROne;
        SIFVersion firstCopy = new SIFVersion(twoDotZeroROne);
        SIFVersion secondCopy = new SIFVersion(twoDotZeroROne);
        SIFVersion different = twoDotStar;
        
        // So we have a sense of what chunks of memory we are comparing.
        assertTrue(original == twoDotZeroROne);
        assertTrue(original != firstCopy);
        assertTrue(original != secondCopy);
        assertTrue(firstCopy != secondCopy);
        assertTrue(original != different);

        System.out.println("(symmetry: if A = B then B = A)");
        // So symmetry holds when things are equal.
        assertTrue(original.equals(firstCopy)
                == firstCopy.equals(original));
        // So symmetry holds when things are different.
        assertTrue(different.equals(firstCopy)
                == firstCopy.equals(different));

        System.out.println("(transitivity: if A = B and B = C then A = C)");
        // So transitivity holds when things are equal.
        assertTrue(original.equals(firstCopy)
                && firstCopy.equals(secondCopy)
                && original.equals(secondCopy));
        // Transitivity does not necessarily hold when things are different!

        // To Do: A consistency test should go here.

        // Ensure non-nullity of equality.
        System.out.println("(non-nullity: A != null) ");
        assertFalse(original.equals(null));        
    }

    /**
     * Test of hashCode method, of class SIFVersion.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        
        // So we have something that is equal to something else.
        System.out.println("(copy)");
        SIFVersion original = twoDotZeroROne;
        SIFVersion firstCopy = new SIFVersion(twoDotZeroROne);
        SIFVersion different = twoDotTwo;
        
        // So we have a sense of what chunks of memory we are comparing.
        assertTrue(original == twoDotZeroROne);
        assertTrue(original != firstCopy);
        assertTrue(original != different);        
        
        // So we know similar things hash the same and diffrent things hash 
        // differently.
        System.out.println("(compare)");
        assertTrue(original.hashCode() == firstCopy.hashCode());
        assertTrue(original.hashCode() != different.hashCode());
        assertTrue(firstCopy.hashCode() != different.hashCode());
    }

    /**
     * Test of compareTo_Wild method, of class SIFVersion.
     */
    @Test
    public void testCompareTo_Wild() {
        System.out.println("compareTo_Wild");

        System.out.println("(if A > B then B < A)");
        // So we check this when things are NOT equal.
        assertTrue(Integer.signum(twoDotTwo.compareTo_Wild(twoDotZeroROne)) 
                == ( -1 * 
                Integer.signum(twoDotZeroROne.compareTo_Wild(twoDotTwo))));
        // So we check this when things are equal.
        assertTrue(Integer.signum(twoDotStar.compareTo_Wild(star)) 
                == ( -1 * Integer.signum(star.compareTo_Wild(twoDotStar))));
        
        System.out.println("(transitivity: if A > B and B > C then A > C)");
        // So we check this as defined by contract.
        assertTrue(0 < threeDotStar.compareTo_Wild(twoDotStar)
                && 0 < twoDotStar.compareTo_Wild(oneDotStar) 
                && 0 < threeDotStar.compareTo_Wild(oneDotStar));
        // Note: Inconsistent with equals.
        
        /**
         * This is the part of the compareTo contract that is violated!
         
        System.out.println("(if A == B then A compare C yeilds the same sign "
                + "as B compare C)");
        // So we challenge this by including wildcards and a lesser version.
        assertTrue(0 == star.compareTo_Wild(twoDotTwo) 
                && Integer.signum(star.compareTo_Wild(oneDotStar)) 
                == Integer.signum(twoDotTwo.compareTo_Wild(oneDotStar)));
         */
        
        // (x.compareTo(y) == 0) == (x.equals(y)) does not always hold.

        System.out.println("(equals)");
        assertTrue(0 != oneDotStar.compareTo_Wild(twoDotStar));
        assertTrue(0 != twoDotTwo.compareTo_Wild(twoDotZeroROne));
        assertTrue(0 == twoDotStar.compareTo_Wild(twoDotZeroROne));
        assertTrue(0 == twoDotTwo.compareTo_Wild(twoDotStar));
        assertTrue(0 == threeDotZeroDotOne.compareTo_Wild(threeDotStar));
        
        System.out.println("(greater than)");
        assertTrue(0 < twoDotStar.compareTo_Wild(oneDotStar));
        assertTrue(0 < twoDotTwo.compareTo_Wild(twoDotZeroROne));
        
        System.out.println("(less than)");
        assertTrue(0 > oneDotStar.compareTo_Wild(twoDotStar));
        assertTrue(0 > twoDotZeroROne.compareTo_Wild(twoDotTwo));
    }

    /**
     * Test of compareBySymbol method, of class SIFVersion.
     */
    @Test
    public void testCompareBySymbol() {
        System.out.println("compareTo_Wild");
        
        System.out.println("(==)");
        assertTrue(twoDotZeroROne.compareBySymbol("==", twoDotStar));
        assertFalse(twoDotZeroROne.compareBySymbol("==", twoDotTwo));
        
        System.out.println("(>)");
        assertFalse(twoDotZeroROne.compareBySymbol(">", twoDotStar));
        assertFalse(twoDotZeroROne.compareBySymbol(">", twoDotTwo));
        
        System.out.println("(<)");
        assertFalse(twoDotZeroROne.compareBySymbol("<", twoDotStar));
        assertTrue(twoDotZeroROne.compareBySymbol("<", twoDotTwo));
        
        System.out.println("(!=)");
        assertFalse(twoDotZeroROne.compareBySymbol("!=", twoDotStar));
        assertTrue(twoDotZeroROne.compareBySymbol("!=", twoDotTwo));
        
        System.out.println("(>=)");
        assertTrue(twoDotZeroROne.compareBySymbol(">=", twoDotStar));
        assertFalse(twoDotZeroROne.compareBySymbol(">=", twoDotTwo));
        
        System.out.println("(<=)");
        assertTrue(twoDotZeroROne.compareBySymbol("<=", twoDotStar));
        assertTrue(twoDotZeroROne.compareBySymbol("<=", twoDotTwo));
    }    
    
    /**
     * Test of toString method, of class SIFVersion.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        
        System.out.println(star.toString());
        assertEquals("*", star.toString());
        
        System.out.println(oneDotStar.toString());
        assertEquals("1.*", oneDotStar.toString());
        
        System.out.println(twoDotZeroROne.toString());
        assertEquals("2.0r1", twoDotZeroROne.toString());
        
        System.out.println(twoDotStar.toString());
        assertEquals("2.*", twoDotStar.toString());
        
        System.out.println(twoDotTwo.toString());
        assertEquals("2.2", twoDotTwo.toString());
        
        System.out.println(threeDotStar.toString());
        assertEquals("3.*", threeDotStar.toString());
        
        System.out.println(threeDotZeroDotOne.toString());
        assertEquals("3.0.1", threeDotZeroDotOne.toString());
    }
}
