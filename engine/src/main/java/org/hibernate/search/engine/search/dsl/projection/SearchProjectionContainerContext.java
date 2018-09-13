/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;

/**
 * A context allowing to create a projection.
 */
public interface SearchProjectionContainerContext {

	/**
	 * Project the match to a {@link DocumentReference}.
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	DocumentReferenceProjectionContext documentReference();

	/**
	 * Project to a reference to the match.
	 * <p>
	 * The actual type of the reference depends on the mapper used to create the query:
	 * a POJO mapper may return a class/identifier couple, for example.
	 * <p>
	 * As a general rule, a projection on {@link #reference()} will result in the same value
	 * which would have been returned by the query if not using projections
	 * (i.e. if {@link SearchQueryResultDefinitionContext#asReferences()} was called instead of
	 * {@link SearchQueryResultDefinitionContext#asProjections(org.hibernate.search.engine.search.SearchProjection...)}).
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	ReferenceProjectionContext reference();

	/**
	 * Project to an object representing the match.
	 * <p>
	 * The actual type of the object depends on the entry point
	 * for your query: an {@link org.hibernate.search.engine.backend.index.IndexManager}
	 * may return a Java representation of the document,
	 * but a {@link org.hibernate.search.engine.common.SearchManager} will
	 * return a Java representation of the mapped object.
	 * <p>
	 * As a general rule, a projection on {@link #object()} will result in the same value
	 * which would have been returned by the query if not using projections
	 * (i.e. if {@link SearchQueryResultDefinitionContext#asObjects()} was called instead of
	 * {@link SearchQueryResultDefinitionContext#asProjections(org.hibernate.search.engine.search.SearchProjection...)}).
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	ObjectProjectionContext object();

	/**
	 * Project to a field of the indexed document.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param type The resulting type of the projection.
	 * @param <T> The resulting type of the projection.
	 * @return A context allowing to define the projection more precisely.
	 */
	<T> FieldProjectionContext<T> field(String absoluteFieldPath, Class<T> type);

}
