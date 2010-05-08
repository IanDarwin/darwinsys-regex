package darwinsys.regex;

import java.util.logging.*;

/* SEchar - represent one ordinary character.
 * @author Ian Darwin, http://www.darwinsys.com/
 * $Id$
 */
public class SEchar extends SE {
	
	protected static Logger logger = Logger.getLogger("darwinsys.regex");
	
	char val;

	public SEchar(char ch) { val = ch; }

	public String toString() { return "'" + val + "'"; }

	/** amatch -- match one SE with one sub-input.
	 * @return true iff ln.charAt(i)==the character we were constructed with.
	 */
	public boolean amatch(String ln, Int i) {
		logger.fine("SEchar.amatch("+ln+','+i.get() + "), want " + val);
		if (i.get() < ln.length()) {
			boolean success = (ln.charAt(i.get()) == val);
			logger.fine("SEchar.amatch: success="+success);
			if (success)
				i.incr();
			return success;
		} 
		logger.fine("SEchar.amatch: hit end of string");
		return false;					// end of string
	}
}
