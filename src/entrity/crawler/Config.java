package entrity.crawler;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;

public abstract class Config {

	/* Mysql fields */
	static String host;
	static String port;
	static String database;
	static String user;
	static String password;
	
	/* Other fields */
	static int maxThreads;
	
	/* Open config file and get params for mysql connection */
	public static void read() throws IOException {
		ConfigReader.read();
	}	
	
}

class ConfigReader {
	
	private org.jsoup.nodes.Document doc;
	
	public static void read() throws IOException {
		new ConfigReader();
	}
	
	public ConfigReader() throws IOException {
		File input = new File("config.xml");
		doc = Jsoup.parse(input, "UTF-8");
		Config.host = get("host"); 
		Config.port = get("port");
		Config.database = get("database");
		Config.user = get("user");
		Config.password = get("password");
		Config.maxThreads = getInt("threads", 50);
	}
	
	private String get(String selector) {
		org.jsoup.select.Elements elements = doc.select(selector);
		if (elements.isEmpty())
			return null;
		return elements.first().text(); 
	}
	
	private int getInt(String selector, int defaultVal) {
		String value = get(selector);
		if (value == null)
			return defaultVal;
		return Integer.parseInt(value);
	}
}
