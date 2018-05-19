import net.sf.cglib.proxy.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 *
 * @author <a href="mailto:firejq@outlook.com">firejq</a>
 */
public class JConnPool implements DataSource {

	// database connection list
	private static LinkedList<Connection> connList = new LinkedList<>();

	/* default value of connection pool environment parameter */
	private final static Integer DEFAULT_INITIAL_POOL_SIZE = 10;
	private final static Integer DEFAULT_MAX_POOL_SIZE = 50;
	private final static Integer DEFAULT_MIN_POOL_SIZE = 2;
	/* end */

	/* connection pool environment parameter */
	// initial size of connection pool
	private static Integer initialPoolSize = DEFAULT_INITIAL_POOL_SIZE;
	// max size of connection pool
	private static Integer maxPoolSize = DEFAULT_MAX_POOL_SIZE;
	// min size of connection pool
	private static Integer minPoolSize = DEFAULT_MIN_POOL_SIZE;
	/* end */

	/* customized jdbc connection parameter */
	// JDBC driverClass class
	private static String driverClass;
	// JDBC connection jdbcUrl
	private static String jdbcUrl;
	// database connection username
	private static String username;
	// database connection password
	private static String password;
	/* end */


	// todo default case
	private final static String DEFAULT_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

	static {
		try (InputStream inputStream = JConnPool.class
				.getClassLoader()
				.getResourceAsStream("db.properties")) {
			Properties properties = new Properties();
			properties.load(inputStream);
			JConnPool.jdbcUrl = properties.getProperty("url");
			JConnPool.username = properties.getProperty("username");
			JConnPool.driverClass = properties.getProperty("driver");
			JConnPool.password = properties.getProperty("password");
			// 加载驱动
			Class.forName(driverClass);

			// 获取多个连接，保存在 LinkedList 集合中
			for (int i = 0; i < JConnPool.initialPoolSize; i++) {
				JConnPool.connList.add(DriverManager.getConnection(jdbcUrl,
																   username,
																   password));
			}
			System.out.println("初始化完成，当前连接数：" + connList.size());
		} catch (IOException | ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>Attempts to establish a connection with the data source that
	 * this {@code DataSource} object represents.
	 *
	 * @return a connection to the data source
	 * @throws SQLException        if a database access error occurs
	 * @throws SQLTimeoutException when the driverClass has determined that the
	 *                             timeout value specified by the {@code
	 *                             setLoginTimeout} method has been exceeded and
	 *                             has at least tried to cancel the current
	 *                             database connection attempt
	 */
	@Override
	public Connection getConnection() throws SQLException {
		System.out.println("调用getConn，连接数：" + connList.size());
		if (connList.size() > 0) {
			// get a connection object from the pool
			Connection connection = connList.removeFirst();

			System.out.println("获取一个连接前，连接数:" + connList.size());
			// 返回一个动态代理对象
			return (Connection) Proxy.newProxyInstance(
					JConnPool.class.getClassLoader(),
					connection.getClass().getInterfaces(),
					(proxy, method, args) -> {
						// 如果不是调用 close 方法，就按照正常的来调用
						if (!method.getName().equals("close")) {
							method.invoke(connection, args);
						} else {
							connList.add(connection);
							// 再看看池的大小
							System.out.println(connList.size());
						}
						return null;
					});


//			Enhancer enhancer = new Enhancer();
//			enhancer.setSuperclass(connection.getClass());
//			enhancer.setCallbacks(new Callback[] {
//					(MethodInterceptor) (obj, method, args, proxy) -> {
//						connList.add((Connection) obj);
//						System.out.println("close 被拦截, 连接数" + connList.size());
//						return null;
//					},
//					NoOp.INSTANCE
//			});
//			enhancer.setCallbackFilter(method -> {
//				if (method.getName().equals("close")) {
//					return 0;
//				} else {
//					return 1;
//				}
//			});
//			return (Connection) enhancer.create();

		}
		return null;
	}

	/**
	 * <p>Attempts to establish a connection with the data source that
	 * this {@code DataSource} object represents.
	 *
	 * @param username the database user on whose behalf the connection is being
	 *                 made
	 * @param password the user's password
	 * @return a connection to the data source
	 * @throws SQLException        if a database access error occurs
	 * @throws SQLTimeoutException when the driverClass has determined that the
	 *                             timeout value specified by the {@code
	 *                             setLoginTimeout} method has been exceeded and
	 *                             has at least tried to cancel the current
	 *                             database connection attempt
	 * @since 1.4
	 */
	@Override
	public Connection getConnection(String username, String password)
			throws SQLException {
		return null;
	}

	/**
	 * Returns an object that implements the given interface to allow access to
	 * non-standard methods, or standard methods not exposed by the proxy.
	 * <p>
	 * If the receiver implements the interface then the result is the receiver
	 * or a proxy for the receiver. If the receiver is a wrapper and the wrapped
	 * object implements the interface then the result is the wrapped object or
	 * a proxy for the wrapped object. Otherwise return the the result of
	 * calling <code>unwrap</code> recursively on the wrapped object or a proxy
	 * for that result. If the receiver is not a wrapper and does not implement
	 * the interface, then an <code>SQLException</code> is thrown.
	 *
	 * @param iface A Class defining an interface that the result must
	 *              implement.
	 * @return an object that implements the interface. May be a proxy for the
	 * actual implementing object.
	 * @throws SQLException If no object found that implements the interface
	 * @since 1.6
	 */
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	/**
	 * Returns true if this either implements the interface argument or is
	 * directly or indirectly a wrapper for an object that does. Returns false
	 * otherwise. If this implements the interface then return true, else if
	 * this is a wrapper then return the result of recursively calling
	 * <code>isWrapperFor</code> on the wrapped object. If this does not
	 * implement the interface and is not a wrapper, return false. This method
	 * should be implemented as a low-cost operation compared to
	 * <code>unwrap</code> so that callers can use this method to avoid
	 * expensive <code>unwrap</code> calls that may fail. If this method returns
	 * true then calling <code>unwrap</code> with the same argument should
	 * succeed.
	 *
	 * @param iface a Class defining an interface.
	 * @return true if this implements the interface or directly or indirectly
	 * wraps an object that does.
	 * @throws SQLException if an error occurs while determining whether this is
	 *                      a wrapper for an object with the given interface.
	 * @since 1.6
	 */
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	/**
	 * <p>Retrieves the log writer for this <code>DataSource</code>
	 * object.
	 *
	 * <p>The log writer is a character output stream to which all logging
	 * and tracing messages for this data source will be printed.  This includes
	 * messages printed by the methods of this object, messages printed by
	 * methods of other objects manufactured by this object, and so on.
	 * Messages printed to a data source specific log writer are not printed to
	 * the log writer associated with the <code>java.sql.DriverManager</code>
	 * class.  When a
	 * <code>DataSource</code> object is
	 * created, the log writer is initially null; in other words, the default is
	 * for logging to be disabled.
	 *
	 * @return the log writer for this data source or null if logging is
	 * disabled
	 * @throws SQLException if a database access error occurs
	 * @see #setLogWriter
	 * @since 1.4
	 */
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	/**
	 * <p>Sets the log writer for this <code>DataSource</code>
	 * object to the given <code>java.io.PrintWriter</code> object.
	 *
	 * <p>The log writer is a character output stream to which all logging
	 * and tracing messages for this data source will be printed.  This includes
	 * messages printed by the methods of this object, messages printed by
	 * methods of other objects manufactured by this object, and so on.
	 * Messages printed to a data source- specific log writer are not printed to
	 * the log writer associated with the <code>java.sql.DriverManager</code>
	 * class. When a
	 * <code>DataSource</code> object is created the log writer is
	 * initially null; in other words, the default is for logging to be
	 * disabled.
	 *
	 * @param out the new log writer; to disable logging, set to null
	 * @throws SQLException if a database access error occurs
	 * @see #getLogWriter
	 * @since 1.4
	 */
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {

	}

	/**
	 * <p>Sets the maximum time in seconds that this data source will wait
	 * while attempting to connect to a database.  A value of zero specifies
	 * that the timeout is the default system timeout if there is one;
	 * otherwise, it specifies that there is no timeout. When a
	 * <code>DataSource</code> object is created, the login timeout is initially
	 * zero.
	 *
	 * @param seconds the data source login time limit
	 * @throws SQLException if a database access error occurs.
	 * @see #getLoginTimeout
	 * @since 1.4
	 */
	@Override
	public void setLoginTimeout(int seconds) throws SQLException {

	}

	/**
	 * Gets the maximum time in seconds that this data source can wait while
	 * attempting to connect to a database.  A value of zero means that the
	 * timeout is the default system timeout if there is one; otherwise, it
	 * means that there is no timeout. When a <code>DataSource</code> object is
	 * created, the login timeout is initially zero.
	 *
	 * @return the data source login time limit
	 * @throws SQLException if a database access error occurs.
	 * @see #setLoginTimeout
	 * @since 1.4
	 */
	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	/**
	 * Return the parent Logger of all the Loggers used by this data source.
	 * This should be the Logger farthest from the root Logger that is still an
	 * ancestor of all of the Loggers used by this data source. Configuring this
	 * Logger will affect all of the log messages generated by the data source.
	 * In the worst case, this may be the root Logger.
	 *
	 * @return the parent Logger for this data source
	 * @throws SQLFeatureNotSupportedException if the data source does not use
	 *                                         {@code java.util.logging}
	 * @since 1.7
	 */
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}
