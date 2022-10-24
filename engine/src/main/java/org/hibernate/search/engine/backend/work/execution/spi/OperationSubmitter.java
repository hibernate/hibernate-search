/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.lang.invoke.MethodHandles;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


/**
 * Interface defining how operation should be submitted to the queue.
 */
public enum OperationSubmitter {

	/**
	 * When using this submitter, dding a new element will block the thread when the underlying
	 * queue is a {@link java.util.concurrent.BlockingQueue} and it is at its maximum capacity, until some elements
	 * are removed from the queue
	 */
	BLOCKING {
		@Override
		public <T> void submitToQueue(Queue<T> queue, T element) {
			if ( queue instanceof BlockingQueue ) {
				try {
					( (BlockingQueue<T>) queue ).put( element );
				}
				catch (InterruptedException e) {
					throw log.failedToSubmitToQueue( element, e.getMessage(), e );
				}
			}
			else {
				queue.add( element );
			}
		}
	},
	/**
	 * When using this submitter adding a new element will cause a {@link WorkQueueFullException} when the underlying
	 * queue is a {@link java.util.concurrent.BlockingQueue} and it is at its maximum capacity.
	 */
	REJECTING_EXECUTION_EXCEPTION {
		@Override
		public <T> void submitToQueue(Queue<T> queue, T element) {
			try {
				queue.add( element );
			}
			catch (IllegalStateException e) {
				//means we had a blocking queue, and it's full:
				throw log.cannotAcceptMoreWork( element, e.getMessage(), e );
			}
		}
	};

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );


	/**
	 * Defines how an element will be submitted to the queue. Currently supported implementations:
	 * <ul>
	 *     <li>{@link #BLOCKING}</li>
	 *     <li>{@link #REJECTING_EXECUTION_EXCEPTION}</li>
	 * </ul>
	 * <p>
	 * Depending on the implementation might throw {@link WorkQueueFullException}.
	 *
	 * @param queue
	 * @param element
	 */
	public abstract <T> void submitToQueue(Queue<T> queue, T element);

}
