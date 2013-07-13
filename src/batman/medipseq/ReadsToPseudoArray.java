package batman.medipseq;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.biojava.bio.program.gff.GFFDocumentHandler;
import org.biojava.bio.program.gff.GFFParser;
import org.biojava.bio.program.gff.GFFRecord;
import org.biojava.bio.program.gff.GFFWriter;
import org.biojava.bio.program.gff.SimpleGFFRecord;
import org.biojava.bio.seq.StrandedFeature;
import org.bjv2.util.SmallMap;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.utils.IOTools;

@App(overview="Convert solexa data to pseudo-tiling-array data", generateStub=true)
public class ReadsToPseudoArray {
	private int base = 50;
	private String roiIdPattern = "CHR%s";
	private String probeIdPattern = "%s_%010d";
	private int outputThreshold = Integer.MIN_VALUE;
	private String target = null;
	private boolean includeProbeId = true;
	private boolean includeRoiId = true;
	private int fragmentLength = -1;
	private int fragmentExtension = -1;
	
	@Option(help="Estimated fragment length when using single-ended reads", optional=true)
	public void setFragmentLength(int i) {
		this.fragmentLength = i;
	}
	
	@Option(help="Extended fragment length to use in spatial-smoothing set (for Batman, we currently recommend 500)", optional=true)
	public void setFragmentExtension(int i) {
		this.fragmentExtension = i;
	}
	
	@Option(help="Include generated probe-IDs in the PAD data (required for Batman)", optional=true)
	public void setIncludeProbeId(boolean b) {
		this.includeProbeId = b;
	}
	
	@Option(help="Include generated ROI-IDs in the PAD data (required for Batman)", optional=true)
	public void setIncludeRoiId(boolean b) {
		this.includeProbeId = b;
	}
	
	@Option(help="Sequence to process")
	public void setTarget(String s) {
		this.target = s;
	}
	
	@Option(help="Minimum read-count to include in output (do not use with Batman)", optional=true)
	public void setOutputThreshold(int i) {
		this.outputThreshold = i;
	}
	
	@Option(help="Spacing of probes", optional=true)
	public void setBase(int base) {
		this.base = base;
	}

	@Option(help="Formatting string for auto-generated probe IDs", optional=true)
	public void setProbeIdPattern(String probeIdPattern) {
		this.probeIdPattern = probeIdPattern;
	}

	/**
	 * @param args
	 */
	public void main(String[] args) 
		throws Exception
	{
		final String roiID  = String.format(roiIdPattern, target);
		
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
		final GFFWriter gffw = new GFFWriter(pw);
		new GFFParser().parse(IOTools.inputBufferedReader(args), new GFFDocumentHandler() {
			int[] counts = new int[300000000];
			int maxPos = -1;
			String chr = null;
			
			public void commentLine(String comment) {
			}

			public void endDocument() {
				SimpleGFFRecord r = new SimpleGFFRecord();
				r.setFeature("probe");
				r.setSource("ReadsToPAD");
				r.setSeqName(chr);
				for (int p = 1; p <= maxPos; p += base) {
					if (counts[p] >= outputThreshold) {
						r.setStart(p);
						r.setEnd(p);
						r.setScore(counts[p]);
						Map<String,List<String>> gaga = new SmallMap<String, List<String>>();
						if (includeProbeId) {
							gaga.put("probe", Collections.singletonList(String.format(probeIdPattern, roiID, p)));
						}
						if (includeRoiId) {
							gaga.put("ROI", Collections.singletonList(roiID));
						}
						r.setGroupAttributes(gaga);
						gffw.recordLine(r);
					}
				}
			}

			public void recordLine(GFFRecord record) {
				String cc = record.getSeqName();
				if (cc.startsWith("chr")) {
					cc = cc.substring(3);
				}
				
				if (target != null && !target.equals(cc)) {
					return;
				}
				
				if (chr == null) {
					chr = cc;
				} else if (!chr.equals(cc)) {
					throw new RuntimeException("ReadsToPseudoArray can only handle one chromosome at a time");
				}
				
				int readMin = record.getStart();
				int readMax = record.getEnd();
				if (fragmentLength > 0) {
					if (record.getStrand() != StrandedFeature.NEGATIVE) {
						readMax = readMin + fragmentLength - 1;
					} else {
						readMin = readMax - fragmentLength + 1;
					}
				}
				
				if (fragmentExtension > 0) {
					readMin = (readMin + readMax - fragmentExtension) / 2;
					readMax = readMin + fragmentExtension - 1;
				}
				
				readMin = Math.max(1, readMin);
				
				maxPos = Math.max(maxPos, readMax);
				for (int pos = Math.max(1, readMin); pos <= readMax; ++pos) {
					++counts[pos];
				}
			}

			public void startDocument(String locator) {
			}
			
		});
		pw.flush();
	}
}
