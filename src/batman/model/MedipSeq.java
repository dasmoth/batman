package batman.model;

import batman.ExptRecord;
import batman.matrix.Matrix2D;

public class MedipSeq extends MethModel {

	public MedipSeq(Matrix2D observed, Matrix2D coupling, ExptRecord[] exptMeta) {
		super(observed, coupling, exptMeta);
	}

	protected double likelihood(double obs, double expect, double baseline, double precision) {
		// return Math.log(gaussianProbability(expect, 2.8 * Math.pow(obs, 0.65) - 1.0, 0.5));
		// return Math.log(gaussianProbability(expect, 2.0 * Math.pow(obs, 0.4) - 1.5, 0.5));
		// return Math.log(gaussianProbability(expect, 1.5 * Math.pow(obs, 0.4) - 0.5, 0.5));
		// return Math.log(gaussianProbability(obs, 1.5 * Math.pow(expect, 0.4) - 0.5, 0.5));
		// return Math.log(gaussianProbability(expect * 3, Math.pow(obs, 0.5), 0.1));
		
		
		return Math.log(gaussianProbability(expect / 5, 2.8 * Math.pow(obs / 3, 0.65) - 1.0, 0.5));
	}
}
