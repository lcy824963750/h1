package com.h1.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.h1.util.ConnectionManager;
import com.h1.util.GenericClass;
import com.h1.util.JdbcUtil;

/**
 * dao��װ�� ��ɶ�һЩ���÷�����ͨ�÷�װ
 * 
 * @author yy
 * 
 * @param <T>
 */
public class BaseJdbcDao<T> {

	private Class entityClass; // ����T�������

	private Class getEntityClass() {
		if (entityClass == null) {
			entityClass = GenericClass.getGenericClass(this.getClass());
		}
		return entityClass;
	}

	/**
	 * �õ�������
	 * 
	 * @return
	 */
	private String getTableName() {
		return getEntityClass().getSimpleName();
	}

	/**
	 * ���������õ���Ӧģ�Ͷ����get��������
	 * 
	 * @param name
	 * @return
	 */
	private String getter(String name) {
		return "get" + name.substring(0, 1).toUpperCase()
				+ name.substring(1).toLowerCase();
	}

	/**
	 * ���������õ���Ӧģ�Ͷ����set��������
	 * 
	 * @param name
	 * @return
	 */
	private String setter(String name) {
		return "set" + name.substring(0, 1).toUpperCase()
				+ name.substring(1).toLowerCase();
	}

	/**
	 * ���������õ����ڷ��ͱ�ʾ�������е�get����
	 * 
	 * @param c_name
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private Method getGetterMethod(String c_name) throws SecurityException,
			NoSuchMethodException {
		String getMethodName = getter(c_name);
		return getEntityClass().getDeclaredMethod(getMethodName, null);
	}

	/**
	 * ���������õ����ڷ��ͱ�ʾ�������е�set����
	 * 
	 * @param c_name
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private Method getSetterMethod(String c_name) throws Exception {
		String setMethodName = setter(c_name);
		Method[] methods = getEntityClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(setMethodName)) {
				return methods[i];
			}
		}
		return null;
	}

	/**
	 * ����һ���������ݿ���
	 * 
	 * @param t
	 * @return
	 */
	public int save(T t) {
		String sql1 = "select * from " + getTableName() + " where 1=2";
		Connection conn = ConnectionManager.getConn();
		PreparedStatement ps = null;
		ResultSet rs = null;
		int returnValue = 0;
		try {
			ps = conn.prepareStatement(sql1);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int count = rsmd.getColumnCount();
			String sql2 = "insert into " + getTableName() + " values(";
			// ƴ��?�ַ���
			for (int i = 0; i < count - 1; i++) {
				sql2 += "?,";
			}
			sql2 += "?)";
			ps = conn.prepareStatement(sql2);
			// ��?��ֵ
			for (int i = 1; i <= count; i++) {
				String c_name = rsmd.getColumnName(i);
				Method getterMethod = getGetterMethod(c_name);
				ps.setObject(i, getterMethod.invoke(t, null));
			}
			returnValue = ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} 

		return returnValue;
	}

	/**
	 * �޸�һ������
	 * 
	 * @param t
	 * @return
	 */
	public int update(T t) {
		int returnValue = 0;
		Connection conn = ConnectionManager.getConn();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql1 = "select * from " + getTableName() + " where 1=2";
		try {
			ps = conn.prepareStatement(sql1);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int count = rsmd.getColumnCount();
			String sql2 = "update " + getTableName() + " set ";
			// �����ҵ��������
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // �������������
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}

			// ƴ��?�ʺ�
			for (int i = 1; i <= count; i++) {
				if (pkNames.contains(rsmd.getColumnName(i)))
					continue;
				sql2 += rsmd.getColumnName(i) + "=?,";
			}
			sql2 = sql2.substring(0, sql2.length() - 1);
			sql2 += " where ";
			int pkNum = pkNames.size();
			int flagNum = 0;
			for (String pkName : pkNames) {
				sql2 += pkName + "=? ";
				flagNum++;
				if (flagNum < pkNum)
					sql2 += "and ";
			}
			ps = conn.prepareStatement(sql2);
			// Ϊ?��ֵ
			int index = 1;
			for (int i = 1; i <= count; i++) {
				if (pkNames.contains(rsmd.getColumnName(i)))
					continue;
				Method getterMethod = getGetterMethod(rsmd.getColumnName(i));
				ps.setObject(index++, getterMethod.invoke(t, null));
			}
			// ������ֵ
			for (String pkName : pkNames) {
				Method getterMethod = getGetterMethod(pkName);
				ps.setObject(index++, getterMethod.invoke(t, null));
			}
			returnValue = ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return returnValue;
	}

	/**
	 * ɾ��һ������
	 * 
	 * @param t
	 * @return
	 */
	public int delete(T t) {
		int returnValue = 0;
		Connection conn = ConnectionManager.getConn();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql1 = "select * from " + getTableName() + " where 1=2";
		try {
			ps = conn.prepareStatement(sql1);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int count = rsmd.getColumnCount();
			// ��ȡ��������
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // �������������
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}
			String sql2 = "delete from " + getTableName() + " where ";
			// ƴ��?�ַ���
			int pkNum = pkNames.size();
			int flagIndex = 0;
			for (String pkName : pkNames) {
				sql2 += pkName + "=? ";
				flagIndex++;
				if (flagIndex < pkNum) {
					sql2 += "and ";
				}
			}
			ps = conn.prepareStatement(sql2);
			int index = 1;
			// Ϊ?��ֵ
			for (String pkName : pkNames) {
				Method getterMethod = getGetterMethod(pkName);
				ps.setObject(index++, getterMethod.invoke(t, null));
			}
			returnValue = ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return returnValue;
	}
	
	/**
	 * �ҳ��������е�����
	 * 
	 * @return
	 */
	public List<T> findAll() {
		List<T> list = new ArrayList<T>();
		Connection conn = JdbcUtil.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "select * from " + getTableName();
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int count = rsmd.getColumnCount();
			Map<Integer, Method> map = new HashMap<Integer, Method>(); // ��һ����������set����ӳ��
			for (int i = 1; i <= count; i++) {
				map.put(i, getSetterMethod(rsmd.getColumnName(i)));
			}
			while (rs.next()) {
				@SuppressWarnings("unchecked")
				T t = (T) getEntityClass().newInstance();
				for (int i = 1; i <= count; i++) {
					Method setterMethod = getSetterMethod(rsmd.getColumnName(i));
					Method getterMethod = getGetterMethod(rsmd.getColumnName(i));
					Type type = getterMethod.getReturnType();
					int startIndex = 0;
					if((type.toString().lastIndexOf(".")!=-1)) {
						startIndex = type.toString().lastIndexOf(".")+1;
					}
					String paramType = type.toString().substring(startIndex);
					Method getObj = ResultSet.class.getDeclaredMethod(getter(paramType), int.class);
					Object paramObj = getObj.invoke(rs, i);
					map.get(i).invoke(t, paramObj);
				}
				list.add(t);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;

	}

	/**
	 * ��������������ָ������
	 * 
	 * @param pks
	 * @return
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public T findById(Object[] pks) throws Exception {
		@SuppressWarnings("unchecked")
		T t = (T) getEntityClass().newInstance();
		Connection conn = JdbcUtil.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql1 = "select * from " + getTableName() + " where 1=2";
		try {
			ps = conn.prepareStatement(sql1);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int count = rsmd.getColumnCount();
			// ��ȡ��������
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // �������������
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}
			// ƴ��?
			String sql2 = "select * from " + getTableName() + " where ";
			int colNum = pkNames.size();
			int index = 0;
			for (String pkName : pkNames) {
				sql2 += pkName + "=? ";
				index++;
				if (index < colNum)
					sql2 += "and ";
			}
			// ��?��ֵ
			ps = conn.prepareStatement(sql2);
			for (int i = 0; i < pks.length; i++) {
				ps.setObject(i + 1, pks[i]);
			}
			rs = ps.executeQuery();
			if (rs.next()) {
				for (int i = 1; i <= count; i++) {
					Method setterMethod = getSetterMethod(rsmd.getColumnName(i));
					Method getterMethod = getGetterMethod(rsmd.getColumnName(i));
					Type type = getterMethod.getReturnType();
					int startIndex = 0;
					if((type.toString().lastIndexOf(".")!=-1)) {
						startIndex = type.toString().lastIndexOf(".")+1;
					}
					String paramType = type.toString().substring(startIndex);
					Method getObj = ResultSet.class.getDeclaredMethod(getter(paramType), int.class);
					Object paramObj = getObj.invoke(rs, i);
					setterMethod.invoke(t, paramObj);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return t;
	}

	/**
	 * @param id
	 * @return
	 */
	public int delete(Object[] pks){
		int returnValue = 0;
		Connection conn = ConnectionManager.getConn();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql1 = "select * from " + getTableName() + " where 1=2";
		try {
			ps = conn.prepareStatement(sql1);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int count = rsmd.getColumnCount();
			// ��ȡ��������
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // �������������
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}
			// ƴ��?
			String sql2 = "delete from " + getTableName() + " where ";
			int colNum = pkNames.size();
			int index = 0;
			for (String pkName : pkNames) {
				sql2 += pkName + "=? ";
				index++;
				if (index < colNum)
					sql2 += "and ";
			}
			// ��?��ֵ
			ps = conn.prepareStatement(sql2);
			for (int i = 0; i < pks.length; i++) {
				ps.setObject(i + 1, pks[i]);
			}
			returnValue = ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return returnValue;
	}

}
