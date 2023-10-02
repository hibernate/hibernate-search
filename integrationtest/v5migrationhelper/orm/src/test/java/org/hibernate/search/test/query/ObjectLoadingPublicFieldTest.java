/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.common.AssertionFailure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Tests all kinds of loading when the entities have public fields.
 *
 * We take care to test all of those because they trigger different kinds of loading:
 * <ul>
 * <li>Single result or multiple results
 * <li>Single class or multiple classes
 * <li>list() without projection or list() with THIS projection
 * </ul>
 *
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2541")
class ObjectLoadingPublicFieldTest extends SearchTestBase {

	private Query fieldFooQuery;
	private Query fieldBarQuery;

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();

			// used for the filtering tests
			A a1 = new A();
			a1.id = 1L;
			a1.field = "foo";
			B b = new B();
			b.id = 2L;
			b.field = "foo";
			C c = new C();
			c.id = 3L;
			c.field = "foo";
			session.persist( a1 );
			session.persist( b );
			session.persist( c );

			// used for the not found test
			A a2 = new A();
			a2.id = 4L;
			a2.field = "bar";
			session.persist( a2 );

			tx.commit();
		}

		fieldFooQuery = new TermQuery( new Term( "field", "foo" ) );
		fieldBarQuery = new TermQuery( new Term( "field", "bar" ) );
	}

	@Test
	void singleClass_singleResult() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( fieldFooQuery, A.class );
		List<?> result = fullTextQuery.list();
		assertThat( result ).as( "Should match B only" ).hasSize( 1 );
		assertPopulated( result );

		fullTextSession.clear();
		fullTextQuery = fullTextSession.createFullTextQuery( fieldFooQuery, A.class )
				.setProjection( ProjectionConstants.THIS );
		result = fullTextQuery.list();
		assertPopulated( result );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void singleClass_multipleResults() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Query luceneQuery = new MatchAllDocsQuery();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, A.class );
		List<?> result = fullTextQuery.list();
		assertThat( result ).as( "Should match A only" ).hasSize( 2 );
		assertPopulated( result );

		fullTextSession.clear();
		fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, A.class )
				.setProjection( ProjectionConstants.THIS );
		result = fullTextQuery.list();
		assertPopulated( result );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void twoClasses_singleResult() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( fieldBarQuery, A.class, B.class );
		List<?> result = fullTextQuery.list();
		assertThat( result ).as( "Should match a single A only" ).hasSize( 1 );
		assertPopulated( result );

		fullTextSession.clear();
		fullTextQuery = fullTextSession.createFullTextQuery( fieldBarQuery, A.class, B.class )
				.setProjection( ProjectionConstants.THIS );
		result = fullTextQuery.list();
		assertPopulated( result );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void twoClasses_multipleResults() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( fieldFooQuery, A.class, B.class );
		List<?> result = fullTextQuery.list();
		assertThat( result ).as( "Should match A and B only" ).hasSize( 2 );
		assertPopulated( result );

		fullTextSession.clear();
		fullTextQuery = fullTextSession.createFullTextQuery( fieldFooQuery, A.class, B.class )
				.setProjection( ProjectionConstants.THIS );
		result = fullTextQuery.list();
		assertPopulated( result );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void threeClasses() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
				fieldFooQuery, A.class, B.class, C.class
		);
		List<?> result = fullTextQuery.list();
		assertThat( result ).as( "Should match all types" ).hasSize( 3 );
		assertPopulated( result );

		fullTextSession.clear();
		fullTextQuery = fullTextSession.createFullTextQuery(
				fieldFooQuery, A.class, B.class, C.class
		)
				.setProjection( ProjectionConstants.THIS );
		result = fullTextQuery.list();
		assertPopulated( result );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void implicitAllClasses() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( fieldFooQuery );
		List<?> result = fullTextQuery.list();
		assertThat( result ).as( "Should match all types" ).hasSize( 3 );
		assertPopulated( result );

		fullTextSession.clear();
		fullTextQuery = fullTextSession.createFullTextQuery( fieldFooQuery )
				.setProjection( ProjectionConstants.THIS );
		result = fullTextQuery.list();
		assertPopulated( result );

		tx.commit();
		fullTextSession.close();
	}

	private void assertPopulated(List<?> results) {
		for ( Object result : results ) {
			assertPopulated( result );
		}
	}

	private void assertPopulated(Object result) {
		if ( result instanceof Object[] ) {
			Object[] tuple = (Object[]) result;
			if ( tuple.length != 1 ) {
				throw new AssertionFailure( "Unexpected tuple size for result " + result );
			}
			result = tuple[0];
		}
		if ( result instanceof A ) {
			assertThat( ( (A) result ).id ).isNotNull();
		}
		else if ( result instanceof B ) {
			assertThat( ( (B) result ).id ).isNotNull();
		}
		else if ( result instanceof C ) {
			assertThat( ( (C) result ).id ).isNotNull();
		}
		else {
			throw new AssertionFailure( "Unexpected type for result " + result );
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
	@Table(name = "A")
	@Indexed
	private static class A {
		protected A() {
		}

		@Id
		public Long id;

		@Field
		public String field;
	}

	@Entity
	@Indexed
	private static class B {
		protected B() {
		}

		@Id
		public Long id;

		@Field
		public String field;
	}

	@Entity
	@Indexed
	private static class C {
		protected C() {
		}

		@Id
		public Long id;

		@Field
		public String field;
	}

}
