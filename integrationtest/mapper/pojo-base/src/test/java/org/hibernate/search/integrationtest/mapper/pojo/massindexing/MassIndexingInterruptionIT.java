/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.ThreadSpy;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;

/**
 * Test interruption of a currently executing {@link MassIndexer}.
 */
class MassIndexingInterruptionIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@RegisterExtension
	public ThreadSpy threadSpy = ThreadSpy.create();

	private SearchMapping mapping;

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( Book.NAME );

		mapping = setupHelper.start()
				.expectCustomBeans()
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	void interrupt_mainThread() {
		int expectedThreadCount = 1 // Workspace
				+ 1 // ID loading
				+ 1; // Entity loading

		AtomicReference<Throwable> thrown = new AtomicReference<>();
		AtomicBoolean interruptFlagAfterInterruption = new AtomicBoolean( false );

		Thread massIndexingThread = new Thread( () -> {
			MassIndexer massIndexer = prepareMassIndexingThatWillNotTerminate();
			try {
				massIndexer.startAndWait();
			}
			catch (Throwable t) {
				thrown.set( t );
				interruptFlagAfterInterruption.set( Thread.currentThread().isInterrupted() );
			}
		} );

		massIndexingThread.start();

		waitForMassIndexingThreadsToSpawn( expectedThreadCount );

		// inLenientMode since with interrupt final flush, refresh and merge segment are invoked
		backendMock.inLenientMode( () -> {
			// Interrupt the thread that triggered mass indexing
			massIndexingThread.interrupt();

			waitForMassIndexingThreadsToTerminate( expectedThreadCount );
		} );

		assertThat( thrown.get() )
				.isInstanceOf( InterruptedException.class )
				.extracting( Throwable::getSuppressed, InstanceOfAssertFactories.ARRAY )
				.hasSize( 1 )
				.allSatisfy( t -> assertThat( t )
						.asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.hasMessageContaining(
								"Mass indexing received interrupt signal. The index is left in an unknown state!" ) );
		// Most JDK methods unset the interrupt flag when they throw an InterruptedException:
		// the MassIndexer should do the same.
		assertThat( interruptFlagAfterInterruption ).isFalse();
	}

	@Test
	void interrupt_entityLoading() {
		int expectedThreadCount = 1 // Workspace
				+ 1 // ID loading
				+ 1; // Entity loading

		AtomicReference<Throwable> thrown = new AtomicReference<>();
		AtomicBoolean interruptFlagAfterInterruption = new AtomicBoolean( false );

		Thread massIndexingThread = new Thread( () -> {
			MassIndexer massIndexer = prepareMassIndexingThatWillNotTerminate();
			try {
				massIndexer.startAndWait();
			}
			catch (Throwable t) {
				thrown.set( t );
				interruptFlagAfterInterruption.set( Thread.currentThread().isInterrupted() );
			}
		} );

		massIndexingThread.start();

		waitForMassIndexingThreadsToSpawn( expectedThreadCount );

		// inLenientMode since with interrupt final flush, refresh and merge segment are invoked
		backendMock.inLenientMode( () -> {
			// Interrupt the entity loading thread
			threadSpy.getCreatedThreads( "Entity Loading" ).get( 0 ).interrupt();

			waitForMassIndexingThreadsToTerminate( expectedThreadCount );
		} );

		assertThat( thrown.get() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Mass indexing received interrupt signal. The index is left in an unknown state!" )
				.hasCauseInstanceOf( InterruptedException.class );

		// The interrupt didn't occur on the mass indexing thread, so the interrupt flag shouldn't be set on that thread.
		assertThat( interruptFlagAfterInterruption ).isFalse();
	}

	@Test
	void cancel() {
		int expectedThreadCount = 1 // Coordinator
				+ 1 // Workspace
				+ 1 // ID loading
				+ 1; // Entity loading

		MassIndexer massIndexer = prepareMassIndexingThatWillNotTerminate();
		CompletableFuture<?> future = massIndexer.start().toCompletableFuture();

		waitForMassIndexingThreadsToSpawn( expectedThreadCount );

		// Cancel mass indexing
		// inLenientMode since, with cancel, the final flush, refresh, and merge are invoked
		backendMock.inLenientMode( () -> {
			future.cancel( true );

			waitForMassIndexingThreadsToTerminate( expectedThreadCount );
		} );
	}

	private MassIndexer prepareMassIndexingThatWillNotTerminate() {
		MassIndexer indexer = mapping.scope( Object.class ).massIndexer()
				// Simulate passing information to connect to a DB, ...
				.context( StubLoadingContext.class, loadingContext )
				.typesToIndexInParallel( 1 )
				.threadsToLoadObjects( 1 );

		backendMock.expectIndexScaleWorks( Book.NAME )
				.purge()
				.mergeSegments();

		backendMock.expectWorks(
				Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		)
				// Return a CompletableFuture that will never complete
				.createAndExecuteFollowingWorks( new CompletableFuture<>() )
				.add( "1", b -> b
						.field( "title", TITLE_1 )
						.field( "author", AUTHOR_1 )
				);

		return indexer;
	}

	private void waitForMassIndexingThreadsToSpawn(int expectedThreadCount) {
		// Wait until the work future is requested
		Awaitility.await().untilAsserted(
				() -> backendMock.verifyExpectationsMet()
		);

		// Check that we have exactly the expected number of threads
		List<Thread> massIndexingThreads = threadSpy.getCreatedThreads( "mass index" );
		assertThat( massIndexingThreads )
				.as( "Mass indexing threads" )
				.hasSize( expectedThreadCount );

		// Check that all mass indexing threads are still running
		assertThat( massIndexingThreads )
				// The ID Loading thread may have terminated already, ignore that one
				.filteredOn( t -> !t.getName().contains( "ID loading" ) )
				.hasSize( expectedThreadCount - 1 )
				.allSatisfy( t -> assertThat( t )
						.extracting( Thread::getState )
						.isIn(
								Thread.State.RUNNABLE, Thread.State.TIMED_WAITING,
								Thread.State.WAITING, Thread.State.BLOCKED
						)
				);
	}

	private void waitForMassIndexingThreadsToTerminate(int expectedThreadCount) {
		Awaitility.await().untilAsserted(
				() -> assertThat( threadSpy.getCreatedThreads( "mass index" ) )
						.as( "Mass indexing threads" )
						.hasSize( expectedThreadCount )
						.allSatisfy( t -> assertThat( t )
								.extracting( Thread::getState )
								.isEqualTo( Thread.State.TERMINATED )
						)
		);
	}

	private void initData() {
		persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
	}

	private void persist(Book book) {
		loadingContext.persistenceMap( Book.PERSISTENCE_KEY ).put( book.id, book );
	}


	@SearchEntity(name = Book.NAME,
			loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	@Indexed
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
}
