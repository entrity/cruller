package entrity.crawler;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class Helpers {
	
	/* Return URL with #fragment stripped */
	static URL removeUrlFragment(URL url) {
		try { 
			java.net.URI uri = new java.net.URI(url.getProtocol(),
					url.getUserInfo(), url.getHost(), url.getPort(),
					url.getPath(), url.getQuery(), null);
			url = uri.toURL();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return url;
	}
	
	/* Close given MySQL objects */
	static void cleanup(PreparedStatement ps, ResultSet rs) {
		try {
			if (ps != null)
				ps.close();
			if (rs != null)
				rs.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
