/**
 * Another attempt at a toy regular expressions package for Java.
 *
 * This is a very simple implementation, compiling the pattern
 * into a StringBuffer, and interpreting it. As Henry Spencer
 * said of his (much more sophisticated) C-language regexp,
 * replacing the innards of egrep with this code would be a mistake.
 *
 * NOT YET WORKING -- need to finish converting from C,
 * especially the places where an int is computed & should be returned
 * but a boolean is the actual return type. Also several charAt(i)==\0!
 *
 * @author Ian F. Darwin, ian@darwinsys.com.
 * Patterned after a version I wrote years ago in C as part of a
 * text editor implementation, in turn based on Kernighan & Plaughers
 * <I>Software Tools In Pascal</I> implementation.
 *
 * @version $Id$
 */

//+
public class RE {

	public static final char CLOSURE_ANY = *;
	public static final char CL_ZERO_OR_ONE = ?;	// XXX
	public static final char CL_ONE_OR_MORE = +;	// XXX
	public static final char EOL = $;
	public static final char BOL = ^;
	public static final char ANY = .;
	public static final char CCL = [;
	public static final char CCLEND = ];
	public static final char OR = |;			// XXX
	public static final char GRP = (;			// XXX
	public static final char GRPEND = );		// XXX
	public static final char WORD = w;
	public static final char LITCHAR = \\;
	public static final char NEGATE = ^;
	public static final char NCCL = x;
	protected static final int CLOSIZE = 1;
	public static final char DASH = -;
	final int ERR = -1;

	protected StringBuffer myPat;
	protected String origPatt;

	/** Construct an RE object, given a pattern.
	 * @throws RESyntaxException if bad syntax.
	 */
	public RE(String patt) throws RESyntaxException {
		origPatt = patt;
		myPat = compile(patt);
	}

	public String toString() {
		return "RE[" + origPatt + "-->" + myPat + "]";
	}

	/** Match a pattern in a given string. Designed for light-duty
	 * use, as it compiles the pattern each time.
	 * For multiples uses of the same pattern, construct an RE object,
	 * which stores the pattern inside it.
	 */
	public static Match match(String patt, String str){
		StringBuffer thispat = compile(patt);
		// return match(thispat, str);
		return null;
	}

	/** Match the compiled pattern stored in the RE in a given String.
	 * @return True if the RE matches anyplace in the String.
	 */
	public boolean isMatch(String str){
		return match(myPat, str);
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
	 * @return compiled pattern
	 * @throws RESyntaxException if bad syntax.
	 */
	protected static StringBuffer compile(String arg) throws RESyntaxException {
		Int i = new Int();		// arg index, initially 0
		int j = 0,	// patt "index"
			lastj = 0, lj = 0;
		boolean done = false;

		StringBuffer patt = new StringBuffer(arg.length()*2); // guess length

		while (!done && i.get()<arg.length()) {
			char c = arg.charAt(i.get());
			lj = j;
			if (c == ANY) {
				patt.append(ANY);
			// "^" only special at beginning/
			} else if (c == BOL && i.get() == 0) {
				patt.append(BOL);
			// "$" only special at end */
			} else if (c == EOL && i.get() > 0) {
				patt.append(EOL);
			} else if (c == CCL) {
				if (getCCL(arg, i, patt) == false)
					break;
			// "*" not special unless it follows something
			} else if (c == CLOSURE_ANY && i.get() > 0) {
				lj = lastj;
				if (patt.charAt(lj) == BOL || patt.charAt(lj) == EOL ||
					patt.charAt(lj) == CLOSURE_ANY)
					break;	/* terminate loop */
				else
					/* replaces stclose: lastj==where orig. pattern began */
					patt.insert(lastj, CLOSURE_ANY);
					++j;
			} else {
				// "Ordinary" char, but must be LITCHARd so we dont
				// mix it up with ^ ! x etc.
				patt.append(LITCHAR);		++j;
				patt.append(esc(arg, i));	++j;	// XXX patsize?
			}
			lastj = lj;
			if (!done) i.incr();
		} 
		if (done) {				/* finished early */
			throw new RESyntaxException("incomplete pattern");
		} else
			return patt;
	}

	/** getCCL -- expand char class at arg[i] into pat[j].
	 * @return true if pattern OK, false if a problem.
	 */
	protected static boolean getCCL(String arg, Int i, StringBuffer patt) {
		int jstart;

		i.incr();			/* skip over [ */
		if (i.get()>=arg.length())
			return false;
		if (arg.charAt(i.get()) == NEGATE) {
			patt.append(NCCL);
			i.incr();
		} else
			patt.append(CCL);
		jstart = patt.length();
		// Expand the range
		int len = doDash(CCLEND, arg, i, patt);
		// store the length before it: cast int to 16-bit char.
		patt.insert(jstart, (char)(len));
		return arg.charAt(i.get()) == CCLEND;
	}

	/* doDash - expand dash shorthand set at src[i] to end of dest.
	 * @return number of characters appended to dest.
	 */
	protected static int doDash(
		char dlm, String src, Int i, StringBuffer dest) {

		int startLen = dest.length();

		while (src.charAt(i.get()) != dlm && i.get()<src.length()) {
			if (src.charAt(i.get()) == LITCHAR)
				dest.append(esc(src, i));
			else if (src.charAt(i.get()) != DASH)
				dest.append(src.charAt(i.get()));
			else if (dest.length() == 0 || src.length() == i.get()+1)
				dest.append(DASH);	/* literal - */
			else if (Character.isLetterOrDigit(src.charAt(i.get()-1)) && 
				Character.isLetterOrDigit(src.charAt(i.get()+1)) &&
				src.charAt(i.get()-1) <= src.charAt(i.get()+1)) {
				for (int k = src.charAt(i.get()-1)+1; k <= src.charAt(i.get()+1); k++)
					dest.append(k);
				i.incr();
			}
			else
				dest.append(DASH);
			i.incr();
		}
		return dest.length() - startLen;
	}

	/* match -- find pattern match anywhere on line */
	protected boolean match(StringBuffer patt, String line) {
		int	i = 0, j = 0;

		while (i<line.length() && j == 0) {
			j = amatch(line, i, patt, 0);
			i++;
		}
		return j > 0;
	}

	/* amatch - look for match of patt[j]... at lin[offset]... */
	int amatch(String line, int offset, StringBuffer patt, int j) {
		Int i = new Int(offset);
		int k = 0;
		boolean done = false;

		while (!done && j<patt.length())
			if (patt.charAt(j) == CLOSURE_ANY) {
				j += CLOSIZE;
				i.set(offset);
				/* match as many as possible */
				while (!done && i.get()<line.length())
					if (omatch(line, i, patt, j) != true)
						done = true;
				/*
				 * i points to input char that made omatch fail.
				 * match rest of pattern against rest of input.
				 * shrink closure by 1 after each failure.
				 */
				done = false;
				while (!done && i.get() >= offset) {
					k = amatch(line, i.get(), patt, j+patsize(patt, j)); // recurse
					if (k > 0)	/* matched rest of pattern */
						done = true;
					else
						i.decr();
				}
				offset = k;	/* if k=0 failure, else success */
				done = true;
			}
			// XXX should i.get be i, and change the header?
			else if (omatch(line, i, patt, j) != true) {
				offset = 0;	/* non-closure */
				done = true;
			}
			else	/* omatch succeeded on this pattern element */
				j += patsize(patt, j);
		return offset;
	}

	/* patsize -- return size of pattern entry at patt[n] */
	protected int patsize(StringBuffer patt, int n) {
		switch(patt.charAt(n)) {
		case LITCHAR:
			return 2;
		case BOL:
		case EOL:
		case ANY:
			return 1;
		case CCL:
		case NCCL:
			return patt.charAt(n+1) + 2;
		case CLOSURE_ANY:
			return CLOSIZE;
		default:
			System.err.println("in patsize: invalid case, cant happen");
			return 0;
		}
	}

	/* omatch -- match one pattern element at patt[j], return boolean */
	protected boolean omatch(String line, Int i, StringBuffer patt, int j) {
		int advance = -1;

		if (line.length() == i.get()+1)
			return false;
		else
			switch(patt.charAt(j)) {
			case LITCHAR:
System.out.println("i="+i+", j=" + j);
				if (line.charAt(i.get()) == patt.charAt(j+1))
					advance = 1;
				break;
			case BOL:
				if (i.get() == 0)
					advance = 0;
			case ANY:
				if (line.charAt(i.get()) != \0)	// XXX
					advance = 1;
				break;
			case EOL:
				if (line.charAt(i.get()) == \0)	// XXX
					advance = 0;
				break;
			case CCL:
				if (locate(line.charAt(i.get()), patt, j+1))
					advance = 1;
				break;
			case NCCL:
				if (line.charAt(i.get()) != \0 && !locate(line.charAt(i.get()), patt, j+1))
					advance = 1;
				break;
			default:
				System.err.println("omatch: bad case, cant happen");
				return false;
			}
		if (advance >= 0) {
			i.incr(advance);
			return true;
		} else
			return false;
	}

	/* locate -- look for c in character class at patt[offset] */
	protected boolean locate(char c, StringBuffer patt, int offset) {
		int i;
		boolean status = false;

		/* size of class is at patt[offset], characters follow*/
		i = offset + patt.charAt(offset);	/* last position */
		while (i > offset)
			if (c == patt.charAt(i)) {
				status = true;
				i = offset; /* force loop termination - should break? */
			}
			else
				--i;
		return status;
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
//+
}
//-
