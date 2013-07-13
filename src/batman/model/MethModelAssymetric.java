package batman.model;

import batman.ExptRecord;
import batman.matrix.Matrix2D;
import cern.jet.stat.Gamma;

public class MethModelAssymetric extends MethModel {

	private double gammaPDF(double x, double k, double theta) {
		return Math.pow(x, k - 1) * Math.exp(-x/theta) / Math.pow(theta, k) / Gamma.gamma(k);
	}
	
	public MethModelAssymetric(Matrix2D observed, Matrix2D coupling, ExptRecord[] exptMeta) {
		super(observed, coupling, exptMeta);
	}

	protected double likelihood(double obs, double expect, double baseline, double precision) {
		double gbl = 2.25;
		if (obs < gbl) {
			return 0; // ignore.  shouldn't be many of these!
		} else {
			double theta = 0.35;
			double k = 1 + (expect - gbl) / theta;
			return Math.log(gammaPDF(obs - gbl, k, theta));
		}
	}
}
