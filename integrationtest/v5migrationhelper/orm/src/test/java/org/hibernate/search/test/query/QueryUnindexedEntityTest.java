/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * HSEARCH-162 - trying to index an entity which is not marked with @Indexed
 *
 * @author Hardy Ferentschik
 */
class QueryUnindexedEntityTest extends SearchTestBase {

	@Test
	void testQueryOnAllEntities() throws Exception {

		FullTextSession s = Search.getFullTextSession( openSession() );

		Transaction tx = s.beginTransaction();
		Person person = new Person();
		person.setName( "Jon Doe" );
		s.save( person );
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "name", TestConstants.standardAnalyzer );
		Query query = parser.parse( "name:foo" );
		try {
			s.createFullTextQuery( query );
			fail();
		}
		catch (SearchException e) {
			assertThat( e ).hasMessageContainingAll( "No matching indexed entity types for classes [java.lang.Object]",
					"Neither these classes nor any of their subclasses are indexed" );
		}

		tx.rollback();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
		};
	}
}
