package com.h1.util;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.h1.annotations.Column;
import com.h1.annotations.Id;
import com.h1.annotations.JoinColumn;
import com.h1.annotations.ManyToOne;
import com.h1.annotations.OneToMany;
import com.h1.annotations.Table;


/**
 * o3框架核心类
 * @author yy
 * 
 */
public class Session {

	
	private Set<String> tableNames = new HashSet<String>();	//存储已经创建过的表
	private List<String> sqlList = new ArrayList<String>();	//保存待执行的ddl语句
	private List<Element> list = new ArrayList<Element>();	//存放配置文件中的mapping元素
	private boolean showSql = false;
	
	/**
	 * 完成表的自动创建功能
	 * 通过读取o3.xml配置文件
	*/
	public void createTable() {
	
		SAXBuilder builder = new SAXBuilder();
		try {
			Document doc = builder.build(new File("./h1.xml"));
			Element root = doc.getRootElement();
			Element sesFac = root.getChild("session-factory");
			List<Element> allList = sesFac.getChildren();	//存储所有的mapping元素
			
			for (int i = 0; i < allList.size(); i++) {
				String tagName = allList.get(i).getName();
				if(tagName.equals("mapping")) {
					list.add(allList.get(i));
				}else if(tagName.equals("property")) {
					String value = allList.get(i).getAttributeValue("name");
					if(value.equals("showsql")) {
						String txtValue = allList.get(i).getTextTrim();
						if(txtValue.equals("true")) {
							showSql = true;
						}else {
							continue;
						}
					}else {
						continue;
					}
				}
			}
			
			analyseMappingList(list);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * 分析所有加载好的mapping元素
	 * @param list
	 */
	private void analyseMappingList(List<Element> list) {
		
		for (int i = 0; i < list.size(); i++) {
			try {
				Class clazz1 = Class.forName(list.get(i).getAttributeValue("class"));
			analyseBeanClass(clazz1);
			//执行sql语句
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			
		}
		for (int j = 0; j < sqlList.size(); j++) {
			executeCreateSql(sqlList.get(j));
		}
		
		sqlList.clear();
	}
	
	
	/**
	 * 分析class文件
	 * @param clazz
	 */
	private void analyseBeanClass(Class clazz1) {
	
		String simpleName = clazz1.getSimpleName();
		if(tableNames.contains(simpleName)) return;
		try {
			Annotation[] anns1 = clazz1.getAnnotations();
			String className = clazz1.getName();
			boolean hasTable = false;
			Table tableAnno = null;
			for (int j = 0; j < anns1.length; j++) {
				if(anns1[j] instanceof Table) {
					hasTable = true;
					tableAnno = (Table) anns1[j];
					break;
				}
			}
			if(!hasTable) {
				throw new Exception("表创建失败 "+className+"类缺少@Table注解");
		}else {
			String tableName = tableAnno.tableName();
			//遍历类的字段
			Field[] fields = clazz1.getDeclaredFields();
			List<String> fieldSql = new ArrayList<String>();	//field字符串
			for (int j = 0; j < fields.length; j++) {
				fieldSql.add(analyseField(clazz1,fields[j]));
			}
			
			//根据field返回字段信息拼接ddl语句
			String createSql = "create table "+tableName+"(";
			List<String> idList = new ArrayList<String>();	//存放id名称
			String fkName = "";
			String fkField = "";
			String fkTable = "";
			for (int i = 0; i < fieldSql.size(); i++) {
				String fSql = fieldSql.get(i);
				if(!(fSql.length()>0)) continue;
				if(fSql.charAt(0)=='@' && fSql.charAt(1)=='f') {
					int fknameStart = fSql.lastIndexOf("@fkname:");
					createSql += fSql.substring(4, fknameStart)+",";
					fkField=fSql.substring(4,fSql.indexOf(" "));
					fkName=fSql.substring(fSql.indexOf("@fkname:")+8,fSql.lastIndexOf("@fktable:"));
					fkTable=fSql.substring(fSql.lastIndexOf("@fktable:")+9);
				}else if(fSql.charAt(0)=='@' && fSql.charAt(1)=='i') {
					createSql += fSql.substring(4)+",";
					int spaceStart = fSql.indexOf(" ");
					idList.add(fSql.substring(4,spaceStart));
				}else {
					createSql+=fSql+",";
				}
			}
			if(idList.size()>0) {
				createSql+="primary key(";
				for (int i = 0; i < idList.size(); i++) {
					createSql+=idList.get(i);
					if(i!=idList.size()-1) createSql+=",";
				}
				createSql+="),";
			}
			if(!fkName.equals("")) {
				createSql+="constraint "+fkName+" foreign key ("+fkField+")"+" references "+fkTable+"("+fkField+"),";
			}
			createSql = createSql.substring(0,createSql.lastIndexOf(","));
			createSql+=")";
			//拼接完成
				tableNames.add(simpleName);
				sqlList.add(createSql);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 分析实体类的字段
	 * @param field
	 */
	private String analyseField(Class clazz1, Field f) {
		
		String fieldSql = "";
	try {
		Annotation[] fieldAnns = f.getAnnotations();
		Column column = null;
		Id id = null;
		JoinColumn joinColumn = null;
		OneToMany oneToMany = null;
		ManyToOne manyToOne = null;
		//遍历注解
		for (int k = 0; k < fieldAnns.length; k++) {
			Annotation ann = fieldAnns[k];
			if(ann instanceof Column) {
				column = (Column) ann;
			}else if(ann instanceof Id) {
				id = (Id) ann;
			}else if(ann instanceof JoinColumn) {
				joinColumn = (JoinColumn) ann;
			}else if(ann instanceof OneToMany) {
				oneToMany = (OneToMany) ann;
			}else if(ann instanceof ManyToOne) {
				manyToOne = (ManyToOne) ann;
			}
		}
		//进行逻辑判断
		if(column==null && joinColumn==null && oneToMany==null) {
			return "";
		}else {
			
			if(oneToMany!=null) {
				if(column==null && id==null && joinColumn==null && manyToOne==null) {
					//一对多注解的处理
				}else {
					throw new Exception(clazz1.getName()+" @ontToMany注解和其他注解冲突");
				}
			}else if(column!=null) {
				
				if(joinColumn==null && manyToOne==null && oneToMany==null) {
					if(id==null) {
						//普通列注解
						fieldSql = column.fieldName()+" "+column.type();
						if(column.len()!=-1) {
							fieldSql+="("+column.len()+")";
						}
						fieldSql+=" ";
						if(column.isNull()==false) {
							fieldSql+="not null";
						}
						return fieldSql;
					}else {
						//列注解+ID注解	id返回的field串中开头加上@id
						fieldSql="@id:";
						fieldSql += column.fieldName()+" "+column.type();
						if(column.len()!=-1) {
							fieldSql+="("+column.len()+")";
						}
						fieldSql+=" ";
						if(column.isNull()==false) {
							fieldSql+="not null";
						}
						return fieldSql;
					}
				}else {
					throw new Exception(clazz1.getName()+" @column注解和其他注解冲突");
				}
			}else if(id!=null) {
				if(column==null) {
					throw new Exception(clazz1.getName()+" @Id缺失@Column注解");
				}
			}else if(joinColumn!=null) {
				if(manyToOne==null) {
					throw new Exception(clazz1.getName()+" @joinColumn缺失@ManyToOne注解");
				}else {
					
					if(oneToMany==null&&id==null&&column==null) {
						//外键定义
						Class clazz = f.getType();
						String fkSql = getIdAsFkStrByClass(clazz);
						//判断外键所在的实体类是否已经创建好对应的表 
						String fkClassName = clazz.getSimpleName();
						if(!tableNames.contains(fkClassName)) {
							if(!isConfiguration(clazz)) {
								throw new Exception(clazz.getName()+" 类未正确的配置,外键映射失败");
							}
							analyseBeanClass(clazz);
						}
						fkSql+="@fkname:"+joinColumn.name();
						fkSql+="@fktable:"+clazz.getSimpleName();
						return fkSql;
					}else {
						throw new Exception(clazz1.getName()+" @joinColumn注解和其他注解冲突");
					}
					
				}
			}else if(manyToOne!=null) {
				if(joinColumn==null) {
					throw new Exception(clazz1.getName()+" @manyToOne缺失@joinColumn注解");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fieldSql;
	}
	
	
	/**
	 * 分析一个类 得到其主键 并返回其作为外键的基本信息
	 * @return
	 */
	private String  getIdAsFkStrByClass(Class clazz) {
		Field[] fields = clazz.getDeclaredFields();
		String fkSql = "";
	for (int i = 0; i < fields.length; i++) {
		Field field = fields[i];
		Annotation[] anns = field.getAnnotations();
		Column column = null;
		Id id = null;
		for (int j = 0; j < anns.length; j++) {
			Annotation ann = anns[j];
			if(ann instanceof Column) {
				column = (Column) ann;
			}else if(ann instanceof Id) {
				id = (Id) ann;
			}
		}
		try {
			if(id!=null && column==null) {
				throw new Exception(clazz.getName()+" @Id列缺少@column注解");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(column==null || id==null) {
			continue;
		}
		fkSql+="@fk:"+column.fieldName()+" "+column.type();
		if(column.len()!=-1) {
			fkSql+="("+column.len()+")"+" ";
		}else {
			fkSql+=" ";
		}
		if(column.isNull()==false) fkSql+="not null ";
	}
	if(fkSql.equals("")) {
		try {
			throw new Exception(clazz.getName()+" 缺少主键  外键映射失败");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return fkSql;
		
	}
	
	/**
	 * 给定一个类  判断是否已在配置文件中配置
	 * @param clazz
	 * @return
	 */
	private boolean isConfiguration(Class clazz) {
		boolean flag = false;
		String classPath = clazz.getName();
		for (int i = 0; i < list.size(); i++) {
			if(list.get(i).getAttributeValue("class").equals(classPath))  {
				flag = true;
				break;
			}
		}
		
		return flag;
	}
	
	
	/**
	 * 执行表创建语句
	 * @param sql
	 */
	private void executeCreateSql(String sql) {
				
		Connection conn = JdbcUtil.getConnection();
		PreparedStatement ps = null;
		try {
			String tableName = sql.substring(sql.indexOf("table")+5, sql.indexOf("(")).trim();
		String preSql = "DECLARE num NUMBER;BEGIN SELECT COUNT(1) INTO num FROM USER_TABLES WHERE TABLE_NAME = UPPER(?) ;"+
						"IF num > 0 THEN EXECUTE IMMEDIATE 'DROP TABLE "+tableName+" cascade constraints' ;END IF; END; ";
				
				ps = conn.prepareStatement(preSql);
				ps.setObject(1, tableName);
				if(showSql) {
					System.out.println(preSql);
				}
				ps.execute();
				ps = conn.prepareStatement(sql);
				if(showSql) {
					System.out.println(sql);
				}
				ps.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
}