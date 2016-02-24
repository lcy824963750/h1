package com.test;

import com.dao.PersonDao;
import com.model.Person;

public class InsertData {
	
	
	public static void main(String[] args) {
		PersonDao dao = new PersonDao();
		Person from = new Person(1, "张三", 100);
		Person to = new Person(2,"李四",20);
		dao.save(from);
		dao.save(to);
		Object[] obj = {1};
		try {
			Person person = dao.findById(obj);
			System.out.println(person.getMoney());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
