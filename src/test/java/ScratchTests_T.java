import junit.framework.TestCase;

import org.apache.commons.math3.special.Gamma;
import org.junit.Test;


public class ScratchTests_T extends TestCase {

	/**
	 * @param args
	 */
	@Test
	public static void testGammaFunction() {
		
		System.out.println(Gamma.digamma(1.2));
		
	}
	
	@Test
	public static void testStringSplit() {
		String line = "";
		String [] s = line.split("\\s+");
		System.out.println("Length of result: " + s.length);
		System.out.println(s[0].equals(""));
	}
	
	@Test 
	public static void testPrimitiveArrays() {
		String [] s = new String[0];
		System.out.println(s.length);
	}
	
	
}
