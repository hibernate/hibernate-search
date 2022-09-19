/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.ThreadSpy;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.reflect.RuntimeHelper;

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
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ThreadSpy threadSpy = new ThreadSpy();

	@Test
	@TestForIssue(jiraKey = {"HSEARCH-4218", "HSEARCH-4236"})
	public void identifierLoading() {
		SessionFactory sessionFactory = setup( builder ->
				builder.setProperty(
						AvailableSettings.AUTO_SESSION_EVENTS_LISTENER,
						JdbcStatementErrorOnIdLoadingThreadListener.class.getName()
				)
		);

		String errorMessage = JdbcStatementErrorOnIdLoadingThreadListener.MESSAGE;

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessageContainingAll( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED )
		);

		assertNoFailureHandling();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4236")
	public void entityLoading() {
		SessionFactory sessionFactory = setup( builder ->
				builder.setProperty(
						AvailableSettings.AUTO_SESSION_EVENTS_LISTENER,
						JdbcStatementErrorOnEntityLoadingThreadListener.class.getName()
				)
		);

		// We need more than 1000 batches in order to reproduce HSEARCH-4236.
		// That's because of the size of the queue:
		// see org.hibernate.search.mapper.orm.massindexing.impl.PojoProducerConsumerQueue.DEFAULT_BUFF_LENGTH
		with( sessionFactory ).runInTransaction( session -> {
			for ( int i = 4; i < 1500; i++ ) {
				session.persist( new Book( i, "title " + i, "author " + i ) );
			}
		} );

		String errorMessage = JdbcStatementErrorOnEntityLoadingThreadListener.MESSAGE;

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer()
						.threadsToLoadObjects( 1 ) // Just to simplify the assertions
						.batchSizeToLoadObjects( 1 ), // We need more than 1000 batches in order to reproduce HSEARCH-4236
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessageContainingAll( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED )
		);

		assertNoFailureHandling();
	}

	@Test
	public void indexing() {
		SessionFactory sessionFactory = setup();

		String errorMessage = "Indexing error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
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
		SessionFactory sessionFactory = setup();

		String errorMessage = "getId error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
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
		SessionFactory sessionFactory = setup();

		String errorMessage = "getTitle error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
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
		SessionFactory sessionFactory = setup();

		String errorMessage = "DROP_AND_CREATE error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer().dropAndCreateSchemaOnStart( true ),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectSchemaManagementWorkException( StubSchemaManagementWork.Type.DROP_AND_CREATE )
		);

		assertNoFailureHandling();
	}

	@Test
	public void purge() {
		SessionFactory sessionFactory = setup();

		String errorMessage = "PURGE error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedError.class )
						.hasMessage( errorMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.ERROR )
		);

		assertNoFailureHandling();
	}

	@Test
	public void mergeSegmentsBefore() {
		SessionFactory sessionFactory = setup();

		String errorMessage = "MERGE_SEGMENTS error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
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
		SessionFactory sessionFactory = setup();

		String errorMessage = "MERGE_SEGMENTS error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer()
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
		SessionFactory sessionFactory = setup();

		String errorMessage = "FLUSH error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
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
		SessionFactory sessionFactory = setup();

		String errorMessage = "REFRESH error";

		expectNoFailureHandling();

		doMassIndexingWithError(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
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
			Runnable ... expectationSetters) {
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
			Runnable ... expectationSetters) {
		Book.errorOnBook2GetId.set( ExecutionExpectation.ERROR.equals( book2GetIdExpectation ) );
		Book.errorOnBook2GetTitle.set( ExecutionExpectation.ERROR.equals( book2GetTitleExpectation ) );
		try {
			MassIndexingFailureHandler massIndexingFailureHandler = getMassIndexingFailureHandler();
			if ( massIndexingFailureHandler != null ) {
				massIndexer.failureHandler( massIndexingFailureHandler );
			}

			for ( Runnable expectationSetter : expectationSetters ) {
				expectationSetter.run();
			}

			assertThatThrownBy( () -> {
				try {
					massIndexer.startAndWait();
				}
				catch (InterruptedException e) {
					fail( "Unexpected InterruptedException: " + e.getMessage() );
				}
			} )
					.asInstanceOf( InstanceOfAssertFactories.type( Throwable.class ) )
					.satisfies( thrownExpectation );
			backendMock.verifyExpectationsMet();

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
		finally {
			Book.errorOnBook2GetId.set( false );
			Book.errorOnBook2GetTitle.set( false );
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

	private SessionFactory setup() {
		return setup( ignored -> { } );
	}

	private SessionFactory setup(Consumer<SimpleSessionFactoryBuilder> configuration) {
		assertBeforeSetup();

		backendMock.expectAnySchema( Book.NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLED, false )
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, getBackgroundFailureHandlerReference() )
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.withConfiguration( configuration )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
		} );

		assertAfterSetup();

		return sessionFactory;
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

	@Entity(name = Book.NAME)
	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";

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

		@Id // This must be on the getter, so that Hibernate Search uses getters instead of direct field access
		public Integer getId() {
			if ( id == 2
					// Only fail for Hibernate Search, not for Hibernate ORM
					&& RuntimeHelper.firstNonSelfNonJdkCaller().map( RuntimeHelper::isHibernateSearch ).orElse( false )
					&& errorOnBook2GetId.getAndSet( false ) ) {
				throw new SimulatedError( "getId error" );
			}
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getTitle() {
			if ( id == 2
					// Only fail for Hibernate Search, not for Hibernate ORM
					&& RuntimeHelper.firstNonSelfNonJdkCaller().map( RuntimeHelper::isHibernateSearch ).orElse( false )
					&& errorOnBook2GetTitle.getAndSet( false ) ) {
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

	public static class JdbcStatementErrorOnIdLoadingThreadListener extends BaseSessionEventListener {
		private static final String MESSAGE = "Simulated JDBC statement error on ID loading";

		@Override
		public void jdbcExecuteStatementStart() {
			if ( Thread.currentThread().getName().contains( "- ID loading" ) ) {
				throw new SimulatedError( MESSAGE );
			}
		}
	}

	public static class JdbcStatementErrorOnEntityLoadingThreadListener extends BaseSessionEventListener {
		private static final String MESSAGE = "Simulated JDBC statement error on entity loading";

		@Override
		public void jdbcExecuteStatementStart() {
			if ( Thread.currentThread().getName().contains( "- Entity loading" ) ) {
				throw new SimulatedError( MESSAGE );
			}
		}
	}
}
