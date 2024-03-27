/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.engine;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.test.SearchTestBase;

/**
 * @author Emmanuel Bernard
 */
class TransactionSynchronizationTest extends SearchTestBase {

	@org.junit.jupiter.api.Test
	void testProperExceptionPropagation() {
		/*
		 * This test relies on the fact that Hibernate Search needs to call getFailing()
		 * during indexing, and that this method will throw an exception.
		 */
		FullTextSession fts = Search.getFullTextSession( openSession() );
		boolean raised = false;
		final Transaction transaction = fts.beginTransaction();
		try {
			Test test = new Test();
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

		@Field
		@Transient
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		public String getFailing() {
			throw new IllegalStateException( "Simulated failure" );
		}
	}
}
