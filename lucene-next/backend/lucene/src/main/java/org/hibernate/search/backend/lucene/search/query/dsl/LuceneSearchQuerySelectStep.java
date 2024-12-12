/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.dsl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;

public interface LuceneSearchQuerySelectStep<SR, R, E, LOS>
		extends SearchQuerySelectStep<
				SR,
				LuceneSearchQueryOptionsStep<SR, E, LOS>,
				R,
				E,
				LOS,
				LuceneSearchProjectionFactory<SR, R, E>,
				LuceneSearchPredicateFactory<SR>>,
		LuceneSearchQueryWhereStep<SR, E, LOS> {

	@Override
	LuceneSearchQueryWhereStep<SR, E, LOS> selectEntity();

	@Override
	LuceneSearchQueryWhereStep<SR, R, LOS> selectEntityReference();

	@Override
	<P> LuceneSearchQueryWhereStep<SR, P, LOS> select(Class<P> objectClass);

	@Override
	<P> LuceneSearchQueryWhereStep<SR, P, LOS> select(
			Function<? super LuceneSearchProjectionFactory<SR, R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> LuceneSearchQueryWhereStep<SR, P, LOS> select(SearchProjection<P> projection);

	@Override
	LuceneSearchQueryWhereStep<SR, List<?>, LOS> select(SearchProjection<?>... projections);

}
