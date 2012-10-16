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
	
	public static Anchor create(org.jsoup.nodes.Element link, Head head) {
		Anchor anchor = new Anchor(link, head);
		anchor.dbInsert();
		return anchor;
	}
	
	/* Create record in db */	
	public void dbInsert() {
		PreparedStatement ps = null;
		try {
			ps = head.crawl.conn.prepareStatement("INSERT INTO anchors (head_id, href, text) VALUES (?, ?, ?)");
			ps.setInt(1, head.id);
			ps.setString(2, href);
			ps.setString(3, text);
			ps.executeUpdate();
		} catch (SQLException ex) {
			if (ex.getErrorCode() == 1406)
				System.err.println(ps.toString());
			ex.printStackTrace();
		} finally {
			Helpers.cleanup(ps, null);
		}
	}
}
