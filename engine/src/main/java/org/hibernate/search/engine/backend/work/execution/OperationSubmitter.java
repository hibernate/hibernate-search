/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;

import org.hibernate.search.util.common.annotation.Incubating;


/**
 * Interface defining how operation should be submitted to the queue.
 */
@Incubating
public enum OperationSubmitter {

	/**
	 * When using this submitter, dding a new element will block the thread when the underlying
	 * queue is a {@link java.util.concurrent.BlockingQueue} and it is at its maximum capacity, until some elements
	 * are removed from the queue
	 */
	BLOCKING {
		@Override
		public <T> void submitToQueue(BlockingQueue<T> queue, T element) throws InterruptedException {
			queue.put( element );
		}
	},
	/**
	 * When using this submitter adding a new element will cause a {@link RejectedExecutionException} when the underlying
	 * queue is a {@link java.util.concurrent.BlockingQueue} and it is at its maximum capacity.
	 */
	REJECTED_EXECUTION_EXCEPTION {
		@Override
		public <T> void submitToQueue(BlockingQueue<T> queue, T element) {
			if ( !queue.offer( element ) ) {
				throw new RejectedExecutionException();
			}
		}
	};

	/**
	 * Defines how an element will be submitted to the queue. Currently supported implementations:
	 * <ul>
	 *     <li>{@link #BLOCKING}</li>
	 *     <li>{@link #REJECTED_EXECUTION_EXCEPTION}</li>
	 * </ul>
	 * <p>
	 * Depending on the implementation might throw {@link RejectedExecutionException}.
	 *
	 * @param queue
	 * @param element
	 */
	public abstract <T> void submitToQueue(BlockingQueue<T> queue, T element) throws InterruptedException;

}
