/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;

/**
 * A predicate factory for fields encoded as a number.
 *
 * @param <F> The field type.
 * @param <E> The encoded number type.
 */
public final class LuceneNumericFieldPredicateBuilderFactory<F, E extends Number>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, AbstractLuceneNumericFieldCodec<F, E>> {

	public LuceneNumericFieldPredicateBuilderFactory(boolean searchable,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( searchable, codec );
	}

	@Override
	public MatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneNumericMatchPredicate.Builder<>( searchContext, field, codec );
	}

	@Override
	public LuceneNumericRangePredicate.Builder<F, E> createRangePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneNumericRangePredicate.Builder<>( searchContext, field, codec );
	}
}
