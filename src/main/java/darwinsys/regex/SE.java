/** Represent one sub-expression in an RE.
 * @author Ian Darwin, ian@darwinsys.com
 * $Id$
 */
public abstract class SE {
	/** Start matching at i in ln. Increment i as much as matches.
	 * Return true if valid match, false if not.
	 */
	public abstract boolean amatch(String ln, Int i);
}

