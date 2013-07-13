package batman;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

@App(overview="Add experiment metadata to a Batman DB", generateStub=true)
public class AddExpt {
	private String dbURL;
	private String dbUser;
	private String dbPass = "";
	private String expt;
	private String array;
	private String tissue;
	private String sample;
	private ChannelType cy3 = ChannelType.IP;
	private ChannelType cy5 = ChannelType.INPUT;
	
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

	@Option(help="Array ID")
	public void setArray(String array) {
		this.array = array;
	}

	@Option(help="Type of sample on cy3 channel", optional=true)
	public void setCy3(ChannelType cy3) {
		this.cy3 = cy3;
	}

	@Option(help="Type of sample on cy5 channel", optional=true)
	public void setCy5(ChannelType cy5) {
		this.cy5 = cy5;
	}

	@Option(help="Experiment name")
	public void setExpt(String expt) {
		this.expt = expt;
	}

	@Option(help="Sample name")
	public void setSample(String sample) {
		this.sample = sample;
	}

	@Option(help="Tissue name")
	public void setTissue(String tissue) {
		this.tissue = tissue;
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
		PreparedStatement insert_expt = db.prepareStatement("insert into medip_expt (expt_name, array_id, tissue, sample, cy3, cy5) values (?, ?, ?, ?, ?, ?)");
		insert_expt.setString(1, expt);
		insert_expt.setString(2, array);
		insert_expt.setString(3, tissue);
		insert_expt.setString(4, sample);
		insert_expt.setString(5, cy3.toString());
		insert_expt.setString(6, cy5.toString());
		insert_expt.executeUpdate();
	}
	
}
