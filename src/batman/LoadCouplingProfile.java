package batman;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;

import org.biojava.utils.JDBCPooledDataSource;
import org.bjv2.util.cli.App;
import org.bjv2.util.cli.Option;

import batman.utils.IOTools;

@App(overview="Load a coupling profile into a Batman DB", generateStub=true)
public class LoadCouplingProfile {

	private String dbURL;
	private String dbUser;
	private String dbPass = "";
	private String name;
	
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


	@Option(help="A name to use while storing the coupling profile in the database")
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * @param args
	 */
	public void main(String[] args) 
		throws Exception
	{
		Connection db = JDBCPooledDataSource.getDataSource(
				"org.gjt.mm.mysql.Driver",
				dbURL,
				dbUser, 
				dbPass
		).getConnection();
		
		PreparedStatement insert_cp = db.prepareStatement("insert into xmeth_coupling_profile (profile_name, offset, coupling) values (?, ?, ?)");
		BufferedReader br = IOTools.inputBufferedReader(args);
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			StringTokenizer toke = new StringTokenizer(line);
			int offset = Integer.parseInt(toke.nextToken());
			int count = Integer.parseInt(toke.nextToken());
			double coupling = Double.parseDouble(toke.nextToken());
			
			if (offset <= 0) {
				insert_cp.setString(1, name);
				insert_cp.setInt(2, -offset);
				insert_cp.setDouble(3, coupling);
				insert_cp.executeUpdate();
			}
		}
		insert_cp.close();
		db.close();
	}

}
