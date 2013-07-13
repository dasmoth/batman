package batman;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biojava.bio.program.gff.GFFDocumentHandler;
import org.biojava.bio.program.gff.GFFParser;
import org.biojava.bio.program.gff.GFFRecord;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.LocationTools;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.bio.symbol.SimpleSymbolList;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.seq.BatSeqFetcher;
import batman.utils.CollectTools;
import batman.utils.Collects;
import batman.utils.IOTools;
import batman.utils.MathsTools;

@App(overview="Perform per-array calibration of the Batman model", generateStub=true)
public class Calibrate {
	private String dbURL = null;
	private String dbSeqURL = null;
	private String dbUser;
	private String dbPass = "";
	private String chrName = "6";
	private String array;
	private String couplingProfile = "flat-400-700";
	private boolean gff = false;
	private double bandWidth = 1.0;
	private double cut = 0.2;
	private File writeScatter = null;
	private File writeTrend = null;
	private boolean writeDB = false;
	private int minBand = 10;
	private double minFit = 3.0;
	private double maxFit = 10.0;
	private boolean calcTM = false;
	private Map<String,Location> mask = null;
	
	@Option(help="...", optional=true)
	public void setMask(File f) 
		throws Exception
	{
		final Map<String,List<Location>> maskLocs = new HashMap<String, List<Location>>();
		new GFFParser().parse(IOTools.fileBufferedReader(f), new GFFDocumentHandler() {
			public void commentLine(String arg0) {
			}

			public void endDocument() {
			}

			public void recordLine(GFFRecord r) {
				Collects.pushOntoMap(maskLocs, r.getSeqName(), new RangeLocation(r.getStart(), r.getEnd()));
			}

			public void startDocument(String arg0) {
			}
		});
		mask = new HashMap<String, Location>();
		for (String chr : maskLocs.keySet()) {
			mask.put(chr, LocationTools.union(maskLocs.get(chr)));
		}
	}
	
	@Option(help="...", optional=true)
	public void setCalcTM(boolean b) {
		this.calcTM = b;
	}
	
	@Option(help="Minimum CpG to use to calibration", optional=true)
	public void setMinFit(double d) {
		this.minFit = d;
	}
	
	@Option(help="Maximum CpG to use to calibration", optional=true)
	public void setMaxFit(double d) {
		this.maxFit = d;
	}
	
	@Option(help="Minimum number of probes for a band to qualify for trend calculation", optional=true)
	public void setMinBand(int i) {
		this.minBand = i;
	}
	
	
	@Option(help="Write out the calibration data in raw scatter-plot format", optional=true)
	public void setWriteScatter(File f) {
		this.writeScatter = f;
	}
	
	@Option(help="Write out the calibration data in trend-line format", optional=true)
	public void setWriteTrend(File f) {
		this.writeTrend = f;
	}
	
	@Option(help="Write the calibration parameters back to the Batman DB", optional=true)
	public void setWriteDB(boolean b) {
		this.writeDB = b;
	}
	
	@Option(help="Width of CpG bands (default=1.0)", optional=true)
	public void setBandWidth(double bandWidth) {
		this.bandWidth = bandWidth;
	}
	
	@Option(help="Fraction of data to ignore when calculating trend (default=0.2)", optional=true)
	public void setCut(double cut) {
		this.cut = cut;
	}
	
	@Option(help="Write scatter-plot data in GFF format", optional=true)
	public void setGff(boolean b) {
		this.gff = b;
	}
	
	@Option(help="Name of a coupling profile to use", optional=true)
	public void setCouplingProfile(String couplingProfile) {
		this.couplingProfile = couplingProfile;
	}
	
	@Option(help="Connection details for an alternative Batman DB to use when retrieving reference sequence data", optional=true)
	public void setDbSeqURL(String dbURL) {
		this.dbSeqURL = dbURL;
	}
	
	@Option(help="Password for connecting to the Batman DB", optional=true)
	public void setDbPass(String dbPass) {
		this.dbPass = dbPass;
	}
	

	@Option(help="Connection details for the Batman DB")
	public void setDbURL(String dbURL) {
		this.dbURL = dbURL;
	}

	@Option(help="Username for connecting to the Batman DB")
	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}
	
	@Option(help="Name of a chromosome to use for calibration purposes", optional=true)
	public void setChr(String s) {
		this.chrName = s;
	}
	
	@Option(help="Name of the experiment to calibrate")
	public void setExpt(String s) {
		this.array = s;
	}
	
	private double tm(SymbolList sl)
	{
		int gc = 0;
		int length = sl.length();
		for (int i = 1 ; i <= length; ++i) {
			Symbol s = sl.symbolAt(i);
			if (s == C || s == G) {
				++gc;
			}
		}
		double percGC = (100.0 * gc) / length;
		
		
		return 81.5 + /* 16.6 * Math.log(naConc) */ + 0.41 * percGC - (600.0 / length);
	}

	private final Symbol C = DNATools.c();
	private final Symbol G = DNATools.g();
	
	private static class ProbeData {
		public String name;
		public double logMeth;
		public double cpgScore;
		public int bin;
	}
	
	public void main(String[] args)
		throws Exception
	{
		Connection db = JDBCPooledDataSource.getDataSource(
				"org.gjt.mm.mysql.Driver",
				dbURL,
				dbUser, 
				dbPass
		).getConnection();
		Connection seqDB = db;
		if (dbSeqURL != null) {
		    seqDB = JDBCPooledDataSource.getDataSource(
					"org.gjt.mm.mysql.Driver",
					dbSeqURL,
					dbUser, 
					dbPass
			).getConnection();
		}
		
		double[] couplingProfile;
		{
			PreparedStatement get_coupling = db.prepareStatement("select offset, coupling from xmeth_coupling_profile where profile_name = ? order by offset");
			get_coupling.setString(1, this.couplingProfile);
			List<Double> cl = new ArrayList<Double>();
			ResultSet rs = get_coupling.executeQuery();
			while (rs.next()) {
				int offset = rs.getInt(1);
				double coupling = rs.getDouble(2);
				if (offset != cl.size()) {
					throw new Exception("FIXME: invalid coupling profile: " + this.couplingProfile);
				}
				cl.add(coupling);
			}
			rs.close();
			get_coupling.close();
			couplingProfile = CollectTools.toDoubleArray(cl);
		}
		
		
		
		int arrayId;
		{
			PreparedStatement get_aid = db.prepareStatement("select id from medip_expt where expt_name = ?");
			get_aid.setString(1, array);
			ResultSet rs = get_aid.executeQuery();
			if (!rs.next()) {
				throw new Exception(String.format("Couldn't find array `%s'", array));
			}
			arrayId = rs.getInt(1);
			rs.close();
			get_aid.close();
		}
		
		PreparedStatement get_medip;
		
		if ("*".equals(chrName)) {
			get_medip = db.prepareStatement(
					"select p.chr, p.min_pos, p.max_pos, d.log_ratio, p.probe_name, r.roi_name from medip_probe as p, medip_data as d, medip_roi as r where d.probe = p.id and d.expt = ? and r.id = p.roi"
				);
				get_medip.setInt(1, arrayId);
		} else {
			get_medip = db.prepareStatement(
				"select p.chr, p.min_pos, p.max_pos, d.log_ratio, p.probe_name, r.roi_name from medip_probe as p, medip_data as d, medip_roi as r  where d.probe = p.id and p.chr = ? and d.expt = ? and r.id = p.roi"
			);
			get_medip.setString(1, chrName);
			get_medip.setInt(2, arrayId);
		}
		ResultSet rs = get_medip.executeQuery();
		List<ProbeData> data = new ArrayList<ProbeData>();
		int maxBin = 0;
		PrintWriter scatterWriter = null;
		if (writeScatter != null) {
			scatterWriter = new PrintWriter(new FileWriter(writeScatter));
		}
		Map<String,Sequence> chrs = new HashMap<String, Sequence>();
		while (rs.next()) {
			String pChrName = rs.getString(1);
			int minP = rs.getInt(2);
			int maxP = rs.getInt(3);
			double medip = rs.getDouble(4);
			String probeName = rs.getString(5);
			String roiName = rs.getString(6);
			
			if (mask != null) {
				Location mloc = mask.get(pChrName);
				// System.err.printf("On chr%s, mcov=%d%n", pChrName, LocationTools.coverage(mloc));
				if (mloc == null || !LocationTools.overlaps(new RangeLocation(minP, maxP), mloc)) {
					// System.err.println("Rejecting " + probeName);
					continue;
				}
			}
			// System.err.println("Accepting " + probeName);
			
			Sequence chr;
			if (chrs.containsKey(pChrName)) {
				chr = chrs.get(pChrName);
			} else {
				chr = BatSeqFetcher.getSequence(seqDB, pChrName);
				chrs.put(pChrName, chr);
			}
			
			int wMin = minP - couplingProfile.length + 1;
			int wMax = maxP + couplingProfile.length - 1;
			if (wMin < 1 || wMax >= chr.length()) {
				System.err.println("Rejecting due to boundary");
				continue;
			}
			double totC = 0;
			for (int p = wMin; p <= wMax; ++p) {
				if (chr.symbolAt(p) == C && chr.symbolAt(p + 1) == G) {
					int dist;
					if (p < minP) {
						dist = minP - p;
					} else if (p > maxP) {
						dist = p - maxP;
					} else {
						dist = 0;
					}
					totC += couplingProfile[dist];
				}
			}
			
			if (scatterWriter != null) {
				if (!gff) {
					scatterWriter.printf("%g\t%g\t%s%n", medip, totC, probeName);
				} else {
					if (calcTM) {
						SymbolList probeSeq = new SimpleSymbolList(chr.subList(minP, maxP));
						scatterWriter.printf("%s\tbatman\tcalibration\t%d\t%d\t.\t.\t.\tA %g;C %g; probe %s; ROI %s; Tm %g; seq %s%n", pChrName, minP, maxP, medip, totC, probeName, roiName, tm(probeSeq), probeSeq.seqString());
					} else {
						scatterWriter.printf("%s\tbatman\tcalibration\t%d\t%d\t.\t.\t.\tA %g;C %g; probe %s; ROI %s%n", pChrName, minP, maxP, medip, totC, probeName, roiName);
					}
				}
			}
			
			ProbeData pd = new ProbeData();
			pd.logMeth = medip;
			pd.cpgScore = totC;
			pd.bin = (int) Math.floor(pd.cpgScore / bandWidth);
			maxBin = Math.max(maxBin, pd.bin);
			data.add(pd);	
		}
		rs.close();
		get_medip.close();
		
		if (scatterWriter != null) {
			scatterWriter.close();
		}
		
		List<ProbeData>[] bins = new List[maxBin + 1];
		for (int i = 0; i < bins.length; ++i) {
			bins[i] = new ArrayList<ProbeData>();
		}
		for (ProbeData pd : data) {
			bins[pd.bin].add(pd);
		}
		
		PrintWriter trendWriter = null;
		if (writeTrend != null) {
			System.out.printf("Wrote scatterplot%n");
			trendWriter = new PrintWriter(new FileWriter(writeTrend));
		}
		
		List<Double> cl = new ArrayList<Double>();
		List<Double> al = new ArrayList<Double>();
		
		for (int b = 0; b < maxBin; ++b) {
			List<ProbeData> band = bins[b];
			if (band.size() >= minBand) {
				Collections.sort(band, new Comparator<ProbeData>() {
					public int compare(ProbeData arg0, ProbeData arg1) {
						return MathsTools.sign(arg0.logMeth - arg1.logMeth);
					}
				});
				
				int minSig = (int) Math.floor(band.size() * cut);
				int maxSig = (int) Math.floor(band.size() * (1.0 - cut));
				double tot = 0;
				for (int x = minSig; x <= maxSig; ++x) {
					tot += band.get(x).logMeth;
				}
				double mean = tot / (maxSig - minSig + 1);
				double c = (0.5 + b) * bandWidth;
				if (trendWriter != null) {
					trendWriter.printf("%g\t%g%n", mean, c);
				}
				if (c >= minFit && c <= maxFit ) {
					al.add(mean);
					cl.add(c);
				}
			}
		}
		
		if (trendWriter != null) {
			trendWriter.close();
		}
		
		double sumC = 0, sumA = 0, sumAA = 0, sumAC = 0;
		int n = cl.size();
		for (int i = 0; i < n; ++i) {
			double a = al.get(i);
			double c = cl.get(i);
			sumC += c;
			sumA += a;
			sumAA += (a * a);
			sumAC += (a * c);
		}
		double beta = ((sumAC * n) - (sumA * sumC)) / ((sumAA * n) - (sumA * sumA));
		double alpha = ((sumC - (beta * sumA)) / n);
		double base = -alpha/beta;
		
		System.out.printf("Baseline=%g     Response=%g%n", base, beta);
		
		if (writeDB) {
			PreparedStatement insert_arrayMeta = db.prepareStatement("insert into xmeth_array_meta (expt, response, baseline) values (?, ?, ?)");
			insert_arrayMeta.setInt(1, arrayId);
			insert_arrayMeta.setDouble(2, beta);
			insert_arrayMeta.setDouble(3, base);
			insert_arrayMeta.executeUpdate();
			insert_arrayMeta.close();
			System.out.printf("Database was updated%n");
		}
		
		db.close();
	}

}
