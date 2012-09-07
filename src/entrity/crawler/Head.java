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
	
	public static Head fetch(String address) throws ClientProtocolException, IOException, SQLException {
		Head head = new Head(address);
		/* Check whether this head needs crawling */
		synchronized(Crawl.conn) {
			if (head.needsRequest()) // check whether this URL has already been crawled
				head.insert(); // create in db immediately so that this head crawl won't be duplicated (in the time it takes to complete this head crawl)
			else
				return head;
		}
		/* Http request & response */
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(new HttpHead(address));
		head.status = response.getStatusLine().getStatusCode();
		HeaderIterator headers = response.headerIterator("Content-type");
		if (headers.hasNext()) head.contentType = headers.nextHeader().getValue();
		client.getConnectionManager().shutdown();
		/* Update in db */
		head.update();
		return head;
	}
	
	public Body fetchBody() throws ClientProtocolException, IOException {
		return Body.fetch(address);
	}
	
	/* Create record in db */
	public void insert() throws SQLException {
		PreparedStatement ps;
		ps = Crawl.conn.prepareStatement("INSERT INTO heads (crawl_id, status, address) VALUES (?, 0, ?)", Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, Crawl.id);
		ps.setString(2, address);
		ps.executeUpdate();
		ResultSet results = ps.getGeneratedKeys();
		System.out.println(results.next());
		id = results.getInt(1);
	}
	
	/* Update status, content type in db */
	public void update() throws SQLException {
		PreparedStatement ps;
		ps = Crawl.conn.prepareStatement("UPDATE heads SET content_type = ?, status = ? WHERE id = ?");
		System.out.println(contentType);
		ps.setString(1, contentType);
		ps.setInt(2, status);
		ps.setInt(3, id);
		ps.executeUpdate();
	}
	
	/* Search db for a Head matching given address. If one is found, this address doesn't need to be scanned again. */
	public static boolean needsRequest(java.net.URL url) {
//		PreparedStatement ps = Crawl.conn.prepareStatement("SELECT id FROM heads WHERE crawl_id = ? AND address = ?");
//		ps.setInt(1, Crawl.id);
//		ps.setString(2, url);
//		ResultSet results = ps.executeQuery();
//		return !results.next();
		return false;
	}
	
	/* Search db for a Head matching this' address */
	public boolean needsRequest() throws SQLException {
		PreparedStatement ps = Crawl.conn.prepareStatement("SELECT id FROM heads WHERE crawl_id = ? AND address = ?");
		ps.setInt(1, Crawl.id);
		ps.setString(2, address);
		ResultSet results = ps.executeQuery();
		return !results.next();
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
