/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.explain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class ExplanationTest extends SearchTestBase {
	@Test
	void testExplanation() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Dvd dvd = new Dvd( "The dark knight",
				"Batman returns with his best enemy the Joker. The dark side of this movies shows up pretty quickly" );
		s.persist( dvd );
		dvd = new Dvd( "Wall-e", "The tiny little robot comes to Earth after the dark times and tries to clean it" );
		s.persist( dvd );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		Map<String, Float> boosts = new HashMap<String, Float>( 2 );
		boosts.put( "title", new Float( 4 ) );
		boosts.put( "description", new Float( 1 ) );
		MultiFieldQueryParser parser = new MultiFieldQueryParser( new String[] { "title", "description" },
				TestConstants.standardAnalyzer, boosts );
		Query luceneQuery = parser.parse( "dark" );
		FullTextQuery ftQuery = s.createFullTextQuery( luceneQuery, Dvd.class )
				.setProjection( FullTextQuery.ID, FullTextQuery.EXPLANATION, FullTextQuery.THIS );
		@SuppressWarnings("unchecked")
		List<Object[]> results = ftQuery.list();
		assertThat( results ).hasSize( 2 );
		for ( Object[] result : results ) {
			assertThat( result[1].toString() ).isEqualTo( ftQuery.explain( result[0] ).toString() );
			s.delete( result[2] );
		}
		tx.commit();
		s.close();

	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Dvd.class
		};
	}
}
