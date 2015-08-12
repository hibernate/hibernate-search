/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.initandlookup;

import java.util.List;
import java.util.Map;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.fest.assertions.Condition;
import org.hibernate.Hibernate;
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
import org.hibernate.search.testsupport.backend.GatedLuceneBackend;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test second level cache and persistence context lookup methods
 *
 * @author Emmanuel Bernard
 */
public class SecondLCAndPCLookupTest extends SearchTestBase {

	@Test
	public void testQueryWoLookup() throws Exception {
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
	public void testQueryWith2LCLookup() throws Exception {
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
	public void testQueryWithPCLookup() throws Exception {
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
	public void testQueryWithPCAndCacheLookup() throws Exception {
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
	public void testStaleCacheWithAsyncIndexer() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		GatedLuceneBackend.open.set( false ); // disable processing of index updates
		Transaction tx = session.beginTransaction();
		List list = session.createCriteria( Kernel.class ).list();
		assertThat( list ).hasSize( 2 );
		session.delete( list.get( 0 ) );
		tx.commit();
		session.clear();
		GatedLuceneBackend.open.set( true );

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery allKernelsQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() )
				.initializeObjectsWith( ObjectLookupMethod.SECOND_LEVEL_CACHE, DatabaseRetrievalMethod.QUERY );

		assertThat( allKernelsQuery.getResultSize() ).isEqualTo( 2 );
		assertThat( allKernelsQuery.list() ).hasSize( 1 );
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
	}

	@Test
	public void testQueryUsingFindByIdInitialization() throws Exception {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		Transaction tx = session.beginTransaction();
		Kernel k = new Kernel();
		k.setCodeName( "notpresent" );
		k.setProduct( "Polgeiser" );
		session.persist( k );
		session.flush();
		Search.getFullTextSession( session ).flushToIndexes();
		tx.rollback(); //ie do not store notpresent

		session.clear();
		//make sure the 2LC is empty
		session.getSessionFactory().getCache().evictEntityRegion( Kernel.class );

		statistics.clear();

		Transaction transaction = session.beginTransaction();
		final FullTextSession fullTextSession = Search.getFullTextSession( session );
		final QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Kernel.class )
				.get();
		final Query luceneQuery = queryBuilder.keyword().onField( "product" ).matching( "Polgeiser" ).createQuery();
		final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Kernel.class );
		fullTextQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
		List list = fullTextQuery.list();
		assertThat( list.size() ).isEqualTo( 2 );
		for ( Object o : list ) {
			assertThat( o ).satisfies( new Condition<Object>() {
				@Override
				public boolean matches(Object value) {
					return Hibernate.isInitialized( value );
				}
			} );
		}

		for ( Object o : list ) {
			o.toString(); //check true initialization
		}

		assertThat( statistics.getSecondLevelCacheHitCount() )
			.isEqualTo( 0 );
		assertThat( statistics.getQueryExecutionCount() )
			.isEqualTo( 0 );
		assertThat( statistics.getEntityLoadCount() )
			.isEqualTo( 2 );

		transaction.commit();
		clearData( session );
		session.close();
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
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.put( "hibernate.search.default.worker.backend", GatedLuceneBackend.class.getName() );
		cfg.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getCanonicalName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Kernel.class };
	}
}
