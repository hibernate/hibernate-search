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

/**
 * A set of indexing events to be sent to an external queue.
 * <p>
 * The external queue will be consumed by a background process which will then
 * perform the indexing operations asynchronously.
 */
public interface PojoIndexingQueueEventSendingPlan {

	/**
	 * Appends an "add" event to the plan, to be sent {@link #sendAndReport(EntityReferenceFactory) later}
	 * and ultimately added to a {@link PojoIndexingQueueEventProcessingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param identifier The non-serialized entity identifier, to report sending errors.
	 * @param serializedId The serialized entity identifier.
	 * @param routes The document routes.
	 * @see PojoIndexingQueueEventProcessingPlan#add(String, String, DocumentRoutesDescriptor)
	 */
	void add(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes);

	/**
	 * Appends an "add-or-update" event to the plan, to be sent {@link #sendAndReport(EntityReferenceFactory) later}
	 * and ultimately added to a {@link PojoIndexingQueueEventProcessingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param identifier The non-serialized entity identifier, to report sending errors.
	 * @param serializedId The serialized entity identifier.
	 * @param routes The document routes.
	 * @see PojoIndexingQueueEventProcessingPlan#addOrUpdate(String, String, DocumentRoutesDescriptor)
	 */
	void addOrUpdate(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes);

	/**
	 * Appends a "delete" event to the plan, to be sent {@link #sendAndReport(EntityReferenceFactory) later}
	 * and ultimately added to a {@link PojoIndexingQueueEventProcessingPlan}.
	 *
	 * @param entityName The name of the entity type.
	 * @param identifier The non-serialized entity identifier, to report sending errors.
	 * @param serializedId The serialized entity identifier.
	 * @param routes The document routes.
	 * @see PojoIndexingQueueEventProcessingPlan#delete(String, String, DocumentRoutesDescriptor)
	 */
	void delete(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes);

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
	 * @param <R> The type of entity references in the returned execution report.
	 * @param entityReferenceFactory A factory for entity references in the returned execution report.
	 * @return A {@link CompletableFuture} that will hold an execution report when all the events are sent.
	 * If sending an event failed, the future will be completed normally,
	 * but the report will contain an exception.
	 */
	<R> CompletableFuture<MultiEntityOperationExecutionReport<R>> sendAndReport(
			EntityReferenceFactory<R> entityReferenceFactory);

}
