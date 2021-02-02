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
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public interface PojoIndexingQueueEventProcessingPlan {

	/**
	 * Appends an "add" event to the plan, received from a {@link PojoIndexingQueueEventSendingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param serializedId The serialized entity identifier.
	 * @param routes The document routes.
	 * @see PojoIndexingQueueEventSendingPlan#add(String, Object, String, DocumentRoutesDescriptor)
	 */
	void add(String entityName, String serializedId, DocumentRoutesDescriptor routes);

	/**
	 * Appends an "add-or-update" event to the plan, received from a {@link PojoIndexingQueueEventSendingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param serializedId The serialized entity identifier.
	 * @param routes The document routes.
	 * @see PojoIndexingQueueEventSendingPlan#addOrUpdate(String, Object, String, DocumentRoutesDescriptor)
	 */
	void addOrUpdate(String entityName, String serializedId, DocumentRoutesDescriptor routes);

	/**
	 * Appends a "delete" event to the plan, received from a {@link PojoIndexingQueueEventSendingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param serializedId The serialized entity identifier.
	 * @param routes The document routes.
	 * @see PojoIndexingQueueEventSendingPlan#delete(String, Object, String, DocumentRoutesDescriptor)
	 */
	void delete(String entityName, String serializedId, DocumentRoutesDescriptor routes);

	/**
	 * Writes all pending changes to the index now,
	 * and clears the plan so that it can be re-used.
	 *
	 * @param <R> The type of entity references in the returned execution report.
	 * @param entityReferenceFactory A factory for entity references in the returned execution report.
	 * @return A {@link CompletableFuture} that will be completed with an execution report when all the works are complete.
	 */
	<R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory);

}
