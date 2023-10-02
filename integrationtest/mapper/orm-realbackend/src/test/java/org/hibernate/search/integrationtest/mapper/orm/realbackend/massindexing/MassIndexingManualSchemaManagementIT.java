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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MassIndexingManualSchemaManagementIT {

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
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void before() {
		entityManagerFactory = setupHelper.start()
				.withProperty( "hibernate.search.indexing.listeners.enabled", false )
				.withProperty( "hibernate.search.schema_management.strategy", "none" )
				.setup( Book.class );

		prepareBooks( entityManagerFactory, NUMBER_OF_BOOKS );
		cleanup();
	}

	@AfterEach
	void cleanup() {
		// Necessary to keep the server (ES) or filesystem (Lucene) clean after the tests,
		// because the schema management strategy is "none"
		with( entityManagerFactory )
				.runInTransaction( entityManager -> Search.session( entityManager ).schemaManager().dropIfExisting()
				);
	}

	@Test
	void testMassIndexingWithAutomaticDropAndCreate() {
		// The index doesn't exist initially, since we delete it in "cleanup()" the schema management strategy is "none"
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			MassIndexer indexer = Search.session( entityManager ).massIndexer()
					.dropAndCreateSchemaOnStart( true );
			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
			setupHelper.assertions().searchAfterIndexChangesAndPotentialRefresh(
					() -> assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) )
							.isEqualTo( NUMBER_OF_BOOKS ) );
		} );
	}

	@Test
	void testMassIndexingWithManualDropAndCreate() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// The index doesn't exist initially, since the schema management strategy is "none"
			Search.session( entityManager ).schemaManager().dropAndCreate();

			assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) ).isZero();

			MassIndexer indexer = Search.session( entityManager ).massIndexer()
					.purgeAllOnStart( false );
			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
			setupHelper.assertions().searchAfterIndexChangesAndPotentialRefresh(
					() -> assertThat( BookCreatorUtils.documentsCount( entityManagerFactory ) )
							.isEqualTo( NUMBER_OF_BOOKS ) );
		} );
	}
}
