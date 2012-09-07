package entrity.crawler;

import java.net.MalformedURLException;
import java.sql.*;

public class Main {

	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].equals("init"))
				initialize();
		} else {			
			test();
//			run(args);
		}
	}
	
	private static void test() {
		try {
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void run(String[] args) {
		String firstAddress = "http://www.trustedquote.com";
		try {
			Crawl.run(firstAddress);
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		}
	}
	
	private static void initialize() {
		try {
			Database.createTables();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
