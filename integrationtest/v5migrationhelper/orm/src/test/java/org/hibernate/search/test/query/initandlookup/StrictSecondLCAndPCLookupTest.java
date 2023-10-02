/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.initandlookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.testsupport.StaticIndexingSwitch;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.cache.CachingRegionFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * Test second level cache and persistence context lookup methods
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
class StrictSecondLCAndPCLookupTest extends SearchTestBase {

	@RegisterExtension
	public StaticIndexingSwitch indexingSwitch = new StaticIndexingSwitch();

	@Test
	void testStaleCacheWithAsyncIndexer() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		indexingSwitch.enable( false ); // disable processing of index updates
		Transaction tx = session.beginTransaction();
		List list = listAll( session, StrictKernel.class );
		assertThat( list ).hasSize( 2 );
		session.delete( list.get( 0 ) );
		tx.commit();
		session.clear();
		indexingSwitch.enable( true );

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery allKernelsQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() )
				.initializeObjectsWith( ObjectLookupMethod.SECOND_LEVEL_CACHE, DatabaseRetrievalMethod.QUERY );

		//Identify the mismatch between index/database:
		assertThat( allKernelsQuery.getResultSize() ).isEqualTo( 2 );
		assertThat( allKernelsQuery.list() ).hasSize( 1 );
	}

	private void setData(Session session, Statistics statistics) {
		Transaction transaction = session.beginTransaction();
		StrictKernel k = new StrictKernel();
		k.setCodeName( "coconut" );
		k.setProduct( "Polgeiser" );
		session.persist( k );
		StrictKernel k2 = new StrictKernel();
		k2.setCodeName( "ballpark" );
		k2.setProduct( "Polgeiser" );
		session.persist( k2 );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		session.get( StrictKernel.class, k.getId() );
		session.get( StrictKernel.class, k2.getId() );
		transaction.commit();

		assertThat( statistics.getSecondLevelCachePutCount() )
				.isEqualTo( 2 );
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		cfg.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getCanonicalName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrictKernel.class };
	}
}
