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
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.ThreadSpy;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

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
	public BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public ThreadSpy threadSpy = new ThreadSpy();

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@Test
	public void indexing() {
		SearchMapping mapping = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String exceptionMessage = "Indexing failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityIndexingFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 entities could not be indexed",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.SUCCEED )
		);

		assertEntityIndexingFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void getId() {
		SearchMapping mapping = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String exceptionMessage = "getId failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityIdGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
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
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.SUCCEED )
		);

		assertEntityIdGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void getTitle() {
		SearchMapping mapping = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String exceptionMessage = "getTitle failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityNonIdGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
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
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SKIP ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.SUCCEED )
		);

		assertEntityNonIdGetterFailureHandling(
				entityName, entityReferenceAsString,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void dropAndCreateSchema_exception() {
		SearchMapping mapping = setup();

		String exceptionMessage = "DROP_AND_CREATE failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SearchException.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer()
						.dropAndCreateSchemaOnStart( true ),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
								.typeContext( Book.class.getName() )
								.failure( exceptionMessage )
								.build() ),
				expectSchemaManagementWorkException( StubSchemaManagementWork.Type.DROP_AND_CREATE )
		);

		assertMassIndexerOperationFailureHandling( SearchException.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void purge() {
		SearchMapping mapping = setup();

		String exceptionMessage = "PURGE failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void mergeSegmentsBefore() {
		SearchMapping mapping = setup();

		String exceptionMessage = "MERGE_SEGMENTS failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void mergeSegmentsAfter() {
		SearchMapping mapping = setup();

		String exceptionMessage = "MERGE_SEGMENTS failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer()
						.mergeSegmentsOnFinish( true ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void flush() {
		SearchMapping mapping = setup();

		String exceptionMessage = "FLUSH failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void refresh() {
		SearchMapping mapping = setup();

		String exceptionMessage = "REFRESH failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SimulatedFailure.class )
						.hasMessageContaining( exceptionMessage ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void indexingAndFlush() {
		SearchMapping mapping = setup();

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
				mapping.scope( Object.class ).massIndexer(),
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
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReferenceAsString,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);
	}

	@Test
	public void indexingAndRefresh() {
		SearchMapping mapping = setup();

		String entityName = Book.NAME;
		String entityReferenceAsString = Book.NAME + "#2";
		String failingEntityIndexingExceptionMessage = "Indexing failure";
		String failingEntityIndexingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";
		String failingMassIndexerOperationExceptionMessage = "REFRESH failure";
		String failingMassIndexerOperationAsString = "MassIndexer operation";

		expectEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReferenceAsString,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);

		doMassIndexingWithFailure(
				mapping.scope( Object.class ).massIndexer(),
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
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.FAIL )
		);

		assertEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReferenceAsString,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);
	}

	protected abstract FailureHandler getBackgroundFailureHandlerReference();

	protected abstract MassIndexingFailureHandler getMassIndexingFailureHandler();

	protected void assertBeforeSetup() {
	}

	protected void assertAfterSetup() {
	}

	protected abstract void expectEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityIndexingFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectEntityIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectEntityNonIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityNonIdGetterFailureHandling(String entityName, String entityReferenceAsString,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString);

	protected abstract void assertMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString);

	protected abstract void expectEntityIndexingAndMassIndexerOperationFailureHandling(
			String entityName, String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString);

	protected abstract void assertEntityIndexingAndMassIndexerOperationFailureHandling(
			String entityName, String entityReferenceAsString,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString);

	private void doMassIndexingWithFailure(MassIndexer massIndexer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable... expectationSetters) {
		doMassIndexingWithFailure(
				massIndexer,
				threadExpectation,
				thrownExpectation,
				ExecutionExpectation.SUCCEED, ExecutionExpectation.SUCCEED,
				expectationSetters
		);
	}

	private void doMassIndexingWithFailure(MassIndexer massIndexer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			ExecutionExpectation book2GetIdExpectation, ExecutionExpectation book2GetTitleExpectation,
			Runnable... expectationSetters) {
		Book.failOnBook2GetId.set( ExecutionExpectation.FAIL.equals( book2GetIdExpectation ) );
		Book.failOnBook2GetTitle.set( ExecutionExpectation.FAIL.equals( book2GetTitleExpectation ) );
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

	private Runnable expectSchemaManagementWorkException(StubSchemaManagementWork.Type type) {
		return () -> {
			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( type.name() + " failure" ) );
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
				case FAIL:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedFailure( type.name() + " failure" ) );
					backendMock.expectIndexScaleWorks( Book.NAME )
							.indexScaleWork( type, failingFuture );
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
							)
							.createdThenExecuted();
					break;
				case FAIL:
					CompletableFuture<?> failingFuture = new CompletableFuture<>();
					failingFuture.completeExceptionally( new SimulatedFailure( "Indexing failure" ) );
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
							.createdThenExecuted();
					backendMock.expectWorks(
							Book.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
							.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
							)
							.createdThenExecuted( failingFuture );
					break;
				case SKIP:
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
							.createdThenExecuted();
					break;
			}
		};
	}

	private SearchMapping setup() {
		assertBeforeSetup();

		backendMock.expectAnySchema( Book.NAME );

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, getBackgroundFailureHandlerReference() )
				.withPropertyRadical( EngineSpiSettings.Radicals.THREAD_PROVIDER, threadSpy.getThreadProvider() )
				.withConfiguration( b -> {
					b.addEntityType( Book.class, c -> c
							.massLoadingStrategy( new StubMassLoadingStrategy<>( Book.PERSISTENCE_KEY ) ) );
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
		FAIL,
		SKIP;
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

		@DocumentId // This must be on the getter, so that Hibernate Search uses getters instead of direct field access
		public Integer getId() {
			if ( id == 2 && failOnBook2GetId.getAndSet( false ) ) {
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
