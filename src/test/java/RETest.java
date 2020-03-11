import static org.junit.Assert.*;

import org.junit.Test;
import darwinsys.regex.RE;


/**
 * Not test again, but test RE.
 * No need for exhaustive tests here; this is to test out the
 * different constructors and method invocations.
 * @author Ian F. Darwin, http://www.darwinsys.com/
 */
public class RETest {
	
	@Test
	public void testOne() {
		//+
		RE r = new RE("^A");
		assertTrue(r.match("Apples"));
		assertFalse(r.match("apples"));
		// Quick test of substitution
		RE s = new RE("the *");
		String input = "in the beginning";
		assertEquals("sub(" + input +" ) ", "in MY_beginning", s.sub(input, "MY_"));
		//-
	}
}
