/** 
 * Tiny class for passing integer in AND getting its updated value back.
 * Like java.lang.Integer, but smaller and much more specialized.
 */
public class Int {
	int val;
	public Int(int i) { val = i; }
	public Int() { val = 0; }
	public void set(int i) { val = i; }
	public int  get() { return val; }
	public void incr() { val++; }
	public void incr(int i) { val += i;}
	public void decr() { val++; }
	public void decr(int i) { val -= i;}
	public String toString() { return Integer.toString(val); }
}
