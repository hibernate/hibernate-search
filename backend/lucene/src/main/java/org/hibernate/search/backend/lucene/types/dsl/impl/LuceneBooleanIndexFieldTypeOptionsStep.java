/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationTypeKeys;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionTypeKeys;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericTermsAggregation;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;

class LuceneBooleanIndexFieldTypeOptionsStep
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<LuceneBooleanIndexFieldTypeOptionsStep, Boolean> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneBooleanIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Boolean.class );
	}

	@Override
	public LuceneBooleanIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return thisAsS();
	}

	@Override
	public LuceneIndexFieldType<Boolean> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );

		LuceneBooleanFieldCodec codec = new LuceneBooleanFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable,
				indexNullAsValue );
		builder.codec( codec );

		builder.predicateBuilderFactory(
				new LuceneNumericFieldPredicateBuilderFactory<>( resolvedSearchable, codec ) );
		builder.sortBuilderFactory(
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, codec ) );

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new LuceneFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new LuceneNumericTermsAggregation.Factory<>( codec ) );
		}

		return builder.build();
	}

	@Override
	protected LuceneBooleanIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

}
