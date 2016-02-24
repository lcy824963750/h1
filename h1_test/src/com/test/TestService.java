package com.test;

import com.h1.proxy.CglibProxy;
import com.service.PersonService;

public class TestService {
	
	public static void main(String[] args)	{
		CglibProxy proxy = new CglibProxy();
		PersonService personService = (PersonService) proxy.getProxy(PersonService.class);
		try {
			personService.charge(1, 2,1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
