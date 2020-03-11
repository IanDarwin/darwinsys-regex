import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import darwinsys.regex.RE;
import darwinsys.regex.RESyntaxException;

/*
 * JUnit test runner for running scripted tests for darwinsys.regex.
 * Each RE is compiled and printed, .match()ed against the string, and
 * the result is applied to output using regsub().
 * Each of the input files named in "fileNames" must consist of lines with
 * five fields:  a r.e., a string to match it against, a result code, a
 * source string for regsub, and the proper result.  Result codes are 'c'
 * for compile failure, 'y' for match success, 'n' for match failure.
 * Field separator is tab.
 * @author Ian Darwin, http://www.darwinsys.com/ - this program
 * @author Henry Spencer, henry@zoo.toronto.edu - test file of 127 RE tests
 */
@RunWith(Parameterized.class)
public class RunScriptedTest {

	private static String[] fileNames = { "tests.txt", "mytests.txt" };
	private TestHolder testHolder;
	
	enum Expected {
		COMPILE_FAIL('c'),
		MATCH_SUCCESS('y'),
		MATCH_FAILURE('n');

		private char c;

		Expected(char c) {
			this.c = c;
		}
		
		public Expected valueOf(char c) {
			for (Expected e : values()) {
				if (e.c == c) {
					return e;
				}
			}
			throw new IllegalArgumentException(c + " not a valid Expected value");
		}
	}

	static class TestHolder {
		String patt;
		String input;
		String expect;  // XXX use enum

		public TestHolder(String patt, String input, String expect) {
			super();
			this.patt = patt;
			this.input = input;
			this.expect = expect;
		}       

		@Override
		public String toString() {
			return String.format("Test[Patt %s, Input %s, expect %s]", patt, input, expect);
		}
	}

	/**
	 * Static block, read the test Files so countTestCases will work.
	 */
	static {
		System.out.println("Static initializer");
		tests = new ArrayList<TestHolder>();
		try {
			for (String fileName : fileNames) {
				readTests(fileName);
			}
		} catch (IOException ex) {
			fail(ex.toString());
		}
	}

	private static List<TestHolder> tests;

	@Parameters(name="{0}")
	public static TestHolder[] getTests() {
		System.out.printf("getTests: n = %d\n", tests.size());
		return tests.toArray(new TestHolder[tests.size()]);
	}

	public RunScriptedTest(TestHolder the) {
		this.testHolder = the;
	}

	private static void readTests(String fileName) throws IOException {		
		BufferedReader is = new BufferedReader(
				new InputStreamReader(RunScriptedTest.class.getResourceAsStream("/" + fileName)));
        String inputLine;

        while ((inputLine = is.readLine()) != null) {
            if (inputLine.length() == 0 ||
				inputLine.startsWith("#"))
					continue;
			StringTokenizer st = new StringTokenizer(inputLine, "\t");
			if (st.countTokens() < 3) {
				System.err.println("INVALID INPUT: " + inputLine);
				continue;
			}
			String patt = st.nextToken();
			String data = st.nextToken();
			String expStr = st.nextToken();
			tests.add(new TestHolder(patt, data, expStr));
        }
        is.close();
	}
	
	protected static final String passedMessage = "Success";
	protected static final String failedMessage = "FAILURE";

	/**
	 * Actually run the Test!
	 */
	@Test
	public void one_test() throws Exception {
		System.out.print(testHolder);

		// First we check the pattern's compilation
		char expect = testHolder.expect.charAt(0);
		RE re;
		try {
			re = new RE(testHolder.patt);
		} catch (RESyntaxException resx) {
			boolean success = (expect == 'c');
			// If we expected success but got failure, throw.
			if (!success) {				
				fail("Unexpected regex compilation failure: " + resx);
			}
			return;
		}
		if (expect == 'c' /* && we are still here */) {
			fail("Pattern compiled, but expected it to fail(c)");
		}

		// Now test the match itself
		boolean matched = re.match(testHolder.input);
		
		if (matched == (expect == 'y')) {
			System.out.println(passedMessage);
		} else {
			fail(failedMessage + ": " + testHolder);
		}

	}
}
