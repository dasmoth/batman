package batman.model;

import cern.jet.stat.Probability;
import batman.ExptRecord;
import batman.matrix.Matrix2D;

public class Rectified extends MethModel {
	public Rectified(Matrix2D observed, Matrix2D coupling, ExptRecord[] exptMeta) {
		super(observed, coupling, exptMeta);
	}
	
	protected double likelihood(double obs, double expect, double baseline, double precision) {
		return Math.log(rectifiedGaussianProbability(obs, expect, precision));
	}

	
    public static double rectifiedGaussianProbability(double x, double mean, double precision) {
    	return Math.sqrt(precision / 2 / Math.PI) * Math.exp(-precision / 2 * Math.pow(x - mean, 2)) / Probability.errorFunctionComplemented(-mean * precision / 2);
    }
}
