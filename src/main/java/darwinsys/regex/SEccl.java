/** SEccl represents on Character Class
 * @author Ian Darwin, ian@darwinsys.com
 * $Id$
 */
public class SEccl extends SE {
	int len = 0;
	boolean negate = false;
	StringBuffer val = new StringBuffer();

	public String toString() {
		return "SEccl[" + val.toString() + "]";
	}

	/** Construct a CCL. Some of this code was in "getCCL()" */
	public SEccl(String arg, Int i) {
		int jstart;

		i.incr();			/* skip over '[' */
		if (i.get()>=arg.length())
			throw new RESyntaxException("pattern ends with [");
		if (arg.charAt(i.get()) == RE.NEGCCL) {
			negate = true;
			i.incr();
		}
		// Expand the range
		len = doDash(arg, i);
		if (arg.charAt(i.get()) != RE.CCLEND)
			throw new RESyntaxException("CCL ends without ]");
	}

	/* doDash - expand dash shorthand set at src[i] to end of dest.
	 * @return number of characters appended to dest.
	 */
	protected int doDash(String src, Int i) {

		int startLen = val.length();

		while (src.charAt(i.get()) != RE.CCLEND && i.get()<src.length()) {
			if (src.charAt(i.get()) == RE.LITCHAR)
				val.append(RE.esc(src, i));
			else if (src.charAt(i.get()) != RE.DASH)
				val.append(src.charAt(i.get()));
			else if (val.length() == 0 || src.length() == i.get()+1)
				val.append(RE.DASH);	/* literal - */
			else if (Character.isLetterOrDigit(src.charAt(i.get()-1)) && 
				Character.isLetterOrDigit(src.charAt(i.get()+1)) &&
				src.charAt(i.get()-1) <= src.charAt(i.get()+1)) {
				for (int k = src.charAt(i.get()-1)+1; k <= src.charAt(i.get()+1); k++)
					val.append((char)k);
				i.incr();
			}
			else
				val.append(RE.DASH);
			i.incr();
		}
		return val.length() - startLen;
	}

	/** match CCLs here */
	public boolean amatch(String ln, Int i) {
		if (!negate) {
			if (i.get()<ln.length() && locate(ln.charAt(i.get()))) {
				i.incr();
				return true;
			}
		} else {
			if (i.get()<ln.length() && !locate(ln.charAt(i.get()))) {
				i.incr();
				return true;
			}
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

	/* esc - handle C-like escapes
	 * if a[i] is \, following char may be special.
	 * updates i if so.
	 * in any case, returns the character.
	 */
	protected static char esc(String a, Int i) {
		char c = a.charAt(i.get());
		if (c != '\\')
			return c;
		if (i.get() >= a.length())
			return '\\';	/* not special at end */
		i.incr();
		c = a.charAt(i.get());
		switch (c) {
		// 'd' not useful here
		case 'n':
			return ('\n');
		case 'r':
			return ('\r');
		case 't':
			return ('\t');
		// 'w' not allowed here
		case '\\':
			return ('\\');
		default:
			return (c);
		}
	}
}