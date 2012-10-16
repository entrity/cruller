/* Holds data gathered from a head http request */

package entrity.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.regex.Pattern;

import org.apache.http.HeaderIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

public class Head {
	/* Constructor */
	public Head(Crawl crawl, String address) {
		this.crawl = crawl;
		this.address = address;
	}
	/* Constructor. (ResultSet should already have next() called on it.) */
	public Head(Crawl crawl, ResultSet result) throws SQLException {
		this.crawl = crawl;
		this.id = result.getInt("id");
		this.address = result.getString(COLNAME_ADDRESS);
	}
	
	
	final Crawl crawl;
	String address;
	int status = UNCLAIMED;
	int id = 0;
	String contentType;
	static final java.util.regex.Pattern CONTENT_TYPE_PATTERN = Pattern.compile("text/html");
	public static final int UNCLAIMED = -1;
	public static final int CLAIMED = 0;
	public static final String COLNAME_ADDRESS = "address";
	
	/* Synchronized against connection. Claims a Head in the db. Returns same. */
	public static Head fetchUnclaimed(Crawl crawl) {
		Head head = null;
		synchronized(crawl.conn) {
			// select unclaimed head
			PreparedStatement ps = null;
			ResultSet results = null;
			try {
				ps = crawl.conn.prepareStatement("SELECT * FROM heads WHERE crawl_id = ? AND status = ? LIMIT 1");
				ps.setInt(1, crawl.id);
				ps.setInt(2, UNCLAIMED);
				results = ps.executeQuery();
				if (results.next()) {
					// claim head
					head = new Head(crawl, results);
					head.status = CLAIMED;
					head.dbUpdate();
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			} finally {
				Helpers.cleanup(ps, results);
			}
		}
		return head;
	}
	
	/* Counts unclaimed Heads in db belonging to given crawl */
	public static int countUnclaimed(Crawl crawl) {
		int count = -1;
		PreparedStatement ps = null;
		ResultSet results = null;
		try {
			ps = crawl.conn.prepareStatement("SELECT count(*) FROM heads WHERE crawl_id = ? AND status = ? LIMIT 1");
			ps.setInt(1, crawl.id);
			ps.setInt(2, UNCLAIMED);
			results = ps.executeQuery();
			results.next();
			count = results.getInt(1);	
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			Helpers.cleanup(ps, results);
		}
		return count;
	}

	/* Execute HTTP HEAD request for address. Collect content-type, status-code, etc. Save to database. */
	public void fetch() throws ClientProtocolException, IOException {
		System.out.printf("tasks: %2d ... queued: %5d ... completed: %5d %n", crawl.tasks, crawl.queued, crawl.completed);
		/* Http request & response */
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(new HttpHead(address));
		status = response.getStatusLine().getStatusCode();
		HeaderIterator headers = response.headerIterator("Content-type");
		if (headers.hasNext()) contentType = headers.nextHeader().getValue();
		client.getConnectionManager().shutdown();
		/* Update in db */
		dbUpdate();
	}
	
	/* Run GET request for this' address. Return Body. This does not save the Body to database. This does not parse the body text. */
	public Body fetchBody() throws ClientProtocolException, IOException {
		return Body.fetch(address, this);
	}
	
	/* Create record in db */
	public void dbInsert() {
		PreparedStatement ps = null;
		ResultSet results = null;
		try {
			ps = crawl.conn.prepareStatement("INSERT INTO heads (crawl_id, status, address) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, crawl.id);
			ps.setInt(2, status);
			ps.setString(3, address);
			ps.executeUpdate();
			results = ps.getGeneratedKeys();
			results.next();
			id = results.getInt(1);
		} catch (SQLException ex) {
			if (ex.getErrorCode() == 1406)
				System.err.println(ps.toString());
			ex.printStackTrace();
		} finally {
			Helpers.cleanup(ps, results);
		}
	}
	
	/* Update status, content type in db */
	public void dbUpdate() {
		PreparedStatement ps = null;
		try {
			ps = crawl.conn.prepareStatement("UPDATE heads SET content_type = ?, status = ? WHERE id = ?");
			ps.setString(1, contentType);
			ps.setInt(2, status);
			ps.setInt(3, id);
			ps.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			Helpers.cleanup(ps, null);
		}
	}
	
	/* Returns true if given string represents a Head (for this crawl) in db */
	public static boolean inDb(Crawl crawl, String address) {
		boolean success = false;
		PreparedStatement ps = null;
		try {
			ps = crawl.conn.prepareStatement("SELECT id, status FROM heads WHERE crawl_id = ? AND address = ? limit 1");
			ps.setInt(1, crawl.id);
			ps.setString(2, address);
			ResultSet results = ps.executeQuery();
			success = results.next();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			Helpers.cleanup(ps, null);
		}
		return success;
	}

	/* Returns int signifying status of given Head:
	 * -1 : Head is in db but has not been requested or claimed by a crawl task
	 * 0 : Head  is claimed by a crawl task
	 * > 0 : Http response code for head request
	 * SQLException is raised if given address is not in db
	 * */
	public static int statusOf(Crawl crawl, String address) {
		int id = -1;
		int status = -2;
		PreparedStatement ps = null;
		ResultSet results = null;
		try {
			ps = crawl.conn.prepareStatement("SELECT id, status FROM heads WHERE crawl_id = ? AND address = ? limit 1");
			ps.setInt(1, crawl.id);
			ps.setString(2, address);
			results = ps.executeQuery();
			results.next();
			id = results.getInt(1);
			status = results.getInt(2);
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			Helpers.cleanup(ps, results);
		}
		System.out.printf("result of statusOf: %d - %d%n", id, status);
		return status; 
	}
	
	/* Content type == text/html and status == 200 */
	public boolean meritsCrawl() throws MalformedURLException {
		if (status >= 300 || status < 200)
			return false;
		if (!crawl.hostMatch(address))
			return false;
		java.util.regex.Matcher matcher = CONTENT_TYPE_PATTERN.matcher(contentType);
		return matcher.find();
	}
}
