/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.definition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A component able to define a projection using the Hibernate Search Projection DSL.
 * <p>
 * Used in particular for projections defined using mapper features, e.g. Java annotations
 * (see {@code org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection},
 * {@code org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection}, etc.).
 *
 * @see SearchProjection
 *
 * @param <P> The type of the element returned by the projection.
 */
@Incubating
public interface ProjectionDefinition<P> {

	/**
	 * Creates a projection with a specific projected type.
	 *
	 * @param factory A projection factory.
	 * If the projection is used in the context of an object field,
	 * this factory expects field paths to be provided relative to that same object field.
	 * This factory is only valid in the present context and must not be used after
	 * {@link ProjectionDefinition#create(SearchProjectionFactory, ProjectionDefinitionContext)} returns.
	 * @param context The context in which the definition is applied.
	 * @return The created {@link SearchPredicate}.
	 * @throws RuntimeException If the creation of the projection fails.
	 * @see SearchPredicateFactory
	 * @see ProjectionDefinitionContext
	 */
	SearchProjection<? extends P> create(SearchProjectionFactory<?, ?> factory, ProjectionDefinitionContext context);

}
