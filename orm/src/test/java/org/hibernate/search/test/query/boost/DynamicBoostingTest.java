/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.boost;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(SkipOnElasticsearch.class) // Dynamic boosting is not supported on Elasticsearch
public class DynamicBoostingTest extends SearchTestBase {

	private static final Log log = LoggerFactory.make();

	@Test
	public void testDynamicBoosts() throws Exception {

		Session session = openSession();
		session.beginTransaction();

		DynamicBoostedDescriptionLibrary lib1 = new DynamicBoostedDescriptionLibrary();
		lib1.setName( "one" );
		session.persist( lib1 );

		DynamicBoostedDescriptionLibrary lib2 = new DynamicBoostedDescriptionLibrary();
		lib2.setName( "two" );
		session.persist( lib2 );

		session.getTransaction().commit();
		session.close();

		float lib1Score = getScore( new TermQuery( new Term( "name", "one" ) ) );
		float lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertEquals( "The scores should be equal", lib1Score, lib2Score, 0f );

		// set dynamic score and reindex!
		session = openSession();
		session.beginTransaction();

		session.refresh( lib2 );
		lib2.setDynScore( 2.0f );

		session.getTransaction().commit();
		session.close();

		lib1Score = getScore( new TermQuery( new Term( "name", "one" ) ) );
		lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertTrue( "lib2score should be greater than lib1score", lib1Score < lib2Score );



		lib1Score = getScore( new TermQuery( new Term( "name", "foobar" ) ) );
		assertEquals( "lib1score should be 0 since term is not yet indexed.", 0.0f, lib1Score, 0f );

		// index foobar
		session = openSession();
		session.beginTransaction();

		session.refresh( lib1 );
		lib1.setName( "foobar" );

		session.getTransaction().commit();
		session.close();

		lib1Score = getScore( new TermQuery( new Term( "name", "foobar" ) ) );
		lib2Score = getScore( new TermQuery( new Term( "name", "two" ) ) );
		assertTrue( "lib1score should be greater than lib2score", lib1Score > lib2Score );
	}

	private float getScore(Query query) {
		Session session = openSession();
		Object[] queryResult;
		float score;
		try {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			List resultList = fullTextSession
					.createFullTextQuery( query, DynamicBoostedDescriptionLibrary.class )
					.setProjection( ProjectionConstants.SCORE, ProjectionConstants.EXPLANATION )
					.setMaxResults( 1 )
					.list();

			if ( resultList.size() == 0 ) {
				score = 0.0f;
			}
			else {
				queryResult = (Object[]) resultList.get( 0 );
				score = (Float) queryResult[0];
				String explanation = queryResult[1].toString();
				log.debugf( "score: %f explanation: %s", score, explanation );
			}
		}
		finally {
			session.close();
		}
		return score;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				DynamicBoostedDescriptionLibrary.class
		};
	}
}
