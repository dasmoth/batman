/**
 * 
 */
package batman;

class CGRecord {
	public final String chr;
	public final int pos;
	public final double weight;
	
	public CGRecord(String chr, int pos, double weight) {
		this.chr = chr;
		this.pos = pos;
		this.weight = weight;
	}
	
	public CGRecord(String chr, int pos) {
		this(chr, pos, 1.0);
	}
}