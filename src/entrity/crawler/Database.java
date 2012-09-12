package entrity.crawler;

import java.io.File;
import java.io.IOException;
import java.sql.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public abstract class Database {
	
	static String host;
	static String port;
	static String database;
	static String user;
	static String password;
	
	/* Open config file and get params for mysql connection */
	private static void getConfig() throws IOException {
		File input = new File("config.xml");
		Document doc = Jsoup.parse(input, "UTF-8");
		host = doc.select("host").first().text();
		port = doc.select("port").first().text();
		database = doc.select("database").first().text();
		user = doc.select("user").first().text();
		password = doc.select("password").first().text();		
	}
	
	public static Connection connect() throws SQLException {
		try {
			getConfig();
		} catch (IOException ex) {}
		String address = String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", host, port, database, user, password);
		return DriverManager.getConnection(address);
	}
	
	public static void createTables() throws SQLException {
		Connection conn = connect();
		Statement statement = conn.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS crawls (id INT NOT NULL AUTO_INCREMENT, first_address VARCHAR(500), created_at DATE, updated_at DATE, PRIMARY KEY (id));");
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS heads  (id INT NOT NULL AUTO_INCREMENT, crawl_id INT, address VARCHAR(500), status INT, content_type VARCHAR(255), PRIMARY KEY (id));");
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS anchors (id INT NOT NULL AUTO_INCREMENT, head_id INT, href VARCHAR(500), text VARCHAR(500), PRIMARY KEY (id));");
	}
	
	public static void dropTables() throws SQLException {
		// todo
	}
}
