package batman;

import java.util.List;

import batman.matrix.Matrix1D;

public class RoiProfile {
	public String name;
	public int min = -1, max = -1;
	public CGRecord[] cgs;
	public List<Matrix1D> samples;
}
