package com.darwinsys.regexp;

/** SEor represents an "or" condition, as in verdi|wagner
 * @author Ian Darwin, ian@darwinsys.com
 * $Id$
 */
public class SEor extends SE {
	/** What SubExpression is on the left? */
	SE left;
	/** What SubExpression is on the right? */
	SE right;

	/** Retuern a printable representation of this SE */
	public String toString() {
		return new StringBuffer("SEor(").append(left).append('|').append(right).toString();
	}

	/** Construct a SEor */
	public SEor(SE l, SE r) {
		left = l;
		right = r;
	}

	/** Match either left OR right at ln[i]. */
	public boolean amatch(String ln, Int i) {
		return left.amatch(ln, i) || right.amatch(ln, i);
	}
}
