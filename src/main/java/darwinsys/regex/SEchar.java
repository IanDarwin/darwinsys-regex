/* SEchar - represent one ordinary character.
 * @author Ian Darwin, ian@darwinsys.com
 * $Id$
 */
public class SEchar extends SE {
	char val;

	public SEchar(char ch) { val = ch; }

	public String toString() { return "SE(" + val + ')'; }

	public boolean amatch(String ln, Int i) {
		System.out.println("SEchar.amatch("+ln+','+i.get() + ')');
		if (i.get() < ln.length()) {
			boolean success = (ln.charAt(i.get()) == val);
			System.out.println("SEchar.amatch: success="+success);
			i.incr();
			return success;
		} 
		return false;					// end of string
	}
}
