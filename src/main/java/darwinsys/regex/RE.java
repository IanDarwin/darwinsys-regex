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

public class RE {

	final char CLOSURE = *;
	final char EOL = $;
	final char BOL = ^;
	final char ANY = ?;
	final char CCL = [;
	final char LITCHAR = \\;
	final char NEGATE = !;
	final char NCCL = x;
	final char CCLEND = ];
	final int CLOSIZE = 1;	/* TODO: check against the book */
	final char DASH = -;
	final int ERR = -1;

	/* make pat -- make pattern from arg[i], stop at delim */

	/* return ERR or index of closing delimiter */
	int
	makepat(String arg, int start, char delim, StringBuffer patt) {
		int	i = start, j = 0, lastj = 0, lj = 0;
		boolean done = false;

		while (!done && arg.charAt(i) != \0 && arg.charAt(i) != delim) {
			lj = j;
			if (arg.charAt(i) == ANY)
				patt.insert(j, ANY);
			else if (arg.charAt(i) == BOL && i == start)
				patt.insert(j, BOL);
			else if (arg.charAt(i) == EOL && arg.charAt(i+1) == delim)
				patt.insert(j, EOL);
			else if (arg.charAt(i) == CCL)
				done = getccl(arg, i, patt, j) == false;
			else if (arg.charAt(i) == CLOSURE && i > start) {
				lj = lastj;
				if (patt.charAt(lj) == BOL || patt.charAt(lj) == EOL ||
					patt.charAt(lj) == CLOSURE)
					done = true;	/* terminate loop */
				else
					stclose(patt, j, lastj);
			} else {
				patt.insert(j, LITCHAR);
				patt.insert(j, esc(arg, i));
			}
			lastj = lj;
			if (!done)
				++i;
		}
		if (done || arg[i] != delim) {		/* finished early */
			System.err.println("incomplete pattern");
			return ERR;
		} else
			return i;
	}

	/** getccl -- expand char class at arg[i] into pat[j].
	 * @return true if pattern OK, false if a problem.
	 */
	protected boolean getccl(String arg, int i, StringBuffer patt, int j) {
		int jstart;

		++i;			/* skip over [ */
		if (arg.charAt(i) == NEGATE) {
			patt.insert(j, NCCL);
			++i;
		} else
			patt.insert(j, CCL);
		jstart = j;
		patt.insert(j, 0);	/* room for count */
		dodash(CCLEND, arg, i, patt, j);
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
	protected void dodash(char dlm, String src, int i, StringBuffer dest, int j) {
		int k;

		while (src.charAt(i) != dlm && src[i] != \0) {
			if (src.charAt(i) == ESCAPE)
				dest.insert(j, esc(src, i));
			else if (src.charAt(i) != DASH)
				dest.insert(j, src.charAt(i));
			else if (j <= 0 || src.charAt(i+1) == \0)
				dest.insert(j, DASH);	/* literal - */
			else if (isalnum(src[i-1]) && isalnum(src.charAt(i+1)) &&
				src.charAt(i-1) <= src.charAt(i+1)) {
				for (k = src.charAt(i-1)+1; k <= src.charAt(i+1); k++)
					dest.insert(j, k);
				++i;
			} else
				dest.insert(j, DASH);
			++i;
		}
	}

	/* match -- actual matching routines: match amatch omatch */

	/* match -- find pattern match anywhere on line */
	boolean match(String line, String patt)
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
}
