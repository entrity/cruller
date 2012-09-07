/* Holds data from <a> tags found in get request bodies */

package entrity.crawler;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Anchor {
	final String href;
	final String text;
	Head head;
	
	public Anchor(org.jsoup.nodes.Element link, Head head) {
		href = link.attr("href");
		text = link.text();
		this.head = head;
	}
	
	public static Anchor create(org.jsoup.nodes.Element link, Head head) throws SQLException {
		Anchor anchor = new Anchor(link, head);
		anchor.dbInsert();
		return anchor;
	}
	
	/* Create record in db */	
	public void dbInsert() throws SQLException {
		PreparedStatement ps = Crawl.conn.prepareStatement("INSERT INTO anchors (head_id, href, text) VALUES (?, ?, ?)");
		ps.setInt(1, head.id);
		ps.setString(2, href);
		ps.setString(3, text);
		ps.executeUpdate();		
	}
}
