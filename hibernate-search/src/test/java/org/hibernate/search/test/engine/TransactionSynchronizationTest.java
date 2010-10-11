package org.hibernate.search.test.engine;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.builtin.ClassBridge;
import org.hibernate.search.test.SearchTestCase;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
public class TransactionSynchronizationTest extends SearchTestCase {

	public void testProperExceptionPropagation() throws Exception {
		/**
		 * This test relies on the fact that a bridge accepting an incompatible type raise
		 * an exception when used.
		 */
		FullTextSession fts = Search.getFullTextSession( openSession() );
		boolean raised = false;
		final Transaction transaction = fts.beginTransaction();
		try {
			Test test = new Test();
			test.setIncorrectType("not a class");
			fts.persist(test);
			transaction.commit();
			fail("An exception should have been raised");
		}
		catch (Exception e) {
			//good
			raised = true;
			transaction.rollback();
		}
		assertTrue("An exception should have been raised", raised);
		fts.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Test.class
		};
	}

	@Entity
	@Indexed
	@Table(name="Test007")
	public static class Test {
		@Id @GeneratedValue
		public Integer getId() { return id; }
		public void setId(Integer id) { this.id = id; }
		private Integer id;

		@Field(bridge = @FieldBridge(impl = ClassBridge.class))
		public String getIncorrectType() { return incorrectType; }
		public void setIncorrectType(String incorrectType) { this.incorrectType = incorrectType; }
		private String incorrectType;
	}
}
