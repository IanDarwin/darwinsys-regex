/** SErep represents one "Closure" or repetition.
 * The four forms in the RE input, * ? + and {}, are all
 * represented by an instance of this class, with the
 * minimum and maximum number set appropriately.
 * @author Ian Darwin, ian@darwinsys.com
 * $Id$
 */
public class SErep extends SE {
	/** The constant meaning no upper bound. */
	public final static int NOMAX = Integer.MAX_VALUE;
	/** The minimum number of times that must match */
	int minimum;
	/** The maximum number number of times allowed for a match */
	int maximum;
	/** What SubExpression is this a closure of? */
	SE target;

	/** Make me printable */
	public String toString() {
		return "SErep(" + target + "{" + minimum + "," + maximum + "})";
	}

	/** Construct a Closure */
	public SErep(int min, int max, SE ta) {
		if (minimum < 0)
			throw new IllegalArgumentException(
				"Minimum must be non-negative");
		minimum = min;
		maximum = max;
		target = ta;
	}

	/** Match a closure */
	public boolean amatch(String ln, Int i) {
		throw new IllegalArgumentException(
			"Closure amatch() called directly");
	}
}
