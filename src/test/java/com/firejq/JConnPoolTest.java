package com.firejq;

import org.junit.Test;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;

public class JConnPoolTest {
	@Test
	public void testSomeLibraryMethod() throws ClassNotFoundException {
		JConnPool jConnPool = new JConnPool();
		String sql = "SELECT * FROM `city` WHERE `id` = 2";
		try {
			Connection conn = jConnPool.getConnection();
			PreparedStatement stmt = conn.prepareStatement(sql);

			ResultSet resultSet = stmt.executeQuery();
			RowSetFactory factory = RowSetProvider.newFactory();
			CachedRowSet cRst = factory.createCachedRowSet();
			cRst.populate(resultSet);
			while (cRst.next()) {
				System.out.println(cRst.getString(2));
			}

			conn.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}


	}


}
