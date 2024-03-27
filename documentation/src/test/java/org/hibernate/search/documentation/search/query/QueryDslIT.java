/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.ManagedAssert.assertThatManaged;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SharedCacheMode;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.stat.Statistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class QueryDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	private static final int MANAGER1_ID = 1;
	private static final int MANAGER2_ID = 2;
	private static final int ASSOCIATE1_ID = 1;
	private static final int ASSOCIATE2_ID = 2;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty( AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE.name() )
				.setup( Book.class, Manager.class, Associate.class );
		initData();
	}

	@Test
	void entryPoint() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint[]
			// Not shown: open a transaction if relevant
			SearchSession searchSession = /* ... */ // <1>
					// end::entryPoint[]
					Search.session( entityManager );
			// tag::entryPoint[]

			SearchResult<Book> result = searchSession.search( Book.class ) // <2>
					.where( f -> f.match() // <3>
							.field( "title" )
							.matching( "robot" ) )
					.fetch( 20 ); // <4>

			long totalHitCount = result.total().hitCount(); // <5>
			List<Book> hits = result.hits(); // <6>
			// Not shown: commit the transaction if relevant
			// end::entryPoint[]

			assertThat( totalHitCount ).isEqualTo( 2 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	void targetingMultipleEntityTypes() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::targeting-multiple[]
			SearchResult<Person> result = searchSession.search( Arrays.asList( // <1>
					Manager.class, Associate.class
			) )
					.where( f -> f.match() // <2>
							.field( "name" )
							.matching( "james" ) )
					.fetch( 20 ); // <3>
			// end::targeting-multiple[]
			List<Person> hits = result.hits();
			assertThat( hits )
					.containsExactlyInAnyOrder(
							entityManager.getReference( Manager.class, MANAGER1_ID ),
							entityManager.getReference( Associate.class, ASSOCIATE1_ID )
					);
		} );
	}

	@Test
	void targetingByEntityName() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::targeting-entityName[]
			SearchResult<Person> result = searchSession.search( // <1>
					searchSession.scope( // <2>
							Person.class,
							Arrays.asList( "Manager", "Associate" )
					)
			)
					.where( f -> f.match() // <3>
							.field( "name" )
							.matching( "james" ) )
					.fetch( 20 ); // <4>
			// end::targeting-entityName[]
			List<Person> hits = result.hits();
			assertThat( hits )
					.containsExactlyInAnyOrder(
							entityManager.getReference( Manager.class, MANAGER1_ID ),
							entityManager.getReference( Associate.class, ASSOCIATE1_ID )
					);
		} );
	}

	@Test
	void fetchingBasics() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.matchAll() )
					.fetch( 20 ); // <2>

			long totalHitCount = result.total().hitCount(); // <3>
			List<Book> hits = result.hits(); // <4>
			// ... // <5>
			// end::fetching-searchResult[]

			assertThat( totalHitCount ).isEqualTo( 4 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-totalHitCount[]
			long totalHitCount = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount();
			// end::fetching-totalHitCount[]

			assertThat( totalHitCount ).isEqualTo( 4 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::fetching-hits[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-singleHit[]
			Optional<Book> hit = searchSession.search( Book.class )
					.where( f -> f.id().matching( 1 ) )
					.fetchSingleHit();
			// end::fetching-singleHit[]

			assertThat( hit ).get().extracting( Book::getId )
					.isEqualTo( BOOK1_ID );
		} );
	}

	@Test
	void fetchingAllHits() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-all-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.id().matchingAny( Arrays.asList( 1, 2 ) ) )
					.fetchAll();

			long totalHitCount = result.total().hitCount();
			List<Book> hits = result.hits();
			// end::fetching-all-searchResult[]

			assertThat( totalHitCount ).isEqualTo( 2 );
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-all-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.id().matchingAny( Arrays.asList( 1, 2 ) ) )
					.fetchAllHits();
			// end::fetching-all-hits[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );
	}

	@Test
	void pagination() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-pagination-searchResult[]
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetch( 40, 20 ); // <1>
			// end::fetching-pagination-searchResult[]
			long totalHitCount = result.total().hitCount();
			List<Book> hits = result.hits();

			assertThat( totalHitCount ).isEqualTo( 4 );
			assertThat( hits ).isEmpty();
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-pagination-hits[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchHits( 40, 20 ); // <1>
			// end::fetching-pagination-hits[]

			assertThat( hits ).isEmpty();
		} );
	}

	@Test
	void scrolling() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			List<Integer> collectedIds = new ArrayList<>();
			long totalHitCount = 0;

			// tag::fetching-scrolling[]
			try ( SearchScroll<Book> scroll = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.scroll( 20 ) ) { // <1>
				for ( SearchScrollResult<Book> chunk = scroll.next(); // <2>
						chunk.hasHits(); chunk = scroll.next() ) { // <3>
					for ( Book hit : chunk.hits() ) { // <4>
						// ... do something with the hits ...
						// end::fetching-scrolling[]
						collectedIds.add( hit.getId() );
						// tag::fetching-scrolling[]
					}

					totalHitCount = chunk.total().hitCount(); // <5>

					entityManager.flush(); // <6>
					entityManager.clear(); // <6>
				}
			}
			// end::fetching-scrolling[]

			assertThat( collectedIds ).hasSize( 4 );
			assertThat( totalHitCount ).isEqualTo( 4 );
		} );
	}

	@Test
	void searchQuery() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::searchQuery[]
			SearchQuery<Book> query = searchSession.search( Book.class ) // <1>
					.where( f -> f.matchAll() )
					.toQuery(); // <2>
			List<Book> hits = query.fetchHits( 20 ); // <3>
			// end::searchQuery[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::searchQuery-toORM[]
			SearchQuery<Book> query = searchSession.search( Book.class ) // <1>
					.where( f -> f.matchAll() )
					.toQuery(); // <2>
			jakarta.persistence.TypedQuery<Book> jpaQuery = Search.toJpaQuery( query ); // <3>
			org.hibernate.query.Query<Book> ormQuery = Search.toOrmQuery( query ); // <4>
			// end::searchQuery-toORM[]
			List<Book> hits = jpaQuery.getResultList();
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
			hits = ormQuery.list();
			assertThat( hits ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	void tookAndTimedOut() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::took-timedOut[]
			SearchQuery<Book> query = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.toQuery();

			SearchResult<Book> result = query.fetch( 20 ); // <1>

			Duration took = result.took(); // <2>
			Boolean timedOut = result.timedOut(); // <3>
			// end::took-timedOut[]

			assertThat( took ).isNotNull();
			assertThat( timedOut ).isNotNull();
		} );
	}

	@Test
	void truncateAfter() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::truncateAfter[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.truncateAfter( 500, TimeUnit.MILLISECONDS ) // <2>
					.fetch( 20 ); // <3>

			Duration took = result.took(); // <4>
			Boolean timedOut = result.timedOut(); // <5>
			// end::truncateAfter[]

			assertThat( took ).isNotNull();
			assertThat( timedOut ).isNotNull();
		} );
	}

	@Test
	void failAfter() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::failAfter[]
			try {
				SearchResult<Book> result = searchSession.search( Book.class ) // <1>
						.where( f -> f.match()
								.field( "title" )
								.matching( "robot" ) )
						.failAfter( 500, TimeUnit.MILLISECONDS ) // <2>
						.fetch( 20 ); // <3>
			}
			catch (SearchTimeoutException e) { // <4>
				// ...
			}
			// end::failAfter[]
		} );
	}

	@Test
	void cacheLookupStrategy() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Statistics statistics = entityManagerFactory.unwrap( SessionFactory.class ).getStatistics();
			statistics.setStatisticsEnabled( true );
			statistics.clear();

			SearchSession searchSession = Search.session( entityManager );

			// tag::cacheLookupStrategy-persistenceContextThenSecondLevelCache[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.loading( o -> o.cacheLookupStrategy( // <2>
							EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
					) )
					.fetch( 20 ); // <3>
			// end::cacheLookupStrategy-persistenceContextThenSecondLevelCache[]

			assertThat( result.hits() ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
			assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 2 );
		} );
	}

	@Test
	void fetchSize() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::fetchSize[]
			SearchResult<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.loading( o -> o.fetchSize( 50 ) ) // <2>
					.fetch( 200 ); // <3>
			// end::fetchSize[]

			assertThat( result.hits() ).extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	void resultTotal() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-resultTotal[]
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetch( 20 );

			SearchResultTotal resultTotal = result.total(); // <1>
			long totalHitCount = resultTotal.hitCount(); // <2>
			long totalHitCountLowerBound = resultTotal.hitCountLowerBound(); // <3>
			boolean hitCountExact = resultTotal.isHitCountExact(); // <4>
			boolean hitCountLowerBound = resultTotal.isHitCountLowerBound(); // <5>
			// end::fetching-resultTotal[]

			assertThat( totalHitCount ).isEqualTo( 4 );
			assertThat( totalHitCountLowerBound ).isEqualTo( 4 );
			assertThat( hitCountExact ).isEqualTo( true );
			assertThat( hitCountLowerBound ).isEqualTo( false );
		} );
	}

	@Test
	void resultTotal_totalHitCountThreshold() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::fetching-totalHitCountThreshold[]
			SearchResult<Book> result = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.totalHitCountThreshold( 1000 ) // <1>
					.fetch( 20 );

			SearchResultTotal resultTotal = result.total(); // <2>
			long totalHitCountLowerBound = resultTotal.hitCountLowerBound(); // <3>
			boolean hitCountExact = resultTotal.isHitCountExact(); // <4>
			boolean hitCountLowerBound = resultTotal.isHitCountLowerBound(); // <5>
			// end::fetching-totalHitCountThreshold[]

			assertThat( totalHitCountLowerBound ).isLessThanOrEqualTo( 4 );
		} );
	}

	@Test
	void graph() {
		// By default associates are not loaded
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			SearchResult<Manager> result = searchSession.search( Manager.class ) // <1>
					.where( f -> f.match()
							.field( "name" )
							.matching( "james" ) )
					.fetch( 20 ); // <3>

			assertThat( result.hits() )
					.isNotEmpty()
					.allSatisfy( manager -> assertThatManaged( manager.getAssociates() ).isNotInitialized() );
		} );

		with( entityManagerFactory ).runInTransaction( theEntityManager -> {
			// tag::graph-byReference[]
			EntityManager entityManager = /* ... */
					// end::graph-byReference[]
					theEntityManager;
			// tag::graph-byReference[]

			EntityGraph<Manager> graph = entityManager.createEntityGraph( Manager.class ); // <1>
			graph.addAttributeNodes( "associates" );

			SearchResult<Manager> result = Search.session( entityManager ).search( Manager.class ) // <2>
					.where( f -> f.match()
							.field( "name" )
							.matching( "james" ) )
					.loading( o -> o.graph( graph, GraphSemantic.FETCH ) ) // <3>
					.fetch( 20 ); // <4>
			// end::graph-byReference[]

			assertThat( result.hits() )
					.isNotEmpty()
					.allSatisfy( manager -> assertThatManaged( manager.getAssociates() ).isInitialized() );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::graph-byName[]
			SearchResult<Manager> result = Search.session( entityManager ).search( Manager.class ) // <1>
					.where( f -> f.match()
							.field( "name" )
							.matching( "james" ) )
					.loading( o -> o.graph( "preload-associates", GraphSemantic.FETCH ) ) // <2>
					.fetch( 20 ); // <3>
			// end::graph-byName[]

			assertThat( result.hits() )
					.isNotEmpty()
					.allSatisfy( manager -> assertThatManaged( manager.getAssociates() ).isInitialized() );
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
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

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Manager manager1 = new Manager();
			manager1.setId( MANAGER1_ID );
			manager1.setName( "James Green" );

			Manager manager2 = new Manager();
			manager2.setId( MANAGER2_ID );
			manager2.setName( "John Doe" );

			Associate associate1 = new Associate();
			associate1.setId( ASSOCIATE1_ID );
			associate1.setName( "James Harper" );
			associate1.setManager( manager1 );
			manager1.getAssociates().add( associate1 );

			Associate associate2 = new Associate();
			associate2.setId( ASSOCIATE2_ID );
			associate2.setName( "John Sutherland" );
			associate2.setManager( manager2 );
			manager2.getAssociates().add( associate2 );

			entityManager.persist( manager1 );
			entityManager.persist( manager2 );
			entityManager.persist( associate1 );
			entityManager.persist( associate2 );
		} );
	}


}
