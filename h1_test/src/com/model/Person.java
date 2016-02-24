package com.model;

import com.h1.annotations.Column;
import com.h1.annotations.Id;
import com.h1.annotations.Table;

@Table(tableName="Person",lazy=false)
public class Person {
	
	@Id
	@Column(fieldName="pid",isNull=false,len=-1,type="number")
	private int pid;
	public Person(int pid, String name, int money) {
		super();
		this.pid = pid;
		this.name = name;
		this.money = money;
	}

	@Override
	public String toString() {
		return "Person [pid=" + pid + ", name=" + name + ", money=" + money
				+ "]";
	}

	public Person() {
		
	}
	
	@Column(fieldName="name",isNull=false,len=20,type="varchar2")
	private String name;
	@Column(fieldName="money",isNull=false,len=-1,type="number")
	private int money;

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMoney() {
		return money;
	}

	public void setMoney(int money) {
		this.money = money;
	}

	
	
	
}
