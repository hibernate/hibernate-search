/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * @author Hardy Ferentschik
 */
class QueryValidationTest extends SearchTestBase {
	private FullTextSession fullTextSession;

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();

		Transaction tx = openSession().beginTransaction();
		getSession().save( new A() );
		tx.commit();
		getSession().close();

		this.fullTextSession = Search.getFullTextSession( openSession() );
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		fullTextSession.close();
		super.tearDown();
	}

	@Test
	void testTargetingNonIndexedEntityThrowsException() {
		TermQuery query = new TermQuery( new Term( "foo", "bar" ) );
		assertThatThrownBy( () -> fullTextSession.createFullTextQuery( query, C.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "No matching indexed entity types for classes [" + C.class.getName() + "]",
						"Neither these classes nor any of their subclasses are indexed" );
	}

	@Test
	void testTargetingNonConfiguredEntityThrowsException() {
		TermQuery query = new TermQuery( new Term( "foo", "bar" ) );
		assertThatThrownBy( () -> fullTextSession.createFullTextQuery( query, D.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "No matching indexed entity types for classes [" + D.class.getName() + "]",
						"Neither these classes nor any of their subclasses are indexed" );
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
		@Column(name = "\"value\"")
		private String value;
	}

	@Entity
	@Indexed
	public static class B {
		@Id
		@GeneratedValue
		private long id;

		@Field
		@Column(name = "\"value\"")
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

