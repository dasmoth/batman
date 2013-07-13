package batman;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.biojava.bio.program.gff.GFFDocumentHandler;
import org.biojava.bio.program.gff.GFFParser;
import org.biojava.bio.program.gff.GFFRecord;
import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.utils.IOTools;

@App(overview="Load almost-Nimblegen-style GFF log-ratio data into Batman DB", generateStub=true)
public class LoadRatsGFF {
	private String dbURL;
	private String dbUser;
	private String dbPass = "";
	private String array;
	private boolean log = false;
	
	@Option(help="Log-transform data on loading", optional=true)
	public void setLog(boolean b) {
		this.log = b;
	}
	
	@Option(help="Experiment name")
	public void setExpt(String s) {
		this.array = s;
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
		
		final PreparedStatement get_expt = batdb.prepareStatement(
				"select id from medip_expt where expt_name = ?"
		);
		final PreparedStatement get_probe = batdb.prepareStatement(
				"select id from medip_probe where probe_name = ?"
		);
		final PreparedStatement insert_rat = batdb.prepareStatement(
				"insert into medip_data (probe, expt, log_ratio) values (?, ?, ?)"
		);
		
		get_expt.setString(1, array);
		ResultSet rs = get_expt.executeQuery();
		rs.next();
		final int exptId = rs.getInt(1);
		rs.close();
		
		new GFFParser().parse(IOTools.inputBufferedReader(args), new GFFDocumentHandler() {
			public void commentLine(String comment) {
			}

			public void endDocument() {
			}

			public void recordLine(GFFRecord record) {
				String probe = gaga(record, "probe");
				
				try {
					get_probe.setString(1, probe);
					ResultSet rs = get_probe.executeQuery();
					int probeId = -1;
					if (rs.next()) {
						probeId = rs.getInt(1);
					}
					rs.close();
					
					if (probeId < 0) {
						System.err.println("Couldn't find " + probe);
						return;
					}
					
					double score = record.getScore();
					if (log) {
						score = Math.log(score) / Math.log(2);
					}
					
					insert_rat.setInt(1, probeId);
					insert_rat.setInt(2, exptId);
					insert_rat.setDouble(3, score);
					insert_rat.executeUpdate();
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

			public void startDocument(String locator) {
			}
		});
		
		get_expt.close();
		get_probe.close();
		insert_rat.close();
		batdb.close();
	}
	
	private String gaga(GFFRecord r, String key)
	{
		return ((List) r.getGroupAttributes().get(key)).get(0).toString();
	}
}
