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

import java.util.Optional;
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
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
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

public abstract class AbstractMassIndexingFailureIT {
	private static final int COUNT = 1500;
	private static final int FAILURE_FLOODING_THRESHOLD = 45;
	private static final int DEFAULT_FAILURE_FLOODING_THRESHOLD = 100;

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

	public int getDefaultFailureFloodingThreshold() {
		return DEFAULT_FAILURE_FLOODING_THRESHOLD;
	}

	@Test
	@TestForIssue(jiraKey = {"HSEARCH-4218", "HSEARCH-4236"})
	public void identifierLoading() {
		SessionFactory sessionFactory = setup( builder ->
				builder.setProperty(
						AvailableSettings.AUTO_SESSION_EVENTS_LISTENER,
						JdbcStatementFailureOnIdLoadingThreadListener.class.getName()
				)
		);

		String exceptionMessage = JdbcStatementFailureOnIdLoadingThreadListener.MESSAGE;
		String failingOperationAsString = "Fetching identifiers of entities to index for entity '"
				+ Book.NAME + "' during mass indexing";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								// Sometimes the JDBC driver of Oracle may (or may not) react to thread interruptions raising other failures.
								// In this case, more failures than 1 can be raised, so we cannot assert the exact failures number.
								"failure(s) occurred during mass indexing",
								"See the logs for details.",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4236")
	public void entityLoading() {
		entityLoading( Optional.empty() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4236")
	public void entityLoadingWithFailureFloodingThreshold() {
		entityLoading( Optional.of( FAILURE_FLOODING_THRESHOLD ) );
	}

	public void entityLoading(Optional<Integer> failureFloodingThreshold) {
		SessionFactory sessionFactory = setup( builder ->
				builder.setProperty(
						AvailableSettings.AUTO_SESSION_EVENTS_LISTENER,
						JdbcStatementFailureOnEntityLoadingThreadListener.class.getName()
				)
		);

		// We need more than 1000 batches in order to reproduce HSEARCH-4236.
		// That's because of the size of the queue:
		// see org.hibernate.search.mapper.orm.massindexing.impl.PojoProducerConsumerQueue.DEFAULT_BUFF_LENGTH
		with( sessionFactory ).runInTransaction( session -> {
			for ( int i = 4; i <= COUNT; i++ ) {
				session.persist( new Book( i, "title " + i, "author " + i ) );
			}
		} );

		String exceptionMessage = JdbcStatementFailureOnEntityLoadingThreadListener.MESSAGE;
		String failingOperationAsString = "Loading and extracting entity data for entity '"
				+ Book.NAME + "' during mass indexing";

		// in case of using default handlers we will get a failure flooding threshold of 100 coming from PojoMassIndexingDelegatingFailureHandler
		Integer actualThreshold = failureFloodingThreshold.orElseGet( this::getDefaultFailureFloodingThreshold );
		expectMassIndexerLoadingOperationFailureHandling(
				SimulatedFailure.class, exceptionMessage,
				actualThreshold,
				failingOperationAsString,
				"Entities that could not be indexed correctly"
		);
		expectMassIndexerLoadingOperationFailureHandling(
				SearchException.class,
				"",
				1,
				( COUNT - actualThreshold ) + " failures went unreported for this operation to avoid flooding."
		);

		MassIndexer massIndexer = Search.mapping( sessionFactory ).scope( Object.class ).massIndexer()
				.threadsToLoadObjects( 1 ) // Just to simplify the assertions
				.batchSizeToLoadObjects( 1 ); // We need more than 1000 batches in order to reproduce HSEARCH-4236
		failureFloodingThreshold.ifPresent( threshold -> {
			massIndexer.failureFloodingThreshold( threshold );
		} );
		doMassIndexingWithFailure(
				massIndexer,
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								// Sometimes the JDBC driver of Oracle may (or may not) react to thread interruptions raising other failures.
								// In this case, more failures than 1 can be raised, so we cannot assert the exact failures number.
								"failure(s) occurred during mass indexing",
								"See the logs for details.",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.SUCCEED )
		);

		assertMassIndexerLoadingOperationFailureHandling(
				SimulatedFailure.class, exceptionMessage, failingOperationAsString,
				actualThreshold,
				SearchException.class,
				( COUNT - actualThreshold ) + " failures went unreported for this operation to avoid flooding.",
				failingOperationAsString
		);
	}

	@Test
	public void indexing() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		EntityReference entityReference = PojoEntityReference.withName( Book.class, Book.NAME, 2 );
		String exceptionMessage = "Indexing failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityIndexingFailureHandling(
				entityName, entityReference,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
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
				entityName, entityReference,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void getId() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		EntityReference entityReference = PojoEntityReference.withName( Book.class, Book.NAME, 2 );
		String exceptionMessage = "getId failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityIdGetterFailureHandling(
				entityName, entityReference,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
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
				entityName, entityReference,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void getTitle() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		EntityReference entityReference = PojoEntityReference.withName( Book.class, Book.NAME, 2 );
		String exceptionMessage = "getTitle failure";
		String failingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";

		expectEntityNonIdGetterFailureHandling(
				entityName, entityReference,
				exceptionMessage, failingOperationAsString
		);

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
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
				entityName, entityReference,
				exceptionMessage, failingOperationAsString
		);
	}

	@Test
	public void dropAndCreateSchema_exception() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "DROP_AND_CREATE failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SearchException.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer().dropAndCreateSchemaOnStart( true ),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.satisfies( FailureReportUtils.hasFailureReport()
								.typeContext( Book.class.getName() )
								.failure( exceptionMessage ) ),
				expectSchemaManagementWorkException( StubSchemaManagementWork.Type.DROP_AND_CREATE )
		);

		assertMassIndexerOperationFailureHandling( SearchException.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void purge() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "PURGE failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
								"See the logs for details.",
								"First failure: ",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void mergeSegmentsBefore() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "MERGE_SEGMENTS failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.NOT_CREATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
								"See the logs for details.",
								"First failure: ",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void mergeSegmentsAfter() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "MERGE_SEGMENTS failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer()
						.mergeSegmentsOnFinish( true ),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
								"See the logs for details.",
								"First failure: ",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void flush() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "FLUSH failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
								"See the logs for details.",
								"First failure: ",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );
	}

	@Test
	public void refresh() {
		SessionFactory sessionFactory = setup();

		String exceptionMessage = "REFRESH failure";
		String failingOperationAsString = "MassIndexer operation";

		expectMassIndexerOperationFailureHandling( SimulatedFailure.class, exceptionMessage, failingOperationAsString );

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"1 failure(s) occurred during mass indexing",
								"See the logs for details.",
								"First failure: ",
								exceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class ),
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
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		EntityReference entityReference = PojoEntityReference.withName( Book.class, Book.NAME, 2 );
		String failingEntityIndexingExceptionMessage = "Indexing failure";
		String failingEntityIndexingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";
		String failingMassIndexerOperationExceptionMessage = "FLUSH failure";
		String failingMassIndexerOperationAsString = "MassIndexer operation";

		expectEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReference,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"2 failure(s) occurred during mass indexing",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								failingEntityIndexingExceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class )
						// The mass indexer operation failure should also be mentioned as a suppressed exception
						.extracting( Throwable::getCause )
						.extracting( Throwable::getSuppressed ).asInstanceOf( InstanceOfAssertFactories.ARRAY )
						.anySatisfy( suppressed -> assertThat( suppressed ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
								.isInstanceOf( SimulatedFailure.class )
								.hasMessageContaining( failingMassIndexerOperationExceptionMessage )
						),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.FAIL )
		);

		assertEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReference,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);
	}

	@Test
	public void indexingAndRefresh() {
		SessionFactory sessionFactory = setup();

		String entityName = Book.NAME;
		EntityReference entityReference = PojoEntityReference.withName( Book.class, Book.NAME, 2 );
		String failingEntityIndexingExceptionMessage = "Indexing failure";
		String failingEntityIndexingOperationAsString = "Indexing instance of entity '" + entityName + "' during mass indexing";
		String failingMassIndexerOperationExceptionMessage = "REFRESH failure";
		String failingMassIndexerOperationAsString = "MassIndexer operation";

		expectEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReference,
				failingEntityIndexingExceptionMessage, failingEntityIndexingOperationAsString,
				failingMassIndexerOperationExceptionMessage, failingMassIndexerOperationAsString
		);

		doMassIndexingWithFailure(
				Search.mapping( sessionFactory ).scope( Object.class ).massIndexer(),
				ThreadExpectation.CREATED_AND_TERMINATED,
				throwable -> assertThat( throwable ).isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"2 failure(s) occurred during mass indexing",
								"See the logs for details.",
								"First failure on entity 'Book#2': ",
								failingEntityIndexingExceptionMessage
						)
						.hasCauseInstanceOf( SimulatedFailure.class )
						// The mass indexer operation failure should also be mentioned as a suppressed exception
						.extracting( Throwable::getCause )
						.extracting( Throwable::getSuppressed ).asInstanceOf( InstanceOfAssertFactories.ARRAY )
						.anySatisfy( suppressed -> assertThat( suppressed ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
								.isInstanceOf( SimulatedFailure.class )
								.hasMessageContaining( failingMassIndexerOperationExceptionMessage )
						),
				expectIndexScaleWork( StubIndexScaleWork.Type.PURGE, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, ExecutionExpectation.SUCCEED ),
				expectIndexingWorks( ExecutionExpectation.FAIL ),
				expectIndexScaleWork( StubIndexScaleWork.Type.FLUSH, ExecutionExpectation.SUCCEED ),
				expectIndexScaleWork( StubIndexScaleWork.Type.REFRESH, ExecutionExpectation.FAIL )
		);

		assertEntityIndexingAndMassIndexerOperationFailureHandling(
				entityName, entityReference,
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

	protected abstract void expectEntityIndexingFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityIndexingFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectEntityIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectEntityNonIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void assertEntityNonIdGetterFailureHandling(String entityName, EntityReference entityReference,
			String exceptionMessage, String failingOperationAsString);

	protected abstract void expectMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString);

	protected abstract void assertMassIndexerOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString);

	protected abstract void expectMassIndexerLoadingOperationFailureHandling(Class<? extends Throwable> exceptionType,
			String exceptionMessage, int count, String failingOperationAsString, String... extraMessages);

	protected abstract void assertMassIndexerLoadingOperationFailureHandling(
			Class<? extends Throwable> exceptionType, String exceptionMessage,
			String failingOperationAsString,
			int failureFloodingThreshold, Class<? extends Throwable> closingExceptionType,
			String closingExceptionMessage, String closingFailingOperationAsString);

	protected abstract void expectEntityIndexingAndMassIndexerOperationFailureHandling(
			String entityName, EntityReference entityReference,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString);

	protected abstract void assertEntityIndexingAndMassIndexerOperationFailureHandling(
			String entityName, EntityReference entityReference,
			String failingEntityIndexingExceptionMessage, String failingEntityIndexingOperationAsString,
			String failingMassIndexerOperationExceptionMessage, String failingMassIndexerOperationAsString);

	private void doMassIndexingWithFailure(MassIndexer massIndexer,
			ThreadExpectation threadExpectation,
			Consumer<Throwable> thrownExpectation,
			Runnable ... expectationSetters) {
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
			Runnable ... expectationSetters) {
		Book.failOnBook2GetId.set( ExecutionExpectation.FAIL.equals( book2GetIdExpectation ) );
		Book.failOnBook2GetTitle.set( ExecutionExpectation.FAIL.equals( book2GetTitleExpectation ) );
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
			Book.failOnBook2GetId.set( false );
			Book.failOnBook2GetTitle.set( false );
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
							);
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
							.createAndExecuteFollowingWorks( failingFuture )
							.add( "2", b -> b
									.field( "title", TITLE_2 )
									.field( "author", AUTHOR_2 )
							);
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
			if ( id == 2
					// Only fail for Hibernate Search, not for Hibernate ORM
					&& RuntimeHelper.firstNonSelfNonJdkCaller().map( RuntimeHelper::isHibernateSearch ).orElse( false )
					&& failOnBook2GetId.getAndSet( false ) ) {
				throw new SimulatedFailure( "getId failure" );
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
					&& failOnBook2GetTitle.getAndSet( false ) ) {
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

	public static class JdbcStatementFailureOnIdLoadingThreadListener extends BaseSessionEventListener {
		private static final String MESSAGE = "Simulated JDBC statement failure on ID loading";

		@Override
		public void jdbcExecuteStatementStart() {
			if ( Thread.currentThread().getName().contains( "- ID loading" ) ) {
				throw new SimulatedFailure( MESSAGE );
			}
		}
	}

	public static class JdbcStatementFailureOnEntityLoadingThreadListener extends BaseSessionEventListener {
		private static final String MESSAGE = "Simulated JDBC statement failure on entity loading";

		@Override
		public void jdbcExecuteStatementStart() {
			if ( Thread.currentThread().getName().contains( "- Entity loading" ) ) {
				throw new SimulatedFailure( MESSAGE );
			}
		}
	}
}
