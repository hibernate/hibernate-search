/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;

public class AutomaticIndexingSynchronizationStrategyIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Let's say 3 seconds are long enough to consider that, if nothing changed after this time, nothing ever will.
	private static final long ALMOST_FOREVER_VALUE = 3L;
	private static final TimeUnit ALMOST_FOREVER_UNIT = TimeUnit.SECONDS;

	private static final long SMALL_DURATION_VALUE = 100L;
	private static final TimeUnit SMALL_DURATION_UNIT = TimeUnit.MILLISECONDS;

	private static final int ENTITY_1_ID = 1;
	private static final int ENTITY_2_ID = 2;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void success_async() throws InterruptedException, ExecutionException, TimeoutException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.ASYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingNoBlock(
				sessionFactory, null,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, indexingWorkFuture
		);

		// The transaction thread should proceed successfully,
		// regardless of the indexing work.
		assertThatFuture( transactionThreadFuture ).isSuccessful();
	}

	@Test
	public void success_writeSync_default() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( null );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the transaction thread
		indexingWorkFuture.complete( null );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed successfully,
		// because the indexing work was successful.
		assertThatFuture( transactionThreadFuture ).isSuccessful();
	}

	@Test
	public void success_writeSync_explicit() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the transaction thread
		indexingWorkFuture.complete( null );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed successfully,
		// because the indexing work was successful.
		assertThatFuture( transactionThreadFuture ).isSuccessful();
	}

	@Test
	public void success_readSync() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.READ_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the transaction thread
		indexingWorkFuture.complete( null );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed successfully,
		// because the indexing work was successful.
		assertThatFuture( transactionThreadFuture ).isSuccessful();
	}

	@Test
	public void success_sync() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the transaction thread
		indexingWorkFuture.complete( null );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed successfully,
		// because the indexing work was successful.
		assertThatFuture( transactionThreadFuture ).isSuccessful();
	}

	@Test
	public void success_override_writeSyncToSync() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, AutomaticIndexingSynchronizationStrategy.sync(),
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the transaction thread
		indexingWorkFuture.complete( null );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed successfully,
		// because the indexing work was successful.
		assertThatFuture( transactionThreadFuture ).isSuccessful();
	}

	@Test
	public void success_override_writeSyncToCustom() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		AtomicReference<CompletableFuture<?>> futureThatTookTooLong = new AtomicReference<>( null );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingNoBlock(
				sessionFactory, new CustomAutomaticIndexingSynchronizationStrategy( futureThatTookTooLong ),
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction should be complete, because the indexing work took too long to execute
		// (this is how the custom automatic indexing strategy is implemented)
		assertThatFuture( transactionThreadFuture ).isSuccessful();

		// Upon timing out, the strategy should have set this reference
		assertThat( futureThatTookTooLong ).doesNotHaveValue( null );
	}

	@Test
	public void success_custom_blocking_submitter() throws InterruptedException, TimeoutException, ExecutionException {
		AtomicReference<CompletableFuture<?>> futureThatTookTooLong = new AtomicReference<>( null );

		SessionFactory sessionFactory = setup(
				new CustomAutomaticIndexingSynchronizationStrategy( futureThatTookTooLong, OperationSubmitter.BLOCKING )
		);
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingNoBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction should be complete, because the indexing work took too long to execute
		// (this is how the custom automatic indexing strategy is implemented)
		assertThatFuture( transactionThreadFuture ).isSuccessful();

		// Upon timing out, the strategy should have set this reference
		assertThat( futureThatTookTooLong ).doesNotHaveValue( null );
	}

	@Test
	public void success_custom_rejected_submitter() throws InterruptedException, TimeoutException, ExecutionException {
		AtomicReference<CompletableFuture<?>> futureThatTookTooLong = new AtomicReference<>( null );

		SessionFactory sessionFactory = setup(
				new CustomAutomaticIndexingSynchronizationStrategy( futureThatTookTooLong, OperationSubmitter.REJECTED_EXECUTION_EXCEPTION )
		);
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingNoBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction should be complete, because the indexing work took too long to execute
		// (this is how the custom automatic indexing strategy is implemented)
		assertThatFuture( transactionThreadFuture ).isSuccessful();

		// Upon timing out, the strategy should have set this reference
		assertThat( futureThatTookTooLong ).doesNotHaveValue( null );
	}

	@Test
	public void failure_async() throws InterruptedException, ExecutionException, TimeoutException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.ASYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		logged.expectEvent(
				Level.ERROR,
				CoreMatchers.sameInstance( indexingWorkException ),
				"Failing operation:",
				"Automatic indexing of Hibernate ORM entities",
				"Entities that could not be indexed correctly:",
				IndexedEntity.NAME + "#" + ENTITY_1_ID + " " + IndexedEntity.NAME + "#" + ENTITY_2_ID
		);

		// This should be ignored by the transaction (see below)
		indexingWorkFuture.completeExceptionally( indexingWorkException );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingNoBlock(
				sessionFactory, null,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, indexingWorkFuture
		);

		// The transaction thread should proceed successfully,
		// regardless of the indexing work.
		assertThatFuture( transactionThreadFuture ).isSuccessful();
	}

	@Test
	public void failure_writeSync_default() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( null );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the transaction thread
		indexingWorkFuture.completeExceptionally( indexingWorkException );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed but throw an exception,
		// because the indexing work failed.
		assertThatFuture( transactionThreadFuture ).isFailed(
				transactionSynchronizationExceptionMatcher( indexingWorkException, ENTITY_1_ID, ENTITY_2_ID )
		);
	}

	@Test
	public void failure_writeSync_explicit() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the thread
		indexingWorkFuture.completeExceptionally( indexingWorkException );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed but throw an exception,
		// because the indexing work failed.
		assertThatFuture( transactionThreadFuture ).isFailed(
				transactionSynchronizationExceptionMatcher( indexingWorkException, ENTITY_1_ID, ENTITY_2_ID )
		);
	}

	@Test
	public void failure_readSync() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.READ_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the thread
		indexingWorkFuture.completeExceptionally( indexingWorkException );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed but throw an exception,
		// because the indexing work failed.
		assertThatFuture( transactionThreadFuture ).isFailed(
				transactionSynchronizationExceptionMatcher( indexingWorkException, ENTITY_1_ID, ENTITY_2_ID )
		);
	}

	@Test
	public void failure_sync() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the thread
		indexingWorkFuture.completeExceptionally( indexingWorkException );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed but throw an exception,
		// because the indexing work failed.
		assertThatFuture( transactionThreadFuture ).isFailed(
				transactionSynchronizationExceptionMatcher( indexingWorkException, ENTITY_1_ID, ENTITY_2_ID )
		);
	}

	@Test
	public void failure_override_writeSyncToSync() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingBlock(
				sessionFactory, AutomaticIndexingSynchronizationStrategy.sync(),
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should be blocked because the indexing work is not complete
		assertThatFuture( transactionThreadFuture ).isPending();

		// Completing the work should allow the synchronization strategy to unblock the thread
		indexingWorkFuture.completeExceptionally( indexingWorkException );
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );
		// The transaction thread should proceed but throw an exception,
		// because the indexing work failed.
		assertThatFuture( transactionThreadFuture ).isFailed(
				transactionSynchronizationExceptionMatcher( indexingWorkException, ENTITY_1_ID, ENTITY_2_ID )
		);
	}

	@Test
	public void failure_override_writeSyncToCustom() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		// This should be ignored by the transaction (see below)
		indexingWorkFuture.completeExceptionally( indexingWorkException );

		AtomicReference<CompletableFuture<?>> futureThatTookTooLong = new AtomicReference<>( null );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingNoBlock(
				sessionFactory, new CustomAutomaticIndexingSynchronizationStrategy( futureThatTookTooLong ),
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should proceed but throw an exception,
		// because the indexing work failed before the timeout
		// (this is how the custom automatic indexing strategy is implemented)
		assertThatFuture( transactionThreadFuture ).isFailed(
				transactionSynchronizationExceptionMatcher( indexingWorkException, ENTITY_1_ID, ENTITY_2_ID )
		);

		// There was no timeout, so the strategy should not have set this reference
		assertThat( futureThatTookTooLong ).hasValue( null );
	}

	@Test
	public void failure_custom() throws InterruptedException, ExecutionException, TimeoutException {
		AtomicReference<CompletableFuture<?>> futureThatTookTooLong = new AtomicReference<>( null );

		SessionFactory sessionFactory = setup( new CustomAutomaticIndexingSynchronizationStrategy( futureThatTookTooLong ) );
		CompletableFuture<?> indexingWorkFuture = new CompletableFuture<>();
		Throwable indexingWorkException = new RuntimeException( "Some message" );

		// This should be ignored by the transaction (see below)
		indexingWorkFuture.completeExceptionally( indexingWorkException );

		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThreadExpectingNoBlock(
				sessionFactory, null,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, indexingWorkFuture
		);

		// The transaction thread should proceed but throw an exception,
		// because the indexing work failed before the timeout
		// (this is how the custom automatic indexing strategy is implemented)
		assertThatFuture( transactionThreadFuture ).isFailed(
				transactionSynchronizationExceptionMatcher( indexingWorkException, ENTITY_1_ID, ENTITY_2_ID )
		);

		// There was no timeout, so the strategy should not have set this reference
		assertThat( futureThatTookTooLong ).hasValue( null );
	}

	@Test
	public void invalidReference() {
		assertThatThrownBy( () -> setup( "invalidName" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid value for configuration property '"
								+ HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY
								+ "': 'invalidName'",
						"Unable to load class 'invalidName'",
						"No beans defined for type '" + AutomaticIndexingSynchronizationStrategy.class.getName()
								+ "' and name 'invalidName' in Hibernate Search's internal registry"
				);
	}

	private CompletableFuture<?> runTransactionInDifferentThreadExpectingBlock(SessionFactory sessionFactory,
			AutomaticIndexingSynchronizationStrategy customStrategy,
			DocumentCommitStrategy expectedCommitStrategy,
			DocumentRefreshStrategy expectedRefreshStrategy,
			CompletableFuture<?> indexingWorkFuture)
			throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThread(
				sessionFactory,
				customStrategy,
				expectedCommitStrategy, expectedRefreshStrategy,
				indexingWorkFuture
		);

		// Wait for some time...
		ALMOST_FOREVER_UNIT.sleep( ALMOST_FOREVER_VALUE );

		// We expect the transaction to block forever, because the work future isn't complete
		assertThatFuture( transactionThreadFuture ).isPending();

		return transactionThreadFuture;
	}

	private CompletableFuture<?> runTransactionInDifferentThreadExpectingNoBlock(SessionFactory sessionFactory,
			AutomaticIndexingSynchronizationStrategy overriddenStrategy,
			DocumentCommitStrategy expectedCommitStrategy,
			DocumentRefreshStrategy expectedRefreshStrategy,
			CompletableFuture<?> indexingWorkFuture)
			throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<?> transactionThreadFuture = runTransactionInDifferentThread(
				sessionFactory,
				overriddenStrategy,
				expectedCommitStrategy, expectedRefreshStrategy,
				indexingWorkFuture
		);

		// We expect the transaction to complete even if the indexing work isn't completed
		Awaitility.await().atMost( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT )
				.until( transactionThreadFuture::isDone );

		return transactionThreadFuture;
	}

	/*
	 * Run a transaction in a different thread so that its progress can be inspected from the current thread.
	 */
	private CompletableFuture<?> runTransactionInDifferentThread(SessionFactory sessionFactory,
			AutomaticIndexingSynchronizationStrategy overriddenStrategy,
			DocumentCommitStrategy expectedCommitStrategy,
			DocumentRefreshStrategy expectedRefreshStrategy,
			CompletableFuture<?> indexingWorkFuture)
			throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<?> justBeforeTransactionCommitFuture = new CompletableFuture<>();
		CompletableFuture<?> transactionThreadFuture = CompletableFuture.runAsync( () -> {
			with( sessionFactory ).runInTransaction( session -> {
				if ( overriddenStrategy != null ) {
					Search.session( session ).automaticIndexingSynchronizationStrategy( overriddenStrategy );
				}
				IndexedEntity entity1 = new IndexedEntity();
				entity1.setId( ENTITY_1_ID );
				entity1.setIndexedField( "initialValue" );
				IndexedEntity entity2 = new IndexedEntity();
				entity2.setId( ENTITY_2_ID );
				entity2.setIndexedField( "initialValue" );

				session.persist( entity1 );
				session.persist( entity2 );

				backendMock.expectWorks( IndexedEntity.NAME, expectedCommitStrategy, expectedRefreshStrategy )
						.createAndExecuteFollowingWorks( indexingWorkFuture )
						.add( "1", b -> b
								.field( "indexedField", entity1.getIndexedField() )
						)
						.add( "2", b -> b
								.field( "indexedField", entity2.getIndexedField() )
						);
				justBeforeTransactionCommitFuture.complete( null );
			} );
			backendMock.verifyExpectationsMet();
		} );

		// Ensure the transaction at least reached the point just before commit
		justBeforeTransactionCommitFuture.get( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT );

		return transactionThreadFuture;
	}

	private SessionFactory setup(Object strategyReference) {
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start();
		if ( strategyReference != null ) {
			setupContext.withProperty(
					HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
					strategyReference
			);
		}

		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "indexedField", String.class )
		);
		SessionFactory sessionFactory = setupContext.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		return sessionFactory;
	}

	private static Consumer<Throwable> transactionSynchronizationExceptionMatcher(Throwable indexingWorkException, int ... entityIds) {
		StringBuilder entityReferences = new StringBuilder();
		for ( int entityId : entityIds ) {
			if ( entityReferences.length() > 0 ) {
				entityReferences.append( ", " );
			}
			entityReferences.append( IndexedEntity.NAME ).append( "#" ).append( entityId );
		}
		return transactionSynchronizationExceptionMatcher( indexingWorkException, entityReferences.toString() );
	}

	private static Consumer<Throwable> transactionSynchronizationExceptionMatcher(Throwable indexingWorkException, String entityReferences) {
		return throwable -> assertThat( throwable ).isInstanceOf( HibernateException.class )
				.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"Unable to index documents for automatic indexing after transaction completion: ",
								"Indexing failure: " + indexingWorkException.getMessage(),
								"The following entities may not have been updated correctly in the index: [" + entityReferences + "]"
						)
				.extracting( Throwable::getCause ).asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"Indexing failure: " + indexingWorkException.getMessage(),
								"The following entities may not have been updated correctly in the index: [" + entityReferences + "]"
						)
				.extracting( Throwable::getCause ).isSameAs( indexingWorkException );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

	}

	private class CustomAutomaticIndexingSynchronizationStrategy implements AutomaticIndexingSynchronizationStrategy {

		private final AtomicReference<CompletableFuture<?>> futureThatTookTooLong;

		private final OperationSubmitter operationSubmitter;

		private CustomAutomaticIndexingSynchronizationStrategy(AtomicReference<CompletableFuture<?>> futureThatTookTooLong) {
			this( futureThatTookTooLong, OperationSubmitter.BLOCKING );
		}

		private CustomAutomaticIndexingSynchronizationStrategy(
				AtomicReference<CompletableFuture<?>> futureThatTookTooLong,
				OperationSubmitter operationSubmitter) {
			this.futureThatTookTooLong = futureThatTookTooLong;
			this.operationSubmitter = operationSubmitter;
		}

		@Override
		public void apply(AutomaticIndexingSynchronizationConfigurationContext context) {
			context.documentCommitStrategy( DocumentCommitStrategy.FORCE );
			context.documentRefreshStrategy( DocumentRefreshStrategy.FORCE );
			context.operationSubmitter( operationSubmitter );
			context.indexingFutureHandler( future -> {
				// try to wait for the future to complete for a small duration...
				try {
					future.get( SMALL_DURATION_VALUE, SMALL_DURATION_UNIT );
					SearchIndexingPlanExecutionReport report = future.get( SMALL_DURATION_VALUE, SMALL_DURATION_UNIT );
					report.throwable().ifPresent( t -> {
						throw log.indexingFailure( t.getMessage(), report.failingEntities(), t );
					} );
				}
				catch (TimeoutException e) {
					/*
					 * If it takes too long, push the the completable future to some background service
					 * to wait on it and report errors asynchronously if necessary.
					 * Here we just simulate this by setting an AtomicReference.
					 */
					futureThatTookTooLong.set( future );
				}
				catch (InterruptedException e) {
					fail( "Unexpected exception: " + e, e );
				}
				catch (ExecutionException e) {
					throw Throwables.toRuntimeException( e.getCause() );
				}
			} );
		}
	}
}
