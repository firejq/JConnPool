package com.firejq;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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
	public static Connection getProxyConnection(Connection conn,
												final LinkedList<Connection> pool) {
		// JDK Proxy方式
		return (Connection) Proxy.newProxyInstance(
				conn.getClass().getClassLoader(),
				conn.getClass().getInterfaces(),
				(proxy, method, args) -> {
					Object res = null;
					if (!method.getName().equals("close")) {
						System.out.println("调用原方法");
						res = method.invoke(conn, args);
					} else {
						System.out.println("拦截 close 方法");
						synchronized (pool) {
							pool.add((Connection) proxy);
							System.out.println(pool.size());
							// todo
							// connPool.notify();//通知等待线程去获取一个连接
						}
					}
					return res;
				});


		// Cglib 方式
		//		Enhancer enhancer = new Enhancer();
		//		enhancer.setSuperclass(conn.getClass());
		//		enhancer.setCallbacks(new Callback[] {
		//				(MethodInterceptor) (obj, method, args, proxy) -> {
		//					pool.add((Connection) obj);
		//					System.out.println("close 被拦截, 连接数" + pool.size());
		//					return null;
		//				},
		//				NoOp.INSTANCE // todo 看 cglib api
		////				(MethodInterceptor) (obj, method, args, proxy) -> {
		////					System.out.println("调用原方法, 连接数" + pool.size());
		////					return proxy.invokeSuper(obj, args);
		////				}
		//		});
		//		enhancer.setCallbackFilter(method -> {
		//			if (method.getName().equals("close")) {
		//				return 0;
		//			} else {
		//				return 1;
		//			}
		//		});
		//		return (Connection) enhancer.create();



	}
}

