/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubMassLoadingStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.ThreadSpy;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;

public abstract class AbstractMassIndexingErrorIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final StandalonePojoMappingSetupHelper setupHelper
			= StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public ThreadSpy threadSpy = new ThreadSpy();

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@Test
	@TestForIssue(jiraKey = {"HSEARCH-4218", "HSEARCH-4236"})
	public void identifierLoading() {
		String errorMessage = "ID loading error";

		SearchMapping mapping = setupWithThrowingIdentifierLoading( errorMessage );

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED )
		);

		assertNoFailureHandling();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4236")
	public void entityLoading() {
		String errorMessage = "entity loading error";

		SearchMapping mapping = setupWithThrowingEntityLoading( errorMessage );

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer()
						.threadsToLoadObjects( 1 ) // Just to simplify the assertions
						.batchSizeToLoadObjects( 1 ), // We need more than 1000 batches in order to reproduce HSEARCH-4236
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED )
		);

		assertNoFailureHandling();
	}

	@Test
	public void indexing() {
		SearchMapping mapping = setup();

		String errorMessage = "Indexing error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void getId() {
		SearchMapping mapping = setup();

		String errorMessage = "getId error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				ExecutionExpectation.ERROR, ExecutionExpectation.STOP,
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.STOP )
		// no final refresh && flush
		);

		assertNoFailureHandling();
	}

	@Test
	public void getTitle() {
		SearchMapping mapping = setup();

		String errorMessage = "getTitle error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				ExecutionExpectation.SUCCEED, ExecutionExpectation.ERROR,
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.STOP )
		);

		assertNoFailureHandling();
	}

	@Test
	public void dropAndCreateSchema_exception() {
		SearchMapping mapping = setup();

		String errorMessage = "DROP_AND_CREATE error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer().dropAndCreateSchemaOnStart( true ),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectSchemaManagementWorkException( StubSchemaManagementWork.Type.DROP_AND_CREATE )
		);

		assertNoFailureHandling();
	}

	@Test
	public void purge() {
		SearchMapping mapping = setup();

		String errorMessage = "PURGE error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void mergeSegmentsBefore() {
		SearchMapping mapping = setup();

		String errorMessage = "MERGE_SEGMENTS error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void mergeSegmentsAfter() {
		SearchMapping mapping = setup();

		String errorMessage = "MERGE_SEGMENTS error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer()
						.mergeSegmentsOnFinish( true ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void flush() {
		SearchMapping mapping = setup();

		String errorMessage = "FLUSH error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void refresh() {
		SearchMapping mapping = setup();

		String errorMessage = "REFRESH error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	protected abstract FailureHandler getBackgroundFailureHandlerReference();

	protected abstract MassIndexingFailureHandler getMassIndexingFailureHandler();

	protected void assertBeforeSetup() {
	}

	protected void assertAfterSetup() {
	}

	protected abstract void expectNoFailureHandling();

	protected abstract void assertNoFailureHandling();

	private void doMassIndexingWithError(MassIndexer massIndexer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable... expectationSetters) {
		doMassIndexingWithError(
				massIndexer,
				threadExpectation,
				thrownExpectation,
				ExecutionExpectation.SUCCEED, ExecutionExpectation.SUCCEED,
				expectationSetters
		);
	}

	private void doMassIndexingWithError(MassIndexer massIndexer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			ExecutionExpectation book2GetIdExpectation, ExecutionExpectation book2GetTitleExpectation,
			Runnable... expectationSetters) {
		Book.errorOnBook2GetId.set( ExecutionExpectation.ERROR.equals( book2GetIdExpectation ) );
		Book.errorOnBook2GetTitle.set( ExecutionExpectation.ERROR.equals( book2GetTitleExpectation ) );
		AssertionError assertionError = null;
		try {
			// Simulate passing information to connect to a DB, ...
			massIndexer.context( StubLoadingContext.class, loadingContext );

			MassIndexingFailureHandler massIndexingFailureHandler = getMassIndexingFailureHandler();
			if ( massIndexingFailureHandler != null ) {
				massIndexer.failureHandler( massIndexingFailureHandler );
			}

			for ( Runnable expectationSetter : expectationSetters ) {
				expectationSetter.run();
			}

			// TODO HSEARCH-3728 simplify this when even indexing exceptions are propagated
			Runnable runnable = () -> {
				try {
					massIndexer.startAndWait();
				}
				catch (InterruptedException e) {
					fail( "Unexpected InterruptedException: " + e.getMessage() );
				}
			};
			if ( thrownExpectation == null ) {
				runnable.run();
			}
			else {
				assertThatThrownBy( runnable::run )
						.asInstanceOf( InstanceOfAssertFactories.type( Throwable.class ) )
						.satisfies( thrownExpectation );
			}
			backendMock.verifyExpectationsMet();
		}
		catch (AssertionError e) {
			assertionError = e;
			throw e;
		}
		finally {
			Book.errorOnBook2GetId.set( false );
			Book.errorOnBook2GetTitle.set( false );

			if ( assertionError == null ) {
				switch ( threadExpectation ) {
					case CREATED_AND_TERMINATED:
						Awaitility.await().untilAsserted(
								() -> assertThat( threadSpy.getCreatedThreads( "mass index" ) )
										.as( "Mass indexing threads" )
										.isNotEmpty()
										.allSatisfy( t -> assertThat( t )
										.extracting( Thread::getState )
										.isEqualTo( Thread.State.TERMINATED )
										)
						);
						break;
					case NOT_CREATED:
						assertThat( threadSpy.getCreatedThreads( "mass index" ) )
								.as( "Mass indexing threads" )
								.isEmpty();
						break;
				}
			}
		}
	}

	private Runnable expectSchemaManagementWorkException(StubSchemaManagementWork.Type type) {
		return () -> {
			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedError( type.name() + " error" ) );
			backendMock.expectSchemaManagementWorks( Book.NAME )
					.work( type, failingFuture );
		};
	}

	private Runnable expectIndexScaleWork(StubIndexScaleWork.Type type, ExecutionExpectation executionExpectation) {
		return () -> {
			switch ( executionExpectation ) {
				case SUCCEED:
					backendMock.expectIndexScaleWorks( Book.NAME )
							.indexScaleWork( type );
					break;
				case ERROR:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedError( type.name() + " error" ) );
					backendMock.expectIndexScaleWorks( Book.NAME )
							.indexScaleWork( type, failingFuture );
					break;
				case STOP:
					break;
			}
		};
	}

	private Runnable expectIndexingWorks(ExecutionExpectation workTwoExecutionExpectation) {
		return () -> {
			switch ( workTwoExecutionExpectation ) {
				case SUCCEED:
					backendMock.expectWorks(
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "1", b -> b
									.field( "title", TITLE_1 )
									.field( "author", AUTHOR_1 )
							)
							.add( "2", b -> b
									.field( "title", TITLE_2 )
									.field( "author", AUTHOR_2 )
							)
							.add( "3", b -> b
									.field( "title", TITLE_3 )
									.field( "author", AUTHOR_3 )
							);
					break;
				case ERROR:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedError( "Indexing error" ) );
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
							.createAndExecuteFollowingWorks( failingFuture )
							.add( "2", b -> b
									.field( "title", TITLE_2 )
									.field( "author", AUTHOR_2 )
							);
					break;
				case STOP:
					backendMock.expectWorks(
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "1", b -> b
									.field( "title", TITLE_1 )
									.field( "author", AUTHOR_1 )
							);
					break;
			}
		};
	}

	private SearchMapping setupWithThrowingIdentifierLoading(String errorMessage) {
		return setup( new MassLoadingStrategy<Book, Integer>() {
			@Override
			public MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<Book> includedTypes,
					MassIdentifierSink<Integer> sink, MassLoadingOptions options) {
				return new MassIdentifierLoader() {
					@Override
					public void close() {
						// Nothing to do
					}

					@Override
					public long totalCount() {
						return 100;
					}

					@Override
					public void loadNext() {
						throw new SimulatedError( errorMessage );
					}
				};
			}

			@Override
			public MassEntityLoader<Integer> createEntityLoader(LoadingTypeGroup<Book> includedTypes,
					MassEntitySink<Book> sink, MassLoadingOptions options) {
				return new MassEntityLoader<Integer>() {
					@Override
					public void close() {
						// Nothing to do
					}

					@Override
					public void load(List<Integer> identifiers) {
						throw new UnsupportedOperationException( "Should not be called" );
					}
				};
			}
		} );
	}

	private SearchMapping setupWithThrowingEntityLoading(String exceptionMessage) {
		return setup( new MassLoadingStrategy<Book, Integer>() {
			@Override
			public MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<Book> includedTypes,
					MassIdentifierSink<Integer> sink, MassLoadingOptions options) {
				return new MassIdentifierLoader() {
					private int i = 0;

					@Override
					public void close() {
						// Nothing to do
					}

					@Override
					public long totalCount() {
						// We need more than 1000 batches in order to reproduce HSEARCH-4236.
						// That's because of the size of the queue:
						// see org.hibernate.search.mapper.orm.massindexing.impl.PojoProducerConsumerQueue.DEFAULT_BUFF_LENGTH
						return 1500;
					}

					@Override
					public void loadNext() throws InterruptedException {
						sink.accept( Collections.singletonList( i++ ) );
						if ( i >= totalCount() ) {
							sink.complete();
						}
					}
				};
			}

			@Override
			public MassEntityLoader<Integer> createEntityLoader(LoadingTypeGroup<Book> includedTypes,
					MassEntitySink<Book> sink, MassLoadingOptions options) {
				return new MassEntityLoader<Integer>() {
					@Override
					public void close() {
						// Nothing to do
					}

					@Override
					public void load(List<Integer> identifiers) {
						throw new SimulatedError( exceptionMessage );
					}
				};
			}
		} );
	}

	private SearchMapping setup() {
		return setup( new StubMassLoadingStrategy<>( Book.PERSISTENCE_KEY ) );
	}

	private SearchMapping setup(MassLoadingStrategy<Book, Integer> loadingStrategy) {
		assertBeforeSetup();

		backendMock.expectAnySchema( Book.NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, getBackgroundFailureHandlerReference() )
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.withConfiguration( b -> {
					b.addEntityType( Book.class, c -> c.massLoadingStrategy( loadingStrategy ) );
				} )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
		persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
		persist( new Book( 3, TITLE_3, AUTHOR_3 ) );

		assertAfterSetup();

		return mapping;
	}

	private void persist(Book book) {
		loadingContext.persistenceMap( Book.PERSISTENCE_KEY ).put( book.id, book );
	}

	private enum ExecutionExpectation {
		SUCCEED,
		ERROR,
		STOP;
	}

	private enum ThreadExpectation {
		CREATED_AND_TERMINATED,
		NOT_CREATED;
	}

	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";
		public static final PersistenceTypeKey<Book, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( Book.class, Integer.class );

		private static final AtomicBoolean errorOnBook2GetId = new AtomicBoolean( false );
		private static final AtomicBoolean errorOnBook2GetTitle = new AtomicBoolean( false );

		private Integer id;

		private String title;

		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		@DocumentId // This must be on the getter, so that Hibernate Search uses getters instead of direct field access
		public Integer getId() {
			if ( id == 2 && errorOnBook2GetId.getAndSet( false ) ) {
				throw new SimulatedError( "getId error" );
			}
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getTitle() {
			if ( id == 2 && errorOnBook2GetTitle.get() ) {
				throw new SimulatedError( "getTitle error" );
			}
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@GenericField
		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}
	}

	protected static class SimulatedError extends Error {
		SimulatedError(String message) {
			super( message );
		}
	}
}
