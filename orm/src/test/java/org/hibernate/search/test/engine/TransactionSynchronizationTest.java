/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Test.class
		};
	}

	@Entity
	@Indexed
	@Table(name = "Test007")
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
