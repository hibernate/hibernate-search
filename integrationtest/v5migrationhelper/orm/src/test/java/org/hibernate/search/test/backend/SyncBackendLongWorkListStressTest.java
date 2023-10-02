/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.countAll;

import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.MatchAllDocsQuery;

class SyncBackendLongWorkListStressTest extends SearchTestBase {

	/* needs to be sensibly higher than org.hibernate.search.batchindexing.Executors.QUEUE_MAX_LENGTH */
	private static final int NUM_SAVED_ENTITIES = 40;

	@Test
	void testWorkLongerThanMaxQueueSize() {
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
		Number count = countAll( s, Clock.class );
		assertThat( count.intValue() ).isEqualTo( NUM_SAVED_ENTITIES );
		tx.commit();
		s.close();

		//we need to close the SessionFactory to wait for all async work to be flushed
		closeSessionFactory();
		//and restart it again..
		openSessionFactory();

		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		int fullTextCount = s.createFullTextQuery( new MatchAllDocsQuery(), Clock.class ).getResultSize();
		assertThat( fullTextCount ).isEqualTo( NUM_SAVED_ENTITIES );
		s.purgeAll( Clock.class );
		tx.commit();
		s.close();
	}

	@Override
	public void tearDown() throws Exception {
		org.hibernate.search.mapper.orm.Search.mapping( getSessionFactory() )
				.scope( Object.class )
				.schemaManager()
				.dropIfExisting();

		super.tearDown();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		// The index content must survive the SessionFactory close/recreate
		cfg.put( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY, SchemaManagementStrategyName.CREATE );

		// The queues must be small enough to be a bottleneck
		// (apparently that was the intent of the original test in Search 5)
		cfg.put( BackendSettings.backendKey( LuceneIndexSettings.INDEXING_QUEUE_SIZE ), 5 );
		cfg.put( BackendSettings.backendKey( LuceneIndexSettings.INDEXING_QUEUE_COUNT ), 2 );

		cfg.put( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
				AutomaticIndexingSynchronizationStrategyNames.SYNC );
	}

}
