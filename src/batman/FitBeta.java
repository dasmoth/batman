package batman;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.bjv2.util.cli.App;

import batman.utils.CollectTools;
import batman.utils.MathsTools;

import cern.jet.stat.Gamma;

@App(overview="MCMC fit of a Beta distribution to some data", generateStub=true)
public class FitBeta {
	private double[] dat;
	private double expectAlpha = Double.NaN;
	private double expectBeta = Double.NaN;
	private double evidence = Double.NaN;
	
	public FitBeta() {
	}
	
	public FitBeta(double[] dat) {
		this.dat = dat;
	}
	
	public double getEvidence() {
		return evidence;
	}
	
	public double getAlpha() {
		return expectAlpha;
	}
	
	public double getBeta() {
		return expectBeta;
	}
	
	public void main(String[] args)
		throws Exception
	{
		{
			List<Double> dl = new ArrayList<Double>();
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				double d = Double.parseDouble(line);
				if (d > 0.0 && d < 1.0) {
					dl.add(d);
				}
			}
			dat = CollectTools.toDoubleArray(dl);
		}
		
		doFitBeta();
		
		System.out.printf("Alpha=%g%nBeta=%g%nEvidence=%g%n", expectAlpha, expectBeta, evidence);
	}
		
	public void doFitBeta() {
		int ensembleSize = 30;
		List<State> ensemble = new ArrayList<State>();
		for (int n = 0; n < ensembleSize; ++n) {
			ensemble.add(randomState());
		}
		int step = 0;
		double maxWeight = Double.NEGATIVE_INFINITY;
		List<State> sl = new ArrayList<State>();
		List<Double> weights = new ArrayList<Double>();
		double ae = Double.NEGATIVE_INFINITY;
		while (true) {
			++step;
			Collections.sort(ensemble, new Comparator<State>() {
				public int compare(State arg0, State arg1) {
					return MathsTools.sign(arg0.likelihood() - arg1.likelihood());
				}
			});
			State sample = ensemble.remove(0);
			double likelihoodFloor = sample.likelihood();
			
			State repl = ensemble.get(r.nextInt(ensemble.size()));
			int samples = 0;
			// for (int i = 0; i < 5; ++i) {
			while (samples < 3) {
				State r2 = perturbState(repl);
				if (r2.likelihood() > likelihoodFloor) {
					repl = r2;
					++samples;
				}
			}
			ensemble.add(repl);
			double penalty = -Math.log(ensembleSize) + step * Math.log((1.0 * ensembleSize) / (ensembleSize + 1));
			double penalizedLikelihood = penalty + likelihoodFloor;
			ae = addLog(ae, penalizedLikelihood);
			sl.add(sample);
			weights.add(penalizedLikelihood);
			maxWeight = Math.max(maxWeight,  penalizedLikelihood);
			if (maxWeight - penalizedLikelihood > 10) {
				break;
			}
			
			// System.out.printf("%g\t%g\t%g\t%g%n", likelihoodFloor, penalizedLikelihood, sample.alpha, sample.beta);
			// System.err.print('.');
		}
		// System.err.println("Done!");
		
		double totAlpha = 0;
		double totBeta = 0;
		int N = 0;
		
		for (int i = 0; i < sl.size(); ++i) {
			if (Math.random() < Math.exp(weights.get(i) - maxWeight)) {
				totAlpha += sl.get(i).alpha;
				totBeta += sl.get(i).beta;
				++N;
			}
		}
		
		expectAlpha = totAlpha/N;
		expectBeta = totBeta/N;
		evidence = ae;
	}
	
	private State randomState() {
		return new State(Math.exp(Math.random() * 3), Math.exp(Math.random() * 3));
	}
	
	Random r = new Random();
	
	private double addLog(double x, double y) {
		if (x == Double.NEGATIVE_INFINITY) {
			return y;
		} else if (y == Double.NEGATIVE_INFINITY) {
			return x;
		} else {
			double base = Math.max(x, y);
			return base + Math.log(Math.exp(x - base) + Math.exp(y - base));
		}
	}
	
	private double mogSample() {
		double rnd = Math.random();
		if (rnd < 0.3) {
			return r.nextGaussian() * 0.3;
		} else if (rnd < 0.9) {
			return r.nextGaussian() * 0.01;
		} else {
			return r.nextGaussian() * 0.000001;
		}
	}
	
	private State perturbState(State s) {
		do {
			if (Math.random() < 0.5) {
				double la = Math.log(s.alpha);
				la += r.nextGaussian() * mogSample();
				if (la >= 0 && la <= 3) {
					return new State(Math.exp(la), s.beta);
				}
			} else {
				double lb = Math.log(s.beta);
				lb += r.nextGaussian() * mogSample();
				if (lb >= 0 && lb <= 3) {
					return new State(s.alpha, Math.exp(lb));
				}
			}
		} while (true);
	}
	
	private class State {
		public final double alpha;
		public final double beta;
		private double L = Double.NaN;
		
		public State(double alpha, double beta) {
			this.alpha = alpha;
			this.beta = beta;
		}
		
		public double likelihood() {
			if (Double.isNaN(L)) {
				L = 0;
				double norm = Math.log(Gamma.beta(alpha, beta));
				for (double x : dat) {
					L += ((alpha - 1) * Math.log(x)) + ((beta - 1) * Math.log(1 - x)) - norm;
				}
			}
			return L;
		}
	}
}
