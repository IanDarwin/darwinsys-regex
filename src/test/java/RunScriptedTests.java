package com.darwinsys.regex_regress;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.darwinsys.regex.RE;
import com.darwinsys.regex.RESyntaxException;


/*
 * JUnit test runner for running scripted tests for com.darwinsys.regex.
 * Each RE is compiled and printed, .match()ed against the string, and
 * the result is applied to output using regsub().
 *Each of the input files named in "fileNames" must contain of lines with
 * five fields:  a r.e., a string to match it against, a result code, a
 * source string for regsub, and the proper result.  Result codes are 'c'
 * for compile failure, 'y' for match success, 'n' for match failure.
 * Field separator is tab.
 * @author Ian Darwin, http://www.darwinsys.com/ - this program
 * @author Henry Spencer, henry@zoo.toronto.edu - test file of 127 RE tests
 * @version $Id$
 */
public class RunScriptedTests extends TestSuite {

	private String[] fileNames = { "tests", "mytests" };
	
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite("Test for regress");
		//$JUnit-BEGIN$
		suite.addTest(new RunScriptedTests());
		//$JUnit-END$
		return suite;
	}
	
	private List<TestHolder> tests = new ArrayList<TestHolder>();
	
	class TestHolder implements Test {
		String patt;
		String input;
		String expect;	// Should make an enum?

		public int countTestCases() {
			return 1;
		}

		public void run(TestResult arg0) {
			throw new IllegalStateException("called run on a TestHolder");
		}

		public TestHolder(String expect, String input, String patt) {
			super();
			this.expect = expect;
			this.input = input;
			this.patt = patt;
		}	
		@Override
		public String toString() {
			return String.format("TestHolder: Patt %s, Input %s, expect %s%n", patt, input, expect);
		}
	}
	
	
	/**
	 * Constructor, read the test Files so countTestCases will work.
	 */
	public RunScriptedTests() throws Exception {
		for (String fileName : fileNames) {
			readTests(fileName);
		}
	}
	
	private void readTests(String fileName) throws Exception {		
		BufferedReader is = new BufferedReader(new FileReader(fileName));
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
			tests.add(new TestHolder(st.nextToken(), st.nextToken(), st.nextToken()));
        }
        is.close();
	}
	
	@Override
	public int countTestCases() {
		return tests.size();
	}
	
	/* Run all the tests.
	 * @see junit.framework.Test#run(junit.framework.TestResult)
	 */
	@Override
	public void run(TestResult results) {
		for (TestHolder oneTest : tests) {
			try {
				results.startTest(oneTest);
				one_test(oneTest);
			} catch (AssertionFailedError e) {
				System.out.println("Caught " + e);
				results.addFailure(oneTest, e);
			} catch (RuntimeException e) {
				System.out.println("Caught " + e);
				results.addFailure(oneTest, new AssertionFailedError(e.toString()));
			} catch (Throwable t) {
				System.out.println("Caught " + t);
				results.addError(oneTest, t);
			} finally {
				results.endTest(oneTest);
			}
		}
	}

	protected static String passedMessage = "Success";
	protected static String failedMessage = "FAILURE";

	public static void one_test(TestHolder test) throws Exception {
		System.out.print("TEST " + test.patt + "; " +
			test.input + "; expect " + test.expect + ' ');
		char expect = test.expect.charAt(0);
		RE re;
		try {
			re = new RE(test.patt);
		} catch (RESyntaxException e) {
			boolean success = (expect == 'c');
			// If we expected success but got failure, throw.
			if (!success) {				
				throw(e);
			}
			return;
		}
		System.out.println("PATTERN WAS " + re);
		boolean matched = re.match(test.input);
		
		if (matched == (expect == 'y')) {
			System.out.println(passedMessage);
		} else {
			throw new RuntimeException("FAILED: " + test);
		}

	}

}
