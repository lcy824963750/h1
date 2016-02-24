package com.service;

import com.dao.PersonDao;
import com.h1.annotations.Transaction;
import com.model.Person;

public class PersonService {
	
	
	@Transaction(needTx=true)
	public void charge(int fromId , int toId, int money) throws Exception {
		PersonDao dao = new PersonDao();
		Object[] obj1 = {fromId};
		Object[] obj2 = {toId};
		Person from = dao.findById(obj1);
		Person to = dao.findById(obj2);
		
		from.setMoney(from.getMoney()-money);
		dao.update(from);
		
		System.out.println(9/0);
		
		
		to.setMoney(to.getMoney()+money);
		dao.update(to);
	}
	

}
