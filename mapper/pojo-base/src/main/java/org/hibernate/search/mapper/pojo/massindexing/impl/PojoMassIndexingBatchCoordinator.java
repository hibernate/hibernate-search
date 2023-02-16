/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContextMessages;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;

/**
 * Makes sure that several different BatchIndexingWorkspace(s)
 * can be started concurrently, sharing the same batch-backend
 * and IndexWriters.
 *
 * @author Sanne Grinovero
 */
public class PojoMassIndexingBatchCoordinator extends PojoMassIndexingFailureHandledRunnable {

	private final PojoMassIndexingMappingContext mappingContext;
	private final PojoMassIndexerAgentStartContextImpl agentStartContext;
	private final List<PojoMassIndexingIndexedTypeGroup<?>> typeGroupsToIndex;

	private final PojoScopeSchemaManager scopeSchemaManager;
	private final Set<String> tenantIds;
	private final PojoScopeDelegate<?, ?, ?> pojoScopeDelegate;
	private final int typesToIndexInParallel;
	private final int documentBuilderThreads;
	private final boolean mergeSegmentsOnFinish;
	private final boolean dropAndCreateSchemaOnStart;
	private final boolean purgeAtStart;
	private final boolean mergeSegmentsAfterPurge;

	private final List<CompletableFuture<?>> indexingFutures = new ArrayList<>();

	private final Collection<SessionContext> sessionContexts = new ArrayList<>();
	private PojoScopeWorkspace allTenantsWorkspace;

	public PojoMassIndexingBatchCoordinator(PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingNotifier notifier,
			List<PojoMassIndexingIndexedTypeGroup<?>> typeGroupsToIndex,
			PojoScopeSchemaManager scopeSchemaManager,
			Set<String> tenantIds,
			PojoScopeDelegate<?, ?, ?> pojoScopeDelegate,
			MassIndexingEnvironment environment,
			int typesToIndexInParallel, int documentBuilderThreads, boolean mergeSegmentsOnFinish,
			boolean dropAndCreateSchemaOnStart, boolean purgeAtStart, boolean mergeSegmentsAfterPurge) {
		super( notifier, environment );
		this.mappingContext = mappingContext;
		this.typeGroupsToIndex = typeGroupsToIndex;

		this.scopeSchemaManager = scopeSchemaManager;
		this.tenantIds = tenantIds;
		this.pojoScopeDelegate = pojoScopeDelegate;
		this.typesToIndexInParallel = typesToIndexInParallel;
		this.documentBuilderThreads = documentBuilderThreads;
		this.mergeSegmentsOnFinish = mergeSegmentsOnFinish;
		this.dropAndCreateSchemaOnStart = dropAndCreateSchemaOnStart;
		this.purgeAtStart = purgeAtStart;
		this.mergeSegmentsAfterPurge = mergeSegmentsAfterPurge;

		this.agentStartContext = new PojoMassIndexerAgentStartContextImpl(
				mappingContext.threadPoolProvider(),
				mappingContext.failureHandler()
		);
	}

	@Override
	public void runWithFailureHandler() throws InterruptedException {
		if ( !indexingFutures.isEmpty() ) {
			throw new AssertionFailure( "BatchCoordinator instance not expected to be reused" );
		}

		beforeBatch(); // purgeAll and mergeSegments if enabled
		try {
			doBatchWork();
			afterBatch(); // mergeSegments if enabled and flush
		}
		catch (MassIndexingOperationHandledFailureException e) {
			// Something is wrong, but it's already been reported.
			// Just stop everything and rely on the notifier to throw the appropriate exception.
			cleanUpOnFailure();
		}
	}

	/**
	 * Operations to do before the multiple-threads start indexing
	 */
	private void beforeBatch() throws InterruptedException {
		allTenantsWorkspace = pojoScopeDelegate.workspace( tenantIds );
		// Prepare the contexts first. These will be used for all batch related work:
		for ( String tenantId : tenantIds ) {
			sessionContexts.add( createSessionContext( tenantId ) );
		}
		// means we are in a single tenant:
		if ( sessionContexts.isEmpty() ) {
			sessionContexts.add( createSessionContext( null ) );
		}

		// Start the agent and wait until concurrent indexing actually gets suspended
		applyToAllContexts( c -> c.agent().start( agentStartContext ) );


		if ( dropAndCreateSchemaOnStart ) {
			RootFailureCollector failureCollector = new RootFailureCollector(
					PojoEventContextMessages.INSTANCE.schemaManagement()
			);
			Futures.unwrappedExceptionGet( scopeSchemaManager.dropAndCreate( failureCollector, OperationSubmitter.blocking() ) );
			failureCollector.checkNoFailure();
		}

		if ( purgeAtStart ) {
			Futures.unwrappedExceptionGet(
					allTenantsWorkspace.purge( Collections.emptySet(), OperationSubmitter.blocking() )
			);

			if ( mergeSegmentsAfterPurge ) {
				Futures.unwrappedExceptionGet(
						allTenantsWorkspace.mergeSegments( OperationSubmitter.blocking() )
				);
			}
		}
	}

	private SessionContext createSessionContext(String tenantId) {
		return new SessionContext(
				// Create an agent to suspend concurrent indexing
				mappingContext.createMassIndexerAgent(
						new PojoMassIndexerAgentCreateContextImpl( mappingContext, tenantId )
				),
				pojoScopeDelegate.workspace( tenantId ),
				tenantId
		);
	}

	/**
	 * Will spawn a thread for each type in rootEntities, they will all re-join
	 * on endAllSignal when finished.
	 *
	 * @throws InterruptedException if interrupted while waiting for endAllSignal.
	 */
	private void doBatchWork() throws InterruptedException {
		ExecutorService executor = mappingContext.threadPoolProvider()
				.newFixedThreadPool( typesToIndexInParallel,
						PojoMassIndexingBatchIndexingWorkspace.THREAD_NAME_PREFIX + "Workspace" );

		for ( PojoMassIndexingIndexedTypeGroup<?> typeGroup : typeGroupsToIndex ) {
			for ( SessionContext context : sessionContexts ) {
				indexingFutures.add( Futures.runAsync( createBatchIndexingWorkspace( typeGroup, context ), executor ) );
			}
		}
		executor.shutdown();

		// Wait for the executor to finish
		Futures.unwrappedExceptionGet(
				CompletableFuture.allOf( indexingFutures.toArray( new CompletableFuture[0] ) )
		);
	}

	private <E> PojoMassIndexingBatchIndexingWorkspace<E, ?> createBatchIndexingWorkspace(
			PojoMassIndexingIndexedTypeGroup<E> typeGroup, SessionContext context) {
		return new PojoMassIndexingBatchIndexingWorkspace<>(
				mappingContext, getNotifier(), getMassIndexingEnvironment(), typeGroup,
				typeGroup.loadingStrategy(),
				documentBuilderThreads,
				context.tenantIdentifier()
		);
	}

	/**
	 * Operations to do after all subthreads finished their work on index
	 */
	private void afterBatch() throws InterruptedException {
		if ( mergeSegmentsOnFinish ) {
			Futures.unwrappedExceptionGet( allTenantsWorkspace.mergeSegments( OperationSubmitter.blocking() ) );
		}
		flushAndRefresh();
		applyToAllContexts(
				context -> context.agent().preStop()
		);
		// NOTE: HSEARCH-4773 this loop was added here on purpose, as composing this stop() operation to the above future
		// was causing an issue when running against Oracle DB. Doing it like this seems to allow a graceful stopping of the agents.
		for ( SessionContext context : sessionContexts ) {
			context.agent().stop();
		}
		sessionContexts.clear();
		agentStartContext.clear();
	}

	private void flushAndRefresh() throws InterruptedException {
		Futures.unwrappedExceptionGet( allTenantsWorkspace.flush( OperationSubmitter.blocking() ) );
		Futures.unwrappedExceptionGet( allTenantsWorkspace.refresh( OperationSubmitter.blocking() ) );
	}

	@Override
	protected void cleanUpOnInterruption() throws InterruptedException {
		try ( Closer<InterruptedException> closer = new Closer<>() ) {
			closer.pushAll( this::cancelPendingTask, indexingFutures );
			// Indexing performed before the exception must still be committed,
			// in order to leave the index in a consistent state
			closer.push( PojoMassIndexingBatchCoordinator::flushAndRefresh, this );
			closer.pushAll( PojoMassIndexerAgent::stop, sessionContexts, SessionContext::agent );
			closer.push( PojoMassIndexerAgentStartContextImpl::clear, agentStartContext );
			sessionContexts.clear();
		}
	}

	@Override
	protected void cleanUpOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( this::cancelPendingTask, indexingFutures );
			closer.pushAll( PojoMassIndexerAgent::stop, sessionContexts, SessionContext::agent );
			closer.push( PojoMassIndexerAgentStartContextImpl::clear, agentStartContext );
			sessionContexts.clear();
		}
	}

	private void cancelPendingTask(Future<?> task) {
		if ( !task.isDone() ) {
			task.cancel( true );
		}
	}

	@Override
	protected void notifySuccess() {
		getNotifier().reportIndexingCompleted();
	}

	@Override
	protected void notifyInterrupted(InterruptedException exception) {
		getNotifier().reportInterrupted( exception );
		getNotifier().reportIndexingCompleted();
	}

	@Override
	protected void notifyFailure(RuntimeException exception) {
		super.notifyFailure( exception );
		// TODO HSEARCH-3729 Call a different method when indexing failed?
		getNotifier().reportIndexingCompleted();
	}

	public static class SessionContext {
		private final PojoMassIndexerAgent agent;
		private final PojoScopeWorkspace scopeWorkspace;
		private final String tenantIdentifier;

		public SessionContext(PojoMassIndexerAgent agent, PojoScopeWorkspace scopeWorkspace, String tenantIdentifier) {
			this.agent = agent;
			this.scopeWorkspace = scopeWorkspace;
			this.tenantIdentifier = tenantIdentifier;
		}

		public PojoMassIndexerAgent agent() {
			return agent;
		}

		public PojoScopeWorkspace scopeWorkspace() {
			return scopeWorkspace;
		}

		public String tenantIdentifier() {
			return tenantIdentifier;
		}
	}

	private void applyToAllContexts(Function<SessionContext, CompletableFuture<?>> operation) throws InterruptedException {
		Futures.unwrappedExceptionGet(
				CompletableFuture.allOf(
						sessionContexts.stream()
								.map( operation::apply )
								.toArray( CompletableFuture[]::new )
				)
		);
	}

}
