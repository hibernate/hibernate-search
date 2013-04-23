package org.hibernate.search.test.integration.jtaspring;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:beans.xml"}) 
//@TransactionConfiguration(transactionManager="transactionManager",defaultRollback=false)
//@Transactional
public class JtaAndSpringMoreComplexTest {
	@Inject
	private BoxDAO boxDAO;

	@Test
	public void testMufins() throws Exception {
		Box box = new Box();
		box.setColor("red-and-white");
		boxDAO.persist(box);
		
		Muffin muffin = new Muffin();
		muffin.setKind("blueberry");
		muffin.setBox(box);
		
		box.addMuffin(muffin);

		boxDAO.merge(box);
	}

	@Test
	public void testDoughnuts() throws Exception {
		Box box = new Box();
		box.setColor("red-and-white");
		boxDAO.persist(box);
		
		Doughnut doughnut = new Doughnut();
		doughnut.setKind("glazed");
		doughnut.setBox(box);
		
		box.addDoughnut(doughnut);

		boxDAO.merge(box);
	}
}
