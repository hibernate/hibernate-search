/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.similarity;

import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Emmanuel Bernard
 */
@Category(SkipOnElasticsearch.class) // Similarity tuning is specific to the Lucene backend
public class SimilarityTest extends SearchTestBase {

	@Test
	public void testConfiguredDefaultSimilarityGetsApplied() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Can can = new Can();
		can.setName( "Green can" );
		s.persist( can );

		can = new Can();
		can.setName( "Green Green Green can" );
		s.persist( can );

		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		TermQuery termQuery = new TermQuery( new Term( "name", "green" ) );
		FullTextSession fullTextSession = Search.getFullTextSession( s );
		List results = fullTextSession.createFullTextQuery( termQuery, Can.class )
				.setProjection( FullTextQuery.SCORE, FullTextQuery.THIS )
				.list();
		assertEquals( 2, results.size() );
		assertEquals(
				"Similarity not overridden by the global setting",
				( (Object[]) results.get( 0 ) )[0],
				( (Object[]) results.get( 1 ) )[0]
		);
		assertFalse(
				"Similarity not overridden by the global setting",
				new Float( 1.0f ).equals( ( (Object[]) results.get( 0 ) )[0] )
		);

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Can.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.SIMILARITY_CLASS, DummySimilarity2.class.getName() );
	}

}
