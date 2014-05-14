/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl.embedded;

import java.util.List;

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Davide D'Alto
 */
public class DslEmbeddedSearchTest extends SearchTestBase {

	private Session s = null;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		EmbeddedEntity ee = new EmbeddedEntity();
		ee.setEmbeddedField( "embedded" );
		ee.setNumber( 7 );

		ContainerEntity pe = new ContainerEntity();
		pe.setEmbeddedEntity( ee );
		pe.setParentStringValue( "theparentvalue" );

		s = openSession();
		s.getTransaction().begin();
		s.persist( pe );
		s.getTransaction().commit();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		s.clear();
		deleteAll( s, ContainerEntity.class );
		s.close();
		super.tearDown();
	}

	@Test
	public void testSearchString() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ContainerEntity.class ).get();
		Query q = qb.keyword().onField( "emb.embeddedField" ).matching( "embedded" ).createQuery();
		List<ContainerEntity> results = execute( fullTextSession, q );

		assertEquals( "DSL didn't find the embedded string field", 1, results.size() );
		assertEquals( "embedded", results.get( 0 ).getEmbeddedEntity().getEmbeddedField() );

	}

	@Test
	public void testSearchNumberWithFieldBridge() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder()
				.forEntity( ContainerEntity.class ).get();
		Query q = qb.keyword().onField( "emb.num" ).matching( 7 ).createQuery();
		List<ContainerEntity> results = execute( fullTextSession, q );

		assertEquals( "DSL didn't find the embedded numeric field", 1, results.size() );
		assertEquals( Integer.valueOf( 7 ), results.get( 0 ).getEmbeddedEntity().getNumber() );
	}

	@SuppressWarnings("unchecked")
	private List<ContainerEntity> execute(FullTextSession fullTextSession, Query q) {
		FullTextQuery combinedQuery = fullTextSession.createFullTextQuery( q, ContainerEntity.class );

		return combinedQuery.list();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ContainerEntity.class };
	}

	private void deleteAll(Session s, Class<?>... classes) {
		Transaction tx = s.beginTransaction();
		for ( Class<?> each : classes ) {
			List<?> list = s.createCriteria( each ).list();
			for ( Object object : list ) {
				s.delete( object );
			}
		}
		tx.commit();
	}
}
