/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.util.Queue;

import org.hibernate.search.engine.backend.work.execution.impl.OperationSubmitterType;


/**
 * Interface defining how operation should be submitted to the queue.
 */
public interface OperationSubmitter {
	OperationSubmitter DEFAULT = OperationSubmitterType.BLOCKING;

	/**
	 * Defines how an element will be submitted to the queue. Currently supported implementations:
	 * <ul>
	 *     <li>{@link OperationSubmitterType#BLOCKING} - adding a new element will block the thread when the underlying
	 *     queue is a {@link java.util.concurrent.BlockingQueue} and it is at its maximum capacity, until some elements
	 *     are removed from the queue.</li>
	 *     <li>{@link OperationSubmitterType#REJECTING_EXECUTION_EXCEPTION} - adding a new element will cause a
	 *     {@link WorkQueueFullException} when the underlying queue is a {@link java.util.concurrent.BlockingQueue}
	 *     and it is at its maximum capacity</li>
	 * </ul>
	 * <p>
	 * Depending on the implementation might throw {@link WorkQueueFullException}.
	 *
	 * @param queue
	 * @param element
	 */
	<T> void submitToQueue(Queue<T> queue, T element);
}
