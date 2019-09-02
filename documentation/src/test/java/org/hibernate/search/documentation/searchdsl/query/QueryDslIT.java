/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.searchdsl.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.stat.Statistics;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class QueryDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public QueryDslIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.withProperty( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE.name() )
				.setup( Book.class );
		initData();
	}

	@Test
	public void entryPoint() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::entryPoint[]
			// Not shown: get the entity manager and open a transaction
			SearchSession searchSession = Search.session( entityManager ); // <1>

			SearchResult<Book> result = searchSession.search( Book.class ) // <2>
					.predicate( f -> f.match() // <3>
							.field( "title" )
							.matching( "robot" ) )
					.fetch( 20 ); // <4>

			long totalHitCount = result.getTotalHitCount(); // <5>
			List<Book> hits = result.getHits(); // <6>
			// Not shown: commit the transaction and close the entity manager
			// end::entryPoint[]

			assertThat( totalHitCount ).isEqualTo( 2 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void fetchingBasics() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.predicate( f -> f.matchAll() )
					.fetch( 20 ); // <2>

			long totalHitCount = result.getTotalHitCount(); // <3>
			List<Book> hits = result.getHits(); // <4>
			// ... // <5>
			// end::fetching-searchResult[]

			assertThat( totalHitCount ).isEqualTo( 4 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-totalHitCount[]
			long totalHitCount = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.fetchTotalHitCount();
			// end::fetching-totalHitCount[]

			assertThat( totalHitCount ).isEqualTo( 4 );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::fetching-hits[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-singleHit[]
			Optional<Book> hit = searchSession.search( Book.class )
					.predicate( f -> f.id().matching( 1 ) )
					.fetchSingleHit();
			// end::fetching-singleHit[]

			assertThat( hit ).get().extracting( Book::getId )
					.isEqualTo( BOOK1_ID );
		} );
	}

	@Test
	public void fetchingAllHits() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-all-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class )
					.predicate( f -> f.id().matchingAny( Arrays.asList( 1, 2 ) ) )
					.fetch();

			long totalHitCount = result.getTotalHitCount();
			List<Book> hits = result.getHits();
			// end::fetching-all-searchResult[]

			assertThat( totalHitCount ).isEqualTo( 2 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-all-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.predicate( f -> f.id().matchingAny( Arrays.asList( 1, 2 ) ) )
					.fetchHits();
			// end::fetching-all-hits[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );
	}

	@Test
	public void cacheLookupStrategy() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Statistics statistics = entityManagerFactory.unwrap( SessionFactory.class ).getStatistics();
			statistics.setStatisticsEnabled( true );
			statistics.clear();

			SearchSession searchSession = Search.session( entityManager );

			// tag::cacheLookupStrategy-persistenceContextThenSecondLevelCache[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.cacheLookupStrategy(
							EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
					) // <2>
					.predicate( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.fetch( 20 ); // <3>
			// end::cacheLookupStrategy-persistenceContextThenSecondLevelCache[]

			assertThat( result.getHits() ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
			assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void fetchSize() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::fetchSize[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.fetchSize( 50 ) // <2>
					.predicate( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.fetch( 200 ); // <3>
			// end::fetchSize[]

			assertThat( result.getHits() ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}


}
