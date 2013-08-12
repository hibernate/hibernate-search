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
package org.hibernate.search.test.shards;

import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Chase Seibert
 */
public class DirectoryProviderForQueryTest extends SearchTestCase {

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
