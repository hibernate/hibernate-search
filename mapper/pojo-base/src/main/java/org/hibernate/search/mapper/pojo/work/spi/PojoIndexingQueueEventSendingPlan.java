/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

/**
 * A set of indexing events to be sent to an external queue.
 * <p>
 * The external queue will be consumed by a background process which will then
 * perform the indexing operations asynchronously.
 */
public interface PojoIndexingQueueEventSendingPlan {

	/**
	 * Appends an event to the plan, to be sent {@link #sendAndReport(OperationSubmitter) later}
	 * and ultimately added to a {@link PojoIndexingQueueEventProcessingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param identifier The non-serialized entity identifier, to report sending errors.
	 * @param serializedId The serialized entity identifier.
	 * @param payload A payload to be forwarded as-is to the processing plan.
	 * @see PojoIndexingQueueEventProcessingPlan#append(String, String, PojoIndexingQueueEventPayload)
	 */
	void append(String entityName, Object identifier, String serializedId, PojoIndexingQueueEventPayload payload);

	/**
	 * Discards all events that were added to this plan, without sending them.
	 */
	void discard();

	/**
	 * Sends the events to the queue.
	 * <p>
	 * When the returned future completes, events are guaranteed to be stored in secure storage
	 * in such a way that they will eventually be processed.
	 *
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 *
	 * @return A {@link CompletableFuture} that will hold an execution report when all the events are sent.
	 * If sending an event failed, the future will be completed normally,
	 * but the report will contain an exception.
	 */
	CompletableFuture<MultiEntityOperationExecutionReport> sendAndReport(OperationSubmitter operationSubmitter);

}
