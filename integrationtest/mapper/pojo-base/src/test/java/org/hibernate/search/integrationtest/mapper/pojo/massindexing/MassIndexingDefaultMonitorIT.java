/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.massindexing.DefaultMassIndexingMonitor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.apache.logging.log4j.Level;

class MassIndexingDefaultMonitorIT {

	private static final int COUNT = 100;
	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@ValueSource(booleans = { true, false })
	@ParameterizedTest
	void countOnBeforeType(boolean doCounts) {
		actualTest( () -> {
			logged.expectEvent( Level.INFO, "Mass indexing complete in ", ". Indexed 100/100 entities" );
			if ( doCounts ) {
				logged.expectEvent( Level.INFO, "Mass indexing is going to index 100 entities" ).once();
			}
			else {
				logged.expectEvent( Level.INFO, "Mass indexing is going to index 100 entities" ).never();
			}
		}, indexer -> indexer.monitor( DefaultMassIndexingMonitor.builder().countOnBeforeType( doCounts ).build() ) );
	}

	@ValueSource(booleans = { true, false })
	@ParameterizedTest
	void countOnStart(boolean doCounts) {
		actualTest( () -> {
			logged.expectEvent( Level.INFO, "Mass indexing complete in ", ". Indexed 100/100 entities" );
			logged.expectEvent( Level.INFO, "Mass indexing is going to index 100 entities" ).once();
			if ( doCounts ) {
				logged.expectEvent( Level.INFO,
						"Mass indexing is going to index approx. 100 entities ([ Book ]). Actual number may change once the indexing starts." )
						.once();
			}
			else {
				logged.expectEvent( Level.INFO,
						"Mass indexing is going to index approx. 100 entities ([ Book ]). Actual number may change once the indexing starts." )
						.never();
			}
		}, indexer -> indexer.monitor( DefaultMassIndexingMonitor.builder().countOnStart( doCounts ).build() ) );
	}

	@Test
	void noCountsAtAll() {
		actualTest( () -> {
			logged.expectEvent( Level.INFO, "Mass indexing complete in ", ". Indexed 100/100 entities" ).once();
			logged.expectEvent( Level.INFO, "Mass indexing is going to index 100 entities" ).never();
			logged.expectEvent( Level.INFO,
					"Mass indexing is going to index approx. 100 entities ([ Book ]). Actual number may change once the indexing starts." )
					.never();
		}, indexer -> indexer
				.monitor( DefaultMassIndexingMonitor.builder().countOnStart( false ).countOnBeforeType( false ).build() ) );
	}

	private void actualTest(Runnable expectedLogs, Consumer<MassIndexer> massIndexerConfiguration) {
		backendMock.expectAnySchema( Book.NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();

		expectedLogs.run();

		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			CompletableFuture<?> indexingFuture = new CompletableFuture<>();
			indexingFuture.completeExceptionally( new SimulatedFailure( "Indexing error" ) );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			BackendMock.DocumentWorkCallListContext expectWorks = backendMock.expectWorks(
					Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			);
			for ( int i = 0; i < COUNT; i++ ) {
				final String id = Integer.toString( i );
				expectWorks
						.add( id, b -> b
								.field( "title", "TITLE_" + id )
								.field( "author", "AUTHOR_" + id )
						);
			}

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				massIndexerConfiguration.accept( indexer );
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		}

		backendMock.verifyExpectationsMet();
	}

	private void initData() {
		for ( int i = 0; i < COUNT; i++ ) {
			persist( new Book( i, "TITLE_" + i, "AUTHOR_" + i ) );
		}
	}

	private void persist(Book book) {
		loadingContext.persistenceMap( Book.PERSISTENCE_KEY ).put( book.id, book );
	}

	@SearchEntity(name = Book.NAME,
			loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";
		public static final PersistenceTypeKey<Book, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( Book.class, Integer.class );

		@DocumentId
		private Integer id;

		@GenericField
		private String title;

		@GenericField
		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
