/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.lang.invoke.MethodHandles;
import java.time.Year;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import jakarta.persistence.EntityManager;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.junit.jupiter.api.Test;

class HibernateOrmMassIndexerIT extends AbstractHibernateOrmMassIndexingIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Test
	void simple() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				// tag::simple[]
				SearchSession searchSession = /* ... */ // <1>
						// end::simple[]
						Search.session( entityManager );
				// tag::simple[]
				searchSession.massIndexer() // <2>
						// end::simple[]
						.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
						// tag::simple[]
						.startAndWait(); // <3>
				// end::simple[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookAndAuthorCount( entityManager, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	void reindexOnly() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				// tag::reindexOnly[]
				SearchSession searchSession = /* ... */ // <1>
						// end::reindexOnly[]
						Search.session( entityManager );
				// tag::reindexOnly[]
				MassIndexer massIndexer = searchSession.massIndexer(); // <2>
				massIndexer.type( Book.class ).reindexOnly( "publicationYear < 1950" ); // <3>
				massIndexer.type( Author.class ).reindexOnly( "birthDate < :cutoff" ) // <4>
						.param( "cutoff", Year.of( 1950 ).atDay( 1 ) ); // <5>
				// end::reindexOnly[]
				if ( !BackendConfigurations.simple().supportsExplicitPurge() ) {
					massIndexer.purgeAllOnStart( false );
				}
				// tag::reindexOnly[]
				massIndexer.startAndWait(); // <6>
				// end::reindexOnly[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookAndAuthorCount( entityManager, 500, 500 );
		} );
	}

	@Test
	void selectType() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				SearchSession searchSession = Search.session( entityManager );
				// tag::select-type[]
				searchSession.massIndexer( Book.class ) // <1>
						// end::select-type[]
						.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
						// tag::select-type[]
						.startAndWait(); // <2>
				// end::select-type[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookAndAuthorCount( entityManager, NUMBER_OF_BOOKS, 0 );
		} );
	}

	@Test
	void async_reactive() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			CompletionStage<?> completionStage =
					// tag::async[]
					searchSession.massIndexer() // <1>
							// end::async[]
							.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
							// tag::async[]
							.start() // <2>
							.thenRun( () -> { // <3>
								log.info( "Mass indexing succeeded!" );
							} )
							.exceptionally( throwable -> {
								log.error( "Mass indexing failed!", throwable );
								return null;
							} );
			// end::async[]
			Future<?> future = completionStage.toCompletableFuture();
			await().untilAsserted( () -> {
				assertThatFuture( future ).isSuccessful();
			} );
			assertBookAndAuthorCount( entityManager, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	void async_future() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::async[]

			// OR
			Future<?> future = searchSession.massIndexer()
					// end::async[]
					.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
					// tag::async[]
					.start()
					.toCompletableFuture(); // <4>
			// end::async[]
			await().untilAsserted( () -> {
				assertThatFuture( future ).isSuccessful();
			} );
			assertBookAndAuthorCount( entityManager, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
	}

	@Test
	void parameters() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				SearchSession searchSession = Search.session( entityManager );
				// tag::parameters[]
				searchSession.massIndexer() // <1>
						.idFetchSize( 150 ) // <2>
						.batchSizeToLoadObjects( 25 ) // <3>
						.threadsToLoadObjects( 12 ) // <4>
						// end::parameters[]
						.purgeAllOnStart( BackendConfigurations.simple().supportsExplicitPurge() )
						// tag::parameters[]
						.startAndWait(); // <5>
				// end::parameters[]
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertBookAndAuthorCount( entityManager, NUMBER_OF_BOOKS, NUMBER_OF_BOOKS );
		} );
	}

	void assertBookAndAuthorCount(EntityManager entityManager, int expectedBookCount, int expectedAuthorCount) {
		setupHelper.assertions().searchAfterIndexChangesAndPotentialRefresh( () -> {
			SearchSession searchSession = Search.session( entityManager );
			assertThat( searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount() )
					.isEqualTo( expectedBookCount );
			assertThat( searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount() )
					.isEqualTo( expectedAuthorCount );
		} );
	}
}
