package com.h1.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Connection;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.h1.annotations.Transaction;
import com.h1.util.ConnectionManager;

public class CglibProxy implements MethodInterceptor {

	private Enhancer enhancer = new Enhancer();

	
	public Object getProxy(Class clazz) {
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(this);
		return enhancer.create();
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args,
			MethodProxy proxy) throws Throwable {

		Object result = null;
		Annotation[] anns = method.getDeclaredAnnotations();
		if (anns == null || anns.length == 0) {
			result = proxy.invokeSuper(obj, args);
		} else {
			boolean flag = false;	//false代表没有事务注解
			for (int i = 0; i < anns.length; i++) {
				if(anns[i] instanceof Transaction) {
					flag = true;
					break;
				}
			}
			if(!flag) {
				result = proxy.invokeSuper(obj, args);
			}else {
				for (Annotation an : anns) {
					if (an instanceof Transaction) {
						Transaction tx = (Transaction) an;
						if (tx.needTx()) {
						// 得到数据库连接 ，并且设置为autoCommit为false
							Connection conn = ConnectionManager.getConn();
							conn.setAutoCommit(false);
							try {
								result = proxy.invokeSuper(obj, args);
								// 提交事物
								conn.commit();
							} catch (Exception e) {
								e.printStackTrace();
								conn.rollback();
							}
						} else {
							result = proxy.invokeSuper(obj, args);
						}
						break;
					}
				}
			}
		}

		return result;
	}

}
