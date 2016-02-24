package com.h1.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 得到一个类所声明泛型的具体类型
 * @author yy
 *
 */
public class GenericClass {

	
	public static Class getGenericClass(Class clazz) {
		return getGenericClass(clazz, 0);
	}

	/**
	 * 
	 * @param clazz
	 * @param index	得到第几个声明的泛型所代表的类型
	 * @return
	 */
	public static Class getGenericClass(Class clazz, int index) {
		Type genType = clazz.getGenericSuperclass();
		if (genType instanceof ParameterizedType) {
			Type[] params = ((ParameterizedType) genType)
					.getActualTypeArguments();
			if ((params != null) && (params.length >= (index - 1))) {
				return (Class) params[index];
			}
		}
		return null;
	}

}
