/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend;

import java.util.Map;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Assert;
import org.junit.Test;

public class SyncBackendLongWorkListStressTest extends SearchTestBase {

	/* needs to be sensibly higher than org.hibernate.search.batchindexing.Executors.QUEUE_MAX_LENGTH */
	private static final int NUM_SAVED_ENTITIES = 40;

	@Test
	public void testWorkLongerThanMaxQueueSize() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );

		for ( int i = 0; i < NUM_SAVED_ENTITIES; i++ ) {
			Transaction tx = s.beginTransaction();
			Clock clock = new Clock( i, "brand num° " + i );
			s.persist( clock );
			tx.commit();
			s.clear();
		}

		Transaction tx = s.beginTransaction();
		// count of entities in database needs to be checked before SF is closed (HSQLDB will forget the entities)
		Number count = (Number) s.createCriteria( Clock.class )
				.setProjection( Projections.rowCount() )
				.uniqueResult();
		Assert.assertEquals( NUM_SAVED_ENTITIES, count.intValue() );
		tx.commit();
		s.close();

		//we need to close the SessionFactory to wait for all async work to be flushed
		closeSessionFactory();
		//and restart it again..
		openSessionFactory();

		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		int fullTextCount = s.createFullTextQuery( new MatchAllDocsQuery(), Clock.class ).getResultSize();
		Assert.assertEquals( NUM_SAVED_ENTITIES, fullTextCount );
		s.purgeAll( Clock.class );
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		//needs FSDirectory to have the index contents survive the SessionFactory close
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.put( "hibernate.search.default.max_queue_length", "5" );
		cfg.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.put( "hibernate.search.default.elasticsearch.index_schema_management_strategy", "update" );

	}

}
