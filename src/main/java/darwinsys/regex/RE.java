import java.util.*;

/**
 * A small regular expressions package for Java.
 *
 * This is a pretty simple implementation, compiling the pattern
 * into an array of SE objects, and interpreting it. 
 * It is more intended to be pedantic than efficient: as Henry Spencer
 * said of his (much more sophisticated) C-language regexp,
 * replacing the innards of egrep with this code would be a mistake.
 *
 * NOT FINISHED. TODO:
 * Get doMatch() working 100% for all types.
 * Pass array of SEs into amatch() for use of Closures, write SEclos.amatch().
 * NO NO NO -- put the iterations in doMatch, not ses amatch!
 *
 * TODO: More Functionality:
 * Write versions that return Match(), not just boolean version.
 * Use that to write sub() functionality.
 * Add Perl-style REs \w \n etc.
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
	public static final char EOL = $;
	public static final char BOL = ^;
	public static final char ANY = .;
	public static final char CCL = [;
	public static final char CCLEND = ];
	public static final char OR = |;			// XXX Extended RE
	public static final char GRP = (;			// XXX Extended RE
	public static final char GRPEND = );		// XXX Extended RE
	public static final char WORD = w;		// XXX Perlish
	public static final char NUMBER = n;		// XXX Perlish
	public static final char LITCHAR = \\;
	public static final char NEGATE = ^;
	public static final char DASH = -;
	final int ERR = -1;

	protected SE[] myPat;
	protected String origPatt;

	/** Construct an RE object, given a pattern.
	 * @throws RESyntaxException if bad syntax.
	 */
	public RE(String patt) throws RESyntaxException {
		origPatt = patt;
		myPat = compile(patt);
	}

	public String toString() {
		StringBuffer res = new StringBuffer("RE[" + origPatt + "-->");
		for (int i=0; i<myPat.length; i++) {
			if (i>0)
				res.append(,);
			res.append(myPat[i]);
		}
		res.append(]);
		return res.toString();
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
		return doMatch(myPat, str);
	}

	/** Match the compiled pattern stored in the RE in a given String.
	 * @return A Match object indicating the position & length of the match.
	 * Null if no match.
	 */
	public Match match(String str){ /*XXX*/ return null; }

	/** Match all occurrences of the compiled pattern stored in the 
	 * RE against the given String.
	 * @return Array of Match objects, each indicating the position & 
	 * length of the match. Array is length 0 if no matches.
	 */
	public Match[] matches(String str){ /*XXX*/ return null; }

	///////////////////////////////////////////////////////////////
	// END OF PUBLIC PART OF API -- remainder omitted in some listings.
	///////////////////////////////////////////////////////////////
	//-

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
				v.addElement(new SEany());
			// "^" only special at beginning.
			} else if (c == BOL && i.get() == 0) {
				v.addElement(new SEbol());
			// "$" only special at end.
			} else if (c == EOL && i.get() == arg.length()-1) {
				v.addElement(new SEeol());
			} else if (c == CCL) {
				v.addElement(new SEccl(arg, i));
			// "*" not special unless it follows something;
			// replace element it follows with CLOSURE referring to it
			} else if (i.get() > 0 &&
				(c == CLOSURE_ANY ||
				 c == CLOSURE_ONE_OR_MORE ||
				 c == CLOSURE_ZERO_OR_ONE)) {
				int last = v.size()-1;
				v.setElementAt(new SEclos(c, (SE)v.elementAt(last)), last);
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
	protected boolean doMatch(SE[] patt, String line) {
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
		if (a.charAt(i.get()+1) == \0)
			return \\;	/* not special at end */
		i.incr();
		if (a.charAt(i.get()) == n)
			return \n;
		if (a.charAt(i.get()) == t)
			return \t;
		return a.charAt(i.get());
	}

	/** Represent one sub-expression in an RE. */
	abstract class SE {
		/** Start matching at i in ln. Increment i as much as matches.
		 * Return true if valid match, false if not.
		 */
		public abstract boolean amatch(String ln, Int i);
	}

	class SEchar extends SE {
		char val;

		public SEchar(char ch) { val = ch; }

		public String toString() { return "SE(" + val + ); }

		public boolean amatch(String ln, Int i) {
			System.out.println("SEchar.amatch("+ln+,+i.get() + ));
			if (i.get() < ln.length()) {
				boolean success = (ln.charAt(i.get()) == val);
				System.out.println("SEchar.amatch: success="+success);
				i.incr();
				return success;
			} 
			return false;					// end of string
		}
	}

	class SEany extends SE {
		public String toString() { return "SE(.)"; }
		public boolean amatch(String ln, Int i) {
			if (ln.length() >= i.get())
				return false;					// end of string
			i.incr();
			return true;
		}
	}

	class SEbol extends SE {
		public String toString() { return "SE(^)"; }
		public boolean amatch(String ln, Int i) {
			if (i.get()>0)
				return false;
			// no i.incr() since we dont use any chars in ln
			return true;
		}
	}

	class SEeol extends SE {
		public String toString() { return "SE($)"; }
		public boolean amatch(String ln, Int i) {
			if (i.get() == ln.length() - 1) {
				// no i.incr() since we dont use any chars in ln
				return true;
			}
			return false;
		}
	}

	/** SEccl represents on Character Class */
	class SEccl extends SE {
		int len = 0;
		boolean negate = false;
		StringBuffer val = new StringBuffer();

		public String toString() {
			return "SEccl[" + val.toString() + "]";
		}

		/** Construct a CCL. Some of this code was in "getCCL()" */
		public SEccl(String arg, Int i) {
			int jstart;

			i.incr();			/* skip over [ */
			if (i.get()>=arg.length())
				throw new RESyntaxException("pattern ends with [");
			if (arg.charAt(i.get()) == NEGATE) {
				negate = true;
				i.incr();
			}
			// Expand the range
			len = doDash(arg, i);
			if (arg.charAt(i.get()) != CCLEND)
				throw new RESyntaxException("CCL ends without ]");
		}

		/* doDash - expand dash shorthand set at src[i] to end of dest.
		 * @return number of characters appended to dest.
		 */
		protected int doDash(String src, Int i) {

			int startLen = val.length();

			while (src.charAt(i.get()) != CCLEND && i.get()<src.length()) {
				if (src.charAt(i.get()) == LITCHAR)
					val.append(esc(src, i));
				else if (src.charAt(i.get()) != DASH)
					val.append(src.charAt(i.get()));
				else if (val.length() == 0 || src.length() == i.get()+1)
					val.append(DASH);	/* literal - */
				else if (Character.isLetterOrDigit(src.charAt(i.get()-1)) && 
					Character.isLetterOrDigit(src.charAt(i.get()+1)) &&
					src.charAt(i.get()-1) <= src.charAt(i.get()+1)) {
					for (int k = src.charAt(i.get()-1)+1; k <= src.charAt(i.get()+1); k++)
						val.append((char)k);
					i.incr();
				}
				else
					val.append(DASH);
				i.incr();
			}
			return val.length() - startLen;
		}

		/** match CCLs here */
		public boolean amatch(String ln, Int i) {
			if (!negate) {
				if (i.get()<ln.length() && locate(ln.charAt(i.get())))
					return true;
			} else {
				if (i.get()<ln.length() && !locate(ln.charAt(i.get())))
					return true;
			}
			return false;
		}

		/* locate -- look for c in character class */
		protected boolean locate(char c) {
			for (int k = 0; k<val.length(); k++) {
				if (val.charAt(k) == c)
					return true;
			}
			return false;
		}
	}

	/** SEclos represents one Closure */
	class SEclos extends SE {
		/** Is this closure *, + or ? */
		int type;
		/** What SubExpression is this a closure of? */
		SE target;

		/** Make me printable */
		public String toString() {
			return "SEclos(\"" + (char)type + "\" -> " + target + ")";
		}

		/** Construct a Closure */
		public SEclos(char ty, SE ta) {
			type = ty;
			target = ta;
		}

		/** Match a closure */
		public boolean amatch(String ln, Int i) {
			throw new IllegalArgumentException(
				"Closure amatch() called directly");
		}
	}
//+
}
//-
