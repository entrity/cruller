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
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Body {
	
	public Body(String address) throws MalformedURLException {
		this.address = address;
		this.url = new URL(address);
	}
	
	final String address;
	final URL url;
	
	static public Body fetch(String address) throws ClientProtocolException, IOException {
		Body body = new Body(address);
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(new HttpGet(address));
		HttpEntity entity = response.getEntity();
		if (entity == null) return body;
		InputStream in = entity.getContent();
		Document doc = Jsoup.parse(in, null, address);
		in.close();
		client.getConnectionManager().shutdown();
		body.crawlDocument(doc);
		return body;
	}
	
	private URL resolveUrl(String address) throws MalformedURLException {
		return new URL(url, address);
	}
	
	public void crawlDocument(Document doc) {
		// Get links' hrefs
		Elements links = doc.select("a[href]");
		Iterator<Element> iterator = links.iterator();
		while (iterator.hasNext()) {
			Element link = iterator.next();
			String href = link.attr("href");
			System.out.println(href);
			try {
				Crawl.addAddress(resolveUrl(href));
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
}
