import java.util.*;

/**
 * A small regular expressions package for Java.
 * <P>
 * This is a pretty simple implementation, compiling the pattern
 * into an array of sub-expression (SE) objects, and interpreting it. 
 * It is more intended to be pedantic than efficient: as Henry Spencer
 * said of his (much more sophisticated) C-language regexp,
 * replacing the innards of egrep with this code would be a mistake.
 * <P>
 * The subset of regular expression characters 
 * that this API accepts is as follows. Consult the OReilly book 
 * <I>Mastering Regular Expressions</I> or any book on UNIX
 * user commands, if you are not familiar with regular expressions.
 * <PRE>
 * ^	begin of line/string
 * $	end of line/string
 * .	any one character
 * [...]	any one character from those listed
 * [^...]	any one character not from those listed
 * {m,n}	multiplier for from m to n repetitions
 * {m,} 	multiplier for from m repetitions on up
 * {,n}		multiplier for zero up to n repetitions
 * *	multiplier for zero or more repetitions (short for {0,})
 * +	multiplier for one or more repetitions (short for {1,})
 * ?	multiplier for zero or one repetitions (short for {0,1})
 * \\t	Tab character
 * \\r	return character (ASCII CR, Mac newline)
 * \\n	newline character (ASCII LF, Unix newline)
 * \\w	character in a word (\w+ for a word)
 * \\d	numeric digit (\d+ for a number)
 * \\s	white space
 * </PRE>
 *
 * @see	bugs.html - the bug list and TODO file.
 * @author Ian F. Darwin, ian@darwinsys.com.
 * Based in part on a version I wrote years ago in C as part of a
 * text editor implementation, in turn based on Kernighan & Plaughers
 * <I>Software Tools In Pascal</I> implementation.
 *
 * @version $Id$
 */

//+
public class RE {
//-

	public static final char MULT_ANY = *;
	public static final char MULT_ZERO_OR_ONE = ?;
	public static final char MULT_ONE_OR_MORE = +;
	public static final char MULT_NUMERIC = {;
	public static final char MULT_NUMERIC_SEP = ,;
	public static final char MULT_NUMERIC_END = };

	public static final char EOL = $;
	public static final char BOL = ^;
	public static final char ANY = .;
	public static final char CCL = [;
	public static final char CCLEND = ];
	public static final char OR = |;			// XXX Extended RE
	public static final char GRP = (;			// XXX Extended RE
	public static final char GRPEND = );		// XXX Extended RE
	public static final char LITCHAR = \\;	// Escape.
	// These are for use in CCLs
	public static final char NEGCCL = ^;
	public static final char DASH = -;
	// These must be preceded by LITCHAR to enable them
	public static final char DIGIT = d;
	public static final char SPACE = s;
	public static final char TAB = t;	
	public static final char UNICHAR = u;		// Unicode char, like Java
	public static final char WORDCHAR = w;

	protected final int ERR = -1;

	protected SE[] myPat; 
	protected String origPatt;

	//+
	/** Construct an RE object, given a pattern.
	 * @throws RESyntaxException if bad syntax.
	 */
	public RE(String patt) throws RESyntaxException {
		origPatt = patt;
		myPat = compile(patt);
	}

	protected static RE singleton;

	/** Match a pattern in a given string. Designed for light-duty
	 * use, as it compiles the pattern each time.
	 * For multiples uses of the same pattern, construct an RE object,
	 * which stores the pattern inside it.
	 */
	public static Match match(String patt, String str){
		if (singleton == null)
			singleton = new RE("");
		SE[] thispat = singleton.compile(patt);
		//return singleton.match(thispat, str);
		return null;
	}

	/** Match the compiled pattern stored in the RE in a given String.
	 * @return True if the RE matches anyplace in the String.
	 */
	public boolean isMatch(String str){
		return doMatch(myPat, str, false);
	}

	/** Match the compiled pattern stored in the RE in a given String,
	 * with control over case sensitivity.
	 * @input str - string to match RE in
	 * param ignoreCase -- true if case is to be ignored.
	 * @return True if the RE matches anyplace in the String.
	 */
	public boolean isMatch(String str, boolean ignoreCase){
		return doMatch(myPat, str, ignoreCase);
	}

	/** Match the compiled pattern stored in the RE in a given String.
	 * @return A Match object indicating the position & length of the match.
	 * Null if no match.
	 */
	public Match match(String str){ return match(str, false); }

	/** Match the compiled pattern in this RE against a given string,
	 * with control over case sensitivity.
	 */
	public Match match(String str, boolean ignoreCase) {
	 	return null;			// XXX Not Implemented Yet
	}

	///////////////////////////////////////////////////////////////
	// END OF PUBLIC PART OF API -- remainder omitted in some listings.
	///////////////////////////////////////////////////////////////
	//-

	/** Return a string representation of the RE */
	public String toString() {
		if (myPat == null || myPat.length == 0)
			return "RE[null]";
		StringBuffer res = new StringBuffer("RE[");
		for (int i=0; i<myPat.length; i++) {
			if (i>0)
				res.append(,);
			res.append(myPat[i]);
		}
		res.append(]);
		return res.toString();
	}

	// Since these next few REs are constant, we can use the "flyweight"
	// Design Pattern for them - we only need one instance of each, ever.
	// This can NOT, of course, extend to multipliers like * + ?
	// Some of them are subclassed right here, since we dont need
	// to make a named class for something as simple as begin-of-line.

	/** The "flyweight" SE for \w */
	protected static SE myWordChars = new SE() {
		public boolean amatch(String a, Int i) {
			boolean match = Character.isLetterOrDigit(a.charAt(i.get()));
			if (match) i.incr();
			return match;
		}
		public String toString() {
			return "\\w";
		}
	};
	/** The "flyweight" SE for \W */
	protected static SE myNOTWordChars = new SE() {
		public boolean amatch(String a, Int i) {
			boolean match = !Character.isLetterOrDigit(a.charAt(i.get()));
			if (match) i.incr();
			return match;
		}
		public String toString() {
			return "\\W";
		}
	};
	/** The "flyweight" SE for \d */
	protected static SE myDigits = new SE() {
		public boolean amatch(String a, Int i) {
			boolean match = Character.isDigit(a.charAt(i.get()));
			if (match) i.incr();
			return match;
		}
		public String toString() {
			return "\\d";
		}
	};
	/** The "flyweight" SE for \D */
	protected static SE myNOTDigits = new SE() {
		public boolean amatch(String a, Int i) {
			boolean match = !Character.isDigit(a.charAt(i.get()));
			if (match) i.incr();
			return match;
		}
		public String toString() {
			return "\\D";
		}
	};
	/** The "flyweight" SE for \f */
	protected static SE myFloatChars = new SEccl("[0-9ef.]", new Int(0));
	/** The "flyweight" SE for \F */
	protected static SE myNOTFloatChars = new SEccl("[^0-9ef.]", new Int(0));
	/** The "flyweight" SE for \s */
	protected static SE mySpaces = new SE() {
		public boolean amatch(String a, Int i) {
			boolean match = Character.isWhitespace(a.charAt(i.get()));
			if (match) i.incr();
			return match;
		}
		public String toString() {
			return "\\s";
		}
	};
	/** The "flyweight" SE for \S */
	protected static SE myNOTSpaces = new SE() {
		public boolean amatch(String a, Int i) {
			boolean match = !Character.isWhitespace(a.charAt(i.get()));
			if (match) i.incr();
			return match;
		}
		public String toString() {
			return "\\S";
		}
	};
	/** The "flyweight" for "." */
	protected static SE myAny = new SE() {
		public String toString() { return "SE(.)"; }
		public boolean amatch(String ln, Int i) {
			if (ln.length() >= i.get())
				return false;					// end of string
			i.incr();
			return true;
		}
	};
	/** The "flyweight" for "^" */
	protected static SE myBOL = new SE() {
		public String toString() { return "BOL"; }
		public boolean amatch(String ln, Int i) {
			if (i.get()>0)
				return false;
			// no i.incr() since we dont use any chars in ln
			return true;
		}
	};
	/** The "flyweight" for "$" */
	protected static SE myEOL = new SE() {
		public String toString() { return "EOL"; }
		public boolean amatch(String ln, Int i) {
			if (i.get() == ln.length()) {
				// no i.incr() since we dont use any chars in ln
				return true;
			}
			return false;
		}
	};

	/* compile -- make pattern from arg.
	 * @return compiled pattern in an array of SE
	 * @throws RESyntaxException if bad syntax.
	 */
	protected SE[] compile(String arg) throws RESyntaxException {
		Int i = new Int();	// arg index
		int j = 0,	// patt "index"
			lastj = 0, lj = 0;
		boolean done = false;
		Vector v = new Vector();

		while (!done && i.get()<arg.length()) {
			char c = arg.charAt(i.get());
			lj = j;
			if (c == ANY) {
				v.addElement(myAny);
			// "^" only special at beginning.
			} else if (c == BOL && i.get() == 0) {
				v.addElement(myBOL);
			// "$" only special at end.
			} else if (c == EOL && i.get() == arg.length()-1) {
				v.addElement(myEOL);
			} else if (c == CCL) {
				v.addElement(new SEccl(arg, i));
			// multipliers (*,+,?,{,} not special unless they follow something.
			// replace element it follows with MULT referring to it
			} else if (i.get() > 0 && c == MULT_ANY) {	// * = {0,}
				int last = v.size()-1;
				v.setElementAt(new SEmult(0, SEmult.NOMAX, (SE)v.elementAt(last)), last);
			} else if (i.get() > 0 && c == MULT_ONE_OR_MORE) { // + -> {1,}
				int last = v.size()-1;
				v.setElementAt(new SEmult(1, SEmult.NOMAX, (SE)v.elementAt(last)), last);
			} else if (i.get() > 0 && c == MULT_ZERO_OR_ONE) { // ? -> {0,1}
				int last = v.size()-1;
				v.setElementAt(new SEmult(0, 1, (SE)v.elementAt(last)), last);
			// {m,n} multiplier
			} else if (i.get() > 0 && c == MULT_NUMERIC) {
				i.incr();		// skip MULT_NUMERIC
				int ii = i.get();
				int mid = arg.indexOf(MULT_NUMERIC_SEP, ii);
				int end = arg.indexOf(MULT_NUMERIC_END, ii);
				if (mid == -1)
					throw new RESyntaxException(MULT_NUMERIC + " needs comma");
				if (end == -1)
					throw new RESyntaxException("unclosed " + MULT_NUMERIC);
				i.set(end);
				String first = arg.substring(ii, mid);
				String second = arg.substring(mid+1, end);
				int min = 0;
				if (first.length() > 0)
					min = Integer.parseInt(first);
				int max = 0;
				if (second.length() > 0)
					max = Integer.parseInt(second);
				Debug.println("SEmult", "Compile: " + min + "," + max);
				// Now replace the target with a MULT referring to it.
				int last = v.size()-1;
				SE mult = new SEmult(min, max, (SE)v.elementAt(last));
				v.setElementAt(mult, last);
			} else {	 
				// ALL ELSE IS AN "Ordinary" character, or one of 
				// the special \-escape characters.
				v.addElement(esc(arg, i));
			}
			lastj = lj;
			if (!done) i.incr();
		} 
		if (done) {				/* finished early */
			throw new RESyntaxException("incomplete pattern");
		} 
		SE[] a = new SE[v.size()];
		v.toArray(a);
		return a;
	}

	/* doMatch -- find pattern match anywhere on line.
	 * The sacred heart of the matching engine.
	 */
	protected boolean doMatch(SE[] patt, String line, boolean ignoreCase) {
		Int	i = new Int();
		int il;
		boolean failed = false;

		// Try patt starting at each char position in line.
		// i gets incr()d by each amatch() to skip over what it looks at.
		for (il=0, i.set(il); il<line.length(); il++, i.set(il)) {
			Debug.println("doMatch", "doMatch: il="+il);
			failed = false;
			for (int ip=0; ip<patt.length; ip++) {
				Debug.println("doMatch", "doMatch: ip="+ip);
				if (!patt[ip].amatch(line, i)) {
					failed = true;
					break;		// out of inner loop
				}
			}
			// If matched all elements of patt, we have ignition!
			if (!failed) {
				Debug.println("doMatch", "doMatch() all patt at " +il+ "; return true");
				return true;
			}
		}
		Debug.println("doMatch", "doMatch: Got to end so return " + !failed);
		return !failed;
	}

	/* esc - handle C-like escapes
	 * if a[i] is \, following char may be special.
	 * updates i if so.
	 * in any case, returns the character.
	 */
	protected static SE esc(String a, Int i) {
		char c = a.charAt(i.get());
		if (c != \\)					// non-escaped character.
			return new SEchar(c);
		if (i.get() >= a.length())
			return new SEchar(\\);	/* not special at end */
		i.incr();
		c = a.charAt(i.get());
		switch (c) {
		case DIGIT:
			return myDigits;
		case D:
			return myNOTDigits;
		case f:
			return myFloatChars;
		case F:
			return myNOTFloatChars;
		case n:
			return new SEchar(\n);
		case r:
			return new SEchar(\r);
		case SPACE:
			return mySpaces;
		case S:
			return myNOTSpaces;
		case TAB:
			return new SEchar(\t);
		case u:
			// assume followed by 4-digit hex string for Unicode character.
			String hex = a.substring(i.get()+1, i.get()+5);
			i.incr(4);
			return new SEchar((char)Integer.parseInt(hex, 16)); 
		case WORDCHAR:
			return myWordChars;
		case W:
			return myNOTWordChars;
		case LITCHAR:
			return new SEchar(LITCHAR);
		default:
			return new SEchar(c);
		}
	}

//+
}
//-
