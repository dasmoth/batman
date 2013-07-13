package batman;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biojava.bio.program.gff.GFFDocumentHandler;
import org.biojava.bio.program.gff.GFFParser;
import org.biojava.bio.program.gff.GFFRecord;
import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.utils.IOTools;

@App(overview="Load probe and ROI data into a Batman database", generateStub=true)
public class LoadProbes {
	private String dbURL;
	private String dbUser;
	private String dbPass = "";

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
	
	/**
	 * @param args
	 */
	public void main(String[] args) 
		throws Exception
	{
		Connection batdb = JDBCPooledDataSource.getDataSource(
				"org.gjt.mm.mysql.Driver",
				dbURL,
				dbUser, 
				dbPass
		).getConnection();
		
		final PreparedStatement insert_roi = batdb.prepareStatement(
				"insert into medip_roi (roi_name) values (?)"
		);
		final PreparedStatement insert_probe = batdb.prepareStatement(
				"insert into medip_probe (roi, probe_name, chr, min_pos, max_pos) " +
				"                 values (?  , ?         , ?  , ?      , ?)"
		);
		
		final Map<String,Integer> roiIdCache = new HashMap<String, Integer>();
		new GFFParser().parse(IOTools.inputBufferedReader(args), new GFFDocumentHandler() {
			public void commentLine(String comment) {
			}

			public void endDocument() {
			}

			public void recordLine(GFFRecord record) {
				String probe = gaga2(record, "probe", "probe.id");
				String roi = gaga(record, "ROI");
				
				try {
					if (!roiIdCache.containsKey(roi)) {
						insert_roi.setString(1, roi);
						insert_roi.executeUpdate();
						ResultSet rs = insert_roi.getGeneratedKeys();
						rs.next();
						int id = rs.getInt(1);
						rs.close();
						roiIdCache.put(roi, id);
					}
					
					insert_probe.setInt(1, roiIdCache.get(roi));
					insert_probe.setString(2, probe);
					insert_probe.setString(3, record.getSeqName());
					insert_probe.setInt(4, record.getStart());
					insert_probe.setInt(5, record.getEnd());
					insert_probe.executeUpdate();
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

			public void startDocument(String locator) {
			}
		});
		
		insert_roi.close();
		insert_probe.close();
		batdb.close();
	}
	
	private String gaga(GFFRecord r, String key)
	{
		return ((List) r.getGroupAttributes().get(key)).get(0).toString();
	}
	
	private String gaga2(GFFRecord r, String key1, String key2)
	{
		Map m = r.getGroupAttributes();
		if (m.containsKey(key1)) {
			return ((List) r.getGroupAttributes().get(key1)).get(0).toString();
		} else {
			return ((List) r.getGroupAttributes().get(key2)).get(0).toString();
		}
	}

}
