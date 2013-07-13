/**
 * 
 */
package batman;

public class ExptRecord {
	public final String name;
	public final int id;
	public final double response;
	public final double baseLine;
	public final double precision;
	
	public ExptRecord(String name, int id, double response, double baseLine, double precision) {
		this.name = name;
		this.id = id;
		this.response = response;
		this.baseLine = baseLine;
		this.precision = precision;
	}
	
	public String toString() {
		return String.format("%s response=%g baseline=%g", name, response, baseLine);
	}
}