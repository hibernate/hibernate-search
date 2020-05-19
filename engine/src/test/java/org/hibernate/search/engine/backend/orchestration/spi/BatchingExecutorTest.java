/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.thread.impl.DefaultThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.After;
import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class BatchingExecutorTest extends EasyMockSupport {

	private static final String NAME = "executor-name";

	private final StubWorkProcessor processorMock = createMock( StubWorkProcessor.class );
	private final FailureHandler failureHandlerMock = createMock( FailureHandler.class );
	private final ThreadPoolProviderImpl threadPoolProvider =
			new ThreadPoolProviderImpl( BeanHolder.of( new DefaultThreadProvider() ) );

	// To execute code asynchronously. Just use more threads than we'll ever need, we don't care about performance.
	private final ForkJoinPool asyncExecutor = new ForkJoinPool( 12 );

	private ScheduledExecutorService executorService;
	private BatchingExecutor<StubWorkProcessor> executor;

	@After
	public void cleanup() {
		if ( executorService != null ) {
			executorService.shutdownNow();
		}
		threadPoolProvider.close();
		asyncExecutor.shutdownNow();
		executor.stop();
	}

	@Test
	public void simple_batchEndsImmediately() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWork work1Mock = createMock( StubWork.class );
		// The batch is already completed when the endBatch() method returns,
		// allowing the executor to handle the next batch immediately.
		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		work1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the queue is empty, works should be considered complete.
		processorMock.complete();
		replayAll();
		executor.submit( work1Mock );
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_someAdditionalWorkBeforeComplete() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWork work1Mock = createMock( StubWork.class );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		work1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the batch didn't end yet, complete() should not be called right away
		replayAll();
		executor.submit( work1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit1 = addPendingCompletionListener();

		// Submit other works before the first batch ends
		StubWork work2Mock = createMock( StubWork.class );
		StubWork work3Mock = createMock( StubWork.class );
		resetAll();
		replayAll();
		executor.submit( work2Mock );
		executor.submit( work3Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit2 = addPendingCompletionListener();

		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it considers works complete.
		CompletableFuture<Object> batch2Future = new CompletableFuture<>();
		resetAll();
		// Since another work was submitted before the batch ended, complete() should not be called right away
		processorMock.beginBatch();
		work2Mock.submitTo( processorMock );
		work3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		replayAll();
		// End the first batch: the second batch should begin
		batch1Future.complete( null );
		verifyAllAsynchronously();

		resetAll();
		// Since the queue is empty, works should be considered complete.
		processorMock.complete();
		completionListenerAfterSubmit1.onComplete();
		completionListenerAfterSubmit2.onComplete();
		replayAll();
		// End the second batch
		batch2Future.complete( null );
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_noAdditionalWork() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWork work1Mock = createMock( StubWork.class );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		work1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( work1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		resetAll();
		// Since the queue is empty, works should be considered complete.
		processorMock.complete();
		completionListenerAfterSubmit.onComplete();
		replayAll();
		// End the first batch
		batch1Future.complete( null );
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void beginBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWork work1Mock = createMock( StubWork.class );
		resetAll();
		replayAll();
		executor.submit( work1Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		processorMock.beginBatch();
		expectLastCall().andThrow( simulatedFailure );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		// The next works should not be submitted to the processor: something is very wrong
		// Since the queue is empty, works should be considered complete.
		processorMock.complete();
		completionListenerAfterSubmit.onComplete();
		replayAll();
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.throwable() )
				.isSameAs( simulatedFailure );
		assertThat( failureContext.failingOperation() ).asString()
				.contains( "Executing task '" + NAME + "'" );

		// The executor should still try to process submitted works, even after a failure
		checkPostExecution();
	}

	@Test
	public void submitFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWork work1Mock = createMock( StubWork.class );
		StubWork work2Mock = createMock( StubWork.class );
		StubWork work3Mock = createMock( StubWork.class );
		resetAll();
		replayAll();
		executor.submit( work1Mock );
		executor.submit( work2Mock );
		executor.submit( work3Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		work1Mock.submitTo( processorMock );
		work2Mock.submitTo( processorMock );
		expectLastCall().andThrow( simulatedFailure );
		work2Mock.markAsFailed( simulatedFailure );
		// The next works should still be submitted to the processor
		work3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the queue is empty, works should be considered complete.
		processorMock.complete();
		completionListenerAfterSubmit.onComplete();
		replayAll();
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		// The executor should still try to process submitted works, even after a failure
		checkPostExecution();
	}

	@Test
	public void endBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWork work1Mock = createMock( StubWork.class );
		StubWork work2Mock = createMock( StubWork.class );
		resetAll();
		replayAll();
		executor.submit( work1Mock );
		executor.submit( work2Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		processorMock.beginBatch();
		work1Mock.submitTo( processorMock );
		work2Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andThrow( simulatedFailure );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		// Since the queue is empty, works should be considered complete.
		processorMock.complete();
		completionListenerAfterSubmit.onComplete();
		replayAll();
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.throwable() )
				.isSameAs( simulatedFailure );
		assertThat( failureContext.failingOperation() ).asString()
				.contains( "Executing task '" + NAME + "'" );

		// The executor should still try to process submitted works, even after a failure
		checkPostExecution();
	}

	private void verifyAllAsynchronously() {
		await().untilAsserted( () -> {
			// Synchronize on the processor, like in the batching executor,
			// so that we don't trigger ConcurrentModificationException in EasyMock.
			synchronized ( processorMock ) {
				verifyAll();
			}
		} );
	}

	/*
	 * Block the executor by submitting a batch that will only complete when the returned runnable is executed.
	 * Used to give us the time to carefully craft the next batch with a specific sequence of works.
	 */
	private Runnable blockExecutor()
			throws InterruptedException {
		StubWork blockingWorkMock = createMock( StubWork.class );
		CompletableFuture<Object> blockingBatchFuture = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		blockingWorkMock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) blockingBatchFuture );
		replayAll();
		executor.submit( blockingWorkMock );
		verifyAllAsynchronously();
		// Return a runnable that will unblock the executor
		return () -> blockingBatchFuture.complete( null );
	}

	private void createAndStartExecutor(int maxTasksPerBatch, boolean fair) {
		this.executor = new BatchingExecutor<>(
				NAME, processorMock, maxTasksPerBatch, fair, failureHandlerMock
		);

		// Having multiple threads should not matter:
		// the batching executor takes care of executing in only one thread at a time.
		this.executorService = threadPoolProvider.newScheduledExecutor( 4, "BatchingExecutorTest" );

		resetAll();
		replayAll();
		executor.start( executorService );
		verifyAll();

		// Initially, there are no works, so works are considered completed.
		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		CompletableFuture<?> completion = executor.completion();
		verifyAll();
		FutureAssert.assertThat( completion ).isSuccessful();
	}

	private StubCompletionListener addPendingCompletionListener() {
		StubCompletionListener listener = createStrictMock( StubCompletionListener.class );

		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		CompletableFuture<?> completion = executor.completion()
				.whenComplete( (result, throwable) -> {
					assertThat( result ).isNull();
					assertThat( throwable ).isNull();
					listener.onComplete();
				} );
		verifyAll();

		return listener;
	}

	private void checkPostExecution() throws InterruptedException {
		// The queue is empty, so works are considered completed.
		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		CompletableFuture<?> completion = executor.completion();
		verifyAll();
		FutureAssert.assertThat( completion ).isSuccessful();

		// The executor still accepts and processes new works.
		StubWork workMock = createMock( StubWork.class );
		CompletableFuture<Object> batchFuture = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workMock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batchFuture );
		// Since the queue is empty, works should be considered complete.
		processorMock.complete();
		replayAll();
		executor.submit( workMock );
		verifyAllAsynchronously();
	}

	private interface StubWork extends BatchedWork<StubWorkProcessor> {
	}

	private interface StubWorkProcessor extends BatchedWorkProcessor {
	}

	private interface StubCompletionListener {
		void onComplete();
	}

	private static class SimulatedFailure extends RuntimeException {
	}
}
