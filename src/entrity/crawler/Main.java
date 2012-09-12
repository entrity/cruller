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
		try {
			Database.createTables();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
