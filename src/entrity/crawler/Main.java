package entrity.crawler;

import java.net.MalformedURLException;
import java.sql.*;

public class Main {

	public static void main(String[] args) {
		test();
//		run(args);
	}
	
	private static void test() {
		try {
			java.net.URL url  = new java.net.URL("http://manderson:clumanderson@www.trustedquote.com/foo/bar.html?k=v#fragthing");
			java.net.URL url2 = new java.net.URL("http://www.trustedquote.com/foo/bar.html?k=v");
			java.net.URL url3 = new java.net.URL("http://www.trustedquote.com/foo/bar.html#fragthing");
			java.net.URL url4 = new java.net.URL("http://www.trustedquote.com/foo/bar.html");
			java.net.URI uri = new java.net.URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null);
			System.out.println(url);
			System.out.println(uri.toURL());
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

}
