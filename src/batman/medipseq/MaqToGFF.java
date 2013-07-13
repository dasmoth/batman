package batman.medipseq;

import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.utils.AbstractLineProcessor;

@App(overview="Filter maq mapview output and conver to GFF", generateStub=true)
public class MaqToGFF extends AbstractLineProcessor {
	private int minConfidence = 10;
	
	@Option(help="Minimum mapping confidence to accept")
	public void setMinConfidence(int i) {
		this.minConfidence = i;
	}
	
	public void processTokens(String[] toks)
	{
		String seqName = toks[1];
		int start = Integer.parseInt(toks[2]);
		String strand = toks[3];
		int qual = Integer.parseInt(toks[6]);
		int len = Integer.parseInt(toks[13]);
		
		if (qual >= minConfidence) {
			System.out.printf("%s\tread\tmaq\t%d\t%d\t.\t%s\t.%n", seqName, start, start + len - 1, strand);
		}
	}
}
