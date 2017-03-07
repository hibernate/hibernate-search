/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.validation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchTestBase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class QueryValidationTest extends SearchTestBase {
	private FullTextSession fullTextSession;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		Transaction tx = openSession().beginTransaction();
		getSession().save( new A() );
		tx.commit();
		getSession().close();

		this.fullTextSession = Search.getFullTextSession( openSession() );
	}

	@After
	@Override
	public void tearDown() throws Exception {
		fullTextSession.close();
		super.tearDown();
	}

	@Test
	public void testTargetStringEncodedFieldWithNumericRangeQueryThrowsException() {
		Query query = NumericFieldUtils.createNumericRangeQuery( "value", 1, 1, true, true );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, A.class );
		try {
			fullTextQuery.list();
			fail();
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000232" ) );
		}
	}

	@Test
	public void testTargetNumericEncodedFieldWithStringQueryThrowsException() {
		TermQuery query = new TermQuery( new Term( "value", "bar" ) );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, B.class );
		try {
			fullTextQuery.list();
			fail();
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000233" ) );
		}
	}

	@Test
	public void testTargetingNonIndexedEntityThrowsException() {
		TermQuery query = new TermQuery( new Term( "foo", "bar" ) );
		try {
			fullTextSession.createFullTextQuery( query, C.class );
		}
		catch (IllegalArgumentException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000234" ) );
		}
	}

	@Test
	public void testTargetingNonConfiguredEntityThrowsException() {
		TermQuery query = new TermQuery( new Term( "foo", "bar" ) );
		try {
			fullTextSession.createFullTextQuery( query, D.class );
		}
		catch (IllegalArgumentException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000332" ) );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class,
				C.class
		};
	}

	@Entity
	@Indexed
	public static class A {
		@Id
		@GeneratedValue
		private long id;

		@Field
		private String value;
	}

	@Entity
	@Indexed
	public static class B {
		@Id
		@GeneratedValue
		private long id;

		@Field
		private long value;
	}

	@Entity
	public static class C {
		@Id
		@GeneratedValue
		private long id;
	}

	@Entity
	public static class D {
		@Id
		@GeneratedValue
		private long id;
	}
}


