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
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.testsupport.StaticIndexingSwitch;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.cache.CachingRegionFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * Test second level cache and persistence context lookup methods
 *
 * @author Emmanuel Bernard
 */
class SecondLCAndPCLookupTest extends SearchTestBase {

	@RegisterExtension
	public StaticIndexingSwitch indexingSwitch = new StaticIndexingSwitch();

	@Test
	void testQueryWoLookup() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		session.clear();

		Transaction transaction = session.beginTransaction();
		final FullTextSession fullTextSession = Search.getFullTextSession( session );
		final QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Kernel.class )
				.get();
		final Query luceneQuery = queryBuilder.keyword().onField( "product" ).matching( "Polgeiser" ).createQuery();
		final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Kernel.class );
		List list = fullTextQuery.list();
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCacheHitCount() )
				.isEqualTo( 0 );
		assertThat( statistics.getQueryExecutionCount() )
				.isEqualTo( 1 );

		transaction.commit();
		clearData( session );
		session.close();
	}

	@Test
	void testQueryWith2LCLookup() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		session.clear();

		Transaction transaction = session.beginTransaction();
		final FullTextSession fullTextSession = Search.getFullTextSession( session );
		final QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Kernel.class )
				.get();
		final Query luceneQuery = queryBuilder.keyword().onField( "product" ).matching( "Polgeiser" ).createQuery();
		final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Kernel.class );
		fullTextQuery.initializeObjectsWith( ObjectLookupMethod.SECOND_LEVEL_CACHE, DatabaseRetrievalMethod.QUERY );
		List list = fullTextQuery.list();
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCacheHitCount() )
				.isEqualTo( 2 );
		assertThat( statistics.getQueryExecutionCount() )
				.isEqualTo( 0 );

		transaction.commit();
		clearData( session );
		session.close();
	}

	@Test
	void testQueryWithPCLookup() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		session.clear();

		Transaction transaction = session.beginTransaction();
		final FullTextSession fullTextSession = Search.getFullTextSession( session );
		session.createQuery( "from " + Kernel.class.getName() ).list();
		statistics.clear();
		final QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Kernel.class )
				.get();
		final Query luceneQuery = queryBuilder.keyword().onField( "product" ).matching( "Polgeiser" ).createQuery();
		final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Kernel.class );
		fullTextQuery.initializeObjectsWith( ObjectLookupMethod.PERSISTENCE_CONTEXT, DatabaseRetrievalMethod.QUERY );
		List list = fullTextQuery.list();
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCacheHitCount() )
				.isEqualTo( 0 );
		assertThat( statistics.getQueryExecutionCount() )
				.describedAs( "entities should be looked up and are already loaded" )
				.isEqualTo( 0 );
		assertThat( statistics.getEntityLoadCount() )
				.describedAs( "entities should be looked up and are already loaded" )
				.isEqualTo( 0 );

		transaction.commit();
		clearData( session );
		session.close();
	}

	@Test
	void testQueryWithPCAndCacheLookup() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		session.clear();

		Transaction transaction = session.beginTransaction();
		final FullTextSession fullTextSession = Search.getFullTextSession( session );
		//load just one object into persistence context:
		List firstLoad = session.createQuery( "from Kernel k where k.codeName = 'coconut'" ).list();
		assertThat( firstLoad.size() ).isEqualTo( 1 );
		statistics.clear();
		final QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Kernel.class )
				.get();
		final Query luceneQuery = queryBuilder.keyword().onField( "product" ).matching( "Polgeiser" ).createQuery();
		final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Kernel.class );
		fullTextQuery.initializeObjectsWith( ObjectLookupMethod.SECOND_LEVEL_CACHE, DatabaseRetrievalMethod.QUERY );
		List list = fullTextQuery.list();
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( statistics.getSecondLevelCacheHitCount() )
				.isEqualTo( 1 );
		assertThat( statistics.getQueryExecutionCount() )
				.describedAs( "entities should be looked up and are already loaded" )
				.isEqualTo( 0 );
		assertThat( statistics.getEntityLoadCount() )
				.describedAs( "entities should be looked up and are already loaded" )
				.isEqualTo( 0 );

		transaction.commit();
		clearData( session );
		session.close();
	}

	@Test
	void testStaleCacheWithAsyncIndexer() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		indexingSwitch.enable( false ); // disable processing of index updates
		Transaction tx = session.beginTransaction();
		List list = listAll( session, Kernel.class );
		assertThat( list ).hasSize( 2 );
		session.delete( list.get( 0 ) );
		tx.commit();
		session.clear();
		indexingSwitch.enable( true );

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery allKernelsQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() )
				.initializeObjectsWith( ObjectLookupMethod.SECOND_LEVEL_CACHE, DatabaseRetrievalMethod.QUERY );

		assertThat( allKernelsQuery.getResultSize() ).isEqualTo( 2 );
		assertThat( allKernelsQuery.list() ).hasSize( 1 );
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
	}

	private void clearData(Session session) {
		final Transaction transaction = session.beginTransaction();
		session.createQuery( "delete from " + Kernel.class.getName() ).executeUpdate();
		transaction.commit();
	}

	private void setData(Session session, Statistics statistics) {
		Transaction transaction = session.beginTransaction();
		Kernel k = new Kernel();
		k.setCodeName( "coconut" );
		k.setProduct( "Polgeiser" );
		session.persist( k );
		Kernel k2 = new Kernel();
		k2.setCodeName( "ballpark" );
		k2.setProduct( "Polgeiser" );
		session.persist( k2 );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		session.get( Kernel.class, k.getId() );
		session.get( Kernel.class, k2.getId() );
		transaction.commit();

		assertThat( statistics.getSecondLevelCachePutCount() )
				.isEqualTo( 2 );
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getCanonicalName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Kernel.class };
	}
}
