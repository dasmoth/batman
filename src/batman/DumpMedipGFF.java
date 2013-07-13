package batman;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

@App(overview="Re-export MeDIP data from a Batman DB", generateStub=true)
public class DumpMedipGFF {
	private String dbURL = null;
	private String dbUser = null;
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
		
		String exptName = args[0];
		PreparedStatement get_exptMeta = batdb.prepareStatement("select id, array_id, tissue, sample, cy3, cy5 from medip_expt where expt_name = ?");
		get_exptMeta.setString(1, exptName);
		ResultSet rs = get_exptMeta.executeQuery();
		rs.next();
		
		int id = rs.getInt(1);
		String arrayId = rs.getString(2);
		String tissue = rs.getString(3);
		String sample = rs.getString(4);
		String cy3 = rs.getString(5);
		String cy5 = rs.getString(6);
		rs.close();
		get_exptMeta.close();
		
		if (tissue.equals("4")) {
			tissue = "GM";
			sample = "GM" + (Integer.parseInt(sample.substring(1)) + 1);
		}
		
		PrintWriter pw = new PrintWriter(new FileWriter(String.format("medip-%s-%s.gff", sample, arrayId)));
		
		pw.printf("# array_id=%s tissue=%s sample=%s cy3=%s, cy5=%s%n", arrayId, tissue, sample, cy3, cy5);
		
		PreparedStatement get_data = batdb.prepareStatement(
				"select p.chr, p.min_pos, p.max_pos, r.roi_name, d.log_ratio " +
				"  from medip_probe as p, medip_roi as r, medip_data as d " +
				" where p.roi = r.id and d.probe = p.id and d.expt = ?"
		);
		get_data.setInt(1, id);
		rs = get_data.executeQuery();
		while (rs.next()) {
			String chr = rs.getString(1);
			int min = rs.getInt(2);
			int max = rs.getInt(3);
			String roi = rs.getString(4);
			double rat = rs.getDouble(5);
			pw.printf("%s\tmedip\tlog_ratio\t%d\t%d\t%g\t.\t.\tROI=%s%n", chr, min, max, rat, roi);
		}
		rs.close();
		get_data.close();
		batdb.close();
		pw.close();
	}

}
