package entrity.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
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
	String host;
	String firstAddress;
	Connection conn;
	HttpClient client;
	int id, 		// holds id of this crawl
			queued,		// how many heads have been added to db for crawling
			tasks, 		// how many tasks are operating or in queue (should be none in queue)
			completed; // how many heads have been scanned
	ExecutorService threadPool; // Thread manager
	
	/* Constructor */
	public Crawl(String firstAddress) {
		this.firstAddress = firstAddress;
	}

	public void run() throws SQLException, MalformedURLException {
		URL url = new URL(firstAddress);
		host = url.getHost();
		conn = Database.connect();
		client = new DefaultHttpClient();
		// write to db
		dbInsert();
		System.out.printf("Crawl id %d%n", id);
		// add first address to db
		Head firstHead = new Head(this, firstAddress);
		firstHead.dbInsert();
		// create thread manager
		threadPool = java.util.concurrent.Executors.newFixedThreadPool(Config.maxThreads);
		// invoke first thread
		threadPool.execute(new CrawlTask(this));
	}

	/* Returns whether the given url's host matches that of the crawl */
	public boolean hostMatch(URL url) { return host.equals(url.getHost()); }

	/* Returns whether the given url's host matches that of the crawl */
	public boolean hostMatch(String address) throws MalformedURLException {
		return host.equals(new URL(address).getHost());
	}

	/* Write this crawl to db */
	void dbInsert() throws SQLException {
		PreparedStatement ps = conn.prepareStatement(
						"insert into crawls (first_address, created_at, updated_at) values (?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, firstAddress);
		Date now = new Date(new java.util.Date().getTime());
		ps.setDate(2, now);
		ps.setDate(3, now);
		ps.executeUpdate();
		ResultSet results = ps.getGeneratedKeys();
		results.next();
		id = results.getInt(1);
		results.close();
		ps.close();
	}

}

class CrawlTask implements Runnable {
	final Crawl crawl;
	String address;

	/* Constructor */
	public CrawlTask(Crawl crawl) {
		this.crawl = crawl;
		synchronized(crawl) { crawl.tasks += 1; }
	}

	@Override
	public void run() {
		// claim a head in db (synchronized there)
		Head head = Head.fetchUnclaimed(crawl);
		// http requests (head and body)
		fetchAndCrawl(head);
		// increment crawl.completed
		synchronized(crawl) { crawl.completed += 1; }
		// create more tasks if db holds more unclaimed heads
		int unclaimedHeadsN = spawnNewTasks();
		// shutdown ExecutorService if indicated
		shutdownIfIndicated( unclaimedHeadsN );
		// decrement tasks
		synchronized(crawl) { crawl.tasks -= 1; }
	}
	
	void fetchAndCrawl(Head head) {
		if (head != null) {
			try {
				// fetch head
				head.fetch();
				// fetch & scan body (if needed)
				if (head.meritsCrawl()) { head.fetchBody().crawlDocument(); }
			} catch (MalformedURLException ex) {
				System.err.printf("MalformedURLException for %s%n%s%n", head.address, ex.getMessage());
				ex.printStackTrace();
			} catch (javax.net.ssl.SSLException ex) {
				System.err.printf("SSLException for %s%n", address);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	int spawnNewTasks() {
		int unclaimedHeadsN;
		synchronized(crawl.conn) {
			unclaimedHeadsN = Head.countUnclaimed(crawl);
			int availableThreads = Config.maxThreads - crawl.tasks;
			int newTasks = Math.min(unclaimedHeadsN, availableThreads);
			for (int i = 0; i < newTasks; i ++)
				{ crawl.threadPool.execute(new CrawlTask(crawl)); }
		}
		return unclaimedHeadsN;
	}
	
	void shutdownIfIndicated(int unclaimedHeads) {
		/* If the db holds no more unclaimed heads and this is the last task running */
		if (unclaimedHeads == 0 && crawl.tasks == 1) {
			try {
				crawl.threadPool.shutdown();
				crawl.threadPool.awaitTermination(500, TimeUnit.MILLISECONDS);
				crawl.conn.close();
				System.out.printf("No more URLs to crawl. No more tasks to run.%n" +
						"Queued:  %d%nCrawled: %d%n", crawl.queued, crawl.completed);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
				System.exit(1);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

}
