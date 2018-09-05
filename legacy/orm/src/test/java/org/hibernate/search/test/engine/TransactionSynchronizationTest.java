/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.builtin.EnumBridge;
import org.hibernate.search.test.SearchTestBase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class TransactionSynchronizationTest extends SearchTestBase {

	@org.junit.Test
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
			test.setIncorrectType( "not a class" );
			fts.persist( test );
			transaction.commit();
			fail( "An exception should have been raised" );
		}
		catch (Exception e) {
			//good
			raised = true;
			transaction.rollback();
		}
		assertTrue( "An exception should have been raised", raised );
		fts.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Test.class
		};
	}

	@Entity
	@Indexed
	@Table(name = "Test007")
	public static class Test {
		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		private Integer id;

		@Field(bridge = @FieldBridge(impl = EnumBridge.class))
		public String getIncorrectType() {
			return incorrectType;
		}

		public void setIncorrectType(String incorrectType) {
			this.incorrectType = incorrectType;
		}

		private String incorrectType;
	}
}
