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
	/*
	 * Pick a value that is:
	 * - large enough that test code executes faster than this number of milliseconds, even on slow machines
	 * - small enough that Awaitility.await does not give up before this number of milliseconds
	 * - small enough that tests do not take forever to execute
	 */
	private static final long NON_ZERO_DELAY = 2000L;

	private final StubWorkProcessor processorMock = createMock( StubWorkProcessor.class );
	private final FailureHandler failureHandlerMock = createMock( FailureHandler.class );
	private final ThreadPoolProviderImpl threadPoolProvider =
			new ThreadPoolProviderImpl( BeanHolder.of( new DefaultThreadProvider() ) );

	// To execute code asynchronously. Just use more threads than we'll ever need, we don't care about performance.
	private final ForkJoinPool asyncExecutor = new ForkJoinPool( 12 );

	private BatchingExecutor<StubWorkSet, StubWorkProcessor> executor;

	@After
	public void cleanup() {
		threadPoolProvider.close();
		asyncExecutor.shutdownNow();
		executor.stop();
	}

	@Test
	public void simple_batchEndsImmediately_completeReturnsZero() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is already completed when the endBatch() method returns,
		// allowing the executor to handle the next batch immediately.
		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() )
				// The processor returns 0 to indicate that all outstanding operations have actually completed.
				.andReturn( 0L );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsImmediately_completeReturnsPositive_noAdditionalWork() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is already completed when the endBatch() method returns,
		// allowing the executor to handle the next batch immediately.
		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() )
				// The processor returns a positive number to indicate that some outstanding operations remain
				// and should be executed after that many milliseconds
				.andReturn( NON_ZERO_DELAY );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		// Some time later, since no work was submitted, the executor should call completeOrDelay() again.
		resetAll();
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit.onComplete();
		replayAll();
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsImmediately_completeReturnsPositive_someAdditionalWork() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is already completed when the endBatch() method returns,
		// allowing the executor to handle the next batch immediately.
		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() )
				// The processor returns a positive number to indicate that some outstanding operations remain
				// and should be executed after that many milliseconds
				.andReturn( NON_ZERO_DELAY );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch2Future = CompletableFuture.completedFuture( null );
		resetAll();
		// Since another workset was submitted before the completion delay expired, completeOrDelay() should not be called right away
		processorMock.beginBatch();
		workSet2Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit.onComplete();
		replayAll();
		executor.submit( workSet2Mock );
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_someAdditionalWorkBeforeCompleteOrDelay() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the batch didn't end yet, completeOrDelay() should not be called right away
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit1 = addPendingCompletionListener();

		// Submit other worksets before the first batch ends
		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet3Mock = createMock( StubWorkSet.class );
		resetAll();
		replayAll();
		executor.submit( workSet2Mock );
		executor.submit( workSet3Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit2 = addPendingCompletionListener();

		CompletableFuture<Object> batch2Future = CompletableFuture.completedFuture( null );
		resetAll();
		// Since another workset was submitted before the batch ended, completeOrDelay() should not be called right away
		processorMock.beginBatch();
		workSet2Mock.submitTo( processorMock );
		workSet3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit1.onComplete();
		completionListenerAfterSubmit2.onComplete();
		replayAll();
		// End the first batch: the second batch should begin
		batch1Future.complete( null );
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_completeReturns0_noAdditionalWork() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		resetAll();
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() )
				// The processor returns a positive number to indicate that some outstanding operations remain
				// and should be executed after that many milliseconds
				.andReturn( NON_ZERO_DELAY );
		replayAll();
		// End the first batch
		batch1Future.complete( null );
		verifyAllAsynchronously();

		// Some time later, since no work was submitted, the executor should call completeOrDelay() again.
		resetAll();
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit.onComplete();
		replayAll();
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_completeReturnsPositive_noAdditionalWork() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		resetAll();
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() )
				// The processor returns a positive number to indicate that some outstanding operations remain
				// and should be executed after that many milliseconds
				.andReturn( NON_ZERO_DELAY );
		replayAll();
		// End the first batch
		batch1Future.complete( null );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterCompleteDelay = addPendingCompletionListener();

		// Some time later, since no work was submitted, the executor should call completeOrDelay() again.
		resetAll();
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit.onComplete();
		completionListenerAfterCompleteDelay.onComplete();
		replayAll();
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_completeReturnsPositive_someAdditionalWork() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		replayAll();
		executor.submit( workSet1Mock );
		verifyAllAsynchronously();

		StubCompletionListener completionListenerAfterSubmit1 = addPendingCompletionListener();

		resetAll();
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() )
				// The processor returns a positive number to indicate that some outstanding operations remain
				// and should be executed after that many milliseconds
				.andReturn( NON_ZERO_DELAY );
		replayAll();
		// End the first batch
		batch1Future.complete( null );
		verifyAllAsynchronously();

		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet3Mock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batch2Future = CompletableFuture.completedFuture( null );
		resetAll();
		// Since another workset was submitted before the completion delay expired, completeOrDelay() should not be called right away
		processorMock.beginBatch();
		workSet2Mock.submitTo( processorMock );
		workSet3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch2Future );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit1.onComplete();
		replayAll();
		executor.submit( workSet2Mock );
		executor.submit( workSet3Mock );
		verifyAllAsynchronously();

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void beginBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		resetAll();
		replayAll();
		executor.submit( workSet1Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		processorMock.beginBatch();
		expectLastCall().andThrow( simulatedFailure );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		// The next worksets should not be submitted to the processor: something is very wrong
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit.onComplete();
		replayAll();
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isSameAs( simulatedFailure );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Work processing in executor '" + NAME + "'" );

		// The executor should still try to process submitted worksets, even after a failure
		checkPostExecution();
	}

	@Test
	public void submitFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet3Mock = createMock( StubWorkSet.class );
		resetAll();
		replayAll();
		executor.submit( workSet1Mock );
		executor.submit( workSet2Mock );
		executor.submit( workSet3Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		workSet2Mock.submitTo( processorMock );
		expectLastCall().andThrow( simulatedFailure );
		workSet2Mock.markAsFailed( simulatedFailure );
		// The next worksets should still be submitted to the processor
		workSet3Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batch1Future );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit.onComplete();
		replayAll();
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		// The executor should still try to process submitted worksets, even after a failure
		checkPostExecution();
	}

	@Test
	public void endBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWorkSet workSet1Mock = createMock( StubWorkSet.class );
		StubWorkSet workSet2Mock = createMock( StubWorkSet.class );
		resetAll();
		replayAll();
		executor.submit( workSet1Mock );
		executor.submit( workSet2Mock );
		verifyAll();

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		Capture<FailureContext> failureContextCapture = Capture.newInstance();
		resetAll();
		processorMock.beginBatch();
		workSet1Mock.submitTo( processorMock );
		workSet2Mock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andThrow( simulatedFailure );
		failureHandlerMock.handle( capture( failureContextCapture ) );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		// Since the processor acknowledged completion by returning 0, works should be considered complete.
		completionListenerAfterSubmit.onComplete();
		replayAll();
		unblockExecutorSwitch.run();
		verifyAllAsynchronously();

		FailureContext failureContext = failureContextCapture.getValue();
		assertThat( failureContext.getThrowable() )
				.isSameAs( simulatedFailure );
		assertThat( failureContext.getFailingOperation() ).asString()
				.contains( "Work processing in executor '" + NAME + "'" );

		// The executor should still try to process submitted worksets, even after a failure
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
	 * Used to give us the time to carefully craft the next batch with a specific sequence of worksets.
	 */
	private Runnable blockExecutor()
			throws InterruptedException {
		StubWorkSet blockingWorkSetMock = createMock( StubWorkSet.class );
		CompletableFuture<Object> blockingBatchFuture = new CompletableFuture<>();
		resetAll();
		processorMock.beginBatch();
		blockingWorkSetMock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) blockingBatchFuture );
		replayAll();
		executor.submit( blockingWorkSetMock );
		verifyAllAsynchronously();
		// Return a runnable that will unblock the executor
		return () -> blockingBatchFuture.complete( null );
	}

	private void createAndStartExecutor(int maxTasksPerBatch, boolean fair) {
		this.executor = new BatchingExecutor<>(
				NAME, processorMock, maxTasksPerBatch, fair, failureHandlerMock
		);

		resetAll();
		replayAll();
		executor.start( threadPoolProvider );
		verifyAll();

		// Initially, there are no worksets, so works are considered completed.
		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		CompletableFuture<?> completion = executor.getCompletion();
		verifyAll();
		FutureAssert.assertThat( completion ).isSuccessful();
	}

	private StubCompletionListener addPendingCompletionListener() {
		StubCompletionListener listener = createStrictMock( StubCompletionListener.class );

		resetAll();
		// This should not trigger any call to the mocks
		replayAll();
		CompletableFuture<?> completion = executor.getCompletion()
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
		CompletableFuture<?> completion = executor.getCompletion();
		verifyAll();
		FutureAssert.assertThat( completion ).isSuccessful();

		// The executor still accepts and processes new worksets.
		StubWorkSet workSetMock = createMock( StubWorkSet.class );
		CompletableFuture<Object> batchFuture = CompletableFuture.completedFuture( null );
		resetAll();
		processorMock.beginBatch();
		workSetMock.submitTo( processorMock );
		expect( processorMock.endBatch() ).andReturn( (CompletableFuture) batchFuture );
		// Since the queue is empty, expect a call to processor.completeOrDelay().
		expect( processorMock.completeOrDelay() ).andReturn( 0L );
		replayAll();
		executor.submit( workSetMock );
		verifyAllAsynchronously();
	}

	private interface StubWorkSet extends BatchingExecutor.WorkSet<StubWorkProcessor> {
	}

	private interface StubWorkProcessor extends BatchingExecutor.WorkProcessor {
	}

	private interface StubCompletionListener {
		void onComplete();
	}

	private static class SimulatedFailure extends RuntimeException {
	}
}
