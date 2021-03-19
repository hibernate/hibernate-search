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
	 * Supply the {@link EntityReferenceInfo} for the provided arguments.
	 *
	 * @param entityName The name of the entity.
	 * @param serializedId The serialized entity identifier.
	 * @return The entity reference info.
	 */
	EntityReferenceInfo entityReference(String entityName, String serializedId);

	final class EntityReferenceInfo {
		private final Class<?> javaClass;
		private final String entityName;
		private final Object id;

		public EntityReferenceInfo(Class<?> javaClass, String entityName, Object id) {
			this.javaClass = javaClass;
			this.entityName = entityName;
			this.id = id;
		}

		public Class<?> javaClass() {
			return javaClass;
		}

		public String entityName() {
			return entityName;
		}

		public Object id() {
			return id;
		}
	}
}
