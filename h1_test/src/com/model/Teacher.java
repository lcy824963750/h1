package com.model;

import java.util.Set;

import com.h1.annotations.Column;
import com.h1.annotations.Id;
import com.h1.annotations.OneToMany;
import com.h1.annotations.Table;

@Table(tableName="teacher",lazy=true)
public class Teacher {
	
	@Column(fieldName="name",isNull=false,len=20,type="varchar2")
	private String name;
	@Id
	@Column(fieldName="tid",isNull=false,len=-1,type="int")
	private int tid;
	
	@OneToMany
	private Set<Student> set;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getTid() {
		return tid;
	}
	public void setTid(int tid) {
		this.tid = tid;
	}
	public Set<Student> getSet() {
		return set;
	}
	public void setSet(Set<Student> set) {
		this.set = set;
	}
	
	
	

}
