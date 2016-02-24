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
 * o3��ܺ�����
 * @author yy
 * 
 */
public class Session {

	
	private Set<String> tableNames = new HashSet<String>();	//�洢�Ѿ��������ı�
	private List<String> sqlList = new ArrayList<String>();	//�����ִ�е�ddl���
	private List<Element> list = new ArrayList<Element>();	//��������ļ��е�mappingԪ��
	private boolean showSql = false;
	
	/**
	 * ��ɱ���Զ���������
	 * ͨ����ȡo3.xml�����ļ�
	*/
	public void createTable() {
	
		SAXBuilder builder = new SAXBuilder();
		try {
			Document doc = builder.build(new File("./h1.xml"));
			Element root = doc.getRootElement();
			Element sesFac = root.getChild("session-factory");
			List<Element> allList = sesFac.getChildren();	//�洢���е�mappingԪ��
			
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
	 * �������м��غõ�mappingԪ��
	 * @param list
	 */
	private void analyseMappingList(List<Element> list) {
		
		for (int i = 0; i < list.size(); i++) {
			try {
				Class clazz1 = Class.forName(list.get(i).getAttributeValue("class"));
			analyseBeanClass(clazz1);
			//ִ��sql���
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
	 * ����class�ļ�
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
				throw new Exception("����ʧ�� "+className+"��ȱ��@Tableע��");
		}else {
			String tableName = tableAnno.tableName();
			//��������ֶ�
			Field[] fields = clazz1.getDeclaredFields();
			List<String> fieldSql = new ArrayList<String>();	//field�ַ���
			for (int j = 0; j < fields.length; j++) {
				fieldSql.add(analyseField(clazz1,fields[j]));
			}
			
			//����field�����ֶ���Ϣƴ��ddl���
			String createSql = "create table "+tableName+"(";
			List<String> idList = new ArrayList<String>();	//���id����
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
			//ƴ�����
				tableNames.add(simpleName);
				sqlList.add(createSql);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * ����ʵ������ֶ�
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
		//����ע��
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
		//�����߼��ж�
		if(column==null && joinColumn==null && oneToMany==null) {
			return "";
		}else {
			
			if(oneToMany!=null) {
				if(column==null && id==null && joinColumn==null && manyToOne==null) {
					//һ�Զ�ע��Ĵ���
				}else {
					throw new Exception(clazz1.getName()+" @ontToManyע�������ע���ͻ");
				}
			}else if(column!=null) {
				
				if(joinColumn==null && manyToOne==null && oneToMany==null) {
					if(id==null) {
						//��ͨ��ע��
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
						//��ע��+IDע��	id���ص�field���п�ͷ����@id
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
					throw new Exception(clazz1.getName()+" @columnע�������ע���ͻ");
				}
			}else if(id!=null) {
				if(column==null) {
					throw new Exception(clazz1.getName()+" @Idȱʧ@Columnע��");
				}
			}else if(joinColumn!=null) {
				if(manyToOne==null) {
					throw new Exception(clazz1.getName()+" @joinColumnȱʧ@ManyToOneע��");
				}else {
					
					if(oneToMany==null&&id==null&&column==null) {
						//�������
						Class clazz = f.getType();
						String fkSql = getIdAsFkStrByClass(clazz);
						//�ж�������ڵ�ʵ�����Ƿ��Ѿ������ö�Ӧ�ı� 
						String fkClassName = clazz.getSimpleName();
						if(!tableNames.contains(fkClassName)) {
							if(!isConfiguration(clazz)) {
								throw new Exception(clazz.getName()+" ��δ��ȷ������,���ӳ��ʧ��");
							}
							analyseBeanClass(clazz);
						}
						fkSql+="@fkname:"+joinColumn.name();
						fkSql+="@fktable:"+clazz.getSimpleName();
						return fkSql;
					}else {
						throw new Exception(clazz1.getName()+" @joinColumnע�������ע���ͻ");
					}
					
				}
			}else if(manyToOne!=null) {
				if(joinColumn==null) {
					throw new Exception(clazz1.getName()+" @manyToOneȱʧ@joinColumnע��");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fieldSql;
	}
	
	
	/**
	 * ����һ���� �õ������� ����������Ϊ����Ļ�����Ϣ
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
				throw new Exception(clazz.getName()+" @Id��ȱ��@columnע��");
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
			throw new Exception(clazz.getName()+" ȱ������  ���ӳ��ʧ��");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return fkSql;
		
	}
	
	/**
	 * ����һ����  �ж��Ƿ����������ļ�������
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
	 * ִ�б������
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