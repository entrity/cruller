/**
 * Usage:
 * Run a crawl by supplying the first url
 * 	java [thisjarfile].jar [first url]
 * 	(todo) --ignore-offsite			don't even make head requests for offsite resources
 * 	(todo) --greater-domain			also crawl linked subdomains as though they were one site
 * Create tables in database
 *  java [thisjar] init [db name] -h [host] -u [username] -p [password] -P [port]
 * 
 */


package entrity.crawler;

import java.net.MalformedURLException;
import java.sql.*;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

public class Main {

	public static void main(String[] args) {
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
			HttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(new HttpHead("https://www.trustedquote.com/life-insurance-quotes"));
			System.out.println(response);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void run(String[] args) {
		for (int i=0; i < args.length; i++) {
			String arg = args[i];
			System.out.printf("%d %s", i, arg);
			Crawl.firstAddress = arg;
		}		
		try {
			System.out.printf("starting crawl for %s...%n", Crawl.firstAddress);
			Crawl.run();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		}
	}
	
	private static void initialize(String[] args) {
		for (int i=1; i < args.length; i ++) {
			if (args[i].charAt(0) == '-') {
				switch(args[i].charAt(1)) {
				case 'h':
					Database.host = args[++i]; break;
				case 'p':
					Database.password = args[++i]; break;
				case 'P':
					Database.port = args[++i]; break;
				case 'u':
					Database.user = args[++i]; break;
				}
			} else {
				Database.database = args[i];
			}
		}
		try {
			Database.createTables();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
