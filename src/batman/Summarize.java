package batman;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.SmallSet;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.matrix.Matrix1D;
import batman.utils.Collects;
import batman.utils.IOTools;
import batman.utils.MathsTools;

@App(overview="...", generateStub=true)
public class Summarize {
	private String dbURL = null;
	private String dbUser = null;
	private String dbPass = "";
	private int windowCore = 100;
	private int windowFlank = 100;
	private boolean trimToROIs = false;
	private boolean trim = false;
	private int tinyTrim = 10;
	private int slide = -1;
	
	@Option(help="Password for connecting to the Batman DB", optional=true)
	public void setDbPass(String dbPass) {
		this.dbPass = dbPass;
	}

	@Option(help="Connection details for the Batman DB", optional=true)
	public void setDbURL(String dbURL) {
		this.dbURL = dbURL;
	}

	@Option(help="Username for connecting to the Batman DB", optional=true)
	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}
	
	@Option(help="Trim to ROI or tiled-region boundaries (not compatible with Batman 0.2)", optional=true)
	public void setTrim(boolean b) {
		this.trim = b;
	}
	
	@Option(help="Historical trimming behaviour (works on old sample-sets, requires database connection)", optional=true)
	public void setOldTrim(boolean b) {
		this.trimToROIs = b;
	}
	
	
	@Option(help="Sliding window core size (default 100)", optional=true)
	public void setWindow(int i) {
		this.windowCore = i;
	}
	
	@Option(help="Sliding window flank size (default 100)", optional=true)
	public void setWindowFlank(int i) {
		this.windowFlank = i;
	}
	
	@Option(help="Sliding window step (default: equal to -window)", optional=true)
	public void setStep(int i) {
		this.slide = i;
	}
	
	private static class RoiRecord {
		public final String chr;
		public final int min;
		public final int max;
		public final String roi;
		
		public RoiRecord(String chr, int min, int max, String roi) {
			this.chr = chr;
			this.min = min;
			this.max = max;
			this.roi = roi;
		}
	}
	
	public void main(String[] args)
		throws Exception
	{
		final Pattern ANA_TILE_PATTERN = Pattern.compile("([0-9A-Za-z_]+):([0-9]+)-([0-9]+)");
		
		if (slide < 0) {
			slide = windowCore;
		}
		
		Map<String,List<RoiRecord>> rois = new HashMap<String, List<RoiRecord>>();
		if (trimToROIs) {
			if (dbURL == null) {
				System.err.println("Used -oldTrim without specifying database options");
				return;
			}
			
			Connection hepdb = JDBCPooledDataSource.getDataSource(
					"org.gjt.mm.mysql.Driver",
					dbURL,
					dbUser, 
					dbPass
			).getConnection();
			
			
			PreparedStatement get_rois = hepdb.prepareStatement(
					"select p.chr, min(p.min_pos), max(p.max_pos), r.roi_name from medip_probe as p, medip_roi as r where p.roi = r.id group by r.roi_name, p.chr"
			);
			ResultSet rs = get_rois.executeQuery();
			while (rs.next()) {
				String chr = rs.getString(1);
				int min = rs.getInt(2);
				int max = rs.getInt(3);
				String roi = rs.getString(4);
				if (max > min) {
					Collects.pushOntoMap(rois, chr, new RoiRecord(chr, min, max, roi));
				}
			}
		}
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader r = factory.createXMLStreamReader(IOTools.inputReader(args));
		RoiProfileReader rpr = new RoiProfileReader(r);
		while (rpr.hasNext()) {
			RoiProfile rp = rpr.next();
			String rpName = rp.name;
			System.err.println(">" + rpName);
			int rStart = rp.cgs[0].pos;
			int rEnd = rp.cgs[rp.cgs.length - 1].pos;
			
			Matcher m = ANA_TILE_PATTERN.matcher(rpName);
			if (m.matches()) {
				rpName = m.group(1);
				rStart = Integer.parseInt(m.group(2));
				rEnd = Integer.parseInt(m.group(3));
			}
			
			for (int wStart = rStart; wStart + windowCore - 1 <=rEnd; wStart += slide) {
				int wEnd = Math.min(rEnd, wStart + windowCore - 1);
				
				int xwStart = Math.max(wStart - windowFlank, rStart);
				int xwEnd = Math.min(wEnd + windowFlank, rEnd);
				
				Set<Integer> windices = new SmallSet<Integer>();
				for (int c = 0; c < rp.cgs.length; ++c) {
					CGRecord cgr = rp.cgs[c];
					if (cgr.pos >= xwStart && cgr.pos <= xwEnd) {
						windices.add(c);
					}
				}
				
				List<Double> vals = new ArrayList<Double>();
				for (Matrix1D sample : rp.samples) {
					double v = 0;
					double w = 0;
					for (int c : windices) {
						double ww = rp.cgs[c].weight;
						w += ww;
						v += ww * sample.get(c);
					}
					if (w > 0) {
						vals.add(MathsTools.bound(0.000001, v / w, 0.999999));
					}
				}
				
				int repStart = wStart, repEnd = wEnd;

				if (vals.size() >= 20) {
					Collections.sort(vals);
					double lowBound = vals.get((int) (0.25 * vals.size()));
					double median = vals.get((int) (0.5 * vals.size()));
					double highBound = vals.get((int) (0.75 * vals.size()));
					
					String chr = rp.cgs[0].chr;
					int outMin = repStart;
					int outMax = repEnd;
					boolean elide = false;
					
					if (trimToROIs) {
						elide = true;
						RoiRecord roi = null;
						List<RoiRecord> roil = rois.get(chr);
						if (roil != null) {
							for (RoiRecord rr : roil) {
								if (outMin <= rr.max && outMax >= rr.min) {
									roi = rr; break;
								}
							}
						}
						
						if (roi != null) {
							int min = Math.max(roi.min, outMin);
							int max = Math.min(roi.max, outMax);
							if ((max - min + 1) > tinyTrim) {
								elide = false;
							}
						}
					}
					
					if (trim) {
						if (rp.min < 0) {
							throw new Exception("Cannot use -trim with old sample-sets");
						}
						outMin = Math.max(rp.min, outMin);
						outMax = Math.min(rp.max, outMax);
						if ((outMax - outMin + 1) <= tinyTrim) {
							elide = true;
						}
					}
					
					if (!elide) {
						System.out.printf("%s\tbatman\tmeth\t%d\t%d\t%g\t.\t.\tROI \"%s\"; batman.iqr %g%n", chr, outMin, outMax, median, rpName, (highBound - lowBound));
					}
				}
				System.err.print('.');
			}
			System.err.println();
		}
	}
}
