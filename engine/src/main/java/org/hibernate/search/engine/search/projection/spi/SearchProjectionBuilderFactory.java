/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.projection.SearchProjection;

/**
 * A factory for search projection builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search projections.
 */
public interface SearchProjectionBuilderFactory {

	SearchProjection<DocumentReference> documentReference();

	<E> SearchProjection<E> entityLoading();

	<R> SearchProjection<R> entityReference();

	<I> SearchProjection<I> id(Class<I> identifierType);

	SearchProjection<Float> score();

	CompositeProjectionBuilder composite();

	<T> SearchProjection<T> constant(T value);

}
