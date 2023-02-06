/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.util.common.annotation.Incubating;


/**
 * Interface defining how operation should be submitted to the queue or executor.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 * <p>
 * Currently supported implementations:
 * <ul>
 *     <li>{@link #blocking()}</li>
 *     <li>{@link #rejecting()}</li>
 *     <li>{@link #offloading(Consumer)}</li>
 * </ul>
 */
@Incubating
public abstract class OperationSubmitter {

	private static final OperationSubmitter BLOCKING = new BlockingOperationSubmitter();
	private static final OperationSubmitter REJECTED_EXECUTION_EXCEPTION = new RejectedExecutionExceptionOperationSubmitter();

	private OperationSubmitter() {
	}

	/**
	 * Defines how an element will be submitted to the queue.
	 * Depending on the implementation might throw {@link RejectedExecutionException} or offload the submit operation to a provided executor.
	 */
	public abstract <T> void submitToQueue(BlockingQueue<? super T> queue, T element,
			Consumer<? super T> blockingRetryProducer) throws InterruptedException;

	/**
	 * Defines how an element will be submitted to the executor.
	 * Depending on the implementation might throw {@link RejectedExecutionException} or offload the submit operation to an offload executor.
	 */
	public abstract <T extends Runnable> void submitToExecutor(SimpleScheduledExecutor executor, T element,
			Consumer<? super T> blockingRetryProducer) throws InterruptedException;

	/**
	 * When using this submitter, dding a new element will block the thread when the underlying
	 * queue is a {@link java.util.concurrent.BlockingQueue} and it is at its maximum capacity, until some elements
	 * are removed from the queue
	 */
	public static OperationSubmitter blocking() {
		return BLOCKING;
	}

	/**
	 * When using this submitter adding a new element will cause a {@link RejectedExecutionException} when the underlying
	 * queue is a {@link java.util.concurrent.BlockingQueue} and it is at its maximum capacity.
	 */
	public static OperationSubmitter rejecting() {
		return REJECTED_EXECUTION_EXCEPTION;
	}

	/**
	 * Creates an operation submitter that would attempt to submit work to a queue, but in case the queue is full it
	 * would offload the submitting of the operation to provided executor.
	 * This would never block the current thread, but the one to which the work is offloaded.
	 *
	 * @param executor executor to offload submit operation to if the queue is full
	 */
	public static OperationSubmitter offloading(Consumer<Runnable> executor) {
		return new OffloadingExecutorOperationSubmitter( executor );
	}

	private static final class BlockingOperationSubmitter extends OperationSubmitter {
		@Override
		public <T> void submitToQueue(BlockingQueue<? super T> queue, T element,
				Consumer<? super T> blockingRetryProducer)
				throws InterruptedException {
			queue.put( element );
		}

		@Override
		public <T extends Runnable> void submitToExecutor(SimpleScheduledExecutor executor, T element,
				Consumer<? super T> blockingRetryProducer) {
			executor.submit( element );
		}
	}

	private static final class RejectedExecutionExceptionOperationSubmitter extends OperationSubmitter {
		@Override
		public <T> void submitToQueue(BlockingQueue<? super T> queue, T element,
				Consumer<? super T> blockingRetryProducer) {
			if ( !queue.offer( element ) ) {
				throw new RejectedExecutionException();
			}
		}

		@Override
		public <T extends Runnable> void submitToExecutor(SimpleScheduledExecutor executor, T element,
				Consumer<? super T> blockingRetryProducer) {
			executor.offer( element );
		}
	}

	private static final class OffloadingExecutorOperationSubmitter extends OperationSubmitter {
		private final Consumer<Runnable> executor;

		private OffloadingExecutorOperationSubmitter(Consumer<Runnable> executor) {
			this.executor = executor;
		}

		@Override
		public <T> void submitToQueue(BlockingQueue<? super T> queue, T element, Consumer<? super T> blockingRetryProducer) {
			if ( !queue.offer( element ) ) {
				executor.accept( () -> blockingRetryProducer.accept( element ) );
			}
		}

		@Override
		public <T extends Runnable> void submitToExecutor(SimpleScheduledExecutor executor, T element,
				Consumer<? super T> blockingRetryProducer) {
			try {
				executor.offer( element );
			}
			catch (RejectedExecutionException e) {
				this.executor.accept( () -> blockingRetryProducer.accept( element ) );
			}
		}
	}

}
