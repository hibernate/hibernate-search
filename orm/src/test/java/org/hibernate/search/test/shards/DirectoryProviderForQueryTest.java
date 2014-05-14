/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import java.util.List;

import org.apache.lucene.queryparser.classic.QueryParser;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chase Seibert
 */
public class DirectoryProviderForQueryTest extends SearchTestBase {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		// this strategy allows the caller to use a pre-search filter to define which index to hit
		cfg.setProperty( "hibernate.search.Email.sharding_strategy", SpecificShardingStrategy.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Email.sharding_strategy.nbr_of_shards", "2" );
	}

	/**
	 * Test that you can filter by shard
	 */
	@Test
	public void testDirectoryProviderForQuery() throws Exception {

		Session s = openSession( );
		Transaction tx = s.beginTransaction();

		Email a = new Email();
		a.setId( 1 );
		a.setBody( "corporate message" );
		s.persist( a );

		a = new Email();
		a.setId( 2 );
		a.setBody( "spam message" );
		s.persist( a );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );

		FullTextQuery fullTextQuery = fts.createFullTextQuery( parser.parse( "body:message" ) );
		List results = fullTextQuery.list();
		assertEquals( "Query with no filter should bring back results from both shards.", 2, results.size() );

		// index is not a field on the entity; the only way to filter on this is by shard
		fullTextQuery.enableFullTextFilter( "shard" ).setParameter( "index", 0 );
		assertEquals( "Query with filter should bring back results from only one shard.", 1, fullTextQuery.list().size() );

		for ( Object o : results ) {
			s.delete( o );
		}
		tx.commit();
		s.close();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Email.class
		};
	}

}
