/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

public interface PojoIndexingQueueEventProcessingPlan {

	/**
	 * Appends an event to the plan, received from a {@link PojoIndexingQueueEventSendingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param serializedId The serialized entity identifier.
	 * @param payload The payload as passed to the sending plan.
	 * @see PojoIndexingQueueEventSendingPlan#append(String, Object, String, PojoIndexingQueueEventPayload)
	 */
	void append(String entityName, String serializedId, PojoIndexingQueueEventPayload payload);

	/**
	 * Writes all pending changes to the index now,
	 * and clears the plan so that it can be re-used.
	 *
	 * @param <R> The type of entity references in the returned execution report.
	 * @param entityReferenceFactory A factory for entity references in the returned execution report.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} that will be completed with an execution report when all the works are complete.
	 */
	<R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory,
			OperationSubmitter operationSubmitter);

	/**
	 * @see #executeAndReport(EntityReferenceFactory, OperationSubmitter)
	 * @deprecated Use {@link #executeAndReport(EntityReferenceFactory, OperationSubmitter)} instead.
	 */
	@Deprecated
	default <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		return executeAndReport( entityReferenceFactory, OperationSubmitter.BLOCKING );
	}

	/**
	 * Convert the identifier to its serialized form.
	 * The identifier type must be the one used by the entity having name {@code entityName}.
	 *
	 * @param entityName The name of the entity.
	 * @param identifier The provided identifier.
	 * @param <I> The type of the identifier of the entity.
	 * @return The serialized form of the provided identifier.
	 */
	<I> String toSerializedId(String entityName, I identifier);

	/**
	 * Convert the serialized id to the original identifier.
	 *
	 * @param entityName The name of the entity.
	 * @param serializedId The serialized id.
	 * @return The original entity identifier.
	 */
	Object toIdentifier(String entityName, String serializedId);

}
