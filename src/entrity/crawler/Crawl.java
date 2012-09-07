package entrity.crawler;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.concurrent.*;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

public class Crawl {
	static String host;
	static String firstAddress;
	static Connection conn;
	static HttpClient client;
	static int id, queued, completed;
	static ExecutorService threadPool; // Thread manager
	
	public static void run(String firstAddress) throws SQLException, MalformedURLException {
		Crawl.firstAddress = firstAddress;
		URL url = new URL(firstAddress);
		Crawl.host = url.getHost();
		Crawl.conn = Database.connect();
		Crawl.client = new DefaultHttpClient();
		// write to db
		save();
		// create thread manager
		threadPool = java.util.concurrent.Executors.newFixedThreadPool(3);
		// invoke first thread
		threadPool.execute(new CrawlTask(firstAddress));
	}
	
	/* Adds another crawl task to the thread pool. (Removes fragment from given URL) */
	public static void addAddress(URL url) {
		try { // remove fragment because we don't want to crawl the same page twice
			java.net.URI uri = new java.net.URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null);
			url = uri.toURL();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		threadPool.execute(new CrawlTask(url));
	}
	
	/* Returns whether the given url's host matches that of the crawl */
	public static boolean hostMatch(URL url) {
		return host.equals(url.getHost());
	}
	
	/* Returns whether the given url's host matches that of the crawl */
	public static boolean hostMatch(String address) throws MalformedURLException {
		return host.equals(new URL(address).getHost());
	}
	
	/* Write to db */
	static void save() throws SQLException {
		PreparedStatement ps = conn.prepareStatement("insert into crawls (first_address, created_at, updated_at) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		ps.setString(1,  firstAddress);
		Date now = new Date(new java.util.Date().getTime());
		ps.setDate(2, now);
		ps.setDate(3, now);
		ps.executeUpdate();
		ResultSet results = ps.getGeneratedKeys();
		results.next();
		id = results.getInt(1);
	}
	
	static void shutdownIfEndReached() {
		if (queued == 0) {
			try {
				threadPool.shutdown();
				threadPool.awaitTermination(500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}
	
}

class CrawlTask implements Runnable {
	final String address;
	
	public CrawlTask(String address) {
		this.address = address;	
	}
	
	public CrawlTask(URL url) {
		this.address = url.toString();
	}

	@Override
	public void run() {
		synchronized(this) {
			Crawl.queued += 1;
		}
		try {
			Head head = Head.fetchIfNew(address);
			if (head.meritsCrawl()) {
				System.out.printf("           =====fetching body %s%n", head.meritsCrawl());
				Body body = head.fetchBody();
				body.crawlDocument();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		synchronized(this) {
			Crawl.queued -= 1; 
			Crawl.completed += 1;
		}
		Crawl.shutdownIfEndReached();
	}
	
}
