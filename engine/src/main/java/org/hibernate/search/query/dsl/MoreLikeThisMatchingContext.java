/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * Super interface offering the way to provide the matching content
 * as well as customize the preceding field.
 *
 * Sub interfaces are expected to clarify whether additional fields can be set.
 *
 * @hsearch.experimental More Like This queries are considered experimental
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface MoreLikeThisMatchingContext {

	/**
	 * Find other entities looking like the entity with the given id.
	 * Only the selected fields will be used for comparison.
	 * @param id the identifier of the entity
	 * @return {@code this} for method chaining
	 */
	MoreLikeThisTermination toEntityWithId(Object id);

	/*
	 * Find other entities looking like the entity provided.
	 * Only the selected fields will be used for comparison.
	 * If the provided entity is already indexed, the index data is used.
	 * Otherwise, we use the value of each property in the instance passed.
	 */
	//TODO genericize it
	MoreLikeThisToEntityContentAndTermination toEntity(Object entity);

}
