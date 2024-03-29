/* Holds data gathered from a get http request */

package entrity.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Body {
	
	public Body(String address, Head head) throws MalformedURLException {
		this.address = address;
		this.url = new URL(address);
		this.head = head;
	}
	
	final String address;
	final URL url;
	Head head;
	Document doc;
	
	static public Body fetch(String address, Head head) throws ClientProtocolException, IOException {
		Body body = new Body(address, head);
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(new HttpGet(address));
		HttpEntity entity = response.getEntity();
		if (entity == null) return body;
		InputStream in = entity.getContent();
		body.doc = Jsoup.parse(in, null, address);
		in.close();
		client.getConnectionManager().shutdown();
		return body;
	}
	
	/*
	 * Resolve relative urls against the URL of the current HTTP request.
	 */
	private URL resolveUrl(String address) throws MalformedURLException {
		return new URL(url, address);
	}
	
	/* 
	 * Parse the HTTP response's body text.
	 * Find hyperlinks and save them to database.
	 */
	public void crawlDocument() throws MalformedURLException {
		// Get links' hrefs
		Elements links = doc.select("a[href]");
		Iterator<Element> iterator = links.iterator();
		while (iterator.hasNext()) {
			Element link = iterator.next();
			Anchor anchor = Anchor.create(link, head);
			try {
				// add url to queue if not javascript or mailto
				if (anchor.href.startsWith("javascript:") || anchor.href.startsWith("mailto:"))
					return;
				addAddress( resolveUrl(anchor.href) );
			} catch (MalformedURLException ex) {
				String msg = String.format("Could not add address {%s} from body {%s} to Crawl", anchor.href, address);
				throw new MalformedURLException(msg);
			}
		}
	}
	
	/* 
	 * Unless address is already in db, adds address to db & increments
	 * crawl.queued. (Removes #fragment from given URL)
	 */
	public void addAddress(URL url) {
		boolean added = false;
		// remove fragment because we don't want to crawl the same page twice
		String address = Helpers.removeUrlFragment(url).toString();		
		synchronized(head.crawl.conn) {
			// check Heads in db for url
			if (!Head.inDb(head.crawl, address)) {
				// create Head for url in db
				Head newhead = new Head(head.crawl, address);
				newhead.dbInsert();
				added = true;
			}
		}
		// increment crawl.queued
		if (added) {
			synchronized(this) { head.crawl.queued += 1; }
		}
	}
	
	
}
