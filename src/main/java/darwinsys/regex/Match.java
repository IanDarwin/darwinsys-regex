/** A Match describes one position at which a RE matches a String. */
public class Match {
	int start;
	int end;
	public Match() {
		this(0,0);
	}
	public Match(int s, int e) {
		start = s;
		end   = e;
	}
}
