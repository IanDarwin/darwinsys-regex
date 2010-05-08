package darwinsys.regex;

/** Represent one sub-expression in an RE.
 * @author Ian Darwin, http://www.darwinsys.com/
 * $Id$
 */
public abstract class SE {
	/** Start matching at i in ln. 
	 * Increment i as much as matches.
	 * @eturn true if valid match, false if not.
	 */
	public abstract boolean amatch(String ln, Int i);

	/** Generate a printable representation of this SubExpression. */
	public String toString() {
		return "SE";
	}
}
