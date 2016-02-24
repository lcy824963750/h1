package com.h1.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * �õ�һ�������������͵ľ�������
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
	 * @param index	�õ��ڼ��������ķ��������������
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
