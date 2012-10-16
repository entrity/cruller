/**
 * Usage:
 * Run a crawl by supplying the first url
 * 	java -jar [thisjar] [first url]
 * 	(todo) --ignore-offsite			don't even make head requests for offsite resources
 * 	(todo) --greater-domain			also crawl linked subdomains as though they were one site
 * Create tables in database
 *  java -jar [thisjar] init
 * 
 */


package entrity.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.*;

import org.apache.http.*;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

public class Main {

	public static void main(String[] args) {
//		test(args); if (true) return;
//		args = new String[1];
//		args[0] = "http://www.wholesaleinsurance.net";
//		args[0] = "http://www.trustedquote.com";
		// check command
		if (args.length == 0) {
			System.out.println("Supply a url to request or a command to execute.");
			System.exit(0);
		}
		if (args[0].equals("init")) { 		// init
			initialize(args);
		} else if (args[0].equals("test")) { // test
			test(args);
		} else { 							// run crawl
			run(args);
		}		
	}
	
	private static void test(String[] args) {
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpHead head = new HttpHead("https://www.trustedquote.com/life-insurance-quotes");
			head.setHeader("Host", "trustedquote.com");
			HttpResponse response = client.execute(head);
			System.out.println(response);
			Config.read();
			System.out.println(Config.maxThreads);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void run(String[] args) {
		String firstAddress = args.length > 0 ? args[args.length-1] : null;
		if (firstAddress == null) {
			System.err.println("Must supply first address");
		} else {
			try {
				Config.read();
				Crawl crawl = new Crawl(firstAddress);
				System.out.printf("Starting crawl w/ %d threads on %s%n", Config.maxThreads, crawl.firstAddress);
				crawl.run();
//				dummy(crawl);
			} catch (SQLException ex) {
				ex.printStackTrace();
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}	
		}		
	}
	
	private static void initialize(String[] args) {
		try {
			Database.createTables();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// temp
	static void dummy(Crawl crawl) throws SQLException {
		Connection conn;
		conn = Database.connect();
//		int id;
//		int status;
		for (int i = 0; i < 1; i ++) {
			
			PreparedStatement ps = conn.prepareStatement("insert into heads (address) values ('Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?')");
			try {
				ps.executeUpdate();
			} catch (SQLException ex) {
				if (ex.getErrorCode() == 1406)
					System.err.println(ps.toString());
				System.err.println(ex.getCause());
				System.err.println(ex.getMessage());
				System.err.println(ex.getErrorCode());
//				ex.printStackTrace();
			}
			
			ps.close();
			System.gc();
		}
	}

}
