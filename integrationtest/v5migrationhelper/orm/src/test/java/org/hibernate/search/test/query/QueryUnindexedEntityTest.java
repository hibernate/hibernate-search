/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * HSEARCH-162 - trying to index an entity which is not marked with @Indexed
 *
 * @author Hardy Ferentschik
 */
public class QueryUnindexedEntityTest extends SearchTestBase {

	@Test
	public void testQueryOnAllEntities() throws Exception {

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
			assertThat( e ).hasMessageContainingAll( "No matching indexed entity types for types: [java.lang.Object]",
					"These types are not indexed entity types, nor is any of their subtypes" );
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
