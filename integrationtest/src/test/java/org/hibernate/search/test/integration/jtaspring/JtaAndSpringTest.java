package org.hibernate.search.test.integration.jtaspring;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:beans.xml"}) 
@TransactionConfiguration(transactionManager="transactionManager",defaultRollback=false)
@Transactional
public class JtaAndSpringTest {
	@Inject
	private SnertDAO snertDAO;
	
	@Test
	public void test() {
		Snert snert = new Snert();
		
		snert.setName("dave");
		snert.setNickname("dude");
		snert.setAge(99);
		snert.setCool(Boolean.TRUE);
		
		snertDAO.persist(snert);
	}
}
