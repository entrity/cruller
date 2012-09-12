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
	public Head(String address) {
		this.address = address;
	}
	
	String address;
	Crawl crawl;
	int status = 0;
	int id = 0;
	String contentType;
	static final java.util.regex.Pattern CONTENT_TYPE_PATTERN = Pattern.compile("text/html");
	
	/* Adds new head to db, returns same if address has yet to be crawled; else returns null */
	public static Head addtoDbIfNew(String address) throws SQLException {
		Head head = null;
		/* Check whether this head needs crawling */
		synchronized(Crawl.conn) {
			if (needsRequest(address)) { // check whether this URL has already been crawled
				head = new Head(address);
				head.dbInsert(); // create in db immediately so that this head crawl won't be duplicated (in the time it takes to complete this head crawl)
			}
		}
		return head;
	}

	/* Execute HTTP HEAD request for address. Collect content-type, status-code, etc. Save to database. */
	public void fetch() throws ClientProtocolException, IOException, SQLException {
		System.out.printf("=============fetching head: %s%n", address);
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
	public Body fetchBody() throws ClientProtocolException, IOException, SQLException {
		return Body.fetch(address, this);
	}
	
	/* Create record in db */
	public void dbInsert() throws SQLException {
		PreparedStatement ps;
		ps = Crawl.conn.prepareStatement("INSERT INTO heads (crawl_id, status, address) VALUES (?, 0, ?)", Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, Crawl.id);
		ps.setString(2, address);
		ps.executeUpdate();
		ResultSet results = ps.getGeneratedKeys();
		results.next();
		id = results.getInt(1);
	}
	
	/* Update status, content type in db */
	public void dbUpdate() throws SQLException {
		PreparedStatement ps;
		ps = Crawl.conn.prepareStatement("UPDATE heads SET content_type = ?, status = ? WHERE id = ?");
		ps.setString(1, contentType);
		ps.setInt(2, status);
		ps.setInt(3, id);
		ps.executeUpdate();
	}
	
	public static boolean needsRequest(String address) throws SQLException {
		PreparedStatement ps = Crawl.conn.prepareStatement("SELECT id FROM heads WHERE crawl_id = ? AND address = ?");
		ps.setInt(1, Crawl.id);
		ps.setString(2, address);
		ResultSet results = ps.executeQuery();
		return !results.next();
	}
	
	/* Search db for a Head matching this' address */
	public boolean needsRequest() throws SQLException {
		return Head.needsRequest(address);
	}
	
	/* Content type == text/html and status == 200 */
	public boolean meritsCrawl() throws MalformedURLException {
		if (status >= 300 || status < 200)
			return false;
		if (!Crawl.hostMatch(address))
			return false;
		java.util.regex.Matcher matcher = CONTENT_TYPE_PATTERN.matcher(contentType);
		return matcher.find();
	}
}
