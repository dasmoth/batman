package batman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.db.HashSequenceDB;
import org.biojava.bio.seq.db.SequenceDB;
import org.biojava.bio.seq.impl.SimpleSequence;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.utils.JDBCPooledDataSource;
import org.biojava.utils.xml.PrettyXMLWriter;
import org.biojava.utils.xml.XMLWriter;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.matrix.Matrix1D;
import batman.matrix.Matrix2D;
import batman.matrix.SimpleMatrix2D;
import batman.model.MethModel;
import batman.model.MethModelAssymetric;
import batman.model.Rectified;
import batman.seq.BatSeqFetcher;
import batman.utils.CollectTools;

import cern.colt.list.DoubleArrayList;

@App(overview="Sample from Batman model for a group of probes", generateStub=true)
public class SampleMethStates {

	private String dbURL = null;
	private String dbSeqURL = null;
	private String dbUser = null;
	private String dbPass = "";
	private SequenceDB seqdb;
	private String couplingProfile = "flat-400-700";
	private String[] arrays;
	private String[] tissue;
	private File roiSamples;
	private String tilepathSamples;
	private int maxSamples = 100;
	private int proxySpacing = 50;
	private int tilePathTile = 300;
	private int tilePathStep = 300;
	private int tilePathFlank = 800;
	private double minValidRatio = Double.NEGATIVE_INFINITY;
	private double maxValidRatio = Double.POSITIVE_INFINITY;
	private boolean oneChannel = false;
	private boolean mseqHack = false;
	private int trimEnds = 0;
	private double precision = 10;
	private double signalMultiplier = 1.0;
	private int nsCycleMax = 20000;
	private int nsCycleMin = 2000;
	private int nsEnsembleSize = 100;
	private double forceResponse = Double.NaN;
	private double forceBaseline = Double.NaN;
	
	@Option(help="...", optional=true)
	public void setForceResponse(double d) {
		this.forceResponse = d;
	}
	
	@Option(help="...", optional=true)
	public void setForceBaseline(double d) {
		this.forceBaseline = d;
	}
	
	@Option(help="Nested sampling cycle limit", optional=true)
	public void setNsCycleMax(int i) {
		this.nsCycleMax = i;
	}
	
	@Option(help="Nested sampling cycle limit", optional=true)
	public void setNsCycleMin(int i) {
		this.nsCycleMin = i;
	}
	
	@Option(help="Nested ensemble size", optional=true)
	public void setNsEnsembleSize(int i) {
		this.nsEnsembleSize = i;
	}
	
	@Option(help="Constant to scale signal", optional=true)
	public void setSignalMultiplier(double d) {
		this.signalMultiplier = d;
	}
	
	@Option(help="Estimated inverse variance of array noise", optional=true)
	public void setPrecision(double d) {
		this.precision = d;
	}
	
	@Option(help="Trim ends of tilepath regions", optional=true)
	public void setTrimEnds(int i) {
		this.trimEnds = i;
	}
	
	@Option(help="Custom analysis for MeDIPSeq PAD", optional=true)
	public void setMseqHack(boolean b) {
		this.mseqHack = b;
	}
	
	@Option(help="Custom analysis for one-channel data", optional=true)
	public void setOneChannel(boolean b) {
		this.oneChannel = b;
	}
	
	@Option(help="...", optional=true)
	public void setMinValidRatio(double d) {
		this.minValidRatio = d;
	}
	
	@Option(help="...", optional=true)
	public void setMaxValidRatio(double d) {
		this.maxValidRatio = d;
	}

	@Option(help="...", optional=true)
	public void setDbSeqURL(String dbURL) {
		this.dbSeqURL = dbURL;
	}
	
	@Option(help="Tile size to use when analysing tilepaths.", optional=true)
	public void setTilepathTile(int i) {
		this.tilePathTile = i;
	}
	
	@Option(help="Step size to use when analysing tilepaths (should normally be equal to -tilepathTile)", optional=true)
	public void setTilepathStep(int i) {
		this.tilePathStep = i;
	}
	
	@Option(help="Amount of flanking sequence to include in tilepath analysis", optional=true)
	public void setTilepathFlank(int i) {
		this.tilePathFlank = i;
	}
	
	@Option(help="Analyse a tilepath ROI", optional=true)
	public void setTilepathSamples(String s) {
		this.tilepathSamples = s;
	}
	
	@Option(help="...", optional=true)
	public void setProxySpacing(int i ) {
		this.proxySpacing = i;
	}
	
	@Option(help="Generate samples for a list of ROIs", optional=true)
	public void setRoiSamples(File f) {
		this.roiSamples = f;
	}
	
	@Option(help="Name of the coupling profile to use", optional=true)
	public void setCouplingProfile(String couplingProfile) {
		this.couplingProfile = couplingProfile;
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


	@Option(help="...", optional=true)
	public void setSeqFile(File seqFile) 
		throws Exception
	{
		seqdb = new HashSequenceDB();
		for (SequenceIterator si = SeqIOTools.readFastaDNA(new BufferedReader(new FileReader(seqFile))); si.hasNext(); ) {
			Sequence seq = si.nextSequence();
			if (seq.getName().startsWith("chromosome:")) {
				// icky e!name
				String properName = seq.getName().split(":")[2];
				seq = new SimpleSequence(seq, null, properName, Annotation.EMPTY_ANNOTATION);
				System.err.printf("Renaming e!seq %s%n", properName);
			}
			seqdb.addSequence(seq);
		}
	}

	@Option(help="Experiment to analyse", optional=true)
	public void setExpt(String[] s) {
		this.arrays = s;
	}
	
	@Option(help="Tissue to analyse", optional=true)
	public void setTissue(String[] s) {
		this.tissue = s;
	}
	
	private static class ProbeRecord {
		public final String probeName;
		public final int probeId;
		public final String probeGroup;
		public final String chr;
		public final int min;
		public final int max;
		
		public ProbeRecord(String probeName, int probeId, String probeGroup, String chr, int min, int max) {
			this.probeName = probeName;
			this.probeId = probeId;
			this.probeGroup = probeGroup;
			this.chr = chr;
			this.min = min;
			this.max = max;
		}
	}
	
	private static class AnnotatedModel {
		public final MethModel model;
		public final CGRecord[] cgs;
		public final String chr;
		public final int mmin;
		public final int mmax;
		
		public AnnotatedModel(MethModel model, CGRecord[] cgs, String chr, int mmin, int mmax) {
			this.model = model;
			this.cgs = cgs;
			
			this.chr = chr;
			this.mmin = mmin;
			this.mmax = mmax;
		}
	}
	
	private AnnotatedModel makeModel(Connection hepdb, Connection hepdbc, String probeGroup)
		throws Exception
	{
		return makeModel(hepdb, hepdbc, probeGroup, null, -1, -1);
	}
	
	private AnnotatedModel makeModel(Connection hepdb, Connection seqdbc, String probeGroup, String regionChr, int regionMin, int regionMax)
		throws Exception
	{
		double[] couplingProfile;
		{
			PreparedStatement get_coupling = hepdb.prepareStatement("select offset, coupling from xmeth_coupling_profile where profile_name = ? order by offset");
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
		
		List<ProbeRecord> probes = new ArrayList<ProbeRecord>();
		{
			PreparedStatement get_probes;
			if (regionChr == null) {
				get_probes = hepdb.prepareStatement("select p.id, p.probe_name, p.chr, p.min_pos, p.max_pos from medip_probe as p, medip_roi as r where p.roi = r.id and r.roi_name = ? order by p.min_pos");
				get_probes.setString(1, probeGroup);
			} else {
				get_probes = hepdb.prepareStatement("select p.id, p.probe_name, p.chr, p.min_pos, p.max_pos from medip_probe as p, medip_roi as r where p.roi = r.id and r.roi_name = ? and p.chr = ? and p.min_pos < ? and p.min_pos > ? and p.max_pos > ? order by p.min_pos");
				get_probes.setString(1, probeGroup);
				get_probes.setString(2, regionChr);
				get_probes.setInt(3, regionMax);
				get_probes.setInt(4, regionMin - 100);
				get_probes.setInt(5, regionMin);
			}
			
			ResultSet rs = get_probes.executeQuery();
			while (rs.next()) {
				int pid = rs.getInt(1);
				String pname = rs.getString(2);
				String chr = rs.getString(3);
				int min = rs.getInt(4);
				int max = rs.getInt(5);
				probes.add(new ProbeRecord(pname, pid, probeGroup, chr, min, max));
			}
			rs.close();
			get_probes.close();
		}
		
		if (probes.size() == 0) {
			return null;
		}
		
		int pgMin = probes.get(0).min;
		int pgMax = probes.get(probes.size() - 1).max;
		
		Sequence seq;
		int seqOri;
		if (seqdb != null) {
			seq = seqdb.getSequence(probes.get(0).chr);
			seqOri = 1;
		} else {
			seq = BatSeqFetcher.getSequence(seqdbc, probes.get(0).chr);
			seqOri = 1;
		}
		
		List<CGRecord> cgs = new ArrayList<CGRecord>();
		{
			int cgSearchMin = Math.max(1, pgMin - couplingProfile.length);
			int cgSearchMax = Math.min(seq.length() - 1, pgMax + couplingProfile.length);
			for (int p = cgSearchMin; p <= cgSearchMax; ++p) {
				int rp = p + 1 - seqOri;
				if (seq.symbolAt(rp) == DNATools.c() && seq.symbolAt(rp + 1) == DNATools.g()) {
					cgs.add(new CGRecord(seq.getName(), p));
				}
			}
		}
		
		if (cgs.size() == 0) {
			return null;
		}
		
		System.err.printf("Coupled %d CpGs to %d probes%n", cgs.size(), probes.size());
		
		Matrix2D coupleMatrix;
		if (proxySpacing <= 1) {
			coupleMatrix = new SimpleMatrix2D(probes.size(), cgs.size());
			for (int p = 0; p < probes.size(); ++p) {
				ProbeRecord pr = probes.get(p);
				for (int c = 0; c < cgs.size(); ++c) {
					CGRecord cr = cgs.get(c);
					int dist;
					if (cr.pos < pr.min) {
						dist = pr.min - cr.pos;
					} else if (cr.pos > pr.max) {
						dist = cr.pos - pr.max;
					} else {
						dist = 0;
					}
					if (dist >= 0 && dist < couplingProfile.length) {
						coupleMatrix.set(p, c, couplingProfile[dist]);
					}
				}
			}
		} else {
			int proxyGatherMin = proxySpacing / 2 - 1;
			int proxyGatherMax = proxySpacing / 2;
			
			List<CGRecord> proxyCgs = new ArrayList<CGRecord>();
			for (int p = pgMin - couplingProfile.length - proxySpacing + 1; p <= pgMax + couplingProfile.length; p += proxySpacing) {
				double w = 0;
				for (CGRecord c : cgs) {
					if (c.pos >= p - proxyGatherMin && c.pos < p + proxyGatherMax) {
						w += c.weight;
					}
				}
				proxyCgs.add(new CGRecord(probes.get(0).chr, p, w));
			}
			
			System.err.printf("Defined %d proxies%n", proxyCgs.size());
			
			coupleMatrix = new SimpleMatrix2D(probes.size(), proxyCgs.size());
			for (int p = 0; p < probes.size(); ++p) {
				ProbeRecord pr = probes.get(p);
				for (int c = 0; c < proxyCgs.size(); ++c) {
					double couple = 0;
					CGRecord pcr = proxyCgs.get(c);
					for (int c2 = 0; c2 < cgs.size(); ++c2) {
						CGRecord cr = cgs.get(c2);
						if (cr.pos >= pcr.pos - proxyGatherMin && cr.pos <= pcr.pos + proxyGatherMax) {
							int dist;
							if (cr.pos < pr.min) {
								dist = pr.min - cr.pos;
							} else if (cr.pos > pr.max) {
								dist = cr.pos - pr.max;
							} else {
								dist = 0;
							}
							if (dist >= 0 && dist < couplingProfile.length) {
								couple += couplingProfile[dist];
							}
						}
					}
					coupleMatrix.set(p, c, couple);
				}
			}
			
			cgs = proxyCgs;
		}
		
		System.err.println("Built coupling matrix");
		
		ExptRecord[] expts = new ExptRecord[arrays.length];
		Matrix2D obsMatrix = new SimpleMatrix2D(probes.size(), arrays.length);
		{
			PreparedStatement get_expt = hepdb.prepareStatement("select e.id, m.response, m.baseline from medip_expt as e, xmeth_array_meta as m where m.expt = e.id and e.expt_name = ?");
			PreparedStatement get_obs = hepdb.prepareStatement("select log_ratio from medip_data where probe = ? and expt = ?");
			for (int a = 0; a < arrays.length; ++a) {
				get_expt.setString(1, arrays[a]);
				ResultSet rs = get_expt.executeQuery();
				rs.next();
				int eid = rs.getInt(1);
				double response = rs.getDouble(2);
				double baseLine = rs.getDouble(3);
				double precision = this.precision; // hardwired for now [FIXME]
				
				if (!Double.isNaN(forceBaseline)) {
					baseLine = forceBaseline;
				}
				if (!Double.isNaN(forceResponse)) {
					response = forceResponse;
				}
				
				expts[a] = new ExptRecord(arrays[a], eid, response, baseLine, precision);
				System.err.println(expts[a]);
				rs.close();
				
				for (int p = 0; p < probes.size(); ++p) {
					get_obs.setInt(1, probes.get(p).probeId);
					get_obs.setInt(2, eid);
					rs = get_obs.executeQuery();
					double obs = Double.NaN;
					if (rs.next()) {
						obs = rs.getDouble(1) * signalMultiplier;
						if (obs < minValidRatio || obs > maxValidRatio) {
							obs = Double.NaN;
						}
					}
					rs.close();
					obsMatrix.set(p, a, obs);
				}
			}	
			get_expt.close();
			get_obs.close();
		}
		
		/*
		Matrix1D breakProbs = new SimpleMatrix1D(cgs.size());
		for (int c = 1; c < cgs.size(); ++c) {
			int dist = cgs.get(c).pos - cgs.get(c - 1).pos;
			breakProbs.set(c, Math.min(0.001 * dist, 0.1));
		}
		*/
		
		MethModel model = makeMethModel(obsMatrix, coupleMatrix, expts);
		
		
		return new AnnotatedModel(model, cgs.toArray(new CGRecord[0]), probes.get(0).chr, pgMin, pgMax);
	}
	
	private MethModel makeMethModel(Matrix2D observed, Matrix2D coupling, ExptRecord[] exptMeta) {
		if (mseqHack) {
			return new Rectified(observed, coupling, exptMeta);
		} else if (oneChannel) {
			return new MethModelAssymetric(observed, coupling, exptMeta);
		} else {
			return new MethModel(observed, coupling, exptMeta);
		}
	}
	
	/**
	 * @param args
	 */
	public void main(String[] args) 
		throws Exception
	{
		Connection hepdb = JDBCPooledDataSource.getDataSource(
				"org.gjt.mm.mysql.Driver",
				dbURL,
				dbUser, 
				dbPass
		).getConnection();
		Connection seqDB = hepdb;
		if (dbSeqURL != null) {
		    seqDB = JDBCPooledDataSource.getDataSource(
					"org.gjt.mm.mysql.Driver",
					dbSeqURL,
					dbUser, 
					dbPass
			).getConnection();
		}
		
		if (arrays == null && tissue != null) {
			PreparedStatement get_arrays = hepdb.prepareStatement("select medip_expt.expt_name from medip_expt, xmeth_array_meta where xmeth_array_meta.expt  = medip_expt.id and medip_expt.tissue = ?");
			List<String> al = new ArrayList<String>();
			
			for (String tt : tissue) {
				get_arrays.setString(1, tt);
				ResultSet rs = get_arrays.executeQuery();
				while (rs.next()) {
					al.add(rs.getString(1));
				}
				rs.close(); 
			}
			get_arrays.close();
			System.err.println("Using arrays " + al);
			arrays = al.toArray(new String[0]);
		}
		
		if (tilepathSamples != null) {
			PreparedStatement get_tpRegion = hepdb.prepareStatement("select p.chr, min(p.min_pos), max(p.max_pos) from medip_probe as p, medip_roi as r where p.roi = r.id and r.roi_name = ? group by p.chr");
			get_tpRegion.setString(1, tilepathSamples);
			ResultSet rs = get_tpRegion.executeQuery();
			rs.next();
			String tpChr = rs.getString(1);
			int tpMin = rs.getInt(2);
			int tpMax = rs.getInt(3);
			
			tpMin += trimEnds;
			tpMax -= trimEnds;
			
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
			XMLWriter xw = new PrettyXMLWriter(pw);
			xw.openTag("xmethRoiAnalysisSamples");
			xw.openTag("xmethMeta");
			for (String a : arrays) {
				xw.openTag("experiment");
				xw.print(a);
				xw.closeTag("experiment");
			}
			xw.closeTag("xmethMeta");

			for (int wStart = tpMin; wStart < tpMax; wStart += tilePathStep) {
				int wEnd = wStart + tilePathTile - 1;
				int fStart = wStart - tilePathFlank;
				int fEnd = wEnd + tilePathFlank;
				System.err.printf("%s:%d,%d%n", tpChr, wStart, wEnd);
				
				AnnotatedModel am = makeModel(hepdb, seqDB, tilepathSamples, tpChr, fStart, fEnd);
				if (am == null) {
					System.err.println("Skipping...");
					continue;
				}
				
				MethModel model = am.model;
				model.init(nsEnsembleSize);
				double lastBayesFactor = Double.POSITIVE_INFINITY;
				double maxBayesFactor = Double.NEGATIVE_INFINITY;
				List<MethModel.Sample> samples = new ArrayList<MethModel.Sample>();
				int stepsSinceBFIncrease = 0;
				double totWeight = 0;
				for (int cycle = 0; cycle < nsCycleMax && (cycle < nsCycleMin || totWeight < maxSamples); ++cycle) {
					MethModel.Sample sample = model.next();
					double bf = sample.getBayesFactor();
					if (bf > lastBayesFactor) {
						stepsSinceBFIncrease = 0;
					} else {
						++stepsSinceBFIncrease;
					}

					samples.add(sample);
					if ((sample.getCycle() % 10) == 0) {
						System.err.print('.');
					}
					
					lastBayesFactor = bf;
					if (bf > maxBayesFactor) {
						maxBayesFactor = bf;
						totWeight = 0;
						for (MethModel.Sample s : samples) {
							double weight = Math.exp(s.getBayesFactor() - maxBayesFactor);
							totWeight += weight;
						}
					} else {
						totWeight += Math.exp(sample.getBayesFactor() - maxBayesFactor);
					}
				}
				System.err.printf("done!  maxBayesFactor=%g\n", maxBayesFactor);
				
				List<MethModel.Sample> postSamples = new ArrayList<MethModel.Sample>();
				DoubleArrayList postSampleVals = new DoubleArrayList();
				for (MethModel.Sample sample : samples) {
					double weight = Math.exp(sample.getBayesFactor() - maxBayesFactor);
					
					double tot = 0;
					int cnt = 0;
					for (int c = 0; c < am.cgs.length; ++c) {
						int pos = am.cgs[c].pos;
						if (pos >= am.mmin && pos <= am.mmax) {
							tot += sample.getMeths().get(c);
							++cnt;
						}
					}
					double x = tot/cnt;
					if (Math.random() < weight) {
						postSamples.add(sample);
						postSampleVals.add(x);
					}
				}
				
				xw.openTag("roi");
				xw.attribute("name", String.format("%s:%d-%d", tilepathSamples, wStart, wEnd));
				xw.attribute("minPos", "" + wStart);
				xw.attribute("maxPos", "" + wEnd);
				xw.openTag("design");
				for (CGRecord c : am.cgs) {
					xw.openTag("cpg");
					xw.attribute("seq", c.chr);
					xw.attribute("pos", "" + c.pos);
					xw.attribute("weight", "" + c.weight);
					xw.closeTag("cpg");
				}
				xw.closeTag("design");
				
				Collections.shuffle(postSamples);
				if (postSamples.size() > maxSamples) {
					postSamples = postSamples.subList(0, maxSamples);
				}
				xw.openTag("samples");
				for (MethModel.Sample sample : postSamples) {
					xw.openTag("sample");
					xw.attribute("likelihood", "" + sample.getLikelihood());
					StringBuilder sb = new StringBuilder();
					Matrix1D meths = sample.getMeths();
					for (int c = 0; c < meths.size(); ++c) {
						if (c > 0) {
							sb.append(' ');
						}
						sb.append(String.format("%.3g", meths.get(c)));
					}
					xw.print(sb.toString());
					xw.closeTag("sample");
				}
				xw.closeTag("samples");
				xw.closeTag("roi");
			}
			xw.closeTag("xmethRoiAnalysisSamples");
			pw.flush();
		} else if (roiSamples != null) {
			BufferedReader br = new BufferedReader(new FileReader(roiSamples));
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
			XMLWriter xw = new PrettyXMLWriter(pw);
			xw.openTag("xmethRoiAnalysisSamples");
			xw.openTag("xmethMeta");
			for (String a : arrays) {
				xw.openTag("experiment");
				xw.print(a);
				xw.closeTag("experiment");
			}
			xw.closeTag("xmethMeta");
			
			for (String rtLine = br.readLine(); rtLine != null; rtLine = br.readLine()) {
				rtLine = rtLine.trim();
				if (rtLine.length() == 0 || rtLine.startsWith("#")) {
					continue;
				}
				
				String[] toks = rtLine.split("\\t");
				String roi = toks[0];
				System.err.print(roi);
				
				AnnotatedModel am;
				try {
					am = makeModel(hepdb, seqDB, roi);
					if (am == null) {
						System.err.println("Empty model");
						continue;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					continue;
				}
				
				MethModel model = am.model;
				model.init(nsEnsembleSize);
				double lastBayesFactor = Double.POSITIVE_INFINITY;
				double maxBayesFactor = Double.NEGATIVE_INFINITY;
				List<MethModel.Sample> samples = new ArrayList<MethModel.Sample>();
				int stepsSinceBFIncrease = 0;
				double totWeight = 0;
				for (int cycle = 0; cycle < nsCycleMax && (cycle < nsCycleMin || totWeight < maxSamples); ++cycle) {
					MethModel.Sample sample = model.next();
					double bf = sample.getBayesFactor();
					if (bf > lastBayesFactor) {
						stepsSinceBFIncrease = 0;
					} else {
						++stepsSinceBFIncrease;
					}

					samples.add(sample);
					if ((sample.getCycle() % 10) == 0) {
						System.err.print('.');
					}
					
					lastBayesFactor = bf;
					if (bf > maxBayesFactor) {
						maxBayesFactor = bf;
						totWeight = 0;
						for (MethModel.Sample s : samples) {
							double weight = Math.exp(s.getBayesFactor() - maxBayesFactor);
							totWeight += weight;
						}
					} else {
						totWeight += Math.exp(sample.getBayesFactor() - maxBayesFactor);
					}
				}
				System.err.println("done!");
				
				List<MethModel.Sample> postSamples = new ArrayList<MethModel.Sample>();
				DoubleArrayList postSampleVals = new DoubleArrayList();
				for (MethModel.Sample sample : samples) {
					double weight = Math.exp(sample.getBayesFactor() - maxBayesFactor);

					if (Math.random() < weight) {
						double tot = 0;
						int cnt = 0;
						for (int c = 0; c < am.cgs.length; ++c) {
							int pos = am.cgs[c].pos;
							if (pos >= am.mmin && pos <= am.mmax) {
								tot += sample.getMeths().get(c);
								++cnt;
							}
						}
						double x = tot/cnt;
						
						postSamples.add(sample);
						postSampleVals.add(x);
					}
				}
				
				xw.openTag("roi");
				xw.attribute("name", roi);
				xw.attribute("minPos", "" + am.mmin);
				xw.attribute("maxPos", "" + am.mmax);
				xw.openTag("design");
				for (CGRecord c : am.cgs) {
					xw.openTag("cpg");
					xw.attribute("seq", c.chr);
					xw.attribute("pos", "" + c.pos);
					xw.attribute("weight", "" + c.weight);
					xw.closeTag("cpg");
				}
				xw.closeTag("design");
				
				Collections.shuffle(postSamples);
				if (postSamples.size() > maxSamples) {
					postSamples = postSamples.subList(0, maxSamples);
				}
				xw.openTag("samples");
				for (MethModel.Sample sample : postSamples) {
					xw.openTag("sample");
					xw.attribute("likelihood", "" + sample.getLikelihood());
					StringBuilder sb = new StringBuilder();
					Matrix1D meths = sample.getMeths();
					for (int c = 0; c < meths.size(); ++c) {
						if (c > 0) {
							sb.append(' ');
						}
						sb.append(String.format("%.3g", meths.get(c)));
					}
					xw.print(sb.toString());
					xw.closeTag("sample");
				}
				xw.closeTag("samples");
				xw.closeTag("roi");
			}
			xw.closeTag("xmethRoiAnalysisSamples");
			pw.flush();
		} 
		
		hepdb.close();
	}
}
