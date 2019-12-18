/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.ThreadSpy;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;

public abstract class AbstractMassIndexingFailureIT {

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ThreadSpy threadSpy = new ThreadSpy();

	@Test
	public void indexing() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String exceptionMessage = "Indexing failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityIndexingFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);

		assertEntityIndexingFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void getId() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String exceptionMessage = "getId failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								"Exception while invoking"
						)
						.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining( "Exception while invoking" ),
				ExecutionExpectation.FAIL, ExecutionExpectation.SKIP,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);

		assertEntityGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void getTitle() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String exceptionMessage = "getTitle failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								"Exception while invoking"
						)
						.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining( "Exception while invoking" ),
				ExecutionExpectation.SUCCEED, ExecutionExpectation.FAIL,
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.SUCCEED )
		);

		assertEntityGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void purge() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "PURGE failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );
	}

	@Test
	public void mergeSegmentsBefore() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "MERGE_SEGMENTS failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );
	}

	@Test
	public void mergeSegmentsAfter() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "MERGE_SEGMENTS failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer().mergeSegmentsOnFinish( true ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );
	}

	@Test
	public void flush() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "FLUSH failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( exceptionMessage, failingOperationAsString );
	}

	@Test
	public void indexingAndFlush() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String failingEntityIndexingExceptionMessage = "Indexing failure";
		String failingEntityIndexingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";
		String failingMassIndexerOperationExceptionMessage = "FLUSH failure";
		String failingMassIndexerOperationAsString = "MassIndexer operation";

		expectEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReferenceAsString,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);

		doMassIndexingWithFailure(
				sessionFactory,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( failingMassIndexerOperationExceptionMessage )
						// Indexing failure should also be mentioned as a suppressed exception
						.extracting( Throwable::getSuppressed ).asInstanceOf( InstanceOfAssertFactories.ARRAY )
						.anySatisfy( suppressed -> assertThat( suppressed ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
								.isInstanceOf( SearchException.class )
								.hasMessageContainingAll(
										"1 entities could not be indexed",
										"See the logs for details.",
										"First failure on entity 'Book#2': ",
										failingEntityIndexingExceptionMessage
								)
								.hasCauseInstanceOf( SimulatedFailure.class )
						),
				expectIndexScopeWork( StubIndexScopeWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScopeWork( StubIndexScopeWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScopeWork( StubIndexScopeWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReferenceAsString,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);
	}

	protected abstract String getBackgroundFailureHandlerReference();

	protected abstract MassIndexingFailureHandler getMassIndexingFailureHandler();

	protected void assertBeforeSetup() {
	}

	protected void assertAfterSetup() {
	}

	protected abstract void expectEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectEntityGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectMassIndexerOperationFailureHandling(
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertMassIndexerOperationFailureHandling(
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectEntityIndexingAndMassIndexerOperationFailureHandling(
			String entityName, String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString);

	protected abstract void assertEntityIndexingAndMassIndexerOperationFailureHandling(
			String entityName, String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString);

	private void doMassIndexingWithFailure(SessionFactory sessionFactory,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable ... expectationSetters) {
		doMassIndexingWithFailure(
				sessionFactory,
				searchSession -> searchSession.massIndexer(),
				threadExpectation,
				thrownExpectation,
				expectationSetters
		);
	}

	private void doMassIndexingWithFailure(SessionFactory sessionFactory,
			Function<SearchSession, MassIndexer> indexerProducer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable ... expectationSetters) {
		doMassIndexingWithFailure(
				sessionFactory,
				indexerProducer,
				threadExpectation,
				thrownExpectation,
				ExecutionExpectation.SUCCEED, ExecutionExpectation.SUCCEED,
				expectationSetters
		);
	}

	private void doMassIndexingWithFailure(SessionFactory sessionFactory,
			Function<SearchSession, MassIndexer> indexerProducer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			ExecutionExpectation book2GetIdExpectation, ExecutionExpectation book2GetTitleExpectation,
			Runnable ... expectationSetters) {
		Book.failOnBook2GetId.set( ExecutionExpectation.FAIL.equals( book2GetIdExpectation ) );
		Book.failOnBook2GetTitle.set( ExecutionExpectation.FAIL.equals( book2GetTitleExpectation ) );
		AssertionError assertionError = null;
		try {
			OrmUtils.withinSession( sessionFactory, session -> {
				SearchSession searchSession = Search.session( session );
				MassIndexer indexer = indexerProducer.apply( searchSession );

				MassIndexingFailureHandler massIndexingFailureHandler = getMassIndexingFailureHandler();
				if ( massIndexingFailureHandler != null ) {
					indexer.failureHandler( massIndexingFailureHandler );
				}

				for ( Runnable expectationSetter : expectationSetters ) {
					expectationSetter.run();
				}

				// TODO HSEARCH-3728 simplify this when even indexing exceptions are propagated
				Runnable runnable = () -> {
					try {
						indexer.startAndWait();
					}
					catch (InterruptedException e) {
						fail( "Unexpected InterruptedException: " + e.getMessage() );
					}
				};
				if ( thrownExpectation == null ) {
					runnable.run();
				}
				else {
					SubTest.expectException( runnable )
							.assertThrown()
							.satisfies( thrownExpectation );
				}
			} );
			backendMock.verifyExpectationsMet();
		}
		catch (AssertionError e) {
			assertionError = e;
			throw e;
		}
		finally {
			Book.failOnBook2GetId.set( false );
			Book.failOnBook2GetTitle.set( false );

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

	private Runnable expectIndexScopeWork(StubIndexScopeWork.Type type, ExecutionExpectation executionExpectation) {
		return () -> {
			switch ( executionExpectation ) {
				case SUCCEED:
					backendMock.expectIndexScopeWorks( Book.NAME )
							.indexScopeWork( type );
					break;
				case FAIL:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedFailure( type.name() + " failure" ) );
					backendMock.expectIndexScopeWorks( Book.NAME )
							.indexScopeWork( type, failingFuture );
					break;
				case SKIP:
					break;
			}
		};
	}

	private Runnable expectIndexingWorks(ExecutionExpectation workTwoExecutionExpectation) {
		return () -> {
			switch ( workTwoExecutionExpectation ) {
				case SUCCEED:
					backendMock.expectWorksAnyOrder(
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
							)
							.processedThenExecuted();
					break;
				case FAIL:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedFailure( "Indexing failure" ) );
					backendMock.expectWorksAnyOrder(
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
							.processedThenExecuted();
					backendMock.expectWorksAnyOrder(
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "2", b -> b
									.field( "title", TITLE_2 )
									.field( "author", AUTHOR_2 )
							)
							.processedThenExecuted( failingFuture );
					break;
				case SKIP:
					backendMock.expectWorksAnyOrder(
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
							.processedThenExecuted();
					break;
			}
		};
	}

	private SessionFactory setup() {
		assertBeforeSetup();

		backendMock.expectAnySchema( Book.NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY, AutomaticIndexingStrategyName.NONE )
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, getBackgroundFailureHandlerReference() )
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.setup( Book.class );

		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
		} );

		assertAfterSetup();

		return sessionFactory;
	}

	private enum ExecutionExpectation {
		SUCCEED,
		FAIL,
		SKIP;
	}

	private enum ThreadExpectation {
		CREATED_AND_TERMINATED,
		NOT_CREATED;
	}

	@Entity(name = Book.NAME)
	@Indexed(index = Book.NAME)
	public static class Book {

		public static final String NAME = "Book";

		private static final AtomicBoolean failOnBook2GetId = new AtomicBoolean( false );
		private static final AtomicBoolean failOnBook2GetTitle = new AtomicBoolean( false );

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
			if ( id == 2 && failOnBook2GetId.get() ) {
				throw new SimulatedFailure( "getId failure" );
			}
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getTitle() {
			if ( id == 2 && failOnBook2GetTitle.get() ) {
				throw new SimulatedFailure( "getTitle failure" );
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

	protected static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
