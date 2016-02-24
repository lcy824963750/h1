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
 * dao封装类 完成对一些常用方法的通用封装
 * 
 * @author yy
 * 
 * @param <T>
 */
public class BaseJdbcDao<T> {

	private Class entityClass; // 泛型T代表的类

	private Class getEntityClass() {
		if (entityClass == null) {
			entityClass = GenericClass.getGenericClass(this.getClass());
		}
		return entityClass;
	}

	/**
	 * 得到表名字
	 * 
	 * @return
	 */
	private String getTableName() {
		return getEntityClass().getSimpleName();
	}

	/**
	 * 根据列名得到对应模型对象的get方法名字
	 * 
	 * @param name
	 * @return
	 */
	private String getter(String name) {
		return "get" + name.substring(0, 1).toUpperCase()
				+ name.substring(1).toLowerCase();
	}

	/**
	 * 根据列名得到对应模型对象的set方法名字
	 * 
	 * @param name
	 * @return
	 */
	private String setter(String name) {
		return "set" + name.substring(0, 1).toUpperCase()
				+ name.substring(1).toLowerCase();
	}

	/**
	 * 根据列名得到其在泛型表示具体类中的get方法
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
	 * 根据列名得到其在泛型表示具体类中的set方法
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
	 * 保存一个对象到数据库中
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
			// 拼接?字符串
			for (int i = 0; i < count - 1; i++) {
				sql2 += "?,";
			}
			sql2 += "?)";
			ps = conn.prepareStatement(sql2);
			// 给?赋值
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
	 * 修改一个对象
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
			// 首先找到表的主键
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // 存放主键的名字
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}

			// 拼接?问号
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
			// 为?赋值
			int index = 1;
			for (int i = 1; i <= count; i++) {
				if (pkNames.contains(rsmd.getColumnName(i)))
					continue;
				Method getterMethod = getGetterMethod(rsmd.getColumnName(i));
				ps.setObject(index++, getterMethod.invoke(t, null));
			}
			// 主键赋值
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
	 * 删除一个对象
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
			// 获取主键名字
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // 存放主键的名字
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}
			String sql2 = "delete from " + getTableName() + " where ";
			// 拼接?字符串
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
			// 为?赋值
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
	 * 找出表中所有的数据
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
			Map<Integer, Method> map = new HashMap<Integer, Method>(); // 存一下列索引和set方法映射
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
	 * 根据主键来查找指定的行
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
			// 获取主键名字
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // 存放主键的名字
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}
			// 拼接?
			String sql2 = "select * from " + getTableName() + " where ";
			int colNum = pkNames.size();
			int index = 0;
			for (String pkName : pkNames) {
				sql2 += pkName + "=? ";
				index++;
				if (index < colNum)
					sql2 += "and ";
			}
			// 给?赋值
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
			// 获取主键名字
			DatabaseMetaData dbmd = conn.getMetaData();
			ResultSet rs2 = dbmd.getPrimaryKeys(null, null, getTableName().toUpperCase());
			Set<String> pkNames = new HashSet<String>(); // 存放主键的名字
			while (rs2.next()) {
				pkNames.add(rs2.getString(4));
			}
			// 拼接?
			String sql2 = "delete from " + getTableName() + " where ";
			int colNum = pkNames.size();
			int index = 0;
			for (String pkName : pkNames) {
				sql2 += pkName + "=? ";
				index++;
				if (index < colNum)
					sql2 += "and ";
			}
			// 给?赋值
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
