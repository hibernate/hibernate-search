/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.integrationtest.mapper.orm.realbackend.util.BookCreatorUtils.prepareBooks;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Book;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.BookCreatorUtils;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

class MassIndexingMonitorIT {

	private static final int NUMBER_OF_BOOKS = 200;
	private static final int MASS_INDEXING_MONITOR_LOG_PERIOD = 50; // This is the default in the implementation, do not change this value

	static {
		checkInvariants();
	}

	@SuppressWarnings("unused")
	private static void checkInvariants() {
		if ( NUMBER_OF_BOOKS < 2 * MASS_INDEXING_MONITOR_LOG_PERIOD ) {
			throw new IllegalStateException(
					"There's a bug in tests: NUMBER_OF_BOOKS should be strictly higher than two times "
							+ MASS_INDEXING_MONITOR_LOG_PERIOD
			);
		}
	}

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void before() {
		entityManagerFactory = setupHelper.start()
				.withProperty( "hibernate.search.indexing.listeners.enabled", false )
				.setup( Book.class );

		prepareBooks( entityManagerFactory, NUMBER_OF_BOOKS );
	}

	@Test
	void testMassIndexingMonitor() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) ).isZero();

			MassIndexer indexer = Search.session( entityManager ).massIndexer()
					.dropAndCreateSchemaOnStart( true )
					// Concurrency leads to an unpredictable number of log events,
					// because we skip logging in some cases where it's triggered concurrently.
					// So, for this test which needs assertions on the number of log events, we avoid concurrency.
					.threadsToLoadObjects( 1 );
			try {
				/*
				 * The default period for logging in the default mass indexing monitor is 50.
				 * We set the batch size to 49.
				 * 50 = 5*5*2
				 * 49 = 7*7
				 * Thus a multiple of 49 cannot be a multiple of 50,
				 * and if we set the batch size to 49, the bug described in HSEARCH-3462
				 * will prevent any log from ever happening, except at the very end
				 *
				 * Regardless of this bug, here we also check that the mass indexing monitor works correctly:
				 * the number of log events should be equal to NUMBER_OF_BOOKS / 50.
				 */
				int batchSize = 49;
				indexer.batchSizeToLoadObjects( batchSize );
				int expectedNumberOfLogs = NUMBER_OF_BOOKS / MASS_INDEXING_MONITOR_LOG_PERIOD;

				// Example:
				// Mass indexing progress: indexed 151 entities in 21 ms.
				logged.expectEvent( Level.INFO, "Mass indexing progress: indexed", "entities in", "ms" ).times(
						expectedNumberOfLogs );

				// Example:
				// Mass indexing progress: 26.50%. Mass indexing speed: 2765.605713 documents/second since last message, 2765.605713 documents/second since start.
				logged.expectEvent(
						Level.INFO, "Mass indexing progress:", "%", "Mass indexing speed:",
						"documents/second since last message", "documents/second since start"
				).times( expectedNumberOfLogs );

				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
			setupHelper.assertions().searchAfterIndexChangesAndPotentialRefresh(
					() -> assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) )
							.isEqualTo( NUMBER_OF_BOOKS ) );
		}
		);

	}

}
