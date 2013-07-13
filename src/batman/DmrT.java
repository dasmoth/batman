package batman;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.biojava.bio.program.gff.GFFDocumentHandler;
import org.biojava.bio.program.gff.GFFParser;
import org.biojava.bio.program.gff.GFFRecord;
import org.biojava.bio.program.gff.GFFWriter;
import org.biojava.bio.program.gff.SimpleGFFRecord;
import org.bjv2.util.SmallMap;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

@App(overview="...", generateStub=true)
public class DmrT {

	private double threshold = 3;
	private double effectThreshold = 0;
	private boolean html = false;
	private boolean raw = false;
	private String urlPrefix="http://www.ensembl.org/Homo_sapiens";
	private int htmlBuffer = 2000;
	private boolean noIQR = false;
	
	@Option(help="...", optional=true)
	public void setNoIQR(boolean b) {
		this.noIQR = b;
	}
	
	@Option(help="...", optional=true)
	public void setEffectThreshold(double d) {
		this.effectThreshold = d;
	}
	
	@Option(help="..", optional=true)
	public void setRaw(boolean b) {
		this.raw = b;
	}
	
	@Option(help="DMR-score threshold", optional=true)
	public void setThreshold(double d) {
		this.threshold = d;
	}
	
	@Option(help="Generate web-formatted output", optional=true)
	public void setHtml(boolean b) {
		this.html = b;
	}
	
	@Option(help="Spacing around DMRs in web output", optional=true)
	public void setHtmlBuffer(int i) {
		this.htmlBuffer = i;
	}
	
	@Option(help="...", optional=true)
	public void setUrlPrefix(String s) {
		this.urlPrefix = s;
	}
	
	/**
	 * @param args
	 */
	public void main(String[] args) 
		throws Exception
	{
		Map<String,GFFRecord> set0 = loadGFF(args[0]);
		Map<String,GFFRecord> set1 = loadGFF(args[1]);
		
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
		GFFWriter gffw = new GFFWriter(pw);
		
		if (html) {
			System.out.println("<table border='1' cellpadding='3'>");
			System.out.println("<tr><th>Region</th><th>DMR-T score</th><th>Meth%-set0</th><th>Meth%-set1</th>");
		}
		
		for (String r : set0.keySet()) {
			GFFRecord r0 = set0.get(r);
			GFFRecord r1 = set1.get(r);
			
			if (r0 == null || r1 == null) {
				continue;
			}
			
			if (r0.getStart() != r1.getStart() || !r0.getSeqName().equals(r1.getSeqName())) {
				System.err.println("Record mismatch " + r);
				continue;
			}
			
			double s0 = r0.getScore();
			double s1 = r1.getScore();
			double i0, i1;
			
			if (noIQR) {
				i0 = i1 = 0.1;
			} else {
				i0 = Double.parseDouble(((List<?>) r0.getGroupAttributes().get("batman.iqr")).get(0).toString());
				i1 = Double.parseDouble(((List<?>) r1.getGroupAttributes().get("batman.iqr")).get(0).toString());
			}
			
			if (i0 < 0.00001 || i1 < 0.00001) {
				// where are these coming from?
				continue;
			}
			
			double t = (s0 - s1) / Math.sqrt(i0 * i0 + i1 * i1);
			double e = s0 - s1;
			if (Math.abs(t) > threshold && Math.abs(e) > effectThreshold) {
				if (raw) {
					System.out.printf("%g\t%g\t%g%n", s0, s1, t);
				} else if (html) {
					System.out.printf("<tr><td><a href='%s/contigview?region=%s;vc_start=%d;vc_end=%d'>chr%s:%d-%d</a></td><td>%g</td><td>%d</td><td>%d</td>%n", urlPrefix, r0.getSeqName(), r0.getStart() - htmlBuffer, r0.getEnd() + htmlBuffer, r0.getSeqName(), r0.getStart(), r0.getEnd(), t, (int) (100 * r0.getScore()), (int) (100 * r1.getScore()));
				} else {
					SimpleGFFRecord dmr = new SimpleGFFRecord();
					dmr.setFeature("dmr");
					dmr.setSource("DmrT");
					dmr.setStart(r0.getStart());
					dmr.setEnd(r0.getEnd());
					dmr.setSeqName(r0.getSeqName());
					dmr.setScore(t);
					Map<String,List<String>> gaga = new SmallMap<String, List<String>>();
					gaga.put("meth.sample0", Collections.singletonList("" + s0));
					gaga.put("meth.sample1", Collections.singletonList("" + s1));
					gaga.put("iqr.sample0", Collections.singletonList("" + i0));
					gaga.put("iqr.sample1", Collections.singletonList("" + i1));
					gaga.put("dmrt.abs", Collections.singletonList("" + Math.abs(t)));
					dmr.setGroupAttributes(gaga);
					gffw.recordLine(dmr);
				}
			}
		}
		pw.flush();
		
		if (html) {
			System.out.println("</table>");
		}
	}

	private Map<String,GFFRecord> loadGFF(String fileName)
		throws Exception
	{
		final Map<String,GFFRecord> rl = new TreeMap<String, GFFRecord>();
		new GFFParser().parse(new BufferedReader(new FileReader(fileName)), new GFFDocumentHandler() {
			public void commentLine(String arg0) {
			}

			public void endDocument() {
			}

			public void recordLine(GFFRecord r) {
				rl.put(String.format("%s_%d", r.getSeqName(), r.getStart()), r);
			}

			public void startDocument(String arg0) {
			}
		});
		return rl;
	}
}
