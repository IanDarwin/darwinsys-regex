/** Another attempt at a toy regular expressions package for Java.
 *
 * This is a very simple implementation, compiling the pattern
 * into a StringBuffer, and interpreting it. As Henry Spencer
 * said of his (much more sophisticated) C-language regexp,
 * replacing the innards of egrep with this code would be a mistake.
 *
 * NOWHERE NEAR WORKING -- need to finish converting from C,
 * especially the places where an int is computed & should be returned
 * but a boolean is the actual return type.
 * Also need to use patt.setLength() to discard extra chars a few places.
 *
 * @author Ian F. Darwin, ian@darwinsys.com.
 * Patterned after a version I wrote years ago in C as part of a
 * text editor implementation, in turn based on Kernighan & Plaughers
 * <I>Software Tools</I>.
 * @version $Id$
 */

//+
public class RE {

	public static final char CLOSURE = *;
	public static final char EOL = $;
	public static final char BOL = ^;
	public static final char ANY = .;
	public static final char CCL = [;
	public static final char LITCHAR = \\;
	public static final char NEGATE = !;
	public static final char NCCL = x;
	final char CCLEND = ];
	final int CLOSIZE = 1;	/* TODO: check against the book */
	final char DASH = -;
	final int ERR = -1;

	protected StringBuffer mypat;

	/** Construct an RE object, given a pattern.
	 * @throws RESyntaxException if bad syntax.
	 */
	public RE(String patt) throws RESyntaxException {
		mypat = compile(patt);
	}

	/** Match a pattern in a given string. Designed for light-duty
	 * use, as it compiles the pattern each time.
	 * For frequent use, construct an RE object, which stores
	 * the pattern inside it.
	 */
	public static Match match(String patt, String str){
		StringBuffer thispat = compile(patt);
		return match(thispat, str);
	}

	/** Match the compiled pattern stored in the RE in a given String.
	 * @return True if the RE matches anyplace in the String.
	 */
	public boolean isMatch(String str){ /*XXX*/ return false; }

	/** Match the compiled pattern stored in the RE in a given String.
	 * @return A Match object indicating the position & length of the match
	 */
	public Match match(String str){ /*XXX*/ return null; }

	/** Match all occurrences of the compiled pattern stored in the 
	 * RE against the given String.
	 * @return Array of Match objects, each indicating the position & 
	 * length of the match.
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
		int lastj = 0, lj = 0;
		boolean done = false;

		StringBuffer patt = new StringBuffer(arg.length()*2); // guess length

		for (int i=0, j=0; i<arg.length; i++) {
			if (arg.charAt(i) == ANY)
				patt.append(ANY);
			// "^" only special at beginning/
			else if (arg.charAt(i) == BOL && i == 0)
				patt.append(BOL);
			// "$" only special at end */
			else if (arg.charAt(i) == EOL && i==arg.length-1)
				patt.append(EOL);
			else if (arg.charAt(i) == CCL)
				if (getccl(arg, i, patt) == false)
					break;
			// "*" not special unless it follows something
			else if (arg.charAt(i) == CLOSURE && i > 0) {
				lj = lastj;
				if (patt.charAt(lj) == BOL || patt.charAt(lj) == EOL ||
					patt.charAt(lj) == CLOSURE)
					break;	/* terminate loop */
				else
					stclose(patt, j, lastj);
			} else {
				patt.append(LITCHAR);
				patt.append(esc(arg, i));
			}
			lastj = lj;
		}
		if (done || arg[i] != delim) {		/* finished early */
			throw new RESyntaxException("incomplete pattern");
		} else
			return patt;
	}

	/** getccl -- expand char class at arg[i] into pat[j].
	 * @return true if pattern OK, false if a problem.
	 */
	protected boolean getccl(String arg, int i, StringBuffer patt) {
		int jstart;

		++i;			/* skip over [ */
		if (arg.charAt(i) == NEGATE) {
			patt.append(NCCL);
			++i;
		} else
			patt.append(CCL);
		jstart = patt.length();
		patt.append(j, 0);	/* room for count */
		// Expand the range
		dodash(CCLEND, arg, i, patt);
		// replace null with count
		patt.setCharAt(jstart, (char)(j - jstart - 1));
		return arg.charAt(i) == CCLEND;
	}

	/* stclose -- store closure into array at patt[j] */
	protected int stclose(StringBuffer patt, int j, int lastj) {
		int jp, jt;

		for (jp = j - 1; jp >= lastj; --jp) {
			jt = jp + CLOSIZE;
			patt.setCharAt(jt, patt.charAt(jp));
		}
		j += CLOSIZE;
		patt.insert(lastj, CLOSURE);	/* where original pattern began */
		return j;
	}

	/* dodash - expand dash shorthand set at scr[i] into dest[j], stop at dlm */
	protected void dodash(char dlm, String src, int i, StringBuffer dest) {
		int k;

		while (src.charAt(i) != dlm && src[i] != \0) {
			if (src.charAt(i) == ESCAPE)
				dest.append(esc(src, i));
			else if (src.charAt(i) != DASH)
				dest.append(src.charAt(i));
			else if (dest.length == 0 || src.length() == i+1)
				dest.append(DASH);	/* literal - */
			/* XXX 
			else if (isalnum(src[i-1]) && isalnum(src.charAt(i+1)) &&
				src.charAt(i-1) <= src.charAt(i+1)) {
				for (k = src.charAt(i-1)+1; k <= src.charAt(i+1); k++)
					dest.append(k);
				++i;
			}
			XXX */
			else
				dest.append(DASH);
			++i;
		}
	}

	/* match -- find pattern match anywhere on line */
	protected boolean match(String line, StringBuffer patt)
	{
		int	i = 0, j = 0;

		while (line[i] != \0 && j == 0) {
			j = amatch(line, i, patt, 0);
			i++;
		}
		return j > 0;
	}

	/* amatch - look for match of patt[j]... at lin[offset]... */
	int amatch(String line, int offset, String patt, int j) {
		int i, k;
		boolean done = false;

		while (!done && patt[j] != \0)
			if (patt[j] == CLOSURE) {
				j += patsize(patt, j);	/* step over CLOSURE */
				i = offset;
				/* match as many as possible */
				while (!done && line[i] != \0)
					if (omatch(line, i, patt, j) != true)
						done = true;
				/*
				 * i points to input char that made omatch fail.
				 * match rest of pattern against rest of input.
				 * shrink closure by 1 after each failure.
				 */
				done = false;
				while (!done && i >= offset) {
					k = amatch(line, i, patt, j+patsize(patt, j));
					if (k > 0)	/* matched rest of pattern */
						done = true;
					else
						--i;
				}
				offset = k;	/* if k=0 failure, else success */
				done = true;
			}
			else if (omatch(line, offset, patt, j) != true) {
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
		case CLOSURE:
			return CLOSIZE;
		default:
			System.err.println("in patsize: invalid case, cant happen");
			return 0;
		}
	}

	/* omatch -- match one pattern element at patt[j], return boolean */
	protected int omatch(StringBuffer line, int i, StringBuffer patt, int j) {
		int advance = -1;

		if (line.length() == i+1)
			return false;
		else
			switch(patt.charAt(j)) {
			case LITCHAR:
				if (line.charAt(i) == patt.charAt(j+1))
					advance = 1;
				break;
			case BOL:
				if (i == 0)
					advance = 0;
			case ANY:
				if (line.charAt(i) != \0)
					advance = 1;
				break;
			case EOL:
				if (line.charAt(i) == \0)
					advance = 0;
				break;
			case CCL:
				if (locate(line.charAt(i), patt, j+1))
					advance = 1;
				break;
			case NCCL:
				if (line.charAt(i) != \0 && !locate(line.charAt(i), patt, j+1))
					advance = 1;
				break;
			default:
				System.err.println("omatch: bad case, cant happen");
				return ERR;
			}
		if (advance >= 0) {
			i += advance;
			return i;
		} else
			return ERR;
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
	protected char esc(StringBuffer a, int i) {
		if (a.charAt(i) != \\)
			return a.charAt(i);
		if (a.charAt(i+1) == \0)
			return \\;	/* not special at end */
		i++;
		if (a.charAt(i) == n)
			return \n;
		if (a.charAt(i) == t)
			return \t;
		return a.charAt(i);
	}
//+
}
//-
