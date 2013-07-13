package batman;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.matrix.Matrix1D;
import batman.utils.Collects;
import batman.utils.IOTools;

@App(overview="...", generateStub=true)
public class SummarizeROIMeans {
	private String dbURL = null;
	private String dbUser = null;
	private String dbPass = "";
	private boolean trimToROIs = false;
	
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
	
	@Option(help="Trim to ROI or tiled-region boundaries", optional=true)
	public void setTrim(boolean b) {
		this.trimToROIs = b;
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
		Map<String,List<RoiRecord>> rois = new HashMap<String, List<RoiRecord>>();
		if (trimToROIs) {
			if (dbURL == null) {
				System.err.println("Used -trim without specifying database options");
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
		XMLStreamReader r = factory.createXMLStreamReader(IOTools.fileReader(new File(args[0])));
		RoiProfileReader rpr = new RoiProfileReader(r);
		while (rpr.hasNext()) {
			RoiProfile rp = rpr.next();
			String rpName = rp.name;
			System.err.println(">" + rpName);
			int rStart = rp.cgs[0].pos;
			int rEnd = rp.cgs[rp.cgs.length - 1].pos;
			
			if (trimToROIs) {
				int midPoint = rp.cgs[rp.cgs.length / 2].pos;
				for (RoiRecord rr : rois.get(rp.cgs[0].chr)) {
					if (rr.min <= midPoint && rr.max >= midPoint) {
						rStart = rr.min;
						rEnd = rr.max;
						break;
					}
				}
			}
			
			List<Double> means = new ArrayList<Double>();
			for (Matrix1D sample : rp.samples) {
				double tot = 0;
				int qual = 0;
				for (int c = 0; c < rp.cgs.length; ++c) {
					if (rp.cgs[c].pos >= rStart && rp.cgs[c].pos <= rEnd) {
						tot += sample.get(c);
						++qual;
					}
				}
				if (qual > 0) {
					means.add(tot / qual);
				}
			}
			
			if (means.size() > 0) {
				Collections.sort(means);
				int ml = means.size();
				System.out.printf("%s\t%s\t%d\t%d\t%g\t%g\t%g\t%g\t%s%n", rpName, rp.cgs[0].chr, rStart, rEnd, means.get((int) (0.5 * ml)), means.get((int) (0.05 * ml)), means.get((int) (0.95 * ml)), means.get((int) (0.75 * ml)) - means.get((int) (0.25 * ml)), join(means, ","));
			}
		}
	}
	
	private String join(List<? extends Object> l, String sep) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<?> i = l.iterator(); i.hasNext(); ) {
			sb.append(i.next().toString());
			if (i.hasNext()) {
				sb.append(sep);
			}
		}
		return sb.toString();
	}
}
