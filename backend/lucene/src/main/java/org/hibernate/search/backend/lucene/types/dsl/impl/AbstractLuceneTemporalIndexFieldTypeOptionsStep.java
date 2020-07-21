/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationTypeKeys;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionTypeKeys;
import org.hibernate.search.backend.lucene.search.sort.impl.SortTypeKeys;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericRangeAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericTermsAggregation;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneStandardFieldSort;
import org.hibernate.search.engine.backend.types.Sortable;

abstract class AbstractLuceneTemporalIndexFieldTypeOptionsStep<
				S extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<S, F>,
				F extends TemporalAccessor
		>
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<S, F> {

	private Sortable sortable = Sortable.DEFAULT;

	AbstractLuceneTemporalIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> fieldType) {
		super( buildContext, fieldType );
	}

	@Override
	public S sortable(Sortable sortable) {
		this.sortable = sortable;
		return thisAsS();
	}

	@Override
	public LuceneIndexFieldType<F> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );

		AbstractLuceneNumericFieldCodec<F, ?> codec = createCodec(
				resolvedProjectable,
				resolvedSearchable,
				resolvedSortable,
				resolvedAggregable,
				indexNullAsValue
		);
		builder.codec( codec );

		builder.predicateBuilderFactory(
				new LuceneNumericFieldPredicateBuilderFactory<>( resolvedSearchable, codec ) );

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.FIELD,
					new LuceneStandardFieldSort.TemporalFieldFactory<>( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new LuceneFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new LuceneNumericTermsAggregation.Factory<>( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.RANGE, new LuceneNumericRangeAggregation.Factory<>( codec ) );
		}

		return builder.build();
	}

	protected abstract AbstractLuceneNumericFieldCodec<F, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			F indexNullAsValue);
}
