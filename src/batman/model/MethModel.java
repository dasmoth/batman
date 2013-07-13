package batman.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import batman.ExptRecord;
import batman.matrix.CommitableMatrix1D;
import batman.matrix.Matrix1D;
import batman.matrix.Matrix2D;
import batman.matrix.MatrixWrapper1D;
import batman.matrix.SimpleCommitableMatrix1D;
import batman.utils.Commitable;
import batman.utils.MathsTools;

/**
 * Bayesian MeDIP model.
 * 
 * @author thomas
 */

public class MethModel {
	private final Matrix2D observed;
	private final Matrix2D coupling;
	private final ExptRecord[] exptMeta;
	private final int P; // number of probes
	private final int C; // Number of CpGs
	private final int A; // Number of expts.
	private double soften = 0.0;
	
	private List<State> ensemble;
	private int step = 0;
	private Random random;
	
    public static double logGaussianProbability(double x, double mean, double precision) {
        return Math.log(Math.sqrt(precision / 2 / Math.PI) * Math.exp(-precision / 2 * Math.pow(x - mean, 2)));
    }
    
    public static double gaussianProbability(double x, double mean, double precision) {
    	return Math.sqrt(precision / 2 / Math.PI) * Math.exp(-precision / 2 * Math.pow(x - mean, 2));
    }
    
    public class Sample {
    	private final Matrix1D meths;
    	private final int cycle;
    	private double likelihood;
    	private double priorMass;
    	
    	private Sample(Matrix1D meths, int cycle, double likelihood, double prior) {
    		this.meths = meths;
    		this.cycle = cycle;
    		this.likelihood = likelihood;
    		this.priorMass = prior;
    	}
    	
    	public int getCycle() {
    		return cycle;
    	}
    	
    	public Matrix1D getMeths() {
    		return meths;
    	}
    	
    	public double getLikelihood() {
    		return likelihood;
    	}
    	
    	public double getPrior() {
    		return priorMass;
    	}
    	
    	public double getBayesFactor() {
    		return likelihood + priorMass;
    	}
    }
    
	private class State implements Commitable {
		private CommitableMatrix1D meths;
		private Matrix1D methsView;
		private double L = Double.NaN;
		
		private void init() {
			methsView = new MatrixWrapper1D(meths) {
				public void set(int i, double d) {
					L = Double.NaN;
					super.set(i, d);
				}
			};
		}
		
		State() {
			meths = new SimpleCommitableMatrix1D(C);
			for (int c = 0; c < C; ++c) {
				meths.set(c, sampleMeth());
			}
			init();
		}
		
		State(State s) {
			meths = new SimpleCommitableMatrix1D(s.meths);
			init();
		}
	
		public Matrix1D getMeths() {
			return methsView;
		}
		
		public double likelihood() {
			if (Double.isNaN(L)) {
				L = 0;
				for (int p = 0; p < P; ++p) {
					double c_m = 0;
					for (int c = 0; c < C; ++c) {
						double couple = coupling.get(p, c);
						if (couple > 0) {
							c_m += couple * meths.get(c);
						}
					}
					for (int a = 0; a < A; ++a) {
						double obs = observed.get(p, a);
						if (!Double.isNaN(obs)) {
							double expect = exptMeta[a].baseLine + c_m / exptMeta[a].response /* - 0.2 */;
							double lt = MethModel.this.likelihood(obs, expect, exptMeta[a].baseLine, exptMeta[a].precision);
							// System.err.printf("response=%g   c_m=%g     L(%g | %g, %g, %g) = %g%n", exptMeta[a].response, c_m, obs, expect, exptMeta[a].baseLine, exptMeta[a].precision, lt);
							L += lt;
						}
					}
				}
			}
			return L;
		}

		public void commit() {
			meths.commit();
		}

		public void rollback() {
			meths.rollback();
			L = Double.NaN;
		}
		
		public boolean isDirty() {
			return meths.isDirty();
		}
	}
	
	protected double likelihood(double obs, double expect, double baseline, double precision) {
		return Math.log(soften + (1.0 - soften) * gaussianProbability(obs, expect, precision));
	}
	
	public MethModel(Matrix2D observed, Matrix2D coupling, ExptRecord[] exptMeta) {
		super();
		this.observed = observed;
		this.coupling = coupling;
		this.exptMeta = exptMeta;
		
		P = coupling.rows();
		C = coupling.columns();
		A = observed.columns();
		
		if (observed.rows() != P) {
			throw new IllegalArgumentException("observation and coupling matrices don't match");
		}
		if (exptMeta.length != A) {
			throw new IllegalArgumentException("wrong number of expt metadata entries.");
		}
	}
	
	public void init(int esize) {
		random = new Random();
		ensemble = new ArrayList<State>();
		for (int s = 0; s < esize; ++s) {
			ensemble.add(new State());
		}
	}
	
	public Sample next()
		throws Exception
	{
		++step;
		Collections.sort(ensemble, new Comparator<State>() {
			public int compare(State o1, State o2) {
				return MathsTools.sign(o1.likelihood() - o2.likelihood());
			}
		});
		
		State floorState = ensemble.remove(0);
		double floor = floorState.likelihood();
		
		/* -- do we want directSampling?
		do {
			State newModel = new State();
			if (newModel.likelihood() > floor) {
				ensemble.add(newModel);
				break;
			}
		} while (true);
		
		*/
		
		State parentState = ensemble.get(random.nextInt(ensemble.size()));
		State newState = new State(parentState);
		decorrelate(newState, floor);
		ensemble.add(newState);
		
		int ensembleSize = ensemble.size();
		double penalty = -Math.log(ensembleSize) + step * Math.log((1.0 * ensembleSize) / (ensembleSize + 1));
		return new Sample(floorState.meths, step, floor, penalty);
	}
	
	
	private double sampleMeth() {
		return Math.random();
	}
	
	
	private double decorrelate(State s, double floor)
		throws Exception
	{
		int minMoves = 1 + C / 5;
		int minProps = 20 + C;
		
		int moves = 0;
		int props = 0;
		
		while (moves < minMoves && props < minProps) {
			Matrix1D meths = s.getMeths();
			int c = random.nextInt(C);
			meths.set(c, sampleMeth());
			
			if (s.likelihood() > floor) {
				s.commit();
				++moves;
			} else {
				s.rollback();
			}
			++props;
		}
		return s.likelihood();
	}
	
}
