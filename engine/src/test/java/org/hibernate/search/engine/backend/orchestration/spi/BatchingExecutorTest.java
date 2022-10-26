/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types
@RunWith(Parameterized.class)
public class BatchingExecutorTest {

	private static final String NAME = "executor-name";

	@Parameterized.Parameters(name = "operation submitter = {0}")
	public static Object[][] params() {
		return Arrays.stream( OperationSubmitter.values() )
				.map( value -> new Object[] { value } )
				.toArray( Object[][]::new );
	}

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private StubWorkProcessor processorMock;
	@Mock
	private FailureHandler failureHandlerMock;

	private final List<Object> mocks = new ArrayList<>();

	private final ThreadPoolProviderImpl threadPoolProvider =
			new ThreadPoolProviderImpl( BeanHolder.of( new EmbeddedThreadProvider() ) );

	// To execute code asynchronously. Just use more threads than we'll ever need, we don't care about performance.
	private final ForkJoinPool asyncExecutor = new ForkJoinPool( 12 );

	private ScheduledExecutorService executorService;
	private BatchingExecutor<StubWorkProcessor> executor;

	private final OperationSubmitter operationSubmitter;

	public BatchingExecutorTest(OperationSubmitter operationSubmitter) {
		this.operationSubmitter = operationSubmitter;
	}

	@Before
	public void setup() {
		mocks.add( processorMock );
		mocks.add( failureHandlerMock );
	}

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

		StubWork work1Mock = workMock( 1 );
		// The batch is already completed when the endBatch() method returns,
		// allowing the executor to handle the next batch immediately.
		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		when( processorMock.endBatch() ).thenReturn( (CompletableFuture) batch1Future );
		executor.submit( work1Mock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work1Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			// Since the queue is empty, works should be considered complete.
			inOrder.verify( processorMock ).complete();
		} );

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_someAdditionalWorkBeforeComplete() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWork work1Mock = workMock( 1 );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		when( processorMock.endBatch() ).thenReturn( (CompletableFuture) batch1Future );
		executor.submit( work1Mock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work1Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			// Since the batch didn't end yet, complete() should not be called right away
		} );

		StubCompletionListener completionListenerAfterSubmit1 = addPendingCompletionListener();

		// Submit other works before the first batch ends
		StubWork work2Mock = workMock( 2 );
		StubWork work3Mock = workMock( 3 );
		executor.submit( work2Mock, operationSubmitter );
		executor.submit( work3Mock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			// No calls expected yet
		} );

		StubCompletionListener completionListenerAfterSubmit2 = addPendingCompletionListener();

		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it considers works complete.
		CompletableFuture<Object> batch2Future = new CompletableFuture<>();
		when( processorMock.endBatch() ).thenReturn( (CompletableFuture) batch2Future );
		// End the first batch: the second batch should begin
		batch1Future.complete( null );
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work2Mock ).submitTo( processorMock );
			inOrder.verify( work3Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			// Since another work was submitted before the batch ended, complete() should not be called right away
		} );

		// End the second batch
		batch2Future.complete( null );
		verifyAsynchronouslyAndReset( inOrder -> {
			// Since the queue is empty, works should be considered complete.
			inOrder.verify( processorMock ).complete();
			// The relative order of these two is undefined
			verify( completionListenerAfterSubmit1 ).onComplete();
			verify( completionListenerAfterSubmit2 ).onComplete();
		} );

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_batchEndsLater_noAdditionalWork() throws InterruptedException {
		createAndStartExecutor( 2, true );

		StubWork work1Mock = workMock( 1 );
		// The batch is not yet completed when the endBatch() method returns,
		// forcing the executor to wait before it handles the next batch.
		CompletableFuture<Object> batch1Future = new CompletableFuture<>();
		when( processorMock.endBatch() ).thenReturn( (CompletableFuture) batch1Future );
		executor.submit( work1Mock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work1Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			// Since the batch didn't end yet, complete() should not be called right away
		} );

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		// End the first batch
		batch1Future.complete( null );
		verifyAsynchronouslyAndReset( inOrder -> {
			// Since the queue is empty, works should be considered complete.
			inOrder.verify( processorMock ).complete();
			inOrder.verify( completionListenerAfterSubmit ).onComplete();
		} );

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void beginBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWork work1Mock = workMock( 1 );
		executor.submit( work1Mock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			// No calls expected yet
		} );

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		ArgumentCaptor<FailureContext> failureContextCaptor = ArgumentCaptor.forClass( FailureContext.class );
		doThrow( simulatedFailure ).when( processorMock ).beginBatch();
		unblockExecutorSwitch.run();
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( failureHandlerMock ).handle( failureContextCaptor.capture() );
			// The next works should not be submitted to the processor: something is very wrong
			// Since the queue is empty, works should be considered complete.
			inOrder.verify( processorMock ).complete();
			inOrder.verify( completionListenerAfterSubmit ).onComplete();
		} );

		FailureContext failureContext = failureContextCaptor.getValue();
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

		StubWork work1Mock = workMock( 1 );
		StubWork work2Mock = workMock( 2 );
		StubWork work3Mock = workMock( 3 );
		executor.submit( work1Mock, operationSubmitter );
		executor.submit( work2Mock, operationSubmitter );
		executor.submit( work3Mock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			// No calls expected yet
		} );

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		CompletableFuture<Object> batch1Future = CompletableFuture.completedFuture( null );
		doThrow( simulatedFailure ).when( work2Mock ).submitTo( processorMock );
		when( processorMock.endBatch() ).thenReturn( (CompletableFuture) batch1Future );
		unblockExecutorSwitch.run();
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work1Mock ).submitTo( processorMock );
			inOrder.verify( work2Mock ).submitTo( processorMock );
			inOrder.verify( work2Mock ).markAsFailed( simulatedFailure );
			// The next works should still be submitted to the processor
			inOrder.verify( work3Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			// Since the queue is empty, works should be considered complete.
			inOrder.verify( processorMock ).complete();
			inOrder.verify( completionListenerAfterSubmit ).onComplete();
		} );

		// The executor should still try to process submitted works, even after a failure
		checkPostExecution();
	}

	@Test
	public void endBatchFailure() throws InterruptedException {
		createAndStartExecutor( 4, true );

		SimulatedFailure simulatedFailure = new SimulatedFailure();

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWork work1Mock = workMock( 1 );
		StubWork work2Mock = workMock( 2 );
		executor.submit( work1Mock, operationSubmitter );
		executor.submit( work2Mock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			// No calls expected yet
		} );

		StubCompletionListener completionListenerAfterSubmit = addPendingCompletionListener();

		ArgumentCaptor<FailureContext> failureContextCaptor = ArgumentCaptor.forClass( FailureContext.class );
		doThrow( simulatedFailure ).when( processorMock ).endBatch();
		unblockExecutorSwitch.run();
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work1Mock ).submitTo( processorMock );
			inOrder.verify( work2Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			inOrder.verify( failureHandlerMock ).handle( failureContextCaptor.capture() );
			// Since the queue is empty, works should be considered complete.
			inOrder.verify( processorMock ).complete();
			inOrder.verify( completionListenerAfterSubmit ).onComplete();
		} );

		FailureContext failureContext = failureContextCaptor.getValue();
		assertThat( failureContext.throwable() )
				.isSameAs( simulatedFailure );
		assertThat( failureContext.failingOperation() ).asString()
				.contains( "Executing task '" + NAME + "'" );

		// The executor should still try to process submitted works, even after a failure
		checkPostExecution();
	}

	@Test
	public void simple_newTasksBlockedException() throws InterruptedException {
		createAndStartExecutor( 2, true );

		assumeTrue(
				"This test only makes sense for nonblocking submitter",
				OperationSubmitter.REJECTED_EXECUTION_EXCEPTION.equals( operationSubmitter )
		);

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWork work1Mock = workMock( 1 );
		StubWork work2Mock = workMock( 2 );
		StubWork work3Mock = workMock( 3 );
		executor.submit( work1Mock, operationSubmitter );
		executor.submit( work2Mock, operationSubmitter );

		assertThatThrownBy( () -> executor.submit( work3Mock, operationSubmitter ) )
				.isInstanceOf( RejectedExecutionException.class );

		when( processorMock.endBatch() ).thenReturn( CompletableFuture.completedFuture( null ) );
		unblockExecutorSwitch.run();

		ArgumentCaptor<FailureContext> failureContextCaptor = ArgumentCaptor.forClass( FailureContext.class );
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work1Mock ).submitTo( processorMock );
			inOrder.verify( work2Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			inOrder.verify( processorMock ).complete();
		} );

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	@Test
	public void simple_newTasksBlockedWaitAndCompletes() throws InterruptedException {
		createAndStartExecutor( 2, true );

		assumeTrue(
				"This test only makes sense for blocking submitter",
				OperationSubmitter.BLOCKING.equals( operationSubmitter )
		);

		Runnable unblockExecutorSwitch = blockExecutor();

		StubWork work1Mock = workMock( 1 );
		StubWork work2Mock = workMock( 2 );
		StubWork work3Mock = workMock( 3 );

		executor.submit( work1Mock, operationSubmitter );
		executor.submit( work2Mock, operationSubmitter );

		CompletableFuture<Boolean> future = CompletableFuture.supplyAsync( () -> {
			try {
				executor.submit( work3Mock, operationSubmitter );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return true;
		} );

		// wait to give some time for the above future to actually block
		TimeUnit.SECONDS.sleep( 2 );

		//queue is full so future won't complete.
		assertThat( future.isDone() ).isFalse();

		when( processorMock.endBatch() ).thenReturn( CompletableFuture.completedFuture( null ) );

		unblockExecutorSwitch.run();

		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work1Mock ).submitTo( processorMock );
			inOrder.verify( work2Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( work3Mock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			inOrder.verify( processorMock ).complete();
		} );

		// Submitting other works should start the executor/processor again
		checkPostExecution();
	}

	private void verifyAsynchronouslyAndReset(Consumer<InOrder> verify) {
		await().untilAsserted( () -> {
			InOrder inOrder = inOrder( mocks.toArray() );
			verify.accept( inOrder );
		} );
		verifyNoMoreInteractions( mocks.toArray() );
		reset( mocks.toArray() );
	}

	/*
	 * Block the executor by submitting a batch that will only complete when the returned runnable is executed.
	 * Used to give us the time to carefully craft the next batch with a specific sequence of works.
	 */
	private Runnable blockExecutor()
			throws InterruptedException {
		StubWork blockingWorkMock = workMock( 0 );
		CompletableFuture<Object> blockingBatchFuture = new CompletableFuture<>();
		when( processorMock.endBatch() ).thenReturn( (CompletableFuture) blockingBatchFuture );
		executor.submit( blockingWorkMock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( blockingWorkMock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			// Since the batch didn't end yet, complete() should not be called right away
		} );
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

		executor.start( executorService );
		verifyAsynchronouslyAndReset( inOrder -> {
			// No calls expected yet
		} );

		CompletableFuture<?> completion = executor.completion();
		verifyAsynchronouslyAndReset( inOrder -> {
			// This should not trigger any call to the mocks
		} );
		// Initially, there are no works, so works are considered completed.
		assertThatFuture( completion ).isSuccessful();
	}

	private StubCompletionListener addPendingCompletionListener() {
		StubCompletionListener listener = mock( StubCompletionListener.class );
		mocks.add( listener );

		executor.completion()
				.whenComplete( (result, throwable) -> {
					assertThat( result ).isNull();
					assertThat( throwable ).isNull();
					listener.onComplete();
				} );
		// We should be pending completion, so the listener shouldn't have been called yet.
		verifyNoInteractions( listener );

		return listener;
	}

	private void checkPostExecution() throws InterruptedException {
		CompletableFuture<?> completion = executor.completion();
		// This should not trigger any call to the mocks
		verifyNoInteractions( mocks.toArray() );
		// The queue is empty, so works are considered completed.
		assertThatFuture( completion ).isSuccessful();

		// The executor still accepts and processes new works.
		StubWork workMock = workMock( 42 );
		CompletableFuture<Object> batchFuture = CompletableFuture.completedFuture( null );
		when( processorMock.endBatch() ).thenReturn( (CompletableFuture) batchFuture );
		executor.submit( workMock, operationSubmitter );
		verifyAsynchronouslyAndReset( inOrder -> {
			inOrder.verify( processorMock ).beginBatch();
			inOrder.verify( workMock ).submitTo( processorMock );
			inOrder.verify( processorMock ).endBatch();
			// Since the queue is empty, works should be considered complete.
			inOrder.verify( processorMock ).complete();
		} );
	}

	private StubWork workMock(int id) {
		StubWork mock = mock( StubWork.class, "work #" + id );
		mocks.add( mock );
		return mock;
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
