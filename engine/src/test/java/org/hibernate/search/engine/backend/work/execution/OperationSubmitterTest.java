/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class OperationSubmitterTest {

	private BlockingQueue<Integer> queue;

	@Before
	public void setUp() throws Exception {
		this.queue = new ArrayBlockingQueue<>( 2 );

		OperationSubmitter.BLOCKING.submitToQueue( queue, 1 );
		OperationSubmitter.BLOCKING.submitToQueue( queue, 2 );
	}

	@Test
	public void blockingOperationSubmitterBlocksTheOperation() throws InterruptedException {
		CompletableFuture<Boolean> future = CompletableFuture.supplyAsync( () -> {
			try {
				OperationSubmitter.BLOCKING.submitToQueue( queue, 3 );
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

		//make some room in the queue:
		queue.take();
		await().untilAsserted( () -> assertThat( future.isDone() ).isTrue() );
	}


	@Test
	public void nonBlockingOperationSubmitterThrowsException() {
		Integer element = 3;
		assertThatThrownBy( () -> OperationSubmitter.REJECTED_EXECUTION_EXCEPTION.submitToQueue( queue, element ) )
				.isInstanceOf( RejectedExecutionException.class );
	}
}
