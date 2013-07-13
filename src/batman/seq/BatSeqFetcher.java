package batman.seq;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.impl.SimpleSequence;

public class BatSeqFetcher {
	public static Sequence getSequence(Connection c, String name)
		throws Exception
	{
		Sequence seq = null;
		PreparedStatement get_whole = c.prepareStatement("select dna from xmeth_genome_sequence where seq_name = ?");
		get_whole.setString(1, name);
		ResultSet rs = get_whole.executeQuery();
		if (rs.next()) {
			seq = new SimpleSequence(DNATools.createDNA(rs.getString(1)), null, name, Annotation.EMPTY_ANNOTATION);
		}
		rs.close();
		get_whole.close();
		if (seq == null) {
			seq = new BatSeq(c, name);
		}
		return seq;
	}
}
