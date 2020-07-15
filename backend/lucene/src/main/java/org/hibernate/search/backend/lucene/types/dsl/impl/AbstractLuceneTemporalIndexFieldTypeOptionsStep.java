/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneTemporalFieldSortBuilderFactory;
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

		builder.predicateBuilderFactory(
				new LuceneNumericFieldPredicateBuilderFactory<>( resolvedSearchable, codec ) );
		builder.sortBuilderFactory(
				new LuceneTemporalFieldSortBuilderFactory<>( resolvedSortable, codec ) );
		builder.projectionBuilderFactory(
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec ) );
		builder.aggregationBuilderFactory(
				new LuceneNumericFieldAggregationBuilderFactory<>( resolvedAggregable, codec ) );

		return builder.build();
	}

	protected abstract AbstractLuceneNumericFieldCodec<F, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			F indexNullAsValue);
}
