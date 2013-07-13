package batman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.db.HashSequenceDB;
import org.biojava.bio.seq.db.SequenceDB;
import org.biojava.bio.seq.impl.SimpleSequence;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.Symbol;
import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.utils.IOTools;

@App(overview="Load genomic sequence into a Batman DB", generateStub=true)
public class LoadGenomeTiles {
	private String dbURL;
	private String dbUser;
	private String dbPass = "";
	private int tileSize = 50000;
	private SequenceDB seqdb;
	private String assembly = "Unknown";

	@Option(help="Name of genome assembly", optional=true)
	public void setAssembly(String s) {
		this.assembly = s;
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
	
	
	@Option(help="Size of sequence tile (default=50000)", optional=true)
	public void setTileSize(int i) {
		this.tileSize = i;
	}

	@Option(help="FASTA sequence file to load")
	public void setSeqFile(File seqFile) 
		throws Exception
	{
		seqdb = new HashSequenceDB();
		for (SequenceIterator si = SeqIOTools.readFastaDNA(IOTools.fileBufferedReader(seqFile)); si.hasNext(); ) {
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
	
	
	/**
	 * @param args
	 */
	public void main(String[] args) 
		throws Exception
	{
		Symbol N = DNATools.n();
		
		Connection hepdb = JDBCPooledDataSource.getDataSource(
				"org.gjt.mm.mysql.Driver",
				dbURL,
				dbUser, 
				dbPass
		).getConnection();
		PreparedStatement insert_seq = hepdb.prepareStatement("insert into xmeth_genome_fragment (assembly, seq_name, seq_min, seq_max, dna) values (?, ?, ?, ?, ?)");
		for (SequenceIterator si = seqdb.sequenceIterator(); si.hasNext(); ) {
			Sequence seq = si.nextSequence();
			for (int ts = 1; ts <= seq.length(); ts += tileSize) {
				int te = Math.min(ts + tileSize - 1, seq.length());
				int nn = 0;
				for (int i = ts; i <= te; ++i) {
					if (seq.symbolAt(i) != N) {
						++nn;
					}
				}
				insert_seq.setString(1, assembly);
				insert_seq.setString(2, seq.getName());
				insert_seq.setInt(3, ts);
				insert_seq.setInt(4, te);
				if (nn > 0) {
					insert_seq.setString(5, nn > 0 ? seq.subStr(ts, te) : null);
				} else {
					insert_seq.setNull(5, Types.VARCHAR);
				}
				
				insert_seq.executeUpdate();
			}
		}
	}

}
