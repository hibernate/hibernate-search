/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.interceptor;

/**
 * This interceptor is called upon indexing operations to optionally change the indexing behavior.
 * <p>
 * The interceptor is applied to a MassIndexer operation, but is ignored when using
 * the explicit indexing control API such {@code org.hibernate.search.FullTextSession.index(T)}
 * or {@code purge} and  {@code purgeAll}.
 * </p>
 * <p>
 * Implementations must be thread-safe and should have a no-arg constructor.
 * </p>
 * <p>
 * Typical use cases include the so called soft delete.
 * </p>
 * @hsearch.experimental {@link IndexingOverride} might be updated
 *
 * @author Emmanuel Bernard
 */
public interface EntityIndexingInterceptor<T> {

	/**
	 * Triggered when an entity instance T should be added to the index, either by an event listener or by the
	 * MassIndexer.
	 * This is not triggered by an explicit API call such as FullTextSession.index(T).
	 *
	 * @param entity
	 *            The entity instance
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance be added to the index as normal; return a
	 *         different value to override the behaviour.
	 */
	IndexingOverride onAdd(T entity);

	/**
	 * Triggered when an entity instance T should be updated in the index.
	 *
	 * @param entity
	 *            The entity instance
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance removed and re-added to the index as
	 *         normal; return a different value to override the behaviour.
	 */
	IndexingOverride onUpdate(T entity);

	/**
	 * Triggered when an entity instance T should be deleted from the index.
	 *
	 * @param entity
	 *            The entity instance
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance removed from the index as normal;
	 *         return a different value to override the behaviour.
	 */
	IndexingOverride onDelete(T entity);

	/**
	 * A CollectionUpdate event is fired on collections included in an indexed entity, for example when using
	 * {@link org.hibernate.search.annotations.IndexedEmbedded} This event is triggered on each indexed domain instance T contained in such a collection;
	 * this is generally similar to a {@link #onUpdate(Object)} event.
	 *
	 * @param entity The entity instance
	 *
	 * @return Return {@link IndexingOverride#APPLY_DEFAULT} to have the instance removed and re-added to the index as
	 *         normal; return a different value to override the behaviour.
	 */
	IndexingOverride onCollectionUpdate(T entity);

}
