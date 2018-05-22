package com.firejq;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.LinkedList;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 *
 * @author <a href="mailto:firejq@outlook.com">firejq</a>
 */
public class ProxyUtil {
	/**
	 * 默认以cglib方式获取动态代理，供工具类外部使用
	 * @param conn
	 * @param pool
	 * @return
	 */
	public static Connection getProxyConnection(Connection conn,
												final LinkedList<Connection> pool) {
		return doProxyByCglib(conn, pool);
	}

	/**
	 * 支持以JDKProxy方式获取动态代理，供工具类接口外部使用
	 * @param conn
	 * @param pool
	 * @param isJdk
	 * @return
	 */
	public static Connection getProxyConnection(Connection conn,
												final LinkedList<Connection> pool,
												Boolean isJdk) {
		return isJdk ? doProxyByJDK(conn, pool) : doProxyByCglib(conn, pool);
	}

	/**
	 * JDK Proxy方式创建动态代理对象实现逻辑，供工具类接口内部调用
	 * @param conn
	 * @param pool
	 * @return
	 */
	private static Connection doProxyByJDK(Connection conn,
										   final LinkedList<Connection> pool) {
		return (Connection) Proxy.newProxyInstance(
				conn.getClass().getClassLoader(),
				conn.getClass().getInterfaces(),
				(proxy, method, args) -> {
					Object res = null;
					if (!method.getName().equals("close")) {
						// System.out.println("调用原方法");
						res = method.invoke(conn, args);
					} else {
						// System.out.println("拦截 close 方法" + pool.size());
						synchronized (pool) {
							res = pool.add((Connection) proxy);
							// connPool.notify();// todo 通知等待线程去获取一个连接
						}
					}
					return res;
				});
	}

	/**
	 * Cglib 方式创建动态代理对象实现逻辑，供工具类接口内部调用
	 * @param conn
	 * @param pool
	 * @return
	 */
	private static Connection doProxyByCglib(Connection conn,
											 final LinkedList<Connection> pool) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(conn.getClass());
		enhancer.setCallbacks(new Callback[] {
				(MethodInterceptor) (obj, method, args, proxy) -> {
					// System.out.println("close 被拦截, 连接数" + pool.size());
					return pool.add((Connection) obj);
				},
				(MethodInterceptor) (obj, method, args, proxy) -> {
					// System.out.println("调用原方法, 连接数" + pool.size());
					return method.invoke(conn, args);
					// connPool.notify();// todo 通知等待线程去获取一个连接
				}
		});
		enhancer.setCallbackFilter(method -> {
			if (method.getName().equals("close")) {
				return 0;
			} else {
				return 1;
			}
		});
		return (Connection) enhancer.create();
	}
}

