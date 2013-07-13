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
public class DumpMedipGFF2 {
	private String dbURL = null;
	private String dbUser = null;
	private String dbPass = "";
	private String tissue;
	private String expt;
	private boolean design = false;
	
	@Option(help="...", optional=true)
	public void setDesign(boolean b) {
		this.design = b;
	}
	
	@Option(help="Experiment to dump", optional=true)
	public void setExpt(String s) {
		this.expt = s;
	}
	
	
	@Option(help="Tissue to dump", optional=true)
	public void setTissue(String s) {
		this.tissue = s;
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
		
		PreparedStatement get_data;
		
		if (tissue != null) {
			get_data = batdb.prepareStatement(
				"select p.chr, p.min_pos, p.max_pos, r.roi_name, sum(d.log_ratio)/count(*), p.probe_name " +
				"  from medip_probe as p, medip_roi as r, medip_data as d, medip_expt as e " +
				" where p.roi = r.id and d.probe = p.id and d.expt = e.id and e.tissue = ? " +
				" group by p.chr, p.min_pos, p.max_pos, r.roi_name, p.probe_name"
			);
			get_data.setString(1, tissue);
		} else {
			get_data = batdb.prepareStatement(
					"select p.chr, p.min_pos, p.max_pos, r.roi_name, d.log_ratio, p.probe_name " +
					"  from medip_probe as p, medip_roi as r, medip_data as d, medip_expt as e " +
					" where p.roi = r.id and d.probe = p.id and d.expt = e.id and e.expt_name = ?"
				);
			get_data.setString(1, expt);
		}
		
		ResultSet rs = get_data.executeQuery();
		while (rs.next()) {
			String chr = rs.getString(1);
			int min = rs.getInt(2);
			int max = rs.getInt(3);
			String roi = rs.getString(4);
			double rat = rs.getDouble(5);
			String probe = rs.getString(6);
			
			if (design) {
				System.out.printf("%s\tarray\tprobe\t%d\t%d\t.\t.\t.\tROI %s; probe %s%n", chr, min, max, roi, probe);
			} else {
				System.out.printf("%s\tmedip\tlog_ratio\t%d\t%d\t%g\t.\t.\tROI %s; probe %s%n", chr, min, max, rat, roi, probe);
			}
		}
		rs.close();
		get_data.close();
		batdb.close();
	}
}
