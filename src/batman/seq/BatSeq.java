package batman.seq;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Iterator;

import org.biojava.bio.Annotation;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.FeatureHolder;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.Feature.Template;
import org.biojava.bio.symbol.AbstractSymbolList;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.DummySymbolList;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.utils.ChangeVetoException;

class BatSeq extends AbstractSymbolList implements Sequence {
	private final String name;
	private final int length;
	private final PreparedStatement get_tile;
	private SymbolList currentTile;
	private int currentTileMin = Integer.MAX_VALUE;
	private int currentTileMax = Integer.MIN_VALUE;

	BatSeq(Connection con, String name)
		throws Exception
	{
		this.name = name;
		PreparedStatement get_length = con.prepareStatement("select max(seq_max) from xmeth_genome_fragment where seq_name = ?");
		get_length.setString(1, name);
		ResultSet rs = get_length.executeQuery();
		rs.next();
		this.length = rs.getInt(1);
		rs.close();
		get_length.close();
		get_tile = con.prepareStatement("select seq_min, seq_max, dna from xmeth_genome_fragment where seq_name = ? and seq_min <= ? and seq_min >= ? and seq_max >= ?");
	}
	
	protected void finalize() throws Throwable {
		get_tile.close();
		super.finalize();
	}
	
	public Alphabet getAlphabet() {
		return DNATools.getDNA();
	}

	public int length() {
		return length;
	}

	public Symbol symbolAt(int index) throws IndexOutOfBoundsException {
		if (index < 1 || index > length) {
			throw new IndexOutOfBoundsException(String.format("%d is not in range 1:%d", index, length));
		}
		
		try {
			if (index < currentTileMin || index > currentTileMax) {
				get_tile.setString(1, name);
				get_tile.setInt(2, index);
				get_tile.setInt(3, index - 1000000);
				get_tile.setInt(4, index);
				ResultSet rs = get_tile.executeQuery();
				if (!rs.next()) {
					throw new IndexOutOfBoundsException();
				}
				currentTileMin = rs.getInt(1);
				currentTileMax = rs.getInt(2);
				String d = rs.getString(3);
				if (d != null) {
					currentTile = DNATools.createDNA(d);
				} else {
					currentTile = new DummySymbolList(DNATools.getDNA(), currentTileMax - currentTileMin + 1);
				}
			}
			return currentTile.symbolAt(index - currentTileMin + 1);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public String getName() {
		return name;
	}

	public String getURN() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean containsFeature(Feature f) {
		return false;
	}

	public int countFeatures() {
		return 0;
	}

	public Feature createFeature(Template ft) throws BioException, ChangeVetoException {
		throw new ChangeVetoException();
	}

	public Iterator features() {
		return Collections.emptySet().iterator();
	}

	public FeatureHolder filter(FeatureFilter fc, boolean recurse) {
		return FeatureHolder.EMPTY_FEATURE_HOLDER;
	}

	public FeatureHolder filter(FeatureFilter filter) {
		return FeatureHolder.EMPTY_FEATURE_HOLDER;
	}

	public FeatureFilter getSchema() {
		return FeatureFilter.none;
	}

	public void removeFeature(Feature f) throws ChangeVetoException, BioException {
		throw new ChangeVetoException();
	}

	public Annotation getAnnotation() {
		return Annotation.EMPTY_ANNOTATION;
	}

}
