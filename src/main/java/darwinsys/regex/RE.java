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
 * \w	characters in a word (\w+ for a word)
 * \d	numeric digits (\d+ for a number)
 * \f	numeric digits plus . e f (\f+ for floating number)
 * <P>
 * NOT FINISHED. TODO:
 * Get doMatch() working 100% for all types.
 * esc() must return an SE so it can do \w \s \d as well as \n \t \b etc.
 * Pass array of SEs into amatch() for use of Closures, write SErep.amatch().
 * NO NO NO -- put the iterations in doMatch, not ses amatch!
 * <P>
 * TODO: More Functionality:
 * Write versions that return Match(), not just boolean version.
 * Use that to write sub() functionality.
 * Add Perl-style RE components \w \d etc.
 * Add alternation and grouping () |
 *
 * @author Ian F. Darwin, ian@darwinsys.com.
 * Based in part on a version I wrote years ago in C as part of a
 * text editor implementation, in turn based on Kernighan & Plaughers
 * <I>Software Tools In Pascal</I> implementation.
 *
 * @version $Id$
 */

//+
public class RE {

	public static final char CLOSURE_ANY = *;
	public static final char CLOSURE_ZERO_OR_ONE = ?;
	public static final char CLOSURE_ONE_OR_MORE = +;
	public static final char CLOSURE_NUMERIC = {;	// XXX
	public static final char CLOSURE_NUMERIC_END = };	// XXX

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
	public static final char DIGIT = d;		// XXX Perlish
	public static final char SPACE = s;		// XXX Perlish
	public static final char TAB = t;			// XXX Perlish
	public static final char UNICHAR = u;		// Unicode char, like Java
	public static final char WORDCHAR = w;	// XXX Perlish

	protected final int ERR = -1;

	protected SE[] myPat;
	protected String origPatt;

	// Since these next five REs are constant, we can use the "flyweight"
	// Design Pattern for them - we only need one instance of each, ever.
	// This does NOT extend to multipliers like * + ?

	/** The "flyweight" SE for \w */
	protected static SE myWord = new SEccl("[a-zA-Z_0-9]", new Int(0));
	/** The "flyweight" SE for \d */
	protected static SE myDigits = new SEccl("[0-9]", new Int(0));
	/** The "flyweight" for "." */
	protected static SE myANY = new SEany();
	/** The "flyweight" for "^" */
	protected static SE myBOL = new SEbol();
	/** The "flyweight" for "$" */
	protected static SE myEOL = new SEeol();

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
	 	return null;			// XXX
	}

	/** Match all occurrences of the compiled pattern stored in the 
	 * RE against the given String.
	 * @return Array of Match objects, each indicating the position & 
	 * length of the match. Array is length 0 if no matches.
	 */
	public Match[] matches(String str){ return matches(str, false); }

	/** Match all occurrences,
	 * with control over case sensitivity.
	 */
	public Match[] matches(String str, boolean ignoreCase) {
	 	return null;			// XXX
	}
 
	///////////////////////////////////////////////////////////////
	// END OF PUBLIC PART OF API -- remainder omitted in some listings.
	///////////////////////////////////////////////////////////////
	//-

	/** Return a string representation of the RE */
	public String toString() {
		if (myPat == null || myPat.length == 0)
			return "RE[null]";
		StringBuffer res = new StringBuffer("RE[" + origPatt + "-->");
		for (int i=0; i<myPat.length; i++) {
			if (i>0)
				res.append(,);
			res.append(myPat[i]);
		}
		res.append(]);
		return res.toString();
	}

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
				v.addElement(new myBOL);
			// "$" only special at end.
			} else if (c == EOL && i.get() == arg.length()-1) {
				v.addElement(myEOL);
			} else if (c == CCL) {
				v.addElement(new SEccl(arg, i));
			// closures (*,+,?,{,} not special unless they follow something.
			// replace element it follows with CLOSURE referring to it
			} else if (i.get() > 0 && c == CLOSURE_ANY) {	// * = {0,}
				int last = v.size()-1;
				v.setElementAt(new SErep(0, SErep.NOMAX, (SE)v.elementAt(last)), last);
			} else if (i.get() > 0 && c == CLOSURE_ONE_OR_MORE) { // + -> {1,}
				int last = v.size()-1;
				v.setElementAt(new SErep(1, SErep.NOMAX, (SE)v.elementAt(last)), last);
			} else if (i.get() > 0 && c == CLOSURE_ZERO_OR_ONE) { // ? -> {0,1}
				int last = v.size()-1;
				v.setElementAt(new SErep(0, 1, (SE)v.elementAt(last)), last);
			} else {
				// "Ordinary" character.
				v.addElement(new SEchar(esc(arg, i)));
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
			System.out.println("doMatch: il="+il);
			failed = false;
			for (int ip=0; ip<patt.length; ip++) {
				System.out.println("doMatch: ip="+ip);
				if (!patt[ip].amatch(line, i)) {
					failed = true;
					break;		// out of inner loop
				}
			}
			// If matched all elements of patt, we have ignition!
			if (!failed)
				return true;
		}
		return !failed;
	}

	/* esc - handle C-like escapes
	 * if a[i] is \, following char may be special.
	 * updates i if so.
	 * in any case, returns the character.
	 */
	protected static char esc(String a, Int i) {
		if (a.charAt(i.get()) != \\)
			return a.charAt(i.get());
		if (i.get() >= a.length())
			return \\;	/* not special at end */
		i.incr();
		if (a.charAt(i.get()) == n)
			return \n;
		if (a.charAt(i.get()) == t)
			return \t;
		return a.charAt(i.get());
	}

//+
}
//-
