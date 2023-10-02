/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MassIndexingMonitorIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );
	@RegisterExtension
	public StaticCounters staticCounters = StaticCounters.create();

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	private SearchMapping setup(String failureHandler) {
		backendMock.expectAnySchema( Book.NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withPropertyRadical( EngineSettings.BACKGROUND_FAILURE_HANDLER, failureHandler )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();

		return mapping;
	}

	@Test
	void simple() {
		SearchMapping mapping = setup( null );

		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext );

			CompletableFuture<?> indexingFuture = new CompletableFuture<>();
			indexingFuture.completeExceptionally( new SimulatedFailure( "Indexing error" ) );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					)
					.createAndExecuteFollowingWorks( indexingFuture )
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.monitor( new StaticCountersMonitor() )
						.startAndWait();
			}
			catch (SearchException ignored) {
				// Expected, but not relevant to this test
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}
		}

		backendMock.verifyExpectationsMet();

		assertThat( staticCounters.get( StaticCountersMonitor.LOADED ) ).isEqualTo( 3 );
		assertThat( staticCounters.get( StaticCountersMonitor.BUILT ) ).isEqualTo( 3 );
		assertThat( staticCounters.get( StaticCountersMonitor.ADDED ) ).isEqualTo( 2 );
		assertThat( staticCounters.get( StaticCountersMonitor.TOTAL ) ).isEqualTo( 3 );
		assertThat( staticCounters.get( StaticCountersMonitor.INDEXING_COMPLETED ) ).isEqualTo( 1 );
	}

	private void initData() {
		persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
		persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
		persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
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

	public static class StaticCountersMonitor implements MassIndexingMonitor {

		public static StaticCounters.Key ADDED = StaticCounters.createKey();
		public static StaticCounters.Key BUILT = StaticCounters.createKey();
		public static StaticCounters.Key LOADED = StaticCounters.createKey();
		public static StaticCounters.Key TOTAL = StaticCounters.createKey();
		public static StaticCounters.Key INDEXING_COMPLETED = StaticCounters.createKey();

		@Override
		public void documentsAdded(long increment) {
			StaticCounters.get().add( ADDED, (int) increment );
		}

		@Override
		public void documentsBuilt(long increment) {
			StaticCounters.get().add( BUILT, (int) increment );
		}

		@Override
		public void entitiesLoaded(long increment) {
			StaticCounters.get().add( LOADED, (int) increment );
		}

		@Override
		public void addToTotalCount(long increment) {
			StaticCounters.get().add( TOTAL, (int) increment );
		}

		@Override
		public void indexingCompleted() {
			StaticCounters.get().increment( INDEXING_COMPLETED );
		}
	}

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
