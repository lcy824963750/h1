package com.h1.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DB工具类
 * @author yy
 *
 */
public class JdbcUtil {
	private static String username = "lcy";
	private static String password = "oracle";
	private static String url = "jdbc:oracle:thin:@127.0.0.1:1521:orcl";
	private static String driver = "oracle.jdbc.driver.OracleDriver";
	
	/**
	 * 得到一个数据库连接
	 * @return
	 */
	public static Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, username, password);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	
	public static void closeAll(Connection conn,Statement st){
		closeAll(conn, st, null);
	}
	
	/**
	 * 关闭资源
	 * @param conn
	 * @param st
	 * @param rs
	 */
	public static void closeAll(Connection conn, Statement st, ResultSet rs) {
		try {
			if (conn != null) {
				conn.close();
			}
			if (st != null) {
				st.close();
			}
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) {
		System.out.println(getConnection());
	}
}
