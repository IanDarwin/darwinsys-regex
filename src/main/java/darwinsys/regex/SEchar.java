/* SEchar - represent one ordinary character.
 * @author Ian Darwin, ian@darwinsys.com
 * $Id$
 */
public class SEchar extends SE {
	char val;

	public SEchar(char ch) { val = ch; }

	public String toString() { return "'" + val + "'"; }

	public boolean amatch(String ln, Int i) {
		System.out.println("SEchar.amatch("+ln+','+i.get() + "), want " + val);
		if (i.get() < ln.length()) {
			boolean success = (ln.charAt(i.get()) == val);
			System.out.println("SEchar.amatch: success="+success);
			if (success)
				i.incr();
			return success;
		} 
		System.out.println("SEchar.amatch: hit end of string");
		return false;					// end of string
	}
}
