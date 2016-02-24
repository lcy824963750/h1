package com.model;

import com.h1.annotations.Column;
import com.h1.annotations.Id;
import com.h1.annotations.JoinColumn;
import com.h1.annotations.ManyToOne;
import com.h1.annotations.Table;


@Table(tableName="student",lazy=false)
public class Student {
	
	@Id
	@Column(fieldName="id",isNull=false,len=-1,type="int")
	private int id;
	@Column(fieldName="name",isNull=false,len=20,type="varchar2")
	private String name;
	@Column(fieldName="age",isNull=false,len=-1,type="int")
	private int age;
	@JoinColumn(name="tea_stu")
	@ManyToOne
	private Teacher t;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public Teacher getT() {
		return t;
	}
	public void setT(Teacher t) {
		this.t = t;
	}
	
	
}
