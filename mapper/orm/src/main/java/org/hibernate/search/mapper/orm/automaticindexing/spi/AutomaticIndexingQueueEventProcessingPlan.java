/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;

public interface AutomaticIndexingQueueEventProcessingPlan {

	/**
	 * Appends an "add" event to the plan, received from a {@link AutomaticIndexingQueueEventSendingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param serializedId The serialized entity identifier.
	 * @param payload The payload as passed to the sending plan.
	 * @see AutomaticIndexingQueueEventSendingPlan#add(String, Object, String, PojoIndexingQueueEventPayload)
	 */
	void add(String entityName, String serializedId, PojoIndexingQueueEventPayload payload);

	/**
	 * Appends an "add-or-update" event to the plan, received from a {@link AutomaticIndexingQueueEventSendingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param serializedId The serialized entity identifier.
	 * @param payload The payload as passed to the sending plan.
	 * @see AutomaticIndexingQueueEventSendingPlan#addOrUpdate(String, Object, String, PojoIndexingQueueEventPayload)
	 */
	void addOrUpdate(String entityName, String serializedId, PojoIndexingQueueEventPayload payload);

	/**
	 * Appends a "delete" event to the plan, received from a {@link AutomaticIndexingQueueEventSendingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param serializedId The serialized entity identifier.
	 * @param payload The payload as passed to the sending plan.
	 * @see AutomaticIndexingQueueEventSendingPlan#delete(String, Object, String, PojoIndexingQueueEventPayload)
	 */
	void delete(String entityName, String serializedId, PojoIndexingQueueEventPayload payload);

	/**
	 * Writes all pending changes to the index now,
	 * and clears the plan so that it can be re-used.
	 *
	 * @return A {@link CompletableFuture} that will be completed with an execution report when all the works are complete.
	 */
	CompletableFuture<MultiEntityOperationExecutionReport<EntityReference>> executeAndReport();

	/**
	 * Convert the identifier to its serialized form.
	 * The identifier type must be the one used by the entity having name {@code entityName}.
	 *
	 * @param entityName The name of the entity.
	 * @param identifier The provided identifier.
	 * @return The serialized for of the provided identifier.
	 */
	String toSerializedId(String entityName, Object identifier);

	/**
	 * Convert the serialized id to the original identifier.
	 *
	 * @param entityName The name of the entity.
	 * @param serializedId The serialized id.
	 * @return The original entity identifier.
	 */
	Object toIdentifier(String entityName, String serializedId);

}
