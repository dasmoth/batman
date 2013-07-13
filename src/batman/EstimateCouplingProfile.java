package batman;

import java.util.Random;

import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

@App(overview="Probe-coupling simulation", generateStub=true)
public class EstimateCouplingProfile {
	private int minLength = 400;
	private int maxLength = 700;
	private int reps = 1000000;
	private int probeLength = 50;
	
	@Option(help="Maximum length of fragment to sample", optional=true)
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	@Option(help="Minimum length of fragment to sample", optional=true)
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	@Option(help="Length of a typical array probe (set to 1 for MeDIP-seq)", optional=true)
	public void setProbeLength(int probeLength) {
		this.probeLength = probeLength;
	}

	@Option(help="Number of samples to simulate", optional=true)
	public void setSamples(int reps) {
		this.reps = reps;
	}


	public void main(String[] args)
		throws Exception
	{
		Random r = new Random();
		
		int basis = maxLength + 100;
		int[] counts = new int[basis * 2 + 1];
		
		
		for (int rep = 0; rep < reps; ++rep) {
			int ori = -10000 - r.nextInt(1000);
			while (ori <= 0) {
				int end = ori + minLength + (maxLength > minLength ? r.nextInt(maxLength - minLength) : 0);
				if (end > probeLength) {
					for (int x = ori; x <= end; ++x) {
						++counts[x + basis];
					}
				}
				ori = end + 1;
			}
		}
		
		for (int x  = 0; x < counts.length; ++x) {
			if (counts[x] > 0) {
				System.out.printf("%d\t%d\t%g%n", x - basis, counts[x], (1.0 * counts[x]) / counts[basis]);
			}
		}
	}
}
