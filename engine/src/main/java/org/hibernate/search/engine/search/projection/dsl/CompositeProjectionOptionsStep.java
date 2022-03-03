/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

/**
 * The final step in a composite projection definition
 * where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <T> The type of composed projections.
 */
public interface CompositeProjectionOptionsStep<S extends CompositeProjectionOptionsStep<?, T>, T>
		extends ProjectionFinalStep<T> {

}
