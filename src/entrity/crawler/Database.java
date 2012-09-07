package entrity.crawler;

import java.sql.*;

public abstract class Database {
	
	public static Connection connect() throws SQLException {
		return DriverManager.getConnection("jdbc:mysql://localhost/crawls?user=root&password=pwd1234");	
	}
	
	public static void createTables() throws SQLException {
		Connection conn = connect();
		Statement statement = conn.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS crawls (id INT NOT NULL AUTO_INCREMENT, first_address VARCHAR(500), created_at DATE, updated_at DATE, PRIMARY KEY (id));");
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS heads  (id INT NOT NULL AUTO_INCREMENT, crawl_id INT, address VARCHAR(500), status INT, content_type VARCHAR(255), PRIMARY KEY (id));");
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS anchors (id INT NOT NULL AUTO_INCREMENT, head_id INT, href VARCHAR(500), text VARCHAR(500), PRIMARY KEY (id));");
	}
	
	public static void dropTables() throws SQLException {
		Connection conn = connect();
	}
}
