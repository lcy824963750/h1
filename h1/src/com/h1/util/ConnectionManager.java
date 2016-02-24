package com.h1.util;

import java.sql.Connection;

public class ConnectionManager {
	
	private static ThreadLocal<Connection> local = new ThreadLocal<Connection>();

	public static Connection getConn() {
		if (local.get() == null) {
			Connection conn = JdbcUtil.getConnection();
			local.set(conn);
			return conn;
		} else {
			return local.get();
		}
	}

}
