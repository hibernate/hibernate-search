/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;

/**
 * A factory for search queries.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search queries.
 *
 * @param <C> The type of predicate collector
 */
public interface SearchQueryFactory<C> {

	<R, O> SearchQueryBuilder<O, C> asObjects(
			SessionContext sessionContext, Function<DocumentReference, R> documentReferenceTransformer,
			HitAggregator<LoadingHitCollector<R>, List<O>> hitAggregator);

	<R, T> SearchQueryBuilder<T, C> asReferences(
			SessionContext sessionContext, Function<DocumentReference, R> documentReferenceTransformer,
			HitAggregator<HitCollector<R>, List<T>> hitAggregator);

	<R, T> SearchQueryBuilder<T, C> asProjections(
			SessionContext sessionContext, Function<DocumentReference, R> documentReferenceTransformer,
			HitAggregator<ProjectionHitCollector<R>, List<T>> hitAggregator, String... projections);

}
